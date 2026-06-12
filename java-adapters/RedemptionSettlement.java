package ch.bank.cmtat.adapters;

import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.generated.Bytes32;
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
 * RedemptionSettlement — Chapter 12 bank-side adapter.
 *
 * Drives bond redemption: burn-against-payment at maturity with the off-chain CHF
 * cash leg and settlement reconciliation. Demonstrates:
 *
 *   1. Declaring the web3j {@link Event} mirror of the Solidity event (indexed flags
 *      MUST match the contract or decoding silently mis-slices the data).
 *   2. {@link EthFilter} replay from a given block — the backfill path, equivalent to
 *      re-reading a payment-message archive after an outage.
 *   3. Decoding indexed fields (one 32-byte topic each) vs non-indexed fields
 *      (ABI-packed together in the data section).
 *   4. Reorg safety: only logs at depth >= CONFIRMATION_DEPTH are booked; everything
 *      shallower is provisional, like an unconfirmed SWIFT message.
 *
 * Solidity -> web3j type map used here:
 *   bytes32 -> Bytes32  -> byte[32]  (right-padded ASCII for booking refs)
 *   address -> Address  -> String    (0x-hex, 20 bytes)
 *   uint256 -> Uint256  -> BigInteger
 *
 */
public final class RedemptionSettlement {

    /** Mirror of: event CouponDeclared(address indexed holder, uint256 snapshotIndex, uint256 amount) */
    @SuppressWarnings("rawtypes")
    public static final Event COUPON_DECLARED = new Event(
            "CouponDeclared",
            Arrays.<TypeReference<?>>asList(
                    new TypeReference<Address>(true) {},   // holder       (indexed -> topic[1])
                    new TypeReference<Uint256>(false) {},  // snapshotIndex (data)
                    new TypeReference<Uint256>(false) {}   // amount        (data)
            ));

    /** Mirror of: event CouponPaid(address indexed holder, uint256 snapshotIndex, uint256 amount) */
    @SuppressWarnings("rawtypes")
    public static final Event COUPON_PAID = new Event(
            "CouponPaid",
            Arrays.<TypeReference<?>>asList(
                    new TypeReference<Address>(true) {},   // holder       (indexed -> topic[1])
                    new TypeReference<Uint256>(false) {},  // snapshotIndex (data)
                    new TypeReference<Uint256>(false) {}   // amount        (data)
            ));

    /** Mirror of: event BondRedeemed(address indexed holder) */
    @SuppressWarnings("rawtypes")
    public static final Event BOND_REDEEMED = new Event(
            "BondRedeemed",
            Arrays.<TypeReference<?>>asList(
                    new TypeReference<Address>(true) {}   // holder       (indexed -> topic[1])
            ));

    /** topic0 for CouponDeclared */
    public static final String COUPON_DECLARED_TOPIC = EventEncoder.encode(COUPON_DECLARED);

    /** topic0 for CouponPaid */
    public static final String COUPON_PAID_TOPIC = EventEncoder.encode(COUPON_PAID);

    /** topic0 for BondRedeemed */
    public static final String BOND_REDEEMED_TOPIC = EventEncoder.encode(BOND_REDEEMED);

    /** Blocks below the chain head we treat as final. Tune per chain (see Chapter 08). */
    public static final BigInteger CONFIRMATION_DEPTH = BigInteger.valueOf(12);

    private final Web3j web3j;
    private final String contractAddress;
    private final TransactionManager transactionManager;

    public RedemptionSettlement(String rpcUrl, String contractAddress, String privateKey) {
        this.web3j = Web3j.build(new HttpService(rpcUrl));
        this.contractAddress = contractAddress;
        this.transactionManager = new RawTransactionManager(
                web3j,
                Credentials.create(privateKey),
                ChainId.MAINNET.id);
    }

    /**
     * Replay CouponDeclared and CouponPaid logs from {@code fromBlock} up to the reorg-safe head,
     * in bounded chunks. This is the backfill path: run it on adapter start-up with
     * the last booked block from the bank's own store, and you recover every entry
     * missed during downtime — events are replayable forever from any archive node.
     */
    public List<Log> replayFrom(BigInteger fromBlock) throws IOException {
        BigInteger safeHead = safeHead();
        List<Log> logs = new ArrayList<>();
        BigInteger start = fromBlock;
        while (start.compareTo(safeHead) <= 0) {
            BigInteger end = start.add(CONFIRMATION_DEPTH).min(safeHead);
            logs.addAll(fetchRange(start, end));
            start = end.add(BigInteger.ONE);
        }
        return logs;
    }

