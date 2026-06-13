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

### Step 1.2 — Decode a transaction using web3j

**Instruction:** Use web3j to decode an encoded transaction string back into its original components.

**Explanation:** Decoding is the reverse process of encoding, ensuring that data can be retrieved and processed correctly after being sent to the blockchain. This step will use `FunctionDecoder` to parse the encoded transaction string.

**Starter code:**
```java
import org.web3j.abi.FunctionDecoder;
import org.web3j.protocol.core.methods.request.Transaction;

public class CmtatBankAdapter {
    public Function decodeSetAdmin(String encodedTransaction) {
        // Your code here
    }
}
```

**Solution:**
```java
import org.web3j.abi.FunctionDecoder;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.abi.datatypes.Function;

public class CmtatBankAdapter {
    public Function decodeSetAdmin(String encodedTransaction) {
        return FunctionDecoder.decode(encodedTransaction);
    }
}
```

**Validation rule:** The method `decodeSetAdmin` correctly decodes the transaction string back into a `Function` object.

```checker
{"id": "ch08-l1-s2", "type": "regex", "pattern": "return\\s+FunctionDecoder\\.decode\\(encodedTransaction\\);", "flags": "", "target": "java", "error_hint": "Ensure the encoded transaction is correctly decoded."}
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

### Step 2.2 — Replay events from a specific block

**Instruction:** Use web3j to replay `Transfer` events starting from a specific block number.

**Explanation:** In case of an outage or system restart, it's crucial to replay missed events to ensure that all transactions are processed correctly. This step will use `ethGetLogs` to fetch logs from a specified block range.

**Starter code:**
```java
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.request.EthFilter;

