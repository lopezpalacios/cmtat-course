package ch.bank.cmtat.adapters;

import io.reactivex.disposables.Disposable;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.Log;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chapter 07 — ComplianceMonitor.
 *
 * Bank-side compliance surveillance for a ComplianceToken + WhitelistRuleEngine
 * deployment. Three duties:
 *
 *   1. Subscribe to Paused / Unpaused / Freeze / Unfreeze / Enforcement /
 *      RuleEngineSet / AddressWhitelisted / AddressDelisted logs and raise
 *      incidents in the bank's monitoring stack (PagerDuty / ServiceNow /
 *      internal MQ — stubbed as raiseIncident()).
 *
 *   2. Pre-trade gate: before the core-banking system submits any bank-initiated
 *      transfer, ask the chain via eth_call detectTransferRestriction(from,to,amount)
 *      (ERC-1404 style) and translate the uint8 code into a typed reject reason
 *      for the ops ticket — no gas burned on doomed transactions.
 *
 *   3. Idempotency: every log is deduplicated on (txHash, logIndex) so a filter
 *      replay after reconnect never double-fires an incident.
 *
 * web3j 4.x. Illustrative — wire web3j + RxJava2 on the classpath to run.
 */
public final class ComplianceMonitor {

    /* ------------------------------------------------------------------ */
    /* Event definitions — must mirror the Solidity declarations exactly. */
    /* ------------------------------------------------------------------ */

    // event Paused(address account) — account NOT indexed, arrives in data.
    private static final Event PAUSED = new Event("Paused",
            Collections.singletonList(new TypeReference<Address>(false) {}));

    private static final Event UNPAUSED = new Event("Unpaused",
            Collections.singletonList(new TypeReference<Address>(false) {}));

    // event Freeze(address indexed enforcer, address indexed account, bytes32 reasonCode)
    private static final Event FREEZE = new Event("Freeze", Arrays.asList(
            new TypeReference<Address>(true) {},   // enforcer  -> topics[1]
            new TypeReference<Address>(true) {},   // account   -> topics[2]
            new TypeReference<Bytes32>(false) {})); // reasonCode -> data

    private static final Event UNFREEZE = new Event("Unfreeze", Arrays.asList(
            new TypeReference<Address>(true) {},
            new TypeReference<Address>(true) {},
            new TypeReference<Bytes32>(false) {}));

    // event Enforcement(address indexed enforcer, address indexed from,
    //                   address indexed to, uint256 amount, bytes32 reasonCode)
    private static final Event ENFORCEMENT = new Event("Enforcement", Arrays.asList(
            new TypeReference<Address>(true) {},
            new TypeReference<Address>(true) {},
            new TypeReference<Address>(true) {},
            new TypeReference<Uint256>(false) {},
            new TypeReference<Bytes32>(false) {}));

    // event RuleEngineSet(address indexed newRuleEngine, address indexed admin)
    private static final Event RULE_ENGINE_SET = new Event("RuleEngineSet", Arrays.asList(
            new TypeReference<Address>(true) {},
            new TypeReference<Address>(true) {}));

    // event AddressWhitelisted(address indexed account, bytes32 kycRef, address indexed operator)
    private static final Event ADDRESS_WHITELISTED = new Event("AddressWhitelisted", Arrays.asList(
            new TypeReference<Address>(true) {},
            new TypeReference<Bytes32>(false) {},
            new TypeReference<Address>(true) {}));

    // event AddressDelisted(address indexed account, bytes32 reasonCode, address indexed operator)
    private static final Event ADDRESS_DELISTED = new Event("AddressDelisted", Arrays.asList(
            new TypeReference<Address>(true) {},
            new TypeReference<Bytes32>(false) {},
            new TypeReference<Address>(true) {}));

    /* ------------------------------------------------------------------ */

    private final Web3j web3j;
    private final String tokenAddress;
    private final String ruleEngineAddress;
    private final String bankQueryAddress; // any funded/unfunded address; eth_call needs a from

    /** (txHash + ":" + logIndex) of every log already processed. */
    private final Set<String> processedLogs = ConcurrentHashMap.newKeySet();

    private Disposable subscription;

    public ComplianceMonitor(Web3j web3j, String tokenAddress,
                             String ruleEngineAddress, String bankQueryAddress) {
        this.web3j = web3j;
        this.tokenAddress = tokenAddress;
        this.ruleEngineAddress = ruleEngineAddress;
        this.bankQueryAddress = bankQueryAddress;
    }