    /** Chain head minus CONFIRMATION_DEPTH — logs above this line may still reorg away. */
    public BigInteger safeHead() throws IOException {
        BigInteger head = web3j.ethBlockNumber().send().getBlockNumber();
        BigInteger safe = head.subtract(CONFIRMATION_DEPTH);
        return safe.signum() < 0 ? BigInteger.ZERO : safe;
    }

    /** One eth_getLogs call: filter = address + topic0, range [fromBlock, toBlock]. */
    private List<Log> fetchRange(BigInteger fromBlock, BigInteger toBlock) throws IOException {
        EthFilter filter = new EthFilter(
                DefaultBlockParameter.valueOf(fromBlock),
                DefaultBlockParameter.valueOf(toBlock),
                contractAddress);
        // topic0 selects the event type; the node does the filtering, not us
        filter.addSingleTopic(COUPON_DECLARED_TOPIC);
        filter.addSingleTopic(COUPON_PAID_TOPIC);

        org.web3j.protocol.core.methods.response.EthLog response = web3j.ethGetLogs(filter).send();
        if (response.hasError()) {
            throw new IOException("eth_getLogs failed: " + response.getError().getMessage());
        }

        return response.getLogs();
    }

    /**
     * Redeem the bond for a given holder.
     *
     * @param holder The address of the bond holder.
     */
    public TransactionReceipt redeemBond(String holder) throws Exception {
        Function function = new Function(
                "redeemBond",
                Arrays.<Type>asList(new Address(holder)),
                Collections.emptyList());

        BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();
        BigInteger nonce = transactionManager.getNonce();

        RawTransaction rawTransaction = RawTransaction.createFunctionCallTransaction(
                nonce,
                gasPrice,
                BigInteger.valueOf(4_000_000), // gas limit
                contractAddress,
                BigInteger.ZERO, // value
                function.encode());

        return transactionManager.sendTransaction(rawTransaction).send();
    }

    /**
     * Decode one raw log into a CouponDeclared or CouponPaid event.
     * topics[0] = event signature, topics[1..] = indexed params (one 32-byte word each),
     * data = ABI encoding of all non-indexed params concatenated.
     */
    public static Event decode(Log log) {
        String topic = log.getTopics().get(0);
        if (topic.equals(COUPON_DECLARED_TOPIC)) {
            return decodeEvent(log, COUPON_DECLARED);
        } else if (topic.equals(COUPON_PAID_TOPIC)) {
            return decodeEvent(log, COUPON_PAID);
        }
        return null;
    }

    private static Event decodeEvent(Log log, Event event) {
        // --- indexed params: each one is its own topic ---
        Address holderRaw = (Address) FunctionReturnDecoder.decodeIndexedValue(
                log.getTopics().get(1), new TypeReference<Address>() {});

        // --- non-indexed params: decoded together from the data section, in order ---
        @SuppressWarnings("rawtypes")
        List<Type> data = FunctionReturnDecoder.decode(
                log.getData(), event.getNonIndexedParameters());
        BigInteger snapshotIndex = ((Uint256) data.get(0)).getValue();
        BigInteger amount = ((Uint256) data.get(1)).getValue();

        return new Event(event.getName(),
                Arrays.asList(holderRaw),
                Arrays.asList(snapshotIndex, amount));
    }

    /**
     * Demo main: backfill from a deployment block and print the feed.
     * Usage: RedemptionSettlement <rpcUrl> <contractAddress> <privateKey> <fromBlock>
     */
    public static void main(String[] args) throws Exception {
        RedemptionSettlement redemptionSettlement = new RedemptionSettlement(args[0], args[1], args[2]);
        BigInteger fromBlock = new BigInteger(args[3]);

        List<Log> logs = redemptionSettlement.replayFrom(fromBlock);
        for (Log log : logs) {
            Event event = decode(log);
            if (event != null) {
                System.out.println(event.getName() + ": " + event.getNonIndexedParameters());
            }
        }

        // Example of redeeming a bond
        String holderAddress = "0x1234567890123456789012345678901234567890";
        TransactionReceipt receipt = redemptionSettlement.redeemBond(holderAddress);
        System.out.println("Redemption transaction hash: " + receipt.getTransactionHash());
    }
}
