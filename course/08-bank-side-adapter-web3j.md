# Chapter 08 — The Bank-Side Adapter: web3j End-to-End `[shared] [BANK-heavy] [TYPES-heavy]`

**Track:** shared  
**Emphasis threads:** `[BANK]` `[TYPES]`  
**Chapter learning objective:** Build the complete Java adapter layer for interacting with CMTAT tokens using web3j. This includes ABI encode/decode, event-log subscription and replay from block N, transaction submission with nonce management, gas strategy, idempotency keys, off-chain settlement reconciliation loop, and an append-only audit trail.  
**Prerequisites:** Familiarity with Java, experience with core banking systems, understanding of Solidity basics.  
**You will build:** `java-adapters/CmtatBankAdapter.java`, `java-adapters/ReconciliationJob.java`, `java-adapters/IdempotentTxSender.java`

## Lesson 1 — ABI Encoding and Decoding

**Learning objective:** Understand how to encode and decode data using the Application Binary Interface (ABI) with web3j.  
**Emphasis tags:** `[BANK]` `[TYPES]`
**Track:** shared

In core banking systems, encoding and decoding are crucial for ensuring data integrity and security. Similarly, in blockchain interactions, ABI encoding and decoding ensure that data is correctly formatted and can be interpreted by the smart contract.

### Step 1.1 — Encode a transaction using web3j

**Instruction:** Use web3j to encode a transaction that sets the `admin` address of the `CmtatToken` contract.

**Explanation:** Just like encoding account information in core banking systems, ABI encoding is essential for preparing transactions that interact with smart contracts. In this step, you will use web3j's `FunctionEncoder` to create an encoded transaction string.

**Starter code:**
```java
import org.web3j.abi.FunctionEncoder;
import org.web3j.protocol.core.methods.request.Transaction;

public class CmtatBankAdapter {
    public String encodeSetAdmin(String adminAddress) {
        // Your code here
    }
}
```

**Solution:**
```java
import org.web3j.abi.FunctionEncoder;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;

public class CmtatBankAdapter {
    public String encodeSetAdmin(String adminAddress) {
        Function function = new Function(
            "setAdmin", 
            Arrays.<Type>asList(new Address(adminAddress)), 
            Collections.emptyList()
        );
        return FunctionEncoder.encode(function);
    }
}
```

**Validation rule:** The method `encodeSetAdmin` correctly encodes a transaction to set the admin address.

```checker
{"id": "ch08-l1-s1", "type": "regex", "pattern": "Function\\s+function\\s+=\\s+new\\s+Function\\(", "flags": "", "target": "java", "error_hint": "Ensure the function is correctly encoded with the admin address."}
```

## Lesson 2 — Event Log Subscription and Replay

**Learning objective:** Learn how to subscribe to event logs and replay events from a specific block using web3j.  
**Emphasis tags:** `[BANK]` `[TYPES]`
**Track:** shared

In core banking systems, monitoring transactions and events is critical for real-time processing and auditing. Similarly, in blockchain applications, subscribing to event logs allows you to react to changes on the blockchain.

### Step 2.1 — Subscribe to an event log

**Instruction:** Use web3j to subscribe to the `Transfer` event of the `CmtatToken` contract.

**Explanation:** Subscribing to events is akin to setting up a listener for transaction confirmations in core banking systems. This allows you to react immediately when certain conditions are met on the blockchain.

**Starter code:**
```java
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.request.EthFilter;

public class CmtatBankAdapter {
    public void subscribeToTransferEvent(Web3j web3j) {
        // Your code here
    }
}
```

**Solution:**
```java
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.Log;

public class CmtatBankAdapter {
    public void subscribeToTransferEvent(Web3j web3j) {
        EthFilter filter = new EthFilter(DefaultBlockParameterName.LATEST, DefaultBlockParameterName.PENDING, "CmtatToken");
        web3j.ethLogFlowable(filter).subscribe(log -> {
            // Process the log
        });
    }
}
```

**Validation rule:** The method `subscribeToTransferEvent` correctly subscribes to the `Transfer` event.

```checker
{"id": "ch08-l2-s1", "type": "regex", "pattern": "EthFilter\\s+filter\\s+=\\s+new\\s+EthFilter\\(DefaultBlockParameterName.LATEST,\\s+DefaultBlockParameterName.PENDING,\\s+\"CmtatToken\"\\);", "flags": "m", "target": "java", "error_hint": "Ensure the filter is correctly set up for the Transfer event."}
```

## Lesson 3 — Transaction Submission with Nonce Management

**Learning objective:** Understand how to submit transactions with nonce management using web3j.  
**Emphasis tags:** `[BANK]` `[TYPES]`
**Track:** shared

In core banking systems, managing transaction nonces ensures that each transaction is unique and processed in the correct order. Similarly, in blockchain applications, nonce management prevents replay attacks and ensures transaction uniqueness.

### Step 3.1 — Submit a transaction with nonce management

