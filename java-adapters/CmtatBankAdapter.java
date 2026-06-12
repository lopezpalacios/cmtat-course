package ch.bank.cmtat.adapter;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;

/**
 * CmtatBankAdapter — the single facade the core-banking estate talks to.
 *
 * Responsibilities:
 *   - connect:  one Web3j client per JSON-RPC endpoint (like one MQ connection factory)
 *   - read:     view calls (balanceOf, paused, frozen) decoded into Java types
 *   - listen:   pull Transfer / TransferRef / Paused / Frozen logs from a block range
 *   - submit:   delegate writes to IdempotentTxSender (nonce queue + dedup)
 *
 * Targets the ComplianceToken built in Chapter 07.
 * Assumption: ComplianceToken exposes transferWithRef(address,uint256,bytes32) and emits
 * TransferRef(bytes32 indexed bankRef, address indexed from, address indexed to, uint256 value),
 * per the course convention "events carry idempotency-friendly identifiers".
 */
public class CmtatBankAdapter {

    // ---- Event definitions (the integration contract, Chapter 03) ----

    /** ERC-20 Transfer(address indexed from, address indexed to, uint256 value) */
    public static final Event TRANSFER_EVENT = new Event("Transfer",
            Arrays.asList(
                    new TypeReference<Address>(true) {},   // indexed -> topic[1]
                    new TypeReference<Address>(true) {},   // indexed -> topic[2]
                    new TypeReference<Uint256>(false) {})); // not indexed -> data

    /** TransferRef(bytes32 indexed bankRef, address indexed from, address indexed to, uint256 value) */
    public static final Event TRANSFER_REF_EVENT = new Event("TransferRef",
            Arrays.asList(
                    new TypeReference<Bytes32>(true) {},
                    new TypeReference<Address>(true) {},
                    new TypeReference<Address>(true) {},
                    new TypeReference<Uint256>(false) {}));

    /** PauseModule: Paused(address account) — modeled on OZ Pausable */
    public static final Event PAUSED_EVENT = new Event("Paused",
            Collections.singletonList(new TypeReference<Address>(false) {}));

    /** EnforcementModule: Freeze(address indexed enforcer, address indexed owner)
     *  Assumption: Chapter 07 ComplianceToken declares this two-address form. */
    public static final Event FREEZE_EVENT = new Event("Freeze",
            Arrays.asList(
                    new TypeReference<Address>(true) {},
                    new TypeReference<Address>(true) {}));

    private final Web3j web3;
    private final String contractAddress;
    private final String callerAddress;       // from-address used for eth_call context
    private final IdempotentTxSender txSender; // all writes go through the queue

    public CmtatBankAdapter(String rpcUrl,
                            String contractAddress,
                            Credentials credentials,
                            long chainId) throws IOException {
        this.web3 = Web3j.build(new HttpService(rpcUrl));
        this.contractAddress = contractAddress;
        this.callerAddress = credentials.getAddress();
        this.txSender = new IdempotentTxSender(web3, credentials, chainId, contractAddress);
        // Fail fast at startup, like pinging the core-banking DB on boot.
        String clientVersion = web3.web3ClientVersion().send().getWeb3ClientVersion();
        System.out.println("[adapter] connected to node: " + clientVersion);
    }

    // =====================================================================
    // READ SIDE — view calls (eth_call): free, instant, no state change
    // =====================================================================

    /** balanceOf(holder) -> position in smallest units (Uint256 -> BigInteger). */
    public BigInteger balanceOf(String holder) throws IOException {
        Function fn = new Function("balanceOf",
                Collections.singletonList(new Address(holder)),
                Collections.singletonList(new TypeReference<Uint256>() {}));
        List<Type> out = ethCall(fn);
        return ((Uint256) out.get(0)).getValue();
    }

    /** PauseModule: paused() -> Bool -> Boolean. Market-wide halt flag. */
    public boolean isPaused() throws IOException {
        Function fn = new Function("paused",
                Collections.emptyList(),
                Collections.singletonList(new TypeReference<Bool>() {}));
        List<Type> out = ethCall(fn);
        return ((Bool) out.get(0)).getValue();
    }

    /** EnforcementModule: frozen(holder) -> Bool. Per-account freeze (sanctions/court order). */
    public boolean isFrozen(String holder) throws IOException {
        Function fn = new Function("frozen",
                Collections.singletonList(new Address(holder)),
                Collections.singletonList(new TypeReference<Bool>() {}));
        List<Type> out = ethCall(fn);
        return ((Bool) out.get(0)).getValue();
    }

    private List<Type> ethCall(Function fn) throws IOException {
        String data = FunctionEncoder.encode(fn);
        EthCall response = web3.ethCall(
                Transaction.createEthCallTransaction(callerAddress, contractAddress, data),
                DefaultBlockParameterName.LATEST).send();
        if (response.isReverted()) {
            throw new IllegalStateException("eth_call reverted: " + response.getRevertReason());
        }
        return FunctionReturnDecoder.decode(response.getValue(), fn.getOutputParameters());
    }

