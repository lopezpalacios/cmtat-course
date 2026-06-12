package ch.bank.cmtat.adapters;

import org.web3j.abi.Function;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicStruct;
import org.web3j.abi.datatypes.StaticArray;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

/**
 * BondMetadataReader — Chapter 10 bank-side adapter.
 *
 * Reads and decodes the bond DebtInfo struct (interestRate bps, parValue,
 * maturityDate, ISIN bytes32) on the bank side. Demonstrates:
 *
 *   1. Declaring the web3j {@link Function} mirror of the Solidity function calls
 *      to fetch individual fields of the DebtInfo struct.
 *   2. Using {@link RawTransactionManager} for transaction management, though
 *      this adapter only reads data and does not send transactions.
 *   3. Decoding complex Solidity structs into Java objects using web3j's type system.
 *
 * Solidity -> web3j type map used here:
 *   bytes32 -> Bytes32  -> byte[32]  (right-padded ASCII for ISIN)
 *   address -> Address  -> String    (0x-hex, 20 bytes)
 *   uint256 -> Uint256  -> BigInteger
 *
 */
public final class BondMetadataReader {

    private final Web3j web3j;
    private final String contractAddress;
    private final RawTransactionManager transactionManager;

    public BondMetadataReader(String rpcUrl, String contractAddress, String fromAddress) {
        this.web3j = Web3j.build(new HttpService(rpcUrl));
        this.contractAddress = contractAddress;
        this.transactionManager = new RawTransactionManager(
                web3j,
                fromAddress,
                BigInteger.valueOf(1000), // gas price
                BigInteger.valueOf(4_300_000), // gas limit
                30, // attempts
                1000); // sleep duration between attempts
    }

    /**
     * Immutable bond metadata entry decoded from the contract — the unit handed to core banking.
     */
    public static final class BondMetadata {
        public final BigInteger interestRate; // in basis points
        public final BigInteger parValue;
        public final BigInteger maturityDate; // timestamp
        public final Bytes32 ISIN;

        BondMetadata(BigInteger interestRate, BigInteger parValue, BigInteger maturityDate, Bytes32 ISIN) {
            this.interestRate = interestRate;
            this.parValue = parValue;
            this.maturityDate = maturityDate;
            this.ISIN = ISIN;
        }

        @Override
        public String toString() {
            return String.format("BondMetadata[interestRate=%s bps, parValue=%s, maturityDate=%s, ISIN=%s]",
                    interestRate, parValue, maturityDate, bytes32ToAscii(ISIN.getValue()));
        }
    }

    /**
     * Reads and decodes the bond metadata from the contract.
     */
    public BondMetadata readBondMetadata() throws IOException {
        Function getInterestRate = new Function(
                "getInterestRate",
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        Function getParValue = new Function(
                "getParValue",
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        Function getMaturityDate = new Function(
                "getMaturityDate",
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        Function getISIN = new Function(
                "getISIN",
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));

        BigInteger interestRate = (BigInteger) executeFunction(getInterestRate);
        BigInteger parValue = (BigInteger) executeFunction(getParValue);
        BigInteger maturityDate = (BigInteger) executeFunction(getMaturityDate);
        Bytes32 ISIN = (Bytes32) executeFunction(getISIN);

        return new BondMetadata(interestRate, parValue, maturityDate, ISIN);
    }

    /**
     * Executes a function call on the contract and returns the result.
     */
    private Object executeFunction(Function function) throws IOException {
        Transaction transaction = Transaction.createEthCallTransaction(
                transactionManager.getFromAddress(),
                contractAddress,
                org.web3j.abi.FunctionEncoder.encode(function));
        String responseValue = web3j.ethCall(transaction, DefaultBlockParameterName.LATEST).send().getValue();
        List<Type> results = FunctionReturnDecoder.decode(responseValue, function.getOutputParameters());
        return results.get(0).getValue();
    }

    /** Right-padded ASCII bytes32 (ISIN style) -> trimmed Java String. */
    public static String bytes32ToAscii(byte[] value) {
        int len = value.length;
        while (len > 0 && value[len - 1] == 0) {
            len--;
        }
        return new String(value, 0, len, StandardCharsets.US_ASCII);
    }

    /**
     * Demo main: read bond metadata and print it.
     * Usage: BondMetadataReader <rpcUrl> <contractAddress> <fromAddress>
     */
    public static void main(String[] args) throws IOException {
        BondMetadataReader reader = new BondMetadataReader(args[0], args[1], args[2]);
        BondMetadata metadata = reader.readBondMetadata();
        System.out.println(metadata);
    }
}
