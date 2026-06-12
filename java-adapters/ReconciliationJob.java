package ch.bank.cmtat.adapter;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;

/**
 * ReconciliationJob — the nightly (or continuous) break-detection loop.
 *
 * Compares two books that must agree:
 *   LEFT:  on-chain TransferRef events (the register of record, Chapter 09 context)
 *   RIGHT: core-banking postings carrying the same bank reference
 *
 * Both sides are keyed by the bytes32 idempotency reference, so matching is a
 * hash-join, exactly like nostro reconciliation keyed on end-to-end payment id.
 *
 * Output:
 *   - matched pairs (advance the checkpoint past them)
 *   - breaks, classified and pushed to a repair queue for operations
 *   - hash-chained audit records: each record commits to the previous record's hash,
 *     so any later tampering with the audit log is detectable (mini blockchain).
 */
public class ReconciliationJob {

    // ---------------------------------------------------------------
    // Ports to the rest of the bank — implemented elsewhere in the estate
    // ---------------------------------------------------------------

    /** Read side of the core-banking ledger (e.g. a view over the posting journal). */
    public interface CoreLedgerClient {
        /** Postings whose value date / booking window overlaps the reconciliation window. */
        List<CorePosting> fetchPostings(Instant fromInclusive, Instant toExclusive);
    }

    /** Durable checkpoint: last fully reconciled block. Production: one DB row, updated last. */
    public interface CheckpointStore {
        BigInteger loadLastReconciledBlock();
        void saveLastReconciledBlock(BigInteger block);
    }

    /** One booking in the core-banking system that should have a chain twin. */
    public static final class CorePosting {
        public final String bankRef;          // business reference (instruction id)
        public final BigInteger amount;       // smallest units, same scale as on-chain
        public final String debitAccount;     // core account expected to map to `from`
        public final String creditAccount;    // core account expected to map to `to`

        public CorePosting(String bankRef, BigInteger amount,
                           String debitAccount, String creditAccount) {
            this.bankRef = bankRef;
            this.amount = amount;
            this.debitAccount = debitAccount;
            this.creditAccount = creditAccount;
        }
    }

    /** Break classification — drives the operations repair playbook. */
    public enum BreakType {
        CHAIN_ONLY,        // event on chain, no core posting  -> book in core (or investigate)
        CORE_ONLY,         // core posting, no chain event     -> resubmit on chain (or cancel)
        AMOUNT_MISMATCH    // both exist, amounts differ        -> four-eyes investigation
    }

    public static final class ReconBreak {
        public final BreakType type;
        public final String refHashHex;
        public final BigInteger chainAmount;  // null when CHAIN side missing
        public final BigInteger coreAmount;   // null when CORE side missing
        public final String detail;

        public ReconBreak(BreakType type, String refHashHex,
                          BigInteger chainAmount, BigInteger coreAmount, String detail) {
            this.type = type;
            this.refHashHex = refHashHex;
            this.chainAmount = chainAmount;
            this.coreAmount = coreAmount;
            this.detail = detail;
        }

        @Override
        public String toString() {
            return type + " ref=" + refHashHex + " chain=" + chainAmount
                    + " core=" + coreAmount + " :: " + detail;
        }
    }

    /** Append-only, hash-chained audit record. */
    public static final class ChainedAuditRecord {
        public final long sequence;
        public final Instant timestamp;
        public final String payload;      // what happened, human/machine readable
        public final String previousHash; // hash of the previous record (genesis: 0x00..00)
        public final String recordHash;   // keccak256(sequence|timestamp|payload|previousHash)

        ChainedAuditRecord(long sequence, Instant timestamp, String payload, String previousHash) {
            this.sequence = sequence;
            this.timestamp = timestamp;
            this.payload = payload;
            this.previousHash = previousHash;
            String preimage = sequence + "|" + timestamp.toEpochMilli() + "|" + payload
                    + "|" + previousHash;
            this.recordHash = Numeric.toHexString(
                    Hash.sha3(preimage.getBytes(StandardCharsets.UTF_8)));
        }
    }

    // ---------------------------------------------------------------
    // State
    // ---------------------------------------------------------------

    private static final int CONFIRMATIONS = 12; // reorg-depth policy, see Lesson 2

    private final CmtatBankAdapter adapter;
    private final CoreLedgerClient coreLedger;
    private final CheckpointStore checkpoints;

    /** Repair queue consumed by an operations UI / case-management system. */
    private final Deque<ReconBreak> repairQueue = new ArrayDeque<>();

    /** In-memory tail of the audit chain. Production: append-only table + WORM storage. */
    private final List<ChainedAuditRecord> auditLog = new ArrayList<>();
    private String lastAuditHash =
            "0x0000000000000000000000000000000000000000000000000000000000000000";
    private long auditSequence = 0;

    public ReconciliationJob(CmtatBankAdapter adapter,
                             CoreLedgerClient coreLedger,
                             CheckpointStore checkpoints) {
        this.adapter = adapter;
        this.coreLedger = coreLedger;
        this.checkpoints = checkpoints;
    }