    // =====================================================================
    // LISTEN SIDE — log range queries (the polling consumer)
    // =====================================================================

    /** Head block minus N confirmations: the highest block we treat as settled. */
    public BigInteger safeHead(int confirmations) throws IOException {
        BigInteger head = web3.ethBlockNumber().send().getBlockNumber();
        BigInteger safe = head.subtract(BigInteger.valueOf(confirmations));
        return safe.signum() < 0 ? BigInteger.ZERO : safe;
    }

    /** All TransferRef logs in [fromBlock, toBlock], decoded into Java records. */
    public List<TransferRefRecord> fetchTransferRefs(BigInteger fromBlock, BigInteger toBlock)
            throws IOException {
        EthFilter filter = new EthFilter(
                DefaultBlockParameter.valueOf(fromBlock),
                DefaultBlockParameter.valueOf(toBlock),
                contractAddress);
        filter.addSingleTopic(EventEncoder.encode(TRANSFER_REF_EVENT)); // topic0 = event sig hash
        EthLog ethLog = web3.ethGetLogs(filter).send();
        List<TransferRefRecord> records = new ArrayList<>();
        for (EthLog.LogResult<?> result : ethLog.getLogs()) {
            Log log = (Log) result.get();
            records.add(decodeTransferRef(log));
        }
        return records;
    }

    /** Decode one TransferRef log: indexed values from topics, the rest from data. */
    public static TransferRefRecord decodeTransferRef(Log log) {
        List<String> topics = log.getTopics();
        // topic[0] is the event signature hash; indexed params start at topic[1]
        Bytes32 bankRef = (Bytes32) FunctionReturnDecoder.decodeIndexedValue(
                topics.get(1), new TypeReference<Bytes32>() {});
        Address from = (Address) FunctionReturnDecoder.decodeIndexedValue(
                topics.get(2), new TypeReference<Address>() {});
        Address to = (Address) FunctionReturnDecoder.decodeIndexedValue(
                topics.get(3), new TypeReference<Address>() {});
        List<Type> data = FunctionReturnDecoder.decode(
                log.getData(), TRANSFER_REF_EVENT.getNonIndexedParameters());
        BigInteger value = ((Uint256) data.get(0)).getValue();
        return new TransferRefRecord(
                Numeric.toHexString(bankRef.getValue()),
                from.getValue(),
                to.getValue(),
                value,
                log.getBlockNumber(),
                log.getTransactionHash(),
                log.getLogIndex());
    }

    // =====================================================================
    // WRITE SIDE — delegated to the idempotent sender
    // =====================================================================

    /**
     * Submit a transfer keyed by a core-banking reference (e.g. payment instruction id).
     * Calling twice with the same bankRef returns the original tx hash — no double booking.
     */
    public String submitTransfer(String bankRef, String to, BigInteger amountSmallestUnits)
            throws Exception {
        return txSender.submitTransferWithRef(bankRef, to, amountSmallestUnits);
    }

    public IdempotentTxSender txSender() {
        return txSender;
    }

    public Web3j web3() {
        return web3;
    }

    public String contractAddress() {
        return contractAddress;
    }

    // ---- Value object for a decoded TransferRef log ----

    /** Immutable, like a SWIFT MT message after parsing: pure data, no behavior. */
    public static final class TransferRefRecord {
        public final String bankRefHex;     // bytes32 -> 0x-prefixed hex (66 chars)
        public final String from;           // address -> checksummed hex string
        public final String to;
        public final BigInteger value;      // uint256 -> BigInteger, smallest units
        public final BigInteger blockNumber;
        public final String txHash;
        public final BigInteger logIndex;

        public TransferRefRecord(String bankRefHex, String from, String to, BigInteger value,
                                 BigInteger blockNumber, String txHash, BigInteger logIndex) {
            this.bankRefHex = bankRefHex;
            this.from = from;
            this.to = to;
            this.value = value;
            this.blockNumber = blockNumber;
            this.txHash = txHash;
            this.logIndex = logIndex;
        }

        /** Globally unique event id: txHash + logIndex. The natural idempotency key for consumers. */
        public String eventId() {
            return txHash + ":" + logIndex;
        }

        @Override
        public String toString() {
            return "TransferRef{ref=" + bankRefHex + ", from=" + from + ", to=" + to
                    + ", value=" + value + ", block=" + blockNumber + ", tx=" + txHash + "}";
        }
    }

    /** Helper: ISIN/LEI string -> right-padded ASCII bytes32, mirroring the Solidity convention. */
    public static byte[] asciiToBytes32(String s) {
        byte[] ascii = s.getBytes(StandardCharsets.US_ASCII);
        if (ascii.length > 32) {
            throw new IllegalArgumentException("identifier longer than 32 ASCII bytes: " + s);
        }
        byte[] padded = new byte[32];
        System.arraycopy(ascii, 0, padded, 0, ascii.length);
        return padded;
    }
}
