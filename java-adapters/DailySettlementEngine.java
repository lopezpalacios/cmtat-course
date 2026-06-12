package ch.bank.cmtat.adapters;

import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.http.HttpService;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * DailySettlementEngine — Chapter 17 bank-side adapter.
 *
 * Consumes the {@code OrderProcessed} event feed of FundShareToken and processes
 * subscription/redemption orders, batch-settles at struck NAV, and reconciles end of day.
 * Demonstrates:
 *
 *   1. Declaring the web3j {@link Event} mirror of the Solidity event (indexed flags
 *      MUST match the contract or decoding silently mis-slices the data).
 *   2. {@link EthFilter} replay from a given block — the backfill path, equivalent to
 *      re-reading a payment-message archive after an outage.
 *   3. Decoding indexed fields (one 32-byte topic each) vs non-indexed fields
 *      (ABI-packed together in the data section).
 *   4. Reorg safety: only logs at depth >= CONFIRMATION_DEPTH are processed; everything
 *      shallower is provisional, like an unconfirmed SWIFT message.
 *
 * Solidity -> web3j type map used here:
 *   address -> Address  -> String    (0x-hex, 20 bytes)
 *   uint256 -> Uint256  -> BigInteger
 */
public final class DailySettlementEngine {

    /** Mirror of: event OrderProcessed(uint256 indexed orderId, address indexed user, uint256 amount, bool isRedemption) */
    @SuppressWarnings("rawtypes")
    public static final Event ORDER_PROCESSED = new Event(
            "OrderProcessed",
            Arrays.<TypeReference<?>>asList(
                    new TypeReference<Uint256>(true) {},   // orderId      (indexed -> topic[1])
                    new TypeReference<Address>(true) {},   // user         (indexed -> topic[2])
                    new TypeReference<Uint256>(false) {},  // amount       (data)
                    new TypeReference<TypeReference.Boolean>() {}     // isRedemption (data)
            ));

    /** topic0 = keccak256("OrderProcessed(uint256,address,uint256,bool)") */
    public static final String ORDER_PROCESSED_TOPIC = EventEncoder.encode(ORDER_PROCESSED);

    /** Blocks below the chain head we treat as final. Tune per chain (see Chapter 08). */
    public static final BigInteger CONFIRMATION_DEPTH = BigInteger.valueOf(12);

    /** eth_getLogs range per request — keeps RPC responses bounded during backfill. */
    private static final BigInteger CHUNK_SIZE = BigInteger.valueOf(5_000);

    private final Web3j web3j;
    private final String contractAddress;

    public DailySettlementEngine(String rpcUrl, String contractAddress) {
        this.web3j = Web3j.build(new HttpService(rpcUrl));
        this.contractAddress = contractAddress;
    }

    /** Immutable order entry decoded from one log — the unit handed to core banking. */
    public static final class OrderEntry {
        public final BigInteger orderId;      // unique identifier for the order
        public final String user;             // 0x-hex address of the user
        public final BigInteger amount;       // smallest units — convert at the display edge only
        public final boolean isRedemption;   // true if redemption, false if subscription
        public final BigInteger blockNumber;  // for finality + audit
        public final String txHash;           // audit-trail anchor
        public final BigInteger logIndex;     // txHash + logIndex = unique log identity

        OrderEntry(BigInteger orderId, String user, BigInteger amount, boolean isRedemption,
                   BigInteger blockNumber, String txHash, BigInteger logIndex) {
            this.orderId = orderId;
            this.user = user;
            this.amount = amount;
            this.isRedemption = isRedemption;
            this.blockNumber = blockNumber;
            this.txHash = txHash;
            this.logIndex = logIndex;
        }

        /** Idempotency key for the bank-side store: dedup on replay/restart. */
        public String idempotencyKey() {
            return txHash + ":" + logIndex;
        }

        @Override
        public String toString() {
            return String.format("OrderEntry[id=%s user=%s amount=%s isRedemption=%b block=%s key=%s]",
                    orderId, user, amount, isRedemption, blockNumber, idempotencyKey());
        }
    }

