package ch.examplebank.cmtat.adapters;

import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.crypto.Hash;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.http.HttpService;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * RoleAuditTrail — Chapter 05 bank-side adapter.
 *
 * Scans RoleGranted / RoleRevoked event logs of a RoleControlled contract,
 * replays them in order to build the CURRENT role matrix (role -> members),
 * exports the matrix as CSV for the bank's IAM access-recertification
 * campaign, and raises alerts on any change to DEFAULT_ADMIN_ROLE.
 *
 * web3j 4.x. Read-only: no signing key required.
 */
public final class RoleAuditTrail {

    // Solidity: event RoleGranted(bytes32 indexed role, address indexed account, address indexed sender)
    private static final Event ROLE_GRANTED = new Event("RoleGranted",
            Arrays.asList(
                    new TypeReference<Bytes32>(true) {},  // role    (topic 1)
                    new TypeReference<Address>(true) {},  // account (topic 2)
                    new TypeReference<Address>(true) {}));// sender  (topic 3)

    // Solidity: event RoleRevoked(bytes32 indexed role, address indexed account, address indexed sender)
    private static final Event ROLE_REVOKED = new Event("RoleRevoked",
            Arrays.asList(
                    new TypeReference<Bytes32>(true) {},
                    new TypeReference<Address>(true) {},
                    new TypeReference<Address>(true) {}));

    private static final String ROLE_GRANTED_TOPIC = EventEncoder.encode(ROLE_GRANTED);
    private static final String ROLE_REVOKED_TOPIC = EventEncoder.encode(ROLE_REVOKED);

    // DEFAULT_ADMIN_ROLE is bytes32(0) on-chain.
    private static final String DEFAULT_ADMIN_ROLE =
            "0x0000000000000000000000000000000000000000000000000000000000000000";

    // role-hash (0x... lowercase) -> human-readable role name.
    // Same derivation as Solidity: keccak256("MINTER_ROLE") etc.
    private static final Map<String, String> KNOWN_ROLES = buildKnownRoles();

    private static Map<String, String> buildKnownRoles() {
        Map<String, String> roles = new HashMap<>();
        roles.put(DEFAULT_ADMIN_ROLE, "DEFAULT_ADMIN_ROLE");
        for (String name : List.of("MINTER_ROLE", "BURNER_ROLE", "PAUSER_ROLE",
                "ENFORCER_ROLE", "SNAPSHOOTER_ROLE")) {
            // Hash.sha3String = keccak256 over the UTF-8 bytes, same as Solidity
            // keccak256("MINTER_ROLE") on a string literal.
            roles.put(Hash.sha3String(name).toLowerCase(), name);
        }
        return roles;
    }

    /** One decoded RoleGranted/RoleRevoked log entry. */
    public static final class RoleChange {
        public final BigInteger blockNumber;
        public final BigInteger logIndex;
        public final String txHash;
        public final boolean granted;     // true = RoleGranted, false = RoleRevoked
        public final String roleHash;     // bytes32 as 0x-hex
        public final String account;      // who received/lost the role
        public final String sender;       // msg.sender that performed the change

        RoleChange(BigInteger blockNumber, BigInteger logIndex, String txHash,
                   boolean granted, String roleHash, String account, String sender) {
            this.blockNumber = blockNumber;
            this.logIndex = logIndex;
            this.txHash = txHash;
            this.granted = granted;
            this.roleHash = roleHash;
            this.account = account;
            this.sender = sender;
        }

        public String roleName() {
            return KNOWN_ROLES.getOrDefault(roleHash, "UNKNOWN(" + roleHash + ")");
        }
    }

    private final Web3j web3j;
    private final String contractAddress;

    public RoleAuditTrail(Web3j web3j, String contractAddress) {
        this.web3j = web3j;
        this.contractAddress = contractAddress;
    }

    /**
     * Fetch every RoleGranted/RoleRevoked log from `fromBlock` to latest,
     * decoded and sorted in chain order (blockNumber, then logIndex).
     */
    public List<RoleChange> fetchRoleChanges(BigInteger fromBlock) throws IOException {
        EthFilter filter = new EthFilter(
                DefaultBlockParameter.valueOf(fromBlock),
                DefaultBlockParameterName.LATEST,
                contractAddress);
        // topic0 = RoleGranted OR RoleRevoked
        filter.addOptionalTopics(ROLE_GRANTED_TOPIC, ROLE_REVOKED_TOPIC);

        EthLog ethLog = web3j.ethGetLogs(filter).send();
        if (ethLog.hasError()) {
            throw new IOException("eth_getLogs failed: " + ethLog.getError().getMessage());
        }

        List<RoleChange> changes = new ArrayList<>();
        for (EthLog.LogResult<?> result : ethLog.getLogs()) {
            Log log = (Log) result.get();
            changes.add(decode(log));
        }
        changes.sort(Comparator
                .comparing((RoleChange c) -> c.blockNumber)
                .thenComparing(c -> c.logIndex));
        return changes;
    }

