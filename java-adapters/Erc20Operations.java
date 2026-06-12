package ch.bank.cmtat.course.ch04;

import io.reactivex.disposables.Disposable;

import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Erc20Operations — bank-side adapter for the BankERC20 share ledger
 * (course Chapter 04). web3j 4.x, plain JSON-RPC, no generated wrappers:
 * every ABI encode/decode step is explicit so the type mapping is visible.
 *
 * Responsibilities:
 *   1. Load the contract (address + ABI hand-encoded Functions).
 *   2. Read balanceOf as BigInteger and scale to BigDecimal via decimals().
 *   3. Send a transfer transaction from the bank's signing key.
 *   4. Subscribe to Transfer events and post them into the internal ledger.
 *   5. Reconcile internal ledger positions against on-chain balances.
 */
public final class Erc20Operations {

    /** Transfer(address indexed from, address indexed to, uint256 value) */
    private static final Event TRANSFER_EVENT = new Event(
            "Transfer",
            List.of(
                    new TypeReference<Address>(true) {},   // from  (indexed -> topic 1)
                    new TypeReference<Address>(true) {},   // to    (indexed -> topic 2)
                    new TypeReference<Uint256>(false) {}   // value (non-indexed -> data)
            ));

    private static final String TRANSFER_TOPIC = EventEncoder.encode(TRANSFER_EVENT);

    private final Web3j web3j;
    private final TransactionManager txManager;
    private final String contractAddress;

    /** Cached once: decimals() is immutable on BankERC20. */
    private final int decimals;

    /**
     * Internal shadow ledger: holder address (lowercase hex) -> position in
     * base units. In production this would be the core-banking position
     * table; here a concurrent map stands in for it.
     */
    private final Map<String, BigInteger> internalLedger = new ConcurrentHashMap<>();

    public Erc20Operations(String rpcUrl,
                           String contractAddress,
                           Credentials bankSigningKey,
                           long chainId) throws IOException {
        this.web3j = Web3j.build(new HttpService(rpcUrl));
        this.contractAddress = contractAddress;
        this.txManager = new RawTransactionManager(web3j, bankSigningKey, chainId);
        this.decimals = fetchDecimals();
    }

    // ------------------------------------------------------------------
    // 1+2. Read path: balanceOf -> BigInteger -> BigDecimal
    // ------------------------------------------------------------------

    /** decimals() returns Solidity uint8 -> web3j Uint8 -> Java BigInteger. */
    private int fetchDecimals() throws IOException {
        Function fn = new Function(
                "decimals",
                Collections.emptyList(),
                List.of(new TypeReference<Uint8>() {}));
        List<Type> out = ethCall(fn);
        return ((Uint8) out.get(0)).getValue().intValueExact();
    }

    /**
     * balanceOf in raw base units. Solidity uint256 -> web3j Uint256 ->
     * java.math.BigInteger. NEVER long: uint256 max is ~1.16e77, far past
     * Long.MAX_VALUE (~9.2e18).
     */
    public BigInteger balanceOfBaseUnits(String holderAddress) throws IOException {
        Function fn = new Function(
                "balanceOf",
                List.of(new Address(holderAddress)),
                List.of(new TypeReference<Uint256>() {}));
        List<Type> out = ethCall(fn);
        return ((Uint256) out.get(0)).getValue();
    }

    /**
     * balanceOf scaled to whole tokens for display/reporting:
     * BigDecimal = baseUnits / 10^decimals, exact (scale = decimals,
     * no rounding — division by a power of ten of the same scale).
     */
    public BigDecimal balanceOf(String holderAddress) throws IOException {
        BigInteger baseUnits = balanceOfBaseUnits(holderAddress);
        return new BigDecimal(baseUnits, decimals);
    }

    /** Inverse scaling for outbound amounts; rejects sub-unit fractions. */
    public BigInteger toBaseUnits(BigDecimal tokens) {
        // ArithmeticException if tokens has more fractional digits than the
        // instrument supports (e.g. 1.5 shares on a decimals==0 register).
        return tokens.setScale(decimals).unscaledValue();
    }

    // ------------------------------------------------------------------
    // 3. Write path: transfer(to, value)
    // ------------------------------------------------------------------

