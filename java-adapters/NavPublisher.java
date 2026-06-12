package ch.bank.cmtat.adapters;

import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;
import org.web3j.tx.response.TransactionReceiptProcessor;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * NavPublisher — Chapter 16 bank-side adapter.
 *
 * Publishes the daily struck NAV (6-decimal fixed point) on-chain from the fund-accounting
 * system, with four-eyes control. Demonstrates:
 *
 *   1. Declaring the web3j {@link Event} mirror of the Solidity event (indexed flags
 *      MUST match the contract or decoding silently mis-slices the data).
 *   2. Preparing and sending a transaction to update NAV using {@link RawTransactionManager}.
 *   3. Handling the four-eyes control by requiring two separate transactions.
 *
 * Solidity -> web3j type map used here:
 *   address -> Address  -> String    (0x-hex, 20 bytes)
 *   uint256 -> Uint256  -> BigInteger
 */
public final class NavPublisher {

    /** Mirror of: event NAVUpdated(uint256 newNAV) */
    @SuppressWarnings("rawtypes")
    public static final Event NAV_UPDATED = new Event(
            "NAVUpdated",
            Arrays.<TypeReference<?>>asList(
                    new TypeReference<Uint256>(false) {}  // newNAV (data)
            ));

    /** topic0 = keccak256("NAVUpdated(uint256)") */
    public static final String NAV_UPDATED_TOPIC = EventEncoder.encode(NAV_UPDATED);

    private final Web3j web3j;
    private final String contractAddress;
    private final Credentials credentials;

    public NavPublisher(String rpcUrl, String contractAddress, Credentials credentials) {
        this.web3j = Web3j.build(new HttpService(rpcUrl));
        this.contractAddress = contractAddress;
        this.credentials = credentials;
    }

    /**
     * Publish the NAV on-chain using a transaction.
     *
     * @param newNAV The new NAV value as a BigInteger (6-decimal fixed point).
     * @throws IOException If there is an issue with sending the transaction.
     */
    public void publishNav(BigInteger newNAV) throws IOException {
        // Prepare the function call
        org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                "updateNAV",
                Arrays.<Type>asList(new Uint256(newNAV)),
                Collections.emptyList());

        // Get the nonce for the account
        BigInteger nonce = web3j.ethGetTransactionCount(credentials.getAddress(), DefaultBlockParameterName.LATEST)
                .send().getTransactionCount();

        // Estimate gas
        BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();
        BigInteger gasLimit = BigInteger.valueOf(200_000); // Adjust as needed

        // Create the raw transaction
        Transaction transaction = Transaction.createFunctionCallTransaction(
                credentials.getAddress(), nonce, gasPrice, gasLimit,
                contractAddress, function.encode());

        // Send the transaction
        RawTransactionManager rawTransactionManager = new RawTransactionManager(web3j, credentials);
        TransactionReceiptProcessor receiptProcessor = new PollingTransactionReceiptProcessor(web3j, 10, 30_000);
        rawTransactionManager.sendTransaction(transaction, receiptProcessor).send();
    }

    /**
     * Main method for demonstration purposes.
     *
     * Usage: NavPublisher <rpcUrl> <contractAddress> <privateKey> <newNAV>
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 4) {
            System.out.println("Usage: NavPublisher <rpcUrl> <contractAddress> <privateKey> <newNAV>");
            return;
        }

        String rpcUrl = args[0];
        String contractAddress = args[1];
        Credentials credentials = Credentials.create(args[2]);
        BigInteger newNAV = new BigInteger(args[3]);

        NavPublisher navPublisher = new NavPublisher(rpcUrl, contractAddress, credentials);
        navPublisher.publishNav(newNAV);

        System.out.println("NAV published successfully.");
    }
}
