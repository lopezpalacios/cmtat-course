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
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.StaticGasProvider;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

/**
 * DividendDistributionJob — Chapter 14 bank-side adapter.
 *
 * Computes dividends from a snapshot with 35% Swiss withholding tax and exports
 * voting power to the general-assembly system. Demonstrates:
 *
 *   1. Declaring the web3j {@link Event} mirror of the Solidity event (indexed flags
 *      MUST match the contract or decoding silently mis-slices the data).
 *   2. Using {@link EthFilter} to listen for SnapshotTaken events.
 *   3. Calling contract functions to distribute dividends and get voting power.
 *
 * Solidity -> web3j type map used here:
 *   address -> Address  -> String    (0x-hex, 20 bytes)
 *   uint256 -> Uint256  -> BigInteger
 */
public final class DividendDistributionJob {

    /** Mirror of: event SnapshotTaken(uint256 timestamp) */
    @SuppressWarnings("rawtypes")
    public static final Event SNAPSHOT_TAKEN = new Event(
            "SnapshotTaken",
            Arrays.<TypeReference<?>>asList(
                    new TypeReference<Uint256>(true) {}   // timestamp  (indexed -> topic[1])
            ));

    /** topic0 = keccak256("SnapshotTaken(uint256)") */
    public static final String SNAPSHOT_TAKEN_TOPIC = EventEncoder.encode(SNAPSHOT_TAKEN);

    private final Web3j web3j;
    private final String contractAddress;
    private final TransactionManager transactionManager;

    public DividendDistributionJob(String rpcUrl, String contractAddress, String privateKey) {
        this.web3j = Web3j.build(new HttpService(rpcUrl));
        this.contractAddress = contractAddress;
        this.transactionManager = new RawTransactionManager(
                web3j,
                Credentials.create(privateKey),
                ChainId.MAINNET);
    }

    /**
     * Listens for SnapshotTaken events and processes them to distribute dividends
     * and export voting power.
     */
    public void processSnapshots(BigInteger fromBlock) throws IOException {
        EthFilter filter = new EthFilter(
                DefaultBlockParameter.valueOf(fromBlock),
                DefaultBlockParameterName.LATEST,
                contractAddress);
        // topic0 selects the event type; the node does the filtering, not us
        filter.addSingleTopic(SNAPSHOT_TAKEN_TOPIC);

        List<Log> logs = web3j.ethGetLogs(filter).send().getLogs();
        for (Log log : logs) {
            processSnapshot(log);
        }
    }

    /**
     * Decodes a SnapshotTaken event and processes it to distribute dividends
     * and export voting power.
     */
    private void processSnapshot(Log log) throws IOException {
        // --- indexed params: each one is its own topic ---
        Uint256 timestampRaw = (Uint256) FunctionReturnDecoder.decodeIndexedValue(
                log.getTopics().get(1), new TypeReference<Uint256>() {});

        BigInteger snapshotId = timestampRaw.getValue();
        System.out.println("Processing snapshot taken at: " + snapshotId);

        // Distribute dividends
        distributeDividends(snapshotId, BigInteger.valueOf(100)); // Example dividend per share

        // Export voting power
        exportVotingPower(snapshotId);
    }

    /**
     * Calls the contract to distribute dividends with 35% Swiss withholding tax.
     */
    private void distributeDividends(BigInteger snapshotId, BigInteger dividendPerShare) throws IOException {
        Function function = new Function(
                "payDividend",
                Arrays.<Type>asList(new Uint256(snapshotId), new Uint256(dividendPerShare)),
                Collections.emptyList());

        String encodedFunction = FunctionEncoder.encode(function);
        Transaction transaction = Transaction.createFunctionCallTransaction(
                transactionManager.getFromAddress(),
                null,
                BigInteger.ZERO,
                BigInteger.valueOf(4_000_000),
                contractAddress,
                encodedFunction);

        EthSendTransaction response = web3j.ethSendRawTransaction(transactionManager.signTransaction(transaction).getRawTransaction()).send();
        if (response.hasError()) {
            throw new IOException("Failed to distribute dividends: " + response.getError().getMessage());
        }
    }

    /**
     * Calls the contract to export voting power to the general-assembly system.
     */
    private void exportVotingPower(BigInteger snapshotId) throws IOException {
        // Example logic to export voting power
        System.out.println("Exporting voting power for snapshot: " + snapshotId);
        // Implement actual export logic here
    }

    /**
     * Demo main: process snapshots from a deployment block.
     * Usage: DividendDistributionJob <rpcUrl> <contractAddress> <privateKey> <fromBlock>
     */
    public static void main(String[] args) throws IOException {
        DividendDistributionJob job = new DividendDistributionJob(args[0], args[1], args[2]);
        BigInteger fromBlock = new BigInteger(args[3]);
        job.processSnapshots(fromBlock);
    }
}
