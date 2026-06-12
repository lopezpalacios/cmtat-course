package ch.bank.cmtat.adapters;

import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.http.HttpService;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * CorporateActionProcessor — Chapter 15 bank-side adapter.
 *
 * Processes corporate actions such as stock split, rights-issue mint, buyback burn,
 * and squeeze-out forced transfer. Demonstrates:
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
public final class CorporateActionProcessor {

    /** Mirror of: event SnapshotTaken(uint256 timestamp) */
    @SuppressWarnings("rawtypes")
    public static final Event SNAPSHOT_TAKEN = new Event(
            "SnapshotTaken",
            Arrays.<TypeReference<?>>asList(
                    new TypeReference<Uint256>(true) {}   // timestamp  (indexed -> topic[1])
            ));

    /** Mirror of: event DividendPaid(address indexed recipient, uint256 amount) */
    @SuppressWarnings("rawtypes")
    public static final Event DIVIDEND_PAID = new Event(
            "DividendPaid",
            Arrays.<TypeReference<?>>asList(
                    new TypeReference<Address>(true) {},   // recipient  (indexed -> topic[1])
                    new TypeReference<Uint256>(false) {}  // amount     (data)
            ));

    /** Mirror of: event SharesBurned(address indexed account, uint256 amount) */
    @SuppressWarnings("rawtypes")
    public static final Event SHARES_BURNED = new Event(
            "SharesBurned",
            Arrays.<TypeReference<?>>asList(
                    new TypeReference<Address>(true) {},   // account  (indexed -> topic[1])
                    new TypeReference<Uint256>(false) {}  // amount   (data)
            ));

    /** topic0 = keccak256("SnapshotTaken(uint256)") */
    public static final String SNAPSHOT_TAKEN_TOPIC = EventEncoder.encode(SNAPSHOT_TAKEN);

    /** topic0 = keccak256("DividendPaid(address,uint256)") */
    public static final String DIVIDEND_PAID_TOPIC = EventEncoder.encode(DIVIDEND_PAID);

    /** topic0 = keccak256("SharesBurned(address,uint256)") */
    public static final String SHARES_BURNED_TOPIC = EventEncoder.encode(SHARES_BURNED);

    /** Blocks below the chain head we treat as final. Tune per chain (see Chapter 08). */
    public static final BigInteger CONFIRMATION_DEPTH = BigInteger.valueOf(12);

    /** eth_getLogs range per request — keeps RPC responses bounded during backfill. */
    private static final BigInteger CHUNK_SIZE = BigInteger.valueOf(5_000);

    private final Web3j web3j;
    private final String contractAddress;

    public CorporateActionProcessor(String rpcUrl, String contractAddress) {
        this.web3j = Web3j.build(new HttpService(rpcUrl));
        this.contractAddress = contractAddress;
    }

    /**
     * Replay SnapshotTaken, DividendPaid, and SharesBurned logs from {@code fromBlock}
     * up to the reorg-safe head, in bounded chunks. This is the backfill path: run it
     * on adapter start-up with the last processed block from the bank's own store,
     * and you recover every entry missed during downtime — events are replayable forever
     * from any archive node.
     */
    public List<CorporateAction> replayFrom(BigInteger fromBlock) throws IOException {
        BigInteger safeHead = safeHead();
        List<CorporateAction> actions = new ArrayList<>();
        BigInteger start = fromBlock;
        while (start.compareTo(safeHead) <= 0) {
            BigInteger end = start.add(CHUNK_SIZE).min(safeHead);
            actions.addAll(fetchRange(start, end));
            start = end.add(BigInteger.ONE);
        }
        return actions;
    }

    /** Chain head minus CONFIRMATION_DEPTH — logs above this line may still reorg away. */
    public BigInteger safeHead() throws IOException {
        BigInteger head = web3j.ethBlockNumber().send().getBlockNumber();
        BigInteger safe = head.subtract(CONFIRMATION_DEPTH);
        return safe.signum() < 0 ? BigInteger.ZERO : safe;
    }

