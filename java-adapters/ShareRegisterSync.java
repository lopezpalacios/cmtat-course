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
 * ShareRegisterSync — Chapter 13 bank-side adapter.
 *
 * Consumes the {@code Transfer} and {@code DividendPaid} events of ShareToken
 * and mirrors them into the bank's share-register system of record. Demonstrates:
 *
 *   1. Declaring the web3j {@link Event} mirror of the Solidity event (indexed flags
 *      MUST match the contract or decoding silently mis-slices the data).
 *   2. {@link EthFilter} replay from a given block — the backfill path, equivalent to
 *      re-reading a payment-message archive after an outage.
 *   3. Decoding indexed fields (one 32-byte topic each) vs non-indexed fields
 *      (ABI-packed together in the data section).
 *   4. Reorg safety: only logs at depth >= CONFIRMATION_DEPTH are booked; everything
 *      shallower is provisional, like an unconfirmed SWIFT message.
 *
 * Solidity -> web3j type map used here:
 *   address -> Address  -> String    (0x-hex, 20 bytes)
 *   uint256 -> Uint256  -> BigInteger
 */
public final class ShareRegisterSync {

    /** Mirror of: event Transfer(address indexed from, address indexed to, uint256 value) */
    @SuppressWarnings("rawtypes")
    public static final Event TRANSFER = new Event(
            "Transfer",
            Arrays.<TypeReference<?>>asList(
                    new TypeReference<Address>(true) {},   // from       (indexed -> topic[1])
                    new TypeReference<Address>(true) {},   // to         (indexed -> topic[2])
                    new TypeReference<Uint256>(false) {}  // value      (data)
            ));

    /** Mirror of: event DividendPaid(address indexed recipient, uint256 amount) */
    @SuppressWarnings("rawtypes")
    public static final Event DIVIDEND_PAID = new Event(
            "DividendPaid",
            Arrays.<TypeReference<?>>asList(
                    new TypeReference<Address>(true) {},   // recipient  (indexed -> topic[1])
                    new TypeReference<Uint256>(false) {}  // amount     (data)
            ));

    /** topic0 = keccak256("Transfer(address,address,uint256)") */
    public static final String TRANSFER_TOPIC = EventEncoder.encode(TRANSFER);

    /** topic0 = keccak256("DividendPaid(address,uint256)") */
    public static final String DIVIDEND_PAID_TOPIC = EventEncoder.encode(DIVIDEND_PAID);

    /** Blocks below the chain head we treat as final. Tune per chain (see Chapter 08). */
    public static final BigInteger CONFIRMATION_DEPTH = BigInteger.valueOf(12);

    /** eth_getLogs range per request — keeps RPC responses bounded during backfill. */
    private static final BigInteger CHUNK_SIZE = BigInteger.valueOf(5_000);

    private final Web3j web3j;
    private final String contractAddress;

    public ShareRegisterSync(String rpcUrl, String contractAddress) {
        this.web3j = Web3j.build(new HttpService(rpcUrl));
        this.contractAddress = contractAddress;
    }

    /** Immutable share transfer entry decoded from one log — the unit handed to core banking. */
    public static final class TransferEntry {
        public final String from;          // 0x-hex address
        public final String to;            // 0x-hex address
        public final BigInteger value;     // number of shares transferred
        public final BigInteger blockNumber; // for finality + audit
        public final String txHash;        // audit-trail anchor
        public final BigInteger logIndex;    // txHash + logIndex = unique log identity

        TransferEntry(String from, String to, BigInteger value,
                     BigInteger blockNumber, String txHash, BigInteger logIndex) {
            this.from = from;
            this.to = to;
            this.value = value;
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
            return String.format("TransferEntry[from=%s to=%s value=%s block=%s key=%s]",
                    from, to, value, blockNumber, idempotencyKey());
        }
    }

    /** Immutable dividend payment entry decoded from one log — the unit handed to core banking. */
    public static final class DividendEntry {
        public final String recipient;     // 0x-hex address
        public final BigInteger amount;      // amount of dividend paid
        public final BigInteger blockNumber; // for finality + audit
        public final String txHash;        // audit-trail anchor
        public final BigInteger logIndex;    // txHash + logIndex = unique log identity

        DividendEntry(String recipient, BigInteger amount,
                     BigInteger blockNumber, String txHash, BigInteger logIndex) {
            this.recipient = recipient;
            this.amount = amount;
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
            return String.format("DividendEntry[recipient=%s amount=%s block=%s key=%s]",
                    recipient, amount, blockNumber, idempotencyKey());
        }
    }