    /**
     * Replay OrderProcessed logs from {@code fromBlock} up to the reorg-safe head,
     * in bounded chunks. This is the backfill path: run it on adapter start-up with
     * the last processed block from the bank's own store, and you recover every entry
     * missed during downtime — events are replayable forever from any archive node.
     */
    public List<OrderEntry> replayFrom(BigInteger fromBlock) throws IOException {
        BigInteger safeHead = safeHead();
        List<OrderEntry> entries = new ArrayList<>();
        BigInteger start = fromBlock;
        while (start.compareTo(safeHead) <= 0) {
            BigInteger end = start.add(CHUNK_SIZE).min(safeHead);
            entries.addAll(fetchRange(start, end));
            start = end.add(BigInteger.ONE);
        }
        return entries;
    }

    /** Chain head minus CONFIRMATION_DEPTH — logs above this line may still reorg away. */
    public BigInteger safeHead() throws IOException {
        BigInteger head = web3j.ethBlockNumber().send().getBlockNumber();
        BigInteger safe = head.subtract(CONFIRMATION_DEPTH);
        return safe.signum() < 0 ? BigInteger.ZERO : safe;
    }

    /** One eth_getLogs call: filter = address + topic0, range [fromBlock, toBlock]. */
    private List<OrderEntry> fetchRange(BigInteger fromBlock, BigInteger toBlock) throws IOException {
        EthFilter filter = new EthFilter(
                DefaultBlockParameter.valueOf(fromBlock),
                DefaultBlockParameter.valueOf(toBlock),
                contractAddress);
        // topic0 selects the event type; the node does the filtering, not us
        filter.addSingleTopic(ORDER_PROCESSED_TOPIC);

        EthLog response = web3j.ethGetLogs(filter).send();
        if (response.hasError()) {
            throw new IOException("eth_getLogs failed: " + response.getError().getMessage());
        }

        List<OrderEntry> out = new ArrayList<>();
        for (EthLog.LogResult<?> result : response.getLogs()) {
            Log log = (Log) result.get();
            if (Boolean.TRUE.equals(log.isRemoved())) {
                continue; // reorged-away log delivered by a streaming filter — never process it
            }
            out.add(decode(log));
        }
        return out;
    }

    /**
     * Decode one raw log into an OrderEntry.
     * topics[0] = event signature, topics[1..] = indexed params (one 32-byte word each),
     * data = ABI encoding of all non-indexed params concatenated.
     */
    public OrderEntry decode(Log log) {
        // --- indexed params: each one is its own topic ---
        Uint256 orderIdRaw = (Uint256) FunctionReturnDecoder.decodeIndexedValue(
                log.getTopics().get(1), new TypeReference<Uint256>() {});
        Address userRaw = (Address) FunctionReturnDecoder.decodeIndexedValue(
                log.getTopics().get(2), new TypeReference<Address>() {});

        // --- non-indexed params: decoded together from the data section, in order ---
        @SuppressWarnings("rawtypes")
        List<Type> data = FunctionReturnDecoder.decode(
                log.getData(), ORDER_PROCESSED.getNonIndexedParameters());
        BigInteger amount = ((Uint256) data.get(0)).getValue();
        boolean isRedemption = (Boolean) data.get(1).getValue();

        return new OrderEntry(
                orderIdRaw.getValue(),
                userRaw.getValue(),
                amount,
                isRedemption,
                log.getBlockNumber(),
                log.getTransactionHash(),
                log.getLogIndex());
    }

    /**
     * Demo main: backfill from a deployment block and print the feed.
     * Usage: DailySettlementEngine <rpcUrl> <contractAddress> <fromBlock>
     */
    public static void main(String[] args) throws IOException {
        DailySettlementEngine engine = new DailySettlementEngine(args[0], args[1]);
        BigInteger fromBlock = new BigInteger(args[2]);
        for (OrderEntry entry : engine.replayFrom(fromBlock)) {
            // In production: process the order in the bank's settlement system keyed
            // on entry.idempotencyKey(); see Chapter 08 ReconciliationJob.
            System.out.println(entry);
        }
    }
}
