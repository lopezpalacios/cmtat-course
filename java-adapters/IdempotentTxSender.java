package ch.bank.cmtat.adapter;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;
import org.web3j.utils.Numeric;

/**
 * IdempotentTxSender — single-sender transaction queue with idempotency keys.
 *
 * Design rules (the same rules a payment-engine sender thread follows):
 *   1. ONE signing key, ONE sender instance, ONE nonce counter. Nonce = sequence number;
 *      gaps stall everything behind them, duplicates are rejected by the node.
 *   2. Every business operation carries a bank reference. We derive bytes32 ref =
 *      keccak256(namespace ":" bankRef) and refuse to send the same ref twice.
 *   3. A stuck transaction is replaced by re-sending WITH THE SAME NONCE at a higher
 *      gas price ("repricing"), never by sending a new nonce — that would double-pay.
 *   4. Nothing is reported "done" until a mined receipt with status == 0x1 is seen.
 *
 * The idempotency store here is an in-memory map for teaching; production = a DB table
 * with a UNIQUE constraint on bank_ref, written in the same local transaction that
 * marks the payment instruction as "submitted to chain".
 */
public class IdempotentTxSender {

    /** Lifecycle of one submission — mirrors a payment status model. */
    public enum TxState { SUBMITTED, CONFIRMED, FAILED }

    /** One row of the idempotency store. */
    public static final class TxEntry {
        public final String bankRef;
        public final String refHash;      // bytes32 idempotency key, 0x-hex
        public volatile String txHash;    // updated on repricing (hash changes, nonce does not)
        public final BigInteger nonce;
        public volatile TxState state;

        TxEntry(String bankRef, String refHash, String txHash, BigInteger nonce, TxState state) {
            this.bankRef = bankRef;
            this.refHash = refHash;
            this.txHash = txHash;
            this.nonce = nonce;
            this.state = state;
        }
    }

    private static final String REF_NAMESPACE = "CH-BANK-CMTAT-V1";
    private static final BigInteger GAS_LIMIT = BigInteger.valueOf(200_000L);
    private static final BigInteger TIP_WEI = BigInteger.valueOf(2_000_000_000L); // 2 gwei priority fee
    private static final int RECEIPT_POLL_MS = 2_000;
    private static final int RECEIPT_POLL_ATTEMPTS = 60; // ~2 minutes before repricing

    private final Web3j web3;
    private final Credentials credentials;
    private final long chainId;
    private final String contractAddress;
    private final PollingTransactionReceiptProcessor receiptProcessor;

    /** Idempotency store: refHash -> entry. Production: DB table, UNIQUE(bank_ref). */
    private final Map<String, TxEntry> store = new ConcurrentHashMap<>();

    /** Next nonce to assign. Guarded by `this` — the single-sender queue discipline. */
    private BigInteger nextNonce;

    public IdempotentTxSender(Web3j web3, Credentials credentials, long chainId,
                              String contractAddress) throws IOException {
        this.web3 = web3;
        this.credentials = credentials;
        this.chainId = chainId;
        this.contractAddress = contractAddress;
        this.receiptProcessor =
                new PollingTransactionReceiptProcessor(web3, RECEIPT_POLL_MS, RECEIPT_POLL_ATTEMPTS);
        // PENDING (not LATEST): include our own not-yet-mined txs in the count,
        // otherwise a restart while txs are in-flight would reuse a nonce.
        this.nextNonce = web3.ethGetTransactionCount(
                credentials.getAddress(), DefaultBlockParameterName.PENDING)
                .send().getTransactionCount();
    }

    /** Derive the on-chain bytes32 idempotency key from the core-banking reference. */
    public static String refHash(String bankRef) {
        byte[] preimage = (REF_NAMESPACE + ":" + bankRef).getBytes(StandardCharsets.UTF_8);
        return Numeric.toHexString(Hash.sha3(preimage)); // keccak-256, 32 bytes
    }

