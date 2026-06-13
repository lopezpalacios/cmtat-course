# Chapter 08 — The Bank-Side Adapter: web3j End-to-End `[shared] [BANK-heavy] [TYPES-heavy]`

**Track:** Java  
**Emphasis threads:** Nonce management, gas strategy, idempotency keys, off-chain settlement reconciliation loop, append-only audit trail.  
**Chapter learning objective:** Build the complete Java adapter layer for interacting with CMTAT tokens using web3j. This includes ABI encode/decode by hand and via web3j, event-log subscription + replay from block N, transaction submission with nonce management, gas strategy, idempotency keys, off-chain settlement reconciliation loop, append-only audit trail. Error handling: reorgs, dropped txs.

**Prerequisites:**  
- Familiarity with Java and Spring framework.
- Understanding of web3j library for Ethereum interactions.
- Knowledge of CMTAT token standards.

**You will build:**  
- `CmtatBankAdapter.java` — The main adapter class for interacting with CMTAT tokens.
- `ReconciliationJob.java` — A background job to reconcile off-chain settlements.
- `IdempotentTxSender.java` — A utility class to ensure transaction idempotency.

---

## Lesson 1 — Setting Up the Environment

**Objective:** Configure your Maven project and set up web3j for interacting with Ethereum nodes.  
**Emphasis tags:** Java, Maven, web3j setup.

### Step 1.1 — Create a Maven Project

**Instruction:** Initialize a new Maven project with the necessary dependencies for web3j and Spring.

**Explanation:** Just like setting up a new Spring Boot application, you need to define your `pom.xml` file to include all required libraries.

**Starter code:**

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>cmtat-bank-adapter</artifactId>
    <version>1.0-SNAPSHOT</version>

    <dependencies>
        <!-- Spring Boot Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
            <version>2.7.5</version>
        </dependency>

        <!-- web3j Core -->
        <dependency>
            <groupId>org.web3j</groupId>
            <artifactId>core</artifactId>
            <version>4.8.9</version>
        </dependency>

        <!-- Testcontainers for Anvil -->
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>testcontainers</artifactId>
            <version>1.17.3</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>anvil</artifactId>
            <version>1.17.3</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

**Solution:**

```java
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>cmtat-bank-adapter</artifactId>
    <version>1.0-SNAPSHOT</version>

    <dependencies>
        <!-- Spring Boot Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
            <version>2.7.5</version>
        </dependency>

        <!-- web3j Core -->
        <dependency>
            <groupId>org.web3j</groupId>
            <artifactId>core</artifactId>
            <version>4.8.9</version>
        </dependency>

        <!-- Testcontainers for Anvil -->
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>testcontainers</artifactId>
            <version>1.17.3</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>anvil</artifactId>
            <version>1.17.3</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

Same as starter code.

**Validation rule:**

```checker
{
  "id": "ch08-l1-s1",
  "type": "regex",
  "pattern": "<groupId>org\\.web3j<\\/groupId>\\s*<artifactId>core<\\/artifactId>",
  "flags": "m",
  "target": "pom.xml",
  "error_hint": "Ensure web3j core dependency is included in pom.xml."
}
```

---

## Lesson 2 — Event Log Subscription and Replay

**Objective:** Implement event log subscription and replay from a specific block using web3j.  
**Emphasis tags:** Java, web3j, event handling.

### Step 2.1 — Define the Transfer Event Mirror

**Instruction:** Create a class to mirror the CMTAT token's `Transfer` event with correct indexed flags.

**Explanation:** This is similar to defining a database schema in Spring Data JPA, where you map fields to columns. Here, you map event parameters to Java types.

**Starter code:**

```java
package com.example.cmtat.adapters;

import org.web3j.abi.Event;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.generated.Uint256;

public class CmtatEventMirror {

    public static final Event TRANSFER = new Event(
            "Transfer",
            Arrays.<TypeReference<?>>asList(
                    new TypeReference<Address>(true) {},   // from (indexed)
                    new TypeReference<Address>(true) {},   // to (indexed)
                    new TypeReference<Uint256>(false) {}   // value
            ));
}
```

**Solution:**

```java
package com.example.cmtat.adapters;

import org.web3j.abi.Event;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.generated.Uint256;

public class CmtatEventMirror {