    /** One eth_getLogs call: filter = address + topic0, range [fromBlock, toBlock]. */
    private List<CorporateAction> fetchRange(BigInteger fromBlock, BigInteger toBlock) throws IOException {
        EthFilter filter = new EthFilter(
                DefaultBlockParameter.valueOf(fromBlock),
                DefaultBlockParameter.valueOf(toBlock),
                contractAddress);
        // topic0 selects the event type; the node does the filtering, not us
        filter.addSingleTopic(SNAPSHOT_TAKEN_TOPIC);
        filter.addSingleTopic(DIVIDEND_PAID_TOPIC);
        filter.addSingleTopic(SHARES_BURNED_TOPIC);

        EthLog response = web3j.ethGetLogs(filter).send();
        if (response.hasError()) {
            throw new IOException("eth_getLogs failed: " + response.getError().getMessage());
        }

        List<CorporateAction> out = new ArrayList<>();
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
     * Decode one raw log into a CorporateAction.
     * topics[0] = event signature, topics[1..] = indexed params (one 32-byte word each),
     * data = ABI encoding of all non-indexed params concatenated.
     */
    public CorporateAction decode(Log log) {
        String topic0 = log.getTopics().get(0);
        if (topic0.equals(SNAPSHOT_TAKEN_TOPIC)) {
            return decodeSnapshotTaken(log);
        } else if (topic0.equals(DIVIDEND_PAID_TOPIC)) {
            return decodeDividendPaid(log);
        } else if (topic0.equals(SHARES_BURNED_TOPIC)) {
            return decodeSharesBurned(log);
        }
        throw new IllegalArgumentException("Unknown event topic: " + topic0);
    }

    /** Decode SnapshotTaken event. */
    private CorporateAction decodeSnapshotTaken(Log log) {
        Uint256 timestampRaw = (Uint256) FunctionReturnDecoder.decodeIndexedValue(
                log.getTopics().get(1), new TypeReference<Uint256>() {});

        return new CorporateAction(
                "SNAPSHOT_TAKEN",
                null,
                BigInteger.ZERO,
                timestampRaw.getValue(),
                log.getBlockNumber(),
                log.getTransactionHash(),
                log.getLogIndex());
    }

    /** Decode DividendPaid event. */
    private CorporateAction decodeDividendPaid(Log log) {
        Address recipientRaw = (Address) FunctionReturnDecoder.decodeIndexedValue(
                log.getTopics().get(1), new TypeReference<Address>() {});
        Uint256 amountRaw = (Uint256) FunctionReturnDecoder.decode(
                log.getData(), DIVIDEND_PAID.getNonIndexedParameters()).get(0);

        return new CorporateAction(
                "DIVIDEND_PAID",
                recipientRaw.getValue(),
                amountRaw.getValue(),
                BigInteger.ZERO,
                log.getBlockNumber(),
                log.getTransactionHash(),
                log.getLogIndex());
    }

    /** Decode SharesBurned event. */
    private CorporateAction decodeSharesBurned(Log log) {
        Address accountRaw = (Address) FunctionReturnDecoder.decodeIndexedValue(
                log.getTopics().get(1), new TypeReference<Address>() {});
        Uint256 amountRaw = (Uint256) FunctionReturnDecoder.decode(
                log.getData(), SHARES_BURNED.getNonIndexedParameters()).get(0);

        return new CorporateAction(
                "SHARES_BURNED",
                accountRaw.getValue(),
                amountRaw.getValue(),
                BigInteger.ZERO,
                log.getBlockNumber(),
                log.getTransactionHash(),
                log.getLogIndex());
    }

    /**
     * Immutable corporate action decoded from one log — the unit handed to core banking.
     */
    public static final class CorporateAction {
        public final String eventType;       // Type of event: SNAPSHOT_TAKEN, DIVIDEND_PAID, SHARES_BURNED
        public final String account;         // 0x-hex address (for DividendPaid and SharesBurned)
        public final BigInteger amount;      // Amount involved in the action
        public final BigInteger timestamp;   // Timestamp of the snapshot taken
        public final BigInteger blockNumber; // for finality + audit
        public final String txHash;          // audit-trail anchor
        public final BigInteger logIndex;    // txHash + logIndex = unique log identity

        CorporateAction(String eventType, String account, BigInteger amount, BigInteger timestamp,
                        BigInteger blockNumber, String txHash, BigInteger logIndex) {
            this.eventType = eventType;
            this.account = account;
            this.amount = amount;
            this.timestamp = timestamp;
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
            return String.format("CorporateAction[type=%s account=%s amount=%s timestamp=%s block=%s key=%s]",
                    eventType, account, amount, timestamp, blockNumber, idempotencyKey());
        }
    }

    /**
     * Demo main: backfill from a deployment block and print the feed.
     * Usage: CorporateActionProcessor <rpcUrl> <contractAddress> <fromBlock>
     */
    public static void main(String[] args) throws IOException {
        CorporateActionProcessor processor = new CorporateActionProcessor(args[0], args[1]);
        BigInteger fromBlock = new BigInteger(args[2]);
        for (CorporateAction action : processor.replayFrom(fromBlock)) {
            // In production: idempotent upsert into the bank's corporate action store keyed
            // on action.idempotencyKey(); see Chapter 08 ReconciliationJob.
            System.out.println(action);
        }
    }
}