    // ---------------------------------------------------------------
    // The reconciliation pass
    // ---------------------------------------------------------------

    /**
     * One pass: [checkpoint+1 .. head-CONFIRMATIONS] on chain vs the matching
     * core-banking window. Returns the number of breaks found.
     */
    public int runOnce(Instant coreWindowFrom, Instant coreWindowTo) throws IOException {
        BigInteger fromBlock = checkpoints.loadLastReconciledBlock().add(BigInteger.ONE);
        BigInteger toBlock = adapter.safeHead(CONFIRMATIONS);
        if (toBlock.compareTo(fromBlock) < 0) {
            audit("NOOP no new confirmed blocks, checkpoint=" + fromBlock.subtract(BigInteger.ONE));
            return 0;
        }

        // LEFT: chain events keyed by refHash
        List<CmtatBankAdapter.TransferRefRecord> events =
                adapter.fetchTransferRefs(fromBlock, toBlock);
        Map<String, CmtatBankAdapter.TransferRefRecord> chainByRef = new HashMap<>();
        for (CmtatBankAdapter.TransferRefRecord e : events) {
            // Same ref twice on chain would itself be a defect — last one wins, flag it.
            CmtatBankAdapter.TransferRefRecord prev = chainByRef.put(e.bankRefHex, e);
            if (prev != null) {
                enqueueBreak(new ReconBreak(BreakType.AMOUNT_MISMATCH, e.bankRefHex,
                        e.value, null, "duplicate ref on chain: " + prev.eventId()
                        + " and " + e.eventId()));
            }
        }

        // RIGHT: core postings keyed by the SAME derived refHash
        Map<String, CorePosting> coreByRef = new HashMap<>();
        for (CorePosting p : coreLedger.fetchPostings(coreWindowFrom, coreWindowTo)) {
            coreByRef.put(IdempotentTxSender.refHash(p.bankRef), p);
        }

        int breaks = 0;

        // Pass 1: walk chain events, look for the core twin
        for (Map.Entry<String, CmtatBankAdapter.TransferRefRecord> e : chainByRef.entrySet()) {
            CorePosting posting = coreByRef.remove(e.getKey()); // remove = mark matched
            CmtatBankAdapter.TransferRefRecord ev = e.getValue();
            if (posting == null) {
                enqueueBreak(new ReconBreak(BreakType.CHAIN_ONLY, e.getKey(),
                        ev.value, null, "no core posting for event " + ev.eventId()));
                breaks++;
            } else if (!posting.amount.equals(ev.value)) {
                enqueueBreak(new ReconBreak(BreakType.AMOUNT_MISMATCH, e.getKey(),
                        ev.value, posting.amount,
                        "amount differs, event " + ev.eventId() + " vs ref " + posting.bankRef));
                breaks++;
            } else {
                audit("MATCH ref=" + e.getKey() + " amount=" + ev.value
                        + " event=" + ev.eventId());
            }
        }

        // Pass 2: anything left on the core side has no chain twin
        for (Map.Entry<String, CorePosting> leftover : coreByRef.entrySet()) {
            enqueueBreak(new ReconBreak(BreakType.CORE_ONLY, leftover.getKey(),
                    null, leftover.getValue().amount,
                    "no chain event for core ref " + leftover.getValue().bankRef));
            breaks++;
        }

        // Checkpoint LAST: if we crash before this line, the pass re-runs — and because
        // matching is keyed on refs, the re-run is idempotent (same matches, same breaks).
        checkpoints.saveLastReconciledBlock(toBlock);
        audit("PASS blocks=" + fromBlock + ".." + toBlock + " events=" + events.size()
                + " breaks=" + breaks);
        return breaks;
    }

    private void enqueueBreak(ReconBreak b) {
        repairQueue.addLast(b);
        audit("BREAK " + b);
    }

    // ---------------------------------------------------------------
    // Audit chain
    // ---------------------------------------------------------------

    private void audit(String payload) {
        ChainedAuditRecord rec =
                new ChainedAuditRecord(auditSequence++, Instant.now(), payload, lastAuditHash);
        auditLog.add(rec);
        lastAuditHash = rec.recordHash;
    }

    /** Recompute every hash; any mutated historic record breaks the chain from there on. */
    public boolean verifyAuditChain() {
        String prev = "0x0000000000000000000000000000000000000000000000000000000000000000";
        for (ChainedAuditRecord rec : auditLog) {
            if (!rec.previousHash.equals(prev)) {
                return false;
            }
            String preimage = rec.sequence + "|" + rec.timestamp.toEpochMilli() + "|"
                    + rec.payload + "|" + rec.previousHash;
            String recomputed = Numeric.toHexString(
                    Hash.sha3(preimage.getBytes(StandardCharsets.UTF_8)));
            if (!recomputed.equals(rec.recordHash)) {
                return false;
            }
            prev = rec.recordHash;
        }
        return true;
    }

    public Deque<ReconBreak> repairQueue() {
        return repairQueue;
    }

    public List<ChainedAuditRecord> auditLog() {
        return auditLog;
    }
}