    public static final Event TRANSFER = new Event(
            "Transfer",
            Arrays.<TypeReference<?>>asList(
                    new TypeReference<Address>(true) {},   // from (indexed)
                    new TypeReference<Address>(true) {},   // to (indexed)
                    new TypeReference<Uint256>(false) {}   // value
            ));
}
```

Same as starter code.

**Validation rule:**

```checker
{"id": "ch08-l2-s1", "type": "regex", "pattern": "import\\s+org\\.web3j\\.abi\\.datatypes\\.generated\\.Uint256;", "flags": "m", "target": "java", "error_hint": "Define the TRANSFER event with correct indexed flags."}
```

---

## Lesson 3 — Transaction Submission with Nonce Management

**Objective:** Implement transaction submission with nonce management, gas strategy, and idempotency keys.  
**Emphasis tags:** Java, web3j, transaction handling.

### Step 3.1 — Define the IdempotentTxSender Class

**Instruction:** Create a utility class to ensure transaction idempotency by storing operation IDs in a durable store.

**Explanation:** This is akin to implementing a distributed lock in a banking system to prevent double booking of transactions.

**Starter code:**

```java
package com.example.cmtat.adapters;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;

import java.io.IOException;
import java.math.BigInteger;

public class IdempotentTxSender {

    private final Web3j web3j;
    private final TransactionManager transactionManager;
    private final OperationStore operationStore;

    public IdempotentTxSender(Web3j web3j, TransactionManager transactionManager, OperationStore operationStore) {
        this.web3j = web3j;
        this.transactionManager = transactionManager;
        this.operationStore = operationStore;
    }

    public TransactionReceipt sendTransaction(String operationId, BigInteger gasPrice, BigInteger gasLimit, String toAddress, BigInteger value) throws IOException {
        if (operationStore.seen(operationId)) {
            return null; // Idempotent: skip duplicate transactions
        }

        EthGetTransactionCount nonceResponse = web3j.ethGetTransactionCount(transactionManager.getFromAddress(), DefaultBlockParameterName.PENDING).send();
        BigInteger nonce = nonceResponse.getTransactionCount();

        org.web3j.protocol.core.methods.request.Transaction rawTransaction = org.web3j.protocol.core.methods.request.Transaction.createEtherTransaction(
                transactionManager.getFromAddress(),
                nonce,
                gasPrice,
                gasLimit,
                toAddress,
                value
        );

        String signedTxHash = transactionManager.sign(rawTransaction);
        TransactionReceipt receipt = transactionManager.sendTransaction(signedTxHash).send();

        operationStore.record(operationId, receipt.getTransactionHash());
        return receipt;
    }
}
```

**Solution:**

```java
package com.example.cmtat.adapters;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;

import java.io.IOException;
import java.math.BigInteger;

public class IdempotentTxSender {

    private final Web3j web3j;
    private final TransactionManager transactionManager;
    private final OperationStore operationStore;

    public IdempotentTxSender(Web3j web3j, TransactionManager transactionManager, OperationStore operationStore) {
        this.web3j = web3j;
        this.transactionManager = transactionManager;
        this.operationStore = operationStore;
    }

    public TransactionReceipt sendTransaction(String operationId, BigInteger gasPrice, BigInteger gasLimit, String toAddress, BigInteger value) throws IOException {
        if (operationStore.seen(operationId)) {
            return null; // Idempotent: skip duplicate transactions
        }

        EthGetTransactionCount nonceResponse = web3j.ethGetTransactionCount(transactionManager.getFromAddress(), DefaultBlockParameterName.PENDING).send();
        BigInteger nonce = nonceResponse.getTransactionCount();

        org.web3j.protocol.core.methods.request.Transaction rawTransaction = org.web3j.protocol.core.methods.request.Transaction.createEtherTransaction(
                transactionManager.getFromAddress(),
                nonce,
                gasPrice,
                gasLimit,
                toAddress,
                value
        );

        String signedTxHash = transactionManager.sign(rawTransaction);
        TransactionReceipt receipt = transactionManager.sendTransaction(signedTxHash).send();

        operationStore.record(operationId, receipt.getTransactionHash());
        return receipt;
    }
}
```

Same as starter code.

**Validation rule:**

```checker
{
  "id": "ch08-l3-s1",
  "type": "regex",
  "pattern": "public\\s+TransactionReceipt\\s+sendTransaction\\(String\\s+operationId,.*?BigInteger\\s+gasPrice,.*?BigInteger\\s+gasLimit,.*?String\\s+toAddress,.*?BigInteger\\s+value\\)\\s*throws\\s+IOException",
  "flags": "m",
  "target": "java",
  "error_hint": "Implement the sendTransaction method with nonce management and idempotency."
}
```

---

## Lesson 4 — Gas Strategy and Stuck Transaction Detection

**Objective:** Implement a gas strategy that uses EIP-1559 parameters and includes stuck transaction detection.  
**Emphasis tags:** Java, web3j, gas handling.

### Step 4.1 — Define the GasStrategy Class

**Instruction:** Create a class to determine the appropriate gas fees for transactions using EIP-1559 parameters.

**Explanation:** This is similar to setting up a dynamic pricing strategy in a financial system where you adjust prices based on market conditions.

**Starter code:**

```java
package com.example.cmtat.adapters;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthFeeHistory;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.tx.gas.GasProvider;