    /**
     * Submit transferWithRef(to, amount, refHash). Idempotent on bankRef:
     * a second call with the same bankRef returns the original tx hash.
     *
     * Assumption: Chapter 07's ComplianceToken exposes
     *   function transferWithRef(address to, uint256 value, bytes32 bankRef) external returns (bool)
     * and reverts with "ref already used" on on-chain replay — belt and braces.
     */
    public String submitTransferWithRef(String bankRef, String to, BigInteger amount)
            throws Exception {
        String refHash = refHash(bankRef);

        // ---- Idempotency gate (bank-side dedup, first line of defense) ----
        TxEntry existing = store.get(refHash);
        if (existing != null && existing.state != TxState.FAILED) {
            System.out.println("[sender] duplicate ref " + bankRef + " -> " + existing.txHash);
            return existing.txHash;
        }

        Function fn = new Function("transferWithRef",
                Arrays.asList(
                        new Address(to),
                        new Uint256(amount),
                        new Bytes32(Numeric.hexStringToByteArray(refHash))),
                Collections.emptyList());
        String data = FunctionEncoder.encode(fn);

        // ---- Nonce assignment: serialized, gap-free ----
        BigInteger nonce;
        synchronized (this) {
            nonce = nextNonce;
            nextNonce = nextNonce.add(BigInteger.ONE);
        }

        BigInteger maxFee = currentMaxFeePerGas(BigInteger.ONE);
        String txHash = signAndSend(nonce, data, TIP_WEI, maxFee);
        TxEntry entry = new TxEntry(bankRef, refHash, txHash, nonce, TxState.SUBMITTED);
        store.put(refHash, entry);
        System.out.println("[sender] submitted ref=" + bankRef + " nonce=" + nonce + " tx=" + txHash);

        // ---- Wait for receipt; reprice with SAME NONCE if it stalls ----
        TransactionReceipt receipt = waitWithRepricing(entry, data);
        if (BigInteger.ONE.equals(receipt.getStatus() == null
                ? BigInteger.ONE : Numeric.decodeQuantity(receipt.getStatus()))) {
            entry.state = TxState.CONFIRMED;
        } else {
            entry.state = TxState.FAILED;
            String reason = fetchRevertReason(data, receipt.getBlockNumber());
            throw new IllegalStateException(
                    "tx reverted, ref=" + bankRef + ", reason=" + reason);
        }
        return entry.txHash;
    }

    /** EIP-1559 fee: maxFee = 2 * baseFee + tip, headroom for `bumps` base-fee doublings. */
    private BigInteger currentMaxFeePerGas(BigInteger bumps) throws IOException {
        EthBlock.Block latest = web3.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false)
                .send().getBlock();
        BigInteger baseFee = latest.getBaseFeePerGas();
        return baseFee.multiply(BigInteger.TWO).multiply(bumps).add(TIP_WEI);
    }

    /** Build, sign, broadcast one EIP-1559 transaction. Returns tx hash. */
    private String signAndSend(BigInteger nonce, String data,
                               BigInteger tip, BigInteger maxFee) throws IOException {
        RawTransaction rawTx = RawTransaction.createTransaction(
                chainId,
                nonce,
                GAS_LIMIT,
                contractAddress,
                BigInteger.ZERO,   // value: no ETH attached, this is a token operation
                data,
                tip,               // maxPriorityFeePerGas
                maxFee);           // maxFeePerGas
        byte[] signed = TransactionEncoder.signMessage(rawTx, credentials);
        EthSendTransaction response =
                web3.ethSendRawTransaction(Numeric.toHexString(signed)).send();
        if (response.hasError()) {
            throw new IOException("broadcast failed: " + response.getError().getMessage());
        }
        return response.getTransactionHash();
    }

    /**
     * Wait for the receipt. If polling times out (base fee spiked, our maxFee too low),
     * resend the SAME payload with the SAME NONCE at a higher fee. The node replaces the
     * old tx; exactly one of the two can ever be mined, because the nonce is the same.
     */
    private TransactionReceipt waitWithRepricing(TxEntry entry, String data) throws Exception {
        int attempt = 1;
        while (true) {
            try {
                return receiptProcessor.waitForTransactionReceipt(entry.txHash);
            } catch (org.web3j.protocol.exceptions.TransactionException timedOut) {
                if (attempt >= 3) {
                    throw new IllegalStateException(
                            "tx stuck after " + attempt + " repricings, nonce=" + entry.nonce
                                    + ", last hash=" + entry.txHash + " — operator action required");
                }
                attempt++;
                BigInteger bumpedTip = TIP_WEI.multiply(BigInteger.valueOf(attempt));
                BigInteger bumpedMax = currentMaxFeePerGas(BigInteger.valueOf(attempt));
                String newHash = signAndSend(entry.nonce, data, bumpedTip, bumpedMax);
                System.out.println("[sender] repriced nonce=" + entry.nonce
                        + " old=" + entry.txHash + " new=" + newHash);
                entry.txHash = newHash; // hash changes, business ref and nonce do not
            }
        }
    }

    /**
     * Replay the failed call as eth_call at the failure block and decode Error(string).
     * Revert data layout: 0x08c379a0 (selector of Error(string)) ++ ABI-encoded string.
     */
    public String fetchRevertReason(String callData, BigInteger blockNumber) throws IOException {
        EthCall call = web3.ethCall(
                Transaction.createEthCallTransaction(
                        credentials.getAddress(), contractAddress, callData),
                org.web3j.protocol.core.DefaultBlockParameter.valueOf(blockNumber)).send();
        String raw = call.getValue();
        if (raw == null || raw.length() < 10 || !raw.startsWith("0x08c379a0")) {
            return "(no Error(string) payload: " + raw + ")";
        }
        String encodedString = "0x" + raw.substring(10); // strip 4-byte selector
        Type decoded = FunctionReturnDecoder.decode(
                encodedString,
                Collections.singletonList((TypeReference) new TypeReference<Utf8String>() {}))
                .get(0);
        return ((Utf8String) decoded).getValue();
    }

    /** Read-only view for the reconciliation job and ops dashboards. */
    public TxEntry lookup(String bankRef) {
        return store.get(refHash(bankRef));
    }
}