public class CmtatBankAdapter {
    public List<Log> replayTransferEventsFromBlock(Web3j web3j, BigInteger startBlock) throws Exception {
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
    public List<Log> replayTransferEventsFromBlock(Web3j web3j, BigInteger startBlock) throws Exception {
        EthFilter filter = new EthFilter(startBlock, DefaultBlockParameterName.LATEST, "CmtatToken");
        return web3j.ethGetLogs(filter).send().getLogs();
    }
}
```

**Validation rule:** The method `replayTransferEventsFromBlock` correctly fetches logs from the specified block range.

```checker
{"id": "ch08-l2-s2", "type": "regex", "pattern": "EthFilter\\s+filter\\s+=\\s+new\\s+EthFilter\\(startBlock, DefaultBlockParameterName.LATEST, \"CmtatToken\"\\);", "flags": "m", "target": "java", "error_hint": "Ensure the filter is correctly set up to replay events from the specified block."}
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

### Step 3.2 — Handle nonce conflicts

**Instruction:** Implement a mechanism to handle nonce conflicts when submitting transactions.

**Explanation:** In high-frequency trading environments, nonce conflicts can occur if multiple transactions are submitted simultaneously. This step will involve checking for existing nonces and adjusting them accordingly.

**Starter code:**
```java
import org.web3j.protocol.Web3j;
import org.web3j.tx.RawTransactionManager;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;

public class CmtatBankAdapter {
    public void submitSetAdminTransactionWithNonceConflictHandling(Web3j web3j, String adminAddress) throws Exception {
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
    public void submitSetAdminTransactionWithNonceConflictHandling(Web3j web3j, String adminAddress) throws Exception {
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

**Validation rule:** The method `submitSetAdminTransactionWithNonceConflictHandling` correctly handles nonce conflicts.

```checker
{"id": "ch08-l3-s2", "type": "regex", "pattern": "EthGetTransactionCount\\s+ethGetTransactionCount\\s+=\\s+web3j\\.ethGetTransactionCount\\(credentials\\.getAddress\\(\\), DefaultBlockParameterName\\.LATEST\\)\\.send\\(\\);", "flags": "m", "target": "java", "error_hint": "Ensure nonce is correctly retrieved and used in the transaction."}
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

### Step 4.2 — Use idempotency keys to prevent duplicate transactions

**Instruction:** Implement a mechanism to use idempotency keys to ensure that each transaction is processed only once.

**Explanation:** Idempotency keys help in preventing duplicate transactions, which can lead to incorrect state updates on the blockchain. This step will involve storing and checking idempotency keys before submitting transactions.

**Starter code:**
```java
import java.util.HashSet;
import java.util.Set;

public class CmtatBankAdapter {
    private Set<String> processedKeys = new HashSet<>();

    public void submitSetAdminTransactionWithIdempotencyKey(String idempotencyKey, Web3j web3j, String adminAddress) throws Exception {
        // Your code here
    }
}
```

**Solution:**
```java
import java.util.HashSet;
import java.util.Set;

public class CmtatBankAdapter {
    private Set<String> processedKeys = new HashSet<>();

    public void submitSetAdminTransactionWithIdempotencyKey(String idempotencyKey, Web3j web3j, String adminAddress) throws Exception {
        if (processedKeys.contains(idempotencyKey)) {
            throw new RuntimeException("Transaction with this idempotency key has already been processed.");
        }

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

        processedKeys.add(idempotencyKey);
    }
}
```

**Validation rule:** The method `submitSetAdminTransactionWithIdempotencyKey` correctly uses idempotency keys to prevent duplicate transactions.

```checker
{"id": "ch08-l4-s2", "type": "regex", "pattern": "if\\s+\\(processedKeys\\.contains\\(idempotencyKey\\)\\)", "flags": "m", "target": "java", "error_hint": "Ensure idempotency key is checked before processing the transaction."}
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
{"id": "ch08-l5-s1", "type": "regex", "pattern": "EthGetTransactionReceipt\\s+receipt\\s+=\\s+web3j\\.ethGetTransactionReceipt\\(hash\\)\\.send\\(\\);", "flags": "m", "target": "java", "error_hint": "Ensure transaction receipts are correctly retrieved and processed."}
```

### Step 5.2 — Handle reorgs in the reconciliation loop

**Instruction:** Implement a mechanism to handle chain reorganizations (reorgs) during the reconciliation process.

**Explanation:** Chain reorganizations can cause transactions to be reverted, which must be accounted for in the off-chain records. This step will involve checking for reorgs and updating records accordingly.

**Starter code:**
```java
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;

public class ReconciliationJob {
    public void reconcileTransactionsWithReorgHandling(Web3j web3j, List<String> transactionHashes) throws Exception {
        // Your code here
    }
}
```

**Solution:**
```java
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;

public class ReconciliationJob {
    public void reconcileTransactionsWithReorgHandling(Web3j web3j, List<String> transactionHashes) throws Exception {
        for (String hash : transactionHashes) {
            EthGetTransactionReceipt receipt = web3j.ethGetTransactionReceipt(hash).send();
            if (receipt.getTransactionReceipt().isPresent()) {
                // Update off-chain records
            } else {
                // Handle reorg by reverting the transaction in off-chain records
            }
        }
    }
}
```

**Validation rule:** The method `reconcileTransactionsWithReorgHandling` correctly handles chain reorganizations.

```checker
{"id": "ch08-l5-s2", "type": "regex", "pattern": "if\\s+\\(receipt\\.getTransactionReceipt\\(\\)\\.isPresent\\(\\)", "flags": "m", "target": "java", "error_hint": "Ensure reorgs are correctly handled during reconciliation."}
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

### Step 6.2 — Secure the audit trail

**Instruction:** Implement a mechanism to secure the audit trail, ensuring that it cannot be tampered with.

**Explanation:** Securing the audit trail is crucial for maintaining trust and compliance. This step will involve using cryptographic techniques to ensure data integrity and authenticity.

**Starter code:**
```java
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CmtatBankAdapter {
    private List<String> auditTrail = new ArrayList<>();

    public void addToSecureAuditTrail(String transactionDetails) throws NoSuchAlgorithmException {
        // Your code here
    }
}
```

**Solution:**
```java
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CmtatBankAdapter {
    private List<String> auditTrail = new ArrayList<>();

    public void addToSecureAuditTrail(String transactionDetails) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = md.digest(transactionDetails.getBytes());
        String hashString = Numeric.toHexString(hashBytes);
        auditTrail.add(hashString);
    }
}
```

**Validation rule:** The method `addToSecureAuditTrail` correctly adds a secure hash of transaction details to the audit trail.

```checker
{"id": "ch08-l6-s2", "type": "regex", "pattern": "MessageDigest\\s+md\\s+=\\s+MessageDigest\\.getInstance\\(\"SHA-256\"\\);", "flags": "m", "target": "java", "error_hint": "Ensure the transaction details are securely hashed before adding to the audit trail."}
```

## Quiz

**Q1 (multiple choice).** What is the primary purpose of ABI encoding in Solidity contracts when interacting with them through web3j?
a) To encrypt data for security purposes — b) To convert function calls and parameters into a format that can be understood by the Ethereum Virtual Machine (EVM) — c) To create a hash of the contract's bytecode — d) To manage gas costs during transaction execution
**Answer: b.** ABI encoding is used to convert function calls and their parameters into a binary format that the EVM can execute.

**Q2 (multiple choice).** In the context of the Bank-Side Adapter, why is it important to implement an off-chain settlement reconciliation loop?
a) To ensure that all transactions are executed on the blockchain — b) To verify the integrity and consistency of transactions between the bank's core system and the blockchain — c) To manage nonce values for transaction submission — d) To handle gas price fluctuations
**Answer: b.** The off-chain settlement reconciliation loop is crucial for verifying that transactions are correctly reflected in both the bank's systems and the blockchain, ensuring data integrity and consistency.

**Q3 (multiple choice).** Which of the following is NOT a component of a good gas strategy when submitting transactions through web3j?
a) Setting an appropriate gas limit — b) Implementing a dynamic gas price adjustment mechanism — c) Using fixed gas prices for all transactions — d) Managing transaction retries with idempotency keys
**Answer: c.** A good gas strategy should not use fixed gas prices for all transactions, as this can lead to inefficient or failed transactions due to fluctuating network conditions.

**Q4 (short answer).** Explain the role of an append-only audit trail in the Bank-Side Adapter.
**Answer:** An append-only audit trail records all actions and transactions performed by the adapter, ensuring transparency and providing a historical record for auditing purposes. This helps in maintaining compliance and facilitating forensic analysis if needed.

**Q5 (short answer).** Describe how nonce management contributes to transaction security in the context of web3j.
**Answer:** Nonce management ensures that each transaction from an account is unique by incrementing a counter with each transaction. This prevents replay attacks, where an attacker resubmits a previously signed transaction, and helps maintain the order and integrity of transactions.