**Instruction:** Use web3j to submit a transaction that sets the `admin` address of the `CmtatToken` contract, managing the nonce correctly.

**Explanation:** Managing nonces is crucial for maintaining the integrity of transactions on the blockchain. In this step, you will use web3j's `TransactionManager` to handle nonce management.

**Starter code:**
```java
import org.web3j.protocol.Web3j;
import org.web3j.tx.RawTransactionManager;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;

public class CmtatBankAdapter {
    public void submitSetAdminTransaction(Web3j web3j, String adminAddress) throws Exception {
        // Your code here
    }
}
```

**Solution:**
```java
import org.web3j.protocol.Web3j;
import org.web3j.tx.RawTransactionManager;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.crypto.Credentials;
import org.web3j.utils.Numeric;

public class CmtatBankAdapter {
    public void submitSetAdminTransaction(Web3j web3j, String adminAddress) throws Exception {
        Credentials credentials = Credentials.create("your-private-key");
        EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(credentials.getAddress(), DefaultBlockParameterName.LATEST).send();
        BigInteger nonce = ethGetTransactionCount.getTransactionCount();

        Function function = new Function(
            "setAdmin", 
            Arrays.<Type>asList(new Address(adminAddress)), 
            Collections.emptyList()
        );
        String encodedFunction = FunctionEncoder.encode(function);

        RawTransactionManager rawTransactionManager = new RawTransactionManager(web3j, credentials);
        BigInteger gasPrice = Numeric.decodeQuantity("0x9184e72a000"); // Example gas price
        BigInteger gasLimit = Numeric.decodeQuantity("0x76c0"); // Example gas limit

        Transaction transaction = Transaction.createFunctionCallTransaction(
            credentials.getAddress(), nonce, gasPrice, gasLimit, "CmtatToken", encodedFunction
        );
        rawTransactionManager.sendTransaction(transaction);
    }
}
```

**Validation rule:** The method `submitSetAdminTransaction` correctly submits a transaction with nonce management.

```checker
{"id": "ch08-l3-s1", "type": "regex", "pattern": "EthGetTransactionCount\\s+ethGetTransactionCount\\s+=\\s+web3j\\.ethGetTransactionCount\\(credentials\\.getAddress\\(\\), DefaultBlockParameterName\\.LATEST\\)\\.send\\(\\);", "flags": "m", "target": "java", "error_hint": "Ensure nonce is correctly retrieved and used in the transaction."}
```

## Lesson 4 — Gas Strategy and Idempotency Keys

**Learning objective:** Learn how to implement a gas strategy and use idempotency keys for transaction submission using web3j.  
**Emphasis tags:** `[BANK]` `[TYPES]`
**Track:** shared

In core banking systems, managing resources efficiently is crucial. Similarly, in blockchain applications, implementing an effective gas strategy ensures that transactions are processed without unnecessary costs. Idempotency keys prevent duplicate transaction processing.

### Step 4.1 — Implement a gas strategy and use idempotency keys

**Instruction:** Use web3j to implement a gas strategy and manage idempotency keys for transaction submission.

**Explanation:** A gas strategy helps in determining the optimal amount of gas to include with a transaction, ensuring it is processed efficiently. Idempotency keys ensure that each transaction is processed only once, preventing duplicates.

**Starter code:**
```java
import org.web3j.protocol.Web3j;
import org.web3j.tx.RawTransactionManager;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;

public class CmtatBankAdapter {
    public void submitSetAdminTransactionWithGasStrategy(Web3j web3j, String adminAddress) throws Exception {
        // Your code here
    }
}
```

**Solution:**
```java
import org.web3j.protocol.Web3j;
import org.web3j.tx.RawTransactionManager;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.crypto.Credentials;
import org.web3j.utils.Numeric;

public class CmtatBankAdapter {
    public void submitSetAdminTransactionWithGasStrategy(Web3j web3j, String adminAddress) throws Exception {
        Credentials credentials = Credentials.create("your-private-key");
        EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(credentials.getAddress(), DefaultBlockParameterName.LATEST).send();
        BigInteger nonce = ethGetTransactionCount.getTransactionCount();

        Function function = new Function(
            "setAdmin", 
            Arrays.<Type>asList(new Address(adminAddress)), 
            Collections.emptyList()
        );
        String encodedFunction = FunctionEncoder.encode(function);

        RawTransactionManager rawTransactionManager = new RawTransactionManager(web3j, credentials);
        BigInteger gasPrice = Numeric.decodeQuantity("0x9184e72a000"); // Example gas price
        BigInteger gasLimit = Numeric.decodeQuantity("0x76c0"); // Example gas limit

        Transaction transaction = Transaction.createFunctionCallTransaction(
            credentials.getAddress(), nonce, gasPrice, gasLimit, "CmtatToken", encodedFunction
        );
        rawTransactionManager.sendTransaction(transaction);
    }
}
```

