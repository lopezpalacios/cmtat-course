package ch.bank.cmtat.adapters;

import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.http.HttpService;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * EventLogParser — Chapter 03 bank-side adapter.
 *
 * Consumes the {@code BookingRecorded} event feed of EventDrivenLedger and turns it
 * into core-banking booking entries. Demonstrates:
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
 *   bytes32 -> Bytes32  -> byte[32]  (right-padded ASCII for booking refs)
 *   address -> Address  -> String    (0x-hex, 20 bytes)
 *   uint256 -> Uint256  -> BigInteger
 *   uint8   -> Uint8    -> BigInteger (narrow to int after range check)
 */
public final class EventLogParser {

    /** Mirror of: event BookingRecorded(bytes32 indexed, address indexed, uint256, uint8) */
    @SuppressWarnings("rawtypes")
    public static final Event BOOKING_RECORDED = new Event(
            "BookingRecorded",
            Arrays.<TypeReference<?>>asList(
                    new TypeReference<Bytes32>(true) {},   // externalRef  (indexed -> topic[1])
                    new TypeReference<Address>(true) {},   // account      (indexed -> topic[2])
                    new TypeReference<Uint256>(false) {},  // amount       (data)
                    new TypeReference<Uint8>(false) {}     // entryType    (data)
            ));

    /** topic0 = keccak256("BookingRecorded(bytes32,address,uint256,uint8)") */
    public static final String BOOKING_RECORDED_TOPIC = EventEncoder.encode(BOOKING_RECORDED);

    /** Blocks below the chain head we treat as final. Tune per chain (see Chapter 08). */
    public static final BigInteger CONFIRMATION_DEPTH = BigInteger.valueOf(12);

    /** eth_getLogs range per request — keeps RPC responses bounded during backfill. */
    private static final BigInteger CHUNK_SIZE = BigInteger.valueOf(5_000);

    private final Web3j web3j;
    private final String contractAddress;

    public EventLogParser(String rpcUrl, String contractAddress) {
        this.web3j = Web3j.build(new HttpService(rpcUrl));
        this.contractAddress = contractAddress;
    }

    /** Immutable booking entry decoded from one log — the unit handed to core banking. */
    public static final class BookingEntry {
        public final String externalRef;     // ASCII-decoded bytes32, trailing zeros stripped
        public final String account;         // 0x-hex address
        public final BigInteger amount;      // smallest units — convert at the display edge only
        public final int entryType;          // 1 = DEBIT, 2 = CREDIT
        public final BigInteger blockNumber; // for finality + audit
        public final String txHash;          // audit-trail anchor
        public final BigInteger logIndex;    // txHash + logIndex = unique log identity

        BookingEntry(String externalRef, String account, BigInteger amount, int entryType,
                     BigInteger blockNumber, String txHash, BigInteger logIndex) {
            this.externalRef = externalRef;
            this.account = account;
            this.amount = amount;
            this.entryType = entryType;
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
            return String.format("BookingEntry[ref=%s account=%s amount=%s type=%d block=%s key=%s]",
                    externalRef, account, amount, entryType, blockNumber, idempotencyKey());
        }
    }

    /**
     * Replay BookingRecorded logs from {@code fromBlock} up to the reorg-safe head,
     * in bounded chunks. This is the backfill path: run it on adapter start-up with
     * the last booked block from the bank's own store, and you recover every entry
     * missed during downtime — events are replayable forever from any archive node.
     */
    public List<BookingEntry> replayFrom(BigInteger fromBlock) throws IOException {
        BigInteger safeHead = safeHead();
        List<BookingEntry> entries = new ArrayList<>();
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
    private List<BookingEntry> fetchRange(BigInteger fromBlock, BigInteger toBlock) throws IOException {
        EthFilter filter = new EthFilter(
                DefaultBlockParameter.valueOf(fromBlock),
                DefaultBlockParameter.valueOf(toBlock),
                contractAddress);
        // topic0 selects the event type; the node does the filtering, not us
        filter.addSingleTopic(BOOKING_RECORDED_TOPIC);

        EthLog response = web3j.ethGetLogs(filter).send();
        if (response.hasError()) {
            throw new IOException("eth_getLogs failed: " + response.getError().getMessage());
        }

        List<BookingEntry> out = new ArrayList<>();
        for (EthLog.LogResult<?> result : response.getLogs()) {
            Log log = (Log) result.get();
            if (Boolean.TRUE.equals(log.isRemoved())) {
                continue; // reorged-away log delivered by a streaming filter — never book it
            }
            out.add(decode(log));
        }
        return out;
    }

    /**
     * Decode one raw log into a BookingEntry.
     * topics[0] = event signature, topics[1..] = indexed params (one 32-byte word each),
     * data = ABI encoding of all non-indexed params concatenated.
     */
    public BookingEntry decode(Log log) {
        // --- indexed params: each one is its own topic ---
        Bytes32 refRaw = (Bytes32) FunctionReturnDecoder.decodeIndexedValue(
                log.getTopics().get(1), new TypeReference<Bytes32>() {});
        Address accountRaw = (Address) FunctionReturnDecoder.decodeIndexedValue(
                log.getTopics().get(2), new TypeReference<Address>() {});

        // --- non-indexed params: decoded together from the data section, in order ---
        @SuppressWarnings("rawtypes")
        List<Type> data = FunctionReturnDecoder.decode(
                log.getData(), BOOKING_RECORDED.getNonIndexedParameters());
        BigInteger amount = ((Uint256) data.get(0)).getValue();
        int entryType = ((Uint8) data.get(1)).getValue().intValueExact();

        return new BookingEntry(
                bytes32ToAscii(refRaw.getValue()),
                accountRaw.getValue(),
                amount,
                entryType,
                log.getBlockNumber(),
                log.getTransactionHash(),
                log.getLogIndex());
    }

    /** Right-padded ASCII bytes32 (booking ref / ISIN style) -> trimmed Java String. */
    public static String bytes32ToAscii(byte[] value) {
        int len = value.length;
        while (len > 0 && value[len - 1] == 0) {
            len--;
        }
        return new String(value, 0, len, StandardCharsets.US_ASCII);
    }

    /**
     * Demo main: backfill from a deployment block and print the feed.
     * Usage: EventLogParser <rpcUrl> <contractAddress> <fromBlock>
     */
    public static void main(String[] args) throws IOException {
        EventLogParser parser = new EventLogParser(args[0], args[1]);
        BigInteger fromBlock = new BigInteger(args[2]);
        for (BookingEntry entry : parser.replayFrom(fromBlock)) {
            // In production: idempotent upsert into the bank's booking store keyed
            // on entry.idempotencyKey(); see Chapter 08 ReconciliationJob.
            System.out.println(entry);
        }
    }
}
