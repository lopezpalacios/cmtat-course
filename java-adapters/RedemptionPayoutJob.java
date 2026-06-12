package ch.bank.cmtat.adapters;

import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

/**
 * RedemptionPayoutJob — Chapter 18 bank-side adapter.
 *
 * Executes MMF redemption payouts with the off-chain CHF leg, honoring liquidity gates and
 * redemption suspension. Demonstrates:
 *
 *   1. Declaring the web3j {@link Event} mirror of the Solidity event (indexed flags
 *      MUST match the contract or decoding silently mis-slices the data).
 *   2. {@link EthFilter} to listen for new OrderProcessed events.
 *   3. Decoding indexed fields (one 32-byte topic each) vs non-indexed fields
 *      (ABI-packed together in the data section).
 *   4. Handling redemption suspension and liquidity gates.
 *
 * Solidity -> web3j type map used here:
 *   address -> Address  -> String    (0x-hex, 20 bytes)
 *   uint256 -> Uint256  -> BigInteger
 */
public final class RedemptionPayoutJob {

    /** Mirror of: event OrderProcessed(uint256 indexed orderId, address indexed user, uint256 amount, bool isRedemption) */
    @SuppressWarnings("rawtypes")
    public static final Event ORDER_PROCESSED = new Event(
            "OrderProcessed",
            Arrays.<TypeReference<?>>asList(
                    new TypeReference<Uint256>(true) {},   // orderId      (indexed -> topic[1])
                    new TypeReference<Address>(true) {},   // user         (indexed -> topic[2])
                    new TypeReference<Uint256>(false) {},  // amount       (data)
                    new TypeReference<TypeReference.Boolean>() {}     // isRedemption (data)
            ));

    /** topic0 = keccak256("OrderProcessed(uint256,address,uint256,bool)") */
    public static final String ORDER_PROCESSED_TOPIC = EventEncoder.encode(ORDER_PROCESSED);

    private final Web3j web3j;
    private final String contractAddress;
    private final TransactionManager transactionManager;

    public RedemptionPayoutJob(String rpcUrl, String contractAddress, Credentials credentials) {
        this.web3j = Web3j.build(new HttpService(rpcUrl));
        this.contractAddress = contractAddress;
        this.transactionManager = new RawTransactionManager(web3j, credentials);
    }

    /**
     * Listens for OrderProcessed events and processes redemptions.
     */
    public void listenAndProcessRedemptions() throws IOException {
        EthFilter filter = new EthFilter(
                DefaultBlockParameterName.LATEST,
                DefaultBlockParameterName.PENDING,
                contractAddress);
        // topic0 selects the event type; the node does the filtering, not us
        filter.addSingleTopic(ORDER_PROCESSED_TOPIC);

        List<Log> logs = web3j.ethGetLogs(filter).send().getLogs();
        for (Log log : logs) {
            if (!Boolean.TRUE.equals(log.isRemoved())) {
                processRedemption(decode(log));
            }
        }
    }

    /**
     * Decode one raw log into an OrderProcessed event.
     * topics[0] = event signature, topics[1..] = indexed params (one 32-byte word each),
     * data = ABI encoding of all non-indexed params concatenated.
     */
    public void decode(Log log) {
        // --- indexed params: each one is its own topic ---
        Uint256 orderIdRaw = (Uint256) FunctionReturnDecoder.decodeIndexedValue(
                log.getTopics().get(1), new TypeReference<Uint256>() {});
        Address userRaw = (Address) FunctionReturnDecoder.decodeIndexedValue(
                log.getTopics().get(2), new TypeReference<Address>() {});

        // --- non-indexed params: decoded together from the data section, in order ---
        @SuppressWarnings("rawtypes")
        List<Type> data = FunctionReturnDecoder.decode(
                log.getData(), ORDER_PROCESSED.getNonIndexedParameters());
        Uint256 amount = (Uint256) data.get(0);
        boolean isRedemption = ((TypeReference.Boolean) data.get(1)).getValue();

        if (isRedemption) {
            processRedemption(orderIdRaw.getValue(), userRaw.getValue(), amount.getValue());
        }
    }

    /**
     * Processes a redemption order, ensuring suspension and liquidity gate checks.
     */
    public void processRedemption(BigInteger orderId, String user, BigInteger amount) throws IOException {
        // Check for global or custom suspension
        boolean isSuspended = checkSuspension();
        if (isSuspended) {
            System.out.println("Redemptions are suspended.");
            return;
        }

        // Check liquidity gate
        boolean hasLiquidity = checkLiquidity(amount);
        if (!hasLiquidity) {
            System.out.println("Liquidity threshold breached.");
            return;
        }

        // Execute the redemption payout (off-chain CHF leg)
        executePayout(user, amount);

        System.out.println("Redemption processed for order ID: " + orderId + ", user: " + user + ", amount: " + amount);
    }

    /**
     * Checks if redemptions are globally or customarily suspended.
     */
    private boolean checkSuspension() throws IOException {
        // Implement suspension logic here
        return false; // Placeholder
    }

    /**
     * Checks if the liquidity threshold is maintained after a redemption.
     */
    private boolean checkLiquidity(BigInteger amount) throws IOException {
        // Implement liquidity gate logic here
        return true; // Placeholder
    }

    /**
     * Executes the off-chain CHF leg of the redemption payout.
     */
    private void executePayout(String user, BigInteger amount) throws IOException {
        // Implement off-chain payout logic here
        System.out.println("Executing off-chain payout for user: " + user + ", amount: " + amount);
    }

    /**
     * Demo main: listen for redemptions and process them.
     * Usage: RedemptionPayoutJob <rpcUrl> <contractAddress> <privateKey>
     */
    public static void main(String[] args) throws IOException {
        String rpcUrl = args[0];
        String contractAddress = args[1];
        Credentials credentials = Credentials.create(args[2]);

        RedemptionPayoutJob job = new RedemptionPayoutJob(rpcUrl, contractAddress, credentials);
        job.listenAndProcessRedemptions();
    }
}