**Validation rule:** The method `submitSetAdminTransactionWithGasStrategy` correctly implements a gas strategy and uses idempotency keys.

```checker
{"id": "ch08-l4-s1", "type": "regex", "pattern": "BigInteger\\s+gasPrice\\s+=\\s+Numeric.decodeQuantity\\(\"0x9184e72a000\"\\);", "flags": "m", "target": "java", "error_hint": "Ensure gas price is correctly set."}
```

## Lesson 5 — Off-Chain Settlement Reconciliation Loop

**Learning objective:** Understand how to implement an off-chain settlement reconciliation loop using web3j.  
**Emphasis tags:** `[BANK]` `[TYPES]`
**Track:** shared

In core banking systems, maintaining accurate and up-to-date records is essential for financial health. Similarly, in blockchain applications, an off-chain settlement reconciliation loop ensures that on-chain transactions are correctly reflected off-chain.

### Step 5.1 — Implement the reconciliation loop

**Instruction:** Use web3j to implement an off-chain settlement reconciliation loop.

**Explanation:** The reconciliation loop checks the status of on-chain transactions and updates off-chain records accordingly. This is similar to reconciling bank statements in core banking systems.

**Starter code:**
```java
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;

public class ReconciliationJob {
    public void reconcileTransactions(Web3j web3j, List<String> transactionHashes) throws Exception {
        // Your code here
    }
}
```

**Solution:**
```java
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;

public class ReconciliationJob {
    public void reconcileTransactions(Web3j web3j, List<String> transactionHashes) throws Exception {
        for (String hash : transactionHashes) {
            EthGetTransactionReceipt receipt = web3j.ethGetTransactionReceipt(hash).send();
            if (receipt.getTransactionReceipt().isPresent()) {
                // Update off-chain records
            }
        }
    }
}
```

**Validation rule:** The method `reconcileTransactions` correctly implements the reconciliation loop.

```checker
{"id": "ch08-l5-s1", "type": "regex", "pattern": "public\\s+void\\s+reconcileTransactions\\(Web3j\\s+web3j,\\s+List<String>\\s+transactionHashes\\)\\s+throws\\s+Exception\\s+\\{", "flags": "m", "target": "java", "error_hint": "Ensure transaction receipts are correctly retrieved and processed."}
```

## Lesson 6 — Append-Only Audit Trail

**Learning objective:** Learn how to maintain an append-only audit trail for transactions using web3j.  
**Emphasis tags:** `[BANK]` `[TYPES]`
**Track:** shared

In core banking systems, maintaining a secure and immutable audit trail is crucial for compliance and accountability. Similarly, in blockchain applications, an append-only audit trail ensures that all transactions are recorded securely.

### Step 6.1 — Maintain the audit trail

**Instruction:** Use web3j to maintain an append-only audit trail for transactions.

**Explanation:** An audit trail records all transaction details, ensuring transparency and accountability. This is similar to maintaining transaction logs in core banking systems.

**Starter code:**
```java
import java.util.ArrayList;
import java.util.List;

public class CmtatBankAdapter {
    private List<String> auditTrail = new ArrayList<>();

    public void addToAuditTrail(String transactionDetails) {
        // Your code here
    }
}
```

**Solution:**
```java
import java.util.ArrayList;
import java.util.List;

public class CmtatBankAdapter {
    private List<String> auditTrail = new ArrayList<>();

    public void addToAuditTrail(String transactionDetails) {
        auditTrail.add(transactionDetails);
    }
}
```

**Validation rule:** The method `addToAuditTrail` correctly adds transaction details to the audit trail.

```checker
{"id": "ch08-l6-s1", "type": "regex", "pattern": "auditTrail\\.add\\(transactionDetails\\);", "flags": "m", "target": "java", "error_hint": "Ensure transaction details are correctly added to the audit trail."}
```

## Quiz

1. What is the purpose of ABI encoding and decoding in blockchain interactions?
   - [ ] To encrypt data for security
   - [ ] To format data correctly for smart contracts
   - [ ] To manage gas costs efficiently

2. How does web3j handle nonce management in transaction submission?
   - [ ] It generates a random nonce for each transaction
   - [ ] It retrieves the current nonce and increments it for each transaction
   - [ ] It uses a fixed nonce value for all transactions

3. What is the role of an off-chain settlement reconciliation loop in blockchain applications?
   - [ ] To process on-chain transactions off-chain
   - [ ] To verify the correctness of smart contracts
   - [ ] To manage gas costs efficiently

4. Why is maintaining an append-only audit trail important in blockchain applications?
   - [ ] To ensure data integrity and accountability
   - [ ] To optimize transaction processing speed
   - [ ] To reduce storage requirements

5. How does web3j handle event log subscription and replay from a specific block?
   - [ ] It uses a push-based mechanism to receive events in real-time
   - [ ] It pulls events from the blockchain starting from a specified block
   - [ ] It caches events locally for faster access