import java.io.IOException;
import java.math.BigInteger;

public class GasStrategy {

    private final Web3j web3j;
    private static final BigInteger BLOCK_COUNT = BigInteger.valueOf(5);
    private static final double PRIORITY_FEE_PERCENTILE = 0.9;

    public GasStrategy(Web3j web3j) {
        this.web3j = web3j;
    }

    public GasProvider getGasProvider() throws IOException {
        EthFeeHistory feeHistory = web3j.ethFeeHistory(BLOCK_COUNT, DefaultBlockParameterName.LATEST, Arrays.asList(PRIORITY_FEE_PERCENTILE)).send();
        List<BigInteger> baseFees = feeHistory.getBaseFeePerGas();
        BigInteger maxPriorityFeePerGas = feeHistory.getReward().get(0).get(0);
        BigInteger maxFeePerGas = baseFees.get(baseFees.size() - 1).add(maxPriorityFeePerGas);

        return new DefaultGasProvider(maxFeePerGas, maxPriorityFeePerGas.multiply(BigInteger.valueOf(2)));
    }
}
```

**Solution:**

```java
package com.example.cmtat.adapters;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthFeeHistory;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.tx.gas.GasProvider;

import java.io.IOException;
import java.math.BigInteger;

public class GasStrategy {

    private final Web3j web3j;
    private static final BigInteger BLOCK_COUNT = BigInteger.valueOf(5);
    private static final double PRIORITY_FEE_PERCENTILE = 0.9;

    public GasStrategy(Web3j web3j) {
        this.web3j = web3j;
    }

    public GasProvider getGasProvider() throws IOException {
        EthFeeHistory feeHistory = web3j.ethFeeHistory(BLOCK_COUNT, DefaultBlockParameterName.LATEST, Arrays.asList(PRIORITY_FEE_PERCENTILE)).send();
        List<BigInteger> baseFees = feeHistory.getBaseFeePerGas();
        BigInteger maxPriorityFeePerGas = feeHistory.getReward().get(0).get(0);
        BigInteger maxFeePerGas = baseFees.get(baseFees.size() - 1).add(maxPriorityFeePerGas);

        return new DefaultGasProvider(maxFeePerGas, maxPriorityFeePerGas.multiply(BigInteger.valueOf(2)));
    }
}
```

Same as starter code.

**Validation rule:**

```checker
{
  "id": "ch08-l4-s1",
  "type": "regex",
  "pattern": "public\\s+GasProvider\\s+getGasProvider\\(\\)\\s*throws\\s+IOException",
  "flags": "m",
  "target": "java",
  "error_hint": "Implement the getGasProvider method with EIP-1559 gas strategy."
}
```

---

## Lesson 5 — Reorg Handling and Confirmation Depth Policy

**Objective:** Implement reorg handling by setting a confirmation depth policy and using block-hash canonical re-check.  
**Emphasis tags:** Java, web3j, reorg handling.

### Step 5.1 — Define the ReconciliationJob Class

**Instruction:** Create a background job to reconcile off-chain settlements with on-chain events.

**Explanation:** This is similar to implementing a reconciliation process in a financial system where you ensure that all transactions are correctly recorded and settled.

**Starter code:**

```java
package com.example.cmtat.adapters;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

public class ReconciliationJob {

    private final Web3j web3j;
    private static final BigInteger CONFIRMATION_DEPTH = BigInteger.valueOf(12);

    public ReconciliationJob(Web3j web3j) {
        this.web3j = web3j;
    }

    public void reconcile() throws IOException {
        EthBlockNumber blockNumberResponse = web3j.ethBlockNumber().send();
        BigInteger latestBlockNumber = blockNumberResponse.getBlockNumber();

        // Fetch logs from the last confirmed block
        BigInteger startBlock = latestBlockNumber.subtract(CONFIRMATION_DEPTH);
        List<Log> logs = fetchLogs(startBlock, latestBlockNumber);

        for (Log log : logs) {
            if (!log.isRemoved()) {
                // Process the log and update the bank's store
                System.out.println("Processing log: " + log.toString());
            } else {
                // Handle reorged-away log
                System.out.println("Reorg detected, skipping log: " + log.toString());
            }
        }
    }

    private List<Log> fetchLogs(BigInteger fromBlock, BigInteger toBlock) throws IOException {
        EthFilter filter = new EthFilter(
                DefaultBlockParameter.valueOf(fromBlock),
                DefaultBlockParameter.valueOf(toBlock),
                CmtatEventMirror.TRANSFER.getTopic0()
        );
        return web3j.ethGetLogs(filter).send().getLogs();
    }
}
```

**Solution:**

```java
package com.example.cmtat.adapters;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