    /**
     * Sends transfer(to, value) signed by the bank's key.
     * Returns the transaction hash — store it on the booking record as the
     * external settlement reference (audit trail + idempotent retry check).
     */
    public String sendTransfer(String toAddress, BigDecimal tokens) throws IOException {
        BigInteger value = toBaseUnits(tokens);
        Function fn = new Function(
                "transfer",
                List.of(new Address(toAddress), new Uint256(value)),
                Collections.emptyList());
        String data = FunctionEncoder.encode(fn);

        EthSendTransaction resp = txManager.sendTransaction(
                BigInteger.valueOf(2_000_000_000L),  // gasPrice — Ch. 08 covers strategy
                BigInteger.valueOf(100_000L),        // gasLimit
                contractAddress,
                data,
                BigInteger.ZERO);                    // no ETH value with the booking

        if (resp.hasError()) {
            throw new IOException("transfer tx rejected: " + resp.getError().getMessage());
        }
        return resp.getTransactionHash();
    }

    // ------------------------------------------------------------------
    // 4. Event subscription: Transfer logs -> internal ledger postings
    // ------------------------------------------------------------------

    /**
     * Subscribes to Transfer events from {@code fromBlock} onward and posts
     * each one into the internal shadow ledger as a debit/credit pair.
     * Replay-from-block makes the consumer restartable: persist the last
     * processed block number and resume from it after a crash.
     */
    public Disposable subscribeTransfers(BigInteger fromBlock) {
        EthFilter filter = new EthFilter(
                DefaultBlockParameter.valueOf(fromBlock),
                DefaultBlockParameterName.LATEST,
                contractAddress);
        filter.addSingleTopic(TRANSFER_TOPIC);

        return web3j.ethLogFlowable(filter).subscribe(
                this::postTransferToLedger,
                err -> System.err.println("Transfer subscription failed: " + err));
    }

    /** Decodes one Transfer log and applies double-entry postings. */
    void postTransferToLedger(Log log) {
        // Indexed params live in topics[1..]; topics[0] is the event signature.
        String from = decodeAddressTopic(log.getTopics().get(1));
        String to = decodeAddressTopic(log.getTopics().get(2));
        // Non-indexed params live ABI-encoded in the data field.
        List<Type> data = FunctionReturnDecoder.decode(
                log.getData(), TRANSFER_EVENT.getNonIndexedParameters());
        BigInteger value = ((Uint256) data.get(0)).getValue();

        boolean isMint = isZeroAddress(from);
        boolean isBurn = isZeroAddress(to);

        if (!isMint) {
            internalLedger.merge(from.toLowerCase(), value.negate(), BigInteger::add);
        }
        if (!isBurn) {
            internalLedger.merge(to.toLowerCase(), value, BigInteger::add);
        }

        System.out.printf("POSTED %s: %s -> %s value=%s tx=%s logIndex=%s%n",
                isMint ? "ISSUANCE" : isBurn ? "CANCELLATION" : "TRANSFER",
                from, to, value, log.getTransactionHash(), log.getLogIndex());
    }

    // ------------------------------------------------------------------
    // 5. Reconciliation: internal ledger vs chain
    // ------------------------------------------------------------------

    /**
     * Compares the internal shadow position against the authoritative
     * on-chain balance. Returns the break (chain minus internal); ZERO
     * means reconciled. Any non-zero break goes to the ops exception queue
     * — never auto-adjusted (audit requirement, see Chapter 08).
     */
    public BigInteger reconcileHolder(String holderAddress) throws IOException {
        BigInteger onChain = balanceOfBaseUnits(holderAddress);
        BigInteger internal = internalLedger.getOrDefault(
                holderAddress.toLowerCase(), BigInteger.ZERO);
        BigInteger breakAmount = onChain.subtract(internal);
        if (breakAmount.signum() != 0) {
            System.err.printf("RECON BREAK holder=%s onChain=%s internal=%s diff=%s%n",
                    holderAddress, onChain, internal, breakAmount);
        }
        return breakAmount;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private List<Type> ethCall(Function fn) throws IOException {
        EthCall response = web3j.ethCall(
                Transaction.createEthCallTransaction(
                        txManager.getFromAddress(), contractAddress, FunctionEncoder.encode(fn)),
                DefaultBlockParameterName.LATEST).send();
        if (response.hasError()) {
            throw new IOException("eth_call failed: " + response.getError().getMessage());
        }
        return FunctionReturnDecoder.decode(response.getValue(), fn.getOutputParameters());
    }

    /** Indexed address topic = 32 bytes, address right-aligned in last 20. */
    private static String decodeAddressTopic(String topic) {
        return "0x" + topic.substring(topic.length() - 40);
    }

    private static boolean isZeroAddress(String address) {
        return new BigInteger(Numeric.cleanHexPrefix(address), 16).signum() == 0;
    }

    public int getDecimals() {
        return decimals;
    }
}