    private RoleChange decode(Log log) {
        List<String> topics = log.getTopics();
        boolean granted = ROLE_GRANTED_TOPIC.equals(topics.get(0));

        // Indexed params live in topics[1..3], one 32-byte word each.
        Bytes32 role = (Bytes32) FunctionReturnDecoder.decodeIndexedValue(
                topics.get(1), new TypeReference<Bytes32>() {});
        Address account = (Address) FunctionReturnDecoder.decodeIndexedValue(
                topics.get(2), new TypeReference<Address>() {});
        Address sender = (Address) FunctionReturnDecoder.decodeIndexedValue(
                topics.get(3), new TypeReference<Address>() {});

        return new RoleChange(
                log.getBlockNumber(),
                log.getLogIndex(),
                log.getTransactionHash(),
                granted,
                toHex(role.getValue()),
                account.getValue(),
                sender.getValue());
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder("0x");
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Replay the ordered change list into the CURRENT entitlement matrix:
     * roleHash -> set of member addresses. Grant adds, revoke removes —
     * exactly the state machine the contract itself ran.
     */
    public Map<String, Set<String>> buildRoleMatrix(List<RoleChange> changes) {
        Map<String, Set<String>> matrix = new LinkedHashMap<>();
        for (RoleChange change : changes) {
            Set<String> members = matrix.computeIfAbsent(
                    change.roleHash, k -> new TreeSet<>());
            if (change.granted) {
                members.add(change.account.toLowerCase());
            } else {
                members.remove(change.account.toLowerCase());
            }
        }
        return matrix;
    }

    /**
     * Export the role matrix as CSV for the IAM recertification campaign.
     * Columns: roleName,roleHash,memberAddress,exportedAt
     */
    public void exportForRecertification(Map<String, Set<String>> matrix, Path csvFile)
            throws IOException {
        String exportedAt = Instant.now().toString();
        try (PrintWriter out = new PrintWriter(
                Files.newBufferedWriter(csvFile, StandardCharsets.UTF_8))) {
            out.println("roleName,roleHash,memberAddress,exportedAt");
            for (Map.Entry<String, Set<String>> entry : matrix.entrySet()) {
                String roleName = KNOWN_ROLES.getOrDefault(entry.getKey(),
                        "UNKNOWN(" + entry.getKey() + ")");
                for (String member : entry.getValue()) {
                    out.printf("%s,%s,%s,%s%n",
                            roleName, entry.getKey(), member, exportedAt);
                }
            }
        }
    }

    /**
     * Every change to DEFAULT_ADMIN_ROLE is a critical IAM event:
     * the admin role administers every other role.
     */
    public List<RoleChange> defaultAdminAlerts(List<RoleChange> changes) {
        List<RoleChange> alerts = new ArrayList<>();
        for (RoleChange change : changes) {
            if (DEFAULT_ADMIN_ROLE.equals(change.roleHash)) {
                alerts.add(change);
            }
        }
        return alerts;
    }

    // ------------------------------------------------------------------
    // Demo entry point:
    //   java RoleAuditTrail <jsonRpcUrl> <contractAddress> <fromBlock>
    // ------------------------------------------------------------------
    public static void main(String[] args) throws Exception {
        String rpcUrl = args.length > 0 ? args[0] : "http://localhost:8545";
        String contract = args.length > 1 ? args[1]
                : "0x0000000000000000000000000000000000000000";
        BigInteger fromBlock = args.length > 2
                ? new BigInteger(args[2]) : BigInteger.ZERO;

        Web3j web3j = Web3j.build(new HttpService(rpcUrl));
        RoleAuditTrail audit = new RoleAuditTrail(web3j, contract);

        List<RoleChange> changes = audit.fetchRoleChanges(fromBlock);
        System.out.printf("Decoded %d role change(s) since block %s%n",
                changes.size(), fromBlock);

        for (RoleChange change : changes) {
            System.out.printf("  block %s  %-12s %-22s account=%s by=%s tx=%s%n",
                    change.blockNumber,
                    change.granted ? "GRANTED" : "REVOKED",
                    change.roleName(),
                    change.account,
                    change.sender,
                    change.txHash);
        }

        Map<String, Set<String>> matrix = audit.buildRoleMatrix(changes);
        Path csv = Path.of("role-recertification.csv");
        audit.exportForRecertification(matrix, csv);
        System.out.println("Recertification export written to " + csv.toAbsolutePath());

        for (RoleChange alert : audit.defaultAdminAlerts(changes)) {
            System.out.printf(
                    "*** ALERT [SEV-1]: DEFAULT_ADMIN_ROLE %s for %s by %s in tx %s ***%n",
                    alert.granted ? "GRANTED" : "REVOKED",
                    alert.account, alert.sender, alert.txHash);
        }

        web3j.shutdown();
    }
}