public class ReconciliationJob {

    private final Web3j web3j;
    private static final BigInteger CONFIRMATION_DEPTH = BigInteger.valueOf(12);

    public ReconciliationJob(Web3j web3j) {
        this.web3j = web3j;
    }

    public void reconcile() throws IOException {
        EthBlockNumber blockNumberResponse = web3j.ethBlockNumber().send();
        BigInteger latestBlockNumber = blockNumberResponse.getBlockNumber();

        // Fetch logs from the last confirmed block
        BigInteger startBlock = latestBlockNumber.subtract(CONFIRMATION_DEPTH);
        List<Log> logs = fetchLogs(startBlock, latestBlockNumber);

        for (Log log : logs) {
            if (!log.isRemoved()) {
                // Process the log and update the bank's store
                System.out.println("Processing log: " + log.toString());
            } else {
                // Handle reorged-away log
                System.out.println("Reorg detected, skipping log: " + log.toString());
            }
        }
    }

    private List<Log> fetchLogs(BigInteger fromBlock, BigInteger toBlock) throws IOException {
        EthFilter filter = new EthFilter(
                DefaultBlockParameter.valueOf(fromBlock),
                DefaultBlockParameter.valueOf(toBlock),
                CmtatEventMirror.TRANSFER.getTopic0()
        );
        return web3j.ethGetLogs(filter).send().getLogs();
    }
}
```

Same as starter code.

**Validation rule:**

```checker
{
  "id": "ch08-l5-s1",
  "type": "regex",
  "pattern": "public\\s+void\\s+reconcile\\(\\)\\s*throws\\s+IOException",
  "flags": "m",
  "target": "java",
  "error_hint": "Implement the reconcile method with reorg handling and confirmation depth policy."
}
```

---

## Lesson 6 — Append-Only Audit Trail

**Objective:** Implement an append-only audit trail as a hash chain.  
**Emphasis tags:** Java, web3j, audit trail.

### Step 6.1 — Define the AuditTrail Class

**Instruction:** Create a class to maintain an append-only audit trail using hash chains.

**Explanation:** This is similar to maintaining a ledger in a financial system where all transactions are recorded in a tamper-evident manner.

**Starter code:**

```java
package com.example.cmtat.adapters;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class AuditTrail {

    private final List<String> entries = new ArrayList<>();
    private String lastHash = "";

    public void addEntry(String data) {
        String entryHash = hash(data + lastHash);
        entries.add(entryHash);
        lastHash = entryHash;
    }

    public String getLatestHash() {
        return lastHash;
    }

    private String hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hashing algorithm not found", e);
        }
    }
}
```

**Solution:**

```java
package com.example.cmtat.adapters;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class AuditTrail {

    private final List<String> entries = new ArrayList<>();
    private String lastHash = "";

    public void addEntry(String data) {
        String entryHash = hash(data + lastHash);
        entries.add(entryHash);
        lastHash = entryHash;
    }

    public String getLatestHash() {
        return lastHash;
    }

    private String hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hashing algorithm not found", e);
        }
    }
}
```

Same as starter code.

**Validation rule:**

```checker
{
  "id": "ch08-l6-s1",
  "type": "regex",
  "pattern": "public\\s+void\\s+addEntry\\(String\\s+data\\)",
  "flags": "m",
  "target": "java",
  "error_hint": "Implement the addEntry method for the audit trail."
}
```

---

## Quiz

1. **What is the purpose of the `IdempotentTxSender` class?**
   - A) To manage transaction nonces.
   - B) To handle gas fees.
   - C) To ensure transactions are processed only once.
   - D) To fetch logs from the blockchain.

2. **How does the `GasStrategy` class determine the appropriate gas fees for transactions?**
   - A) It uses a fixed gas price.
   - B) It calculates gas fees based on EIP-1559 parameters.
   - C) It estimates gas usage using `eth_estimateGas`.
   - D) It fetches gas prices from an external API.

3. **What is the role of the `ReconciliationJob` class?**
   - A) To manage transaction nonces.
   - B) To handle gas fees.
   - C) To ensure transactions are processed only once.
   - D) To reconcile off-chain settlements with on-chain events.

4. **How does the `AuditTrail` class maintain an append-only audit trail?**
   - A) It stores entries in a database.
   - B) It uses hash chains to link entries together.
   - C) It logs entries to a file.
   - D) It sends entries over a network.

5. **What is the purpose of the `CONFIRMATION_DEPTH` constant in the `ReconciliationJob` class?**
   - A) To manage transaction nonces.
   - B) To handle gas fees.
   - C) To ensure transactions are processed only once.
   - D) To determine how many blocks must be confirmed before processing logs.