    /**
     * Replay Transfer and DividendPaid logs from {@code fromBlock} up to the reorg-safe head,
     * in bounded chunks. This is the backfill path: run it on adapter start-up with
     * the last synced block from the bank's own store, and you recover every entry
     * missed during downtime — events are replayable forever from any archive node.
     */
    public List<Object> replayFrom(BigInteger fromBlock) throws IOException {
        BigInteger safeHead = safeHead();
        List<Object> entries = new ArrayList<>();
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
    private List<Object> fetchRange(BigInteger fromBlock, BigInteger toBlock) throws IOException {
        EthFilter transferFilter = new EthFilter(
                DefaultBlockParameter.valueOf(fromBlock),
                DefaultBlockParameter.valueOf(toBlock),
                contractAddress);
        transferFilter.addSingleTopic(TRANSFER_TOPIC);

        EthLog transferResponse = web3j.ethGetLogs(transferFilter).send();
        if (transferResponse.hasError()) {
            throw new IOException("eth_getLogs failed: " + transferResponse.getError().getMessage());
        }

        List<Object> entries = new ArrayList<>();
        for (EthLog.LogResult<?> result : transferResponse.getLogs()) {
            Log log = (Log) result.get();
            if (Boolean.TRUE.equals(log.isRemoved())) {
                continue; // reorged-away log delivered by a streaming filter — never book it
            }
            entries.add(decodeTransfer(log));
        }

        EthFilter dividendFilter = new EthFilter(
                DefaultBlockParameter.valueOf(fromBlock),
                DefaultBlockParameter.valueOf(toBlock),
                contractAddress);
        dividendFilter.addSingleTopic(DIVIDEND_PAID_TOPIC);

        EthLog dividendResponse = web3j.ethGetLogs(dividendFilter).send();
        if (dividendResponse.hasError()) {
            throw new IOException("eth_getLogs failed: " + dividendResponse.getError().getMessage());
        }

        for (EthLog.LogResult<?> result : dividendResponse.getLogs()) {
            Log log = (Log) result.get();
            if (Boolean.TRUE.equals(log.isRemoved())) {
                continue; // reorged-away log delivered by a streaming filter — never book it
            }
            entries.add(decodeDividend(log));
        }

        return entries;
    }

    /**
     * Decode one raw Transfer log into a TransferEntry.
     * topics[0] = event signature, topics[1..] = indexed params (one 32-byte word each),
     * data = ABI encoding of all non-indexed params concatenated.
     */
    public TransferEntry decodeTransfer(Log log) {
        // --- indexed params: each one is its own topic ---
        Address fromRaw = (Address) FunctionReturnDecoder.decodeIndexedValue(
                log.getTopics().get(1), new TypeReference<Address>() {});
        Address toRaw = (Address) FunctionReturnDecoder.decodeIndexedValue(
                log.getTopics().get(2), new TypeReference<Address>() {});

        // --- non-indexed params: decoded together from the data section, in order ---
        @SuppressWarnings("rawtypes")
        List<Type> data = FunctionReturnDecoder.decode(
                log.getData(), TRANSFER.getNonIndexedParameters());
        BigInteger value = ((Uint256) data.get(0)).getValue();

        return new TransferEntry(
                fromRaw.getValue(),
                toRaw.getValue(),
                value,
                log.getBlockNumber(),
                log.getTransactionHash(),
                log.getLogIndex());
    }

    /**
     * Decode one raw DividendPaid log into a DividendEntry.
     * topics[0] = event signature, topics[1..] = indexed params (one 32-byte word each),
     * data = ABI encoding of all non-indexed params concatenated.
     */
    public DividendEntry decodeDividend(Log log) {
        // --- indexed params: each one is its own topic ---
        Address recipientRaw = (Address) FunctionReturnDecoder.decodeIndexedValue(
                log.getTopics().get(1), new TypeReference<Address>() {});

        // --- non-indexed params: decoded together from the data section, in order ---
        @SuppressWarnings("rawtypes")
        List<Type> data = FunctionReturnDecoder.decode(
                log.getData(), DIVIDEND_PAID.getNonIndexedParameters());
        BigInteger amount = ((Uint256) data.get(0)).getValue();

        return new DividendEntry(
                recipientRaw.getValue(),
                amount,
                log.getBlockNumber(),
                log.getTransactionHash(),
                log.getLogIndex());
    }

    /**
     * Demo main: backfill from a deployment block and print the feed.
     * Usage: ShareRegisterSync <rpcUrl> <contractAddress> <fromBlock>
     */
    public static void main(String[] args) throws IOException {
        ShareRegisterSync syncer = new ShareRegisterSync(args[0], args[1]);
        BigInteger fromBlock = new BigInteger(args[2]);
        for (Object entry : syncer.replayFrom(fromBlock)) {
            // In production: idempotent upsert into the bank's share register keyed
            // on entry.idempotencyKey(); see Chapter 08 ReconciliationJob.
            System.out.println(entry);
        }
    }
}