    /* ------------------------------------------------------------------ */
    /* 1. Surveillance: subscribe to compliance events                     */
    /* ------------------------------------------------------------------ */

    /**
     * Start streaming compliance logs from {@code fromBlock} (use the last
     * block persisted by the reconciliation job — see Chapter 08 — so a
     * restart replays anything missed while the adapter was down).
     */
    public void startMonitoring(BigInteger fromBlock) {
        EthFilter filter = new EthFilter(
                DefaultBlockParameter.valueOf(fromBlock),
                DefaultBlockParameterName.LATEST,
                Arrays.asList(tokenAddress, ruleEngineAddress));

        subscription = web3j.ethLogFlowable(filter).subscribe(
                this::dispatch,
                err -> raiseIncident("SEV1", "MONITOR-DOWN",
                        "Compliance log subscription dropped: " + err.getMessage()));
    }

    public void stopMonitoring() {
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
        }
    }

    private void dispatch(Log log) {
        // Idempotency: filter replays must not double-fire incidents.
        String dedupeKey = log.getTransactionHash() + ":" + log.getLogIndex();
        if (!processedLogs.add(dedupeKey)) {
            return;
        }
        if (log.getTopics().isEmpty()) {
            return;
        }
        String topic0 = log.getTopics().get(0);

        if (topic0.equals(EventEncoder.encode(PAUSED))) {
            handlePaused(log, true);
        } else if (topic0.equals(EventEncoder.encode(UNPAUSED))) {
            handlePaused(log, false);
        } else if (topic0.equals(EventEncoder.encode(FREEZE))) {
            handleFreeze(log, true);
        } else if (topic0.equals(EventEncoder.encode(UNFREEZE))) {
            handleFreeze(log, false);
        } else if (topic0.equals(EventEncoder.encode(ENFORCEMENT))) {
            handleEnforcement(log);
        } else if (topic0.equals(EventEncoder.encode(RULE_ENGINE_SET))) {
            handleRuleEngineSet(log);
        } else if (topic0.equals(EventEncoder.encode(ADDRESS_WHITELISTED))) {
            handleWhitelist(log, true);
        } else if (topic0.equals(EventEncoder.encode(ADDRESS_DELISTED))) {
            handleWhitelist(log, false);
        }
        // Other logs (Transfer, Approval, role events) belong to the
        // reconciliation job, not the compliance monitor.
    }

    private void handlePaused(Log log, boolean pausedNow) {
        // account is non-indexed -> decode from the data segment.
        List<Type> data = FunctionReturnDecoder.decode(
                log.getData(), pausedNow ? PAUSED.getNonIndexedParameters()
                                         : UNPAUSED.getNonIndexedParameters());
        String pauser = ((Address) data.get(0)).getValue();

        raiseIncident(pausedNow ? "SEV1" : "SEV3",
                pausedNow ? "MARKET-HALT" : "MARKET-RESUME",
                String.format("%s by pauser %s in tx %s block %s",
                        pausedNow ? "Token PAUSED (all transfers halted)"
                                  : "Token UNPAUSED (transfers resumed)",
                        pauser, log.getTransactionHash(), log.getBlockNumber()));
    }

    private void handleFreeze(Log log, boolean frozenNow) {
        Event evt = frozenNow ? FREEZE : UNFREEZE;
        String enforcer = decodeIndexedAddress(log.getTopics().get(1));
        String account = decodeIndexedAddress(log.getTopics().get(2));
        List<Type> data = FunctionReturnDecoder.decode(log.getData(), evt.getNonIndexedParameters());
        String reasonCode = bytes32ToAscii(((Bytes32) data.get(0)).getValue());

        raiseIncident(frozenNow ? "SEV2" : "SEV3",
                frozenNow ? "ADDRESS-FROZEN" : "ADDRESS-UNFROZEN",
                String.format("account=%s reason=%s enforcer=%s tx=%s",
                        account, reasonCode, enforcer, log.getTransactionHash()));
    }

    private void handleEnforcement(Log log) {
        String enforcer = decodeIndexedAddress(log.getTopics().get(1));
        String from = decodeIndexedAddress(log.getTopics().get(2));
        String to = decodeIndexedAddress(log.getTopics().get(3));
        List<Type> data = FunctionReturnDecoder.decode(
                log.getData(), ENFORCEMENT.getNonIndexedParameters());
        BigInteger amount = ((Uint256) data.get(0)).getValue();
        String reasonCode = bytes32ToAscii(((Bytes32) data.get(1)).getValue());

        // Forced transfer = legal seizure. Always a high-severity audit item.
        raiseIncident("SEV1", "FORCED-TRANSFER",
                String.format("amount=%s from=%s to=%s reason=%s enforcer=%s tx=%s",
                        amount, from, to, reasonCode, enforcer, log.getTransactionHash()));
    }

    private void handleRuleEngineSet(Log log) {
        String newEngine = decodeIndexedAddress(log.getTopics().get(1));
        String admin = decodeIndexedAddress(log.getTopics().get(2));
        // Swapping the compliance plug-in is a change-management event:
        // verify against the approved change ticket.
        raiseIncident("SEV2", "RULE-ENGINE-CHANGED",
                String.format("newRuleEngine=%s admin=%s tx=%s",
                        newEngine, admin, log.getTransactionHash()));
    }

    private void handleWhitelist(Log log, boolean added) {
        Event evt = added ? ADDRESS_WHITELISTED : ADDRESS_DELISTED;
        String account = decodeIndexedAddress(log.getTopics().get(1));
        List<Type> data = FunctionReturnDecoder.decode(log.getData(), evt.getNonIndexedParameters());
        String ref = bytes32ToAscii(((Bytes32) data.get(0)).getValue());

        // Routine KYC sync -> informational; feeds the daily reconciliation
        // between the chain whitelist and the bank's KYC system of record.
        raiseIncident("INFO", added ? "KYC-WHITELISTED" : "KYC-DELISTED",
                String.format("account=%s %s=%s tx=%s",
                        account, added ? "kycRef" : "reason", ref, log.getTransactionHash()));
    }

    /* ------------------------------------------------------------------ */
    /* 2. Pre-trade gate: ERC-1404-style detection before submitting txs   */
    /* ------------------------------------------------------------------ */

    /** Typed result for the core-banking order-management layer. */
    public record TransferCheck(int restrictionCode, String message) {
        public boolean allowed() {
            return restrictionCode == 0;
        }
    }

    /**
     * Call before submitting any bank-initiated transfer. Uses eth_call —
     * free, instant, and returns the SAME code the on-chain hook would
     * revert with, so the pre-trade decision and the chain are consistent.
     */
    public TransferCheck preTradeCheck(String from, String to, BigInteger amount)
            throws Exception {
        Function detect = new Function(
                "detectTransferRestriction",
                Arrays.asList(new Address(from), new Address(to), new Uint256(amount)),
                Collections.singletonList(new TypeReference<Uint8>() {}));

        BigInteger code = ((Uint8) callView(detect).get(0)).getValue();
        if (code.signum() == 0) {
            return new TransferCheck(0, "No restriction");
        }

        Function message = new Function(
                "messageForTransferRestriction",
                Collections.singletonList(new Uint8(code)),
                Collections.singletonList(new TypeReference<Utf8String>() {}));
        String reason = ((Utf8String) callView(message).get(0)).getValue();

        return new TransferCheck(code.intValueExact(), reason);
    }

    private List<Type> callView(Function fn) throws Exception {
        EthCall response = web3j.ethCall(
                Transaction.createEthCallTransaction(
                        bankQueryAddress, tokenAddress, FunctionEncoder.encode(fn)),
                DefaultBlockParameterName.LATEST).send();
        if (response.hasError()) {
            throw new IllegalStateException("eth_call failed: " + response.getError().getMessage());
        }
        return FunctionReturnDecoder.decode(response.getValue(), fn.getOutputParameters());
    }

    /* ------------------------------------------------------------------ */
    /* 3. Helpers                                                          */
    /* ------------------------------------------------------------------ */

    /** An indexed address sits left-padded in a 32-byte topic. */
    private static String decodeIndexedAddress(String topic) {
        return ((Address) FunctionReturnDecoder.decodeIndexedValue(
                topic, new TypeReference<Address>() {})).getValue();
    }

    /** bytes32 reason codes are right-padded ASCII — strip trailing zero bytes. */
    private static String bytes32ToAscii(byte[] raw) {
        int end = raw.length;
        while (end > 0 && raw[end - 1] == 0) {
            end--;
        }
        return new String(raw, 0, end, StandardCharsets.US_ASCII);
    }

    /**
     * Incident sink — in production this posts to the bank's alerting bus
     * (PagerDuty / ServiceNow / Kafka ops topic) with an idempotency key.
     */
    private void raiseIncident(String severity, String code, String details) {
        System.out.printf("[%s] %s %s %s%n", Instant.now(), severity, code, details);
    }
}
