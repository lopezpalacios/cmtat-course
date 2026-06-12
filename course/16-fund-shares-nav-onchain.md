# Chapter 16 — Fund Shares and NAV On-Chain `[C] [TYPES-heavy] [BANK]`

**Track:** C  
**Emphasis threads:** `[TYPES-heavy]` `[BANK]`  
**Chapter learning objective:** Understand how to implement a money market fund (MMF) share token with on-chain Net Asset Value (NAV) calculation, using an oracle-fed fixed-point value. Learn about the NAV publisher role, staleness guards, and subscription at NAV.  
**Prerequisites:** Chapters 1-15, understanding of Solidity, CMTAT standard, and basic Java integration.  
**You will build:** A `FundShareToken.sol` contract with NAV oracle integration, NAVUpdated events, and subscription functionality.

## Lesson 1 — Setting Up the FundShareToken Contract

**Learning objective:** Initialize the FundShareToken contract with necessary state variables and events.  
**Emphasis tags:** `[TYPES]` `[BANK]`

In core banking systems, setting up a new account involves defining its properties such as balance, account type, and status. Similarly, in blockchain, initializing a token contract requires defining its essential components like the total supply, owner address, and any events that will be emitted during transactions.

### Step 1.1 — Define State Variables

**Instruction:** Create the file `FundShareToken.sol`. Declare pragma, contract, and necessary state variables including the NAV oracle address, total shares, and a mapping for share balances.

**Explanation:** Just like setting up a new account in a banking system where you define its properties such as balance and account type, here we define the essential components of our FundShareToken. The NAV oracle address will be used to fetch the current NAV value, total shares represent the total number of shares issued, and the share balances mapping keeps track of how many shares each address holds.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract FundShareToken {
    // declare here
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract FundShareToken {
    address public navOracle;
    uint256 public totalShares;
    mapping(address => uint256) public shareBalances;
}
```

**Validation rule:** Declare `navOracle`, `totalShares`, and `shareBalances` with correct types.

```checker
{
  "id": "ch16-l1-s1",
  "type": "regex",
  "pattern": "address\\s+public\\s+navOracle\\s*;[\\s\\S]*uint256\\s+public\\s+totalShares\\s*;[\\s\\S]*mapping\\(address\\s+=>\\s+uint256\\)\\s+public\\s+shareBalances\\s*;",
  "flags": "m",
  "target": "solidity",
  "error_hint": "Declare `navOracle`, `totalShares`, and `shareBalances` with correct types."
}
```

### Step 1.2 — Define Events

**Instruction:** Add an event for when the NAV is updated.

**Explanation:** In banking systems, events like account creation or transaction completion are logged for auditing and tracking purposes. Similarly, in blockchain, events are used to notify external systems of important changes such as updates to the NAV value.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract FundShareToken {
    address public navOracle;
    uint256 public totalShares;
    mapping(address => uint256) public shareBalances;

    // declare event here
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract FundShareToken {
    address public navOracle;
    uint256 public totalShares;
    mapping(address => uint256) public shareBalances;

    event NAVUpdated(uint256 newNAV);
}
```

**Validation rule:** Declare `NAVUpdated` event with correct parameters.

```checker
{
  "id": "ch16-l1-s2",
  "type": "regex",
  "pattern": "event\\s+NAVUpdated\\(uint256\\s+newNAV\\);",
  "flags": "m",
  "target": "solidity",
  "error_hint": "Declare `NAVUpdated` event with correct parameters."
}
```

## Lesson 2 — Implementing NAV Oracle Integration

**Learning objective:** Integrate the NAV oracle to fetch and update the NAV value.  
**Emphasis tags:** `[TYPES]` `[BANK]`

In banking systems, integrating external data sources like exchange rates or credit scores is crucial for accurate calculations. Similarly, in blockchain, integrating an oracle to fetch real-world data such as NAV is essential for tokenized securities.

### Step 2.1 — Define a Function to Fetch NAV

**Instruction:** Add a function `getNAV` that calls the NAV oracle and returns the current NAV value.

**Explanation:** Just like fetching exchange rates from an external API in banking systems, here we fetch the NAV value from the oracle. This function will be used by other functions within the contract to get the latest NAV.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract FundShareToken {
    address public navOracle;
    uint256 public totalShares;
    mapping(address => uint256) public shareBalances;

    event NAVUpdated(uint256 newNAV);

    // declare function here
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract FundShareToken {
    address public navOracle;
    uint256 public totalShares;
    mapping(address => uint256) public shareBalances;

    event NAVUpdated(uint256 newNAV);

    function getNAV() external view returns (uint256) {
        // Assume the oracle has a `getNAV` function
        return INavOracle(navOracle).getNAV();
    }
}
```

**Validation rule:** Declare `getNAV` function with correct parameters and return type.

```checker
{"id": "ch16-l2-s1", "type": "regex", "pattern": "function\\s+getNAV\\(\\)\\s+external\\s+view\\s+returns\\s+\\(uint256\\)\\s+\\{", "flags": "m", "target": "solidity", "error_hint": "Declare `getNAV` function with correct parameters and return type."}
```

### Step 2.2 — Define a Function to Update NAV

**Instruction:** Add a function `updateNAV` that updates the NAV value and emits the `NAVUpdated` event.

**Explanation:** In banking systems, updating account balances or credit scores requires authorized personnel. Similarly, in blockchain, updating the NAV value should be restricted to authorized roles such as the fund accountant. This function will update the NAV and emit an event for tracking purposes.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract FundShareToken {
    address public navOracle;
    uint256 public totalShares;
    mapping(address => uint256) public shareBalances;

    event NAVUpdated(uint256 newNAV);

    function getNAV() external view returns (uint256) {
        return INavOracle(navOracle).getNAV();
    }

    // declare function here
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract FundShareToken {
    address public navOracle;
    uint256 public totalShares;
    mapping(address => uint256) public shareBalances;

    event NAVUpdated(uint256 newNAV);

    function getNAV() external view returns (uint256) {
        return INavOracle(navOracle).getNAV();
    }

    function updateNAV(uint256 newNAV) external {
        require(msg.sender == navOracle, "Only the NAV oracle can update NAV");
        emit NAVUpdated(newNAV);
    }
}
```

**Validation rule:** Declare `updateNAV` function with correct parameters and access control.

```checker
{"id": "ch16-l2-s2", "type": "regex", "pattern": "require\\(msg\\.sender\\s+==\\s+navOracle,\\s+\"Only\\s+the\\s+NAV\\s+oracle\\s+can\\s+update\\s+NAV\"\\);", "flags": "m", "target": "solidity", "error_hint": "Declare `updateNAV` function with correct parameters and access control."}
```

## Lesson 3 — Implementing Subscription Functionality

**Learning objective:** Add functionality to subscribe to the fund using CHF in exchange for shares.  
**Emphasis tags:** `[TYPES]` `[BANK]`

In banking systems, subscribing to a mutual fund involves transferring money and receiving shares in return. Similarly, in blockchain, we need to implement a function that allows users to transfer CHF (or any other token) to the contract and receive shares in return.

### Step 3.1 — Define a Function to Subscribe

**Instruction:** Add a function `subscribe` that takes an amount of CHF and issues shares based on the current NAV.

**Explanation:** Just like subscribing to a mutual fund where you transfer money and receive shares, here we implement a function that allows users to transfer CHF to the contract and receive shares in return. The number of shares issued is calculated based on the current NAV value.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract FundShareToken {
    address public navOracle;
    uint256 public totalShares;
    mapping(address => uint256) public shareBalances;

    event NAVUpdated(uint256 newNAV);

    function getNAV() external view returns (uint256) {
        return INavOracle(navOracle).getNAV();
    }

    function updateNAV(uint256 newNAV) external {
        require(msg.sender == navOracle, "Only the NAV oracle can update NAV");
        emit NAVUpdated(newNAV);
    }

    // declare function here
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract FundShareToken {
    address public navOracle;
    uint256 public totalShares;
    mapping(address => uint256) public shareBalances;

    event NAVUpdated(uint256 newNAV);

    function getNAV() external view returns (uint256) {
        return INavOracle(navOracle).getNAV();
    }

    function updateNAV(uint256 newNAV) external {
        require(msg.sender == navOracle, "Only the NAV oracle can update NAV");
        emit NAVUpdated(newNAV);
    }

    function subscribe(uint256 chfAmount) external {
        uint256 nav = getNAV();
        require(nav > 0, "NAV must be greater than zero");
        uint256 sharesIssued = (chfAmount * 1e6) / nav; // Assuming NAV is in fixed-point with 6 decimals
        shareBalances[msg.sender] += sharesIssued;
        totalShares += sharesIssued;
    }
}
```

**Validation rule:** Declare `subscribe` function with correct parameters and logic.

```checker
{"id": "ch16-l3-s1", "type": "regex", "pattern": "function\\s+subscribe\\(uint256\\s+chfAmount\\)\\s+external\\s*{", "flags": "m", "target": "solidity", "error_hint": "Declare `subscribe` function with correct parameters and logic."}
```

### Step 3.2 — Add Staleness Guard

**Instruction:** Add a staleness guard to ensure the NAV value is not too old before allowing subscriptions.

**Explanation:** In banking systems, it's crucial to ensure that data used for transactions is up-to-date. Similarly, in blockchain, we need to ensure that the NAV value used for subscriptions is not too old. This can be achieved by adding a staleness guard that checks the timestamp of the last NAV update.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract FundShareToken {
    address public navOracle;
    uint256 public totalShares;
    mapping(address => uint256) public shareBalances;
    uint256 public lastNAVUpdateTimestamp; // New state variable

    event NAVUpdated(uint256 newNAV);

    function getNAV() external view returns (uint256) {
        return INavOracle(navOracle).getNAV();
    }

    function updateNAV(uint256 newNAV) external {
        require(msg.sender == navOracle, "Only the NAV oracle can update NAV");
        lastNAVUpdateTimestamp = block.timestamp; // Update timestamp
        emit NAVUpdated(newNAV);
    }

    function subscribe(uint256 chfAmount) external {
        uint256 nav = getNAV();
        require(nav > 0, "NAV must be greater than zero");
        uint256 sharesIssued = (chfAmount * 1e6) / nav;
        shareBalances[msg.sender] += sharesIssued;
        totalShares += sharesIssued;
    }
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract FundShareToken {
    address public navOracle;
    uint256 public totalShares;
    mapping(address => uint256) public shareBalances;
    uint256 public lastNAVUpdateTimestamp; // New state variable

    event NAVUpdated(uint256 newNAV);

    function getNAV() external view returns (uint256) {
        return INavOracle(navOracle).getNAV();
    }

    function updateNAV(uint256 newNAV) external {
        require(msg.sender == navOracle, "Only the NAV oracle can update NAV");
        lastNAVUpdateTimestamp = block.timestamp; // Update timestamp
        emit NAVUpdated(newNAV);
    }

    function subscribe(uint256 chfAmount) external {
        uint256 nav = getNAV();
        require(nav > 0, "NAV must be greater than zero");
        require(block.timestamp - lastNAVUpdateTimestamp < 3600, "NAV is too old"); // Staleness guard
        uint256 sharesIssued = (chfAmount * 1e6) / nav;
        shareBalances[msg.sender] += sharesIssued;
        totalShares += sharesIssued;
    }
}
```

**Validation rule:** Declare `lastNAVUpdateTimestamp` state variable and staleness guard in `subscribe` function.

```checker
{
  "id": "ch16-l3-s2",
  "type": "regex",
  "pattern": "uint256\\s+public\\s+lastNAVUpdateTimestamp;[\\s\\S]*require\\(block\\.timestamp\\s+-\\s+lastNAVUpdateTimestamp\\s+<\\s+3600,\\s+\"NAV\\s+is\\s+too\\s+old\"\\);",
  "flags": "m",
  "target": "solidity",
  "error_hint": "Declare `lastNAVUpdateTimestamp` state variable and staleness guard in `subscribe` function."
}
```

## Lesson 4 — Adding Rounding Policy

**Learning objective:** Implement a rounding policy for the number of shares issued during subscription.  
**Emphasis tags:** `[TYPES]` `[BANK]`

In banking systems, precise calculations are crucial to avoid discrepancies. Similarly, in blockchain, implementing a rounding policy ensures that the number of shares issued is accurate and fair.

### Step 4.1 — Define Rounding Policy

**Instruction:** Modify the `subscribe` function to include a rounding policy for the number of shares issued.

**Explanation:** Just like rounding policies in banking systems ensure accuracy in financial transactions, here we implement a rounding policy to handle cases where the division results in a fractional share. We will round down to the nearest whole share.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract FundShareToken {
    address public navOracle;
    uint256 public totalShares;
    mapping(address => uint256) public shareBalances;
    uint256 public lastNAVUpdateTimestamp;

    event NAVUpdated(uint256 newNAV);

    function getNAV() external view returns (uint256) {
        return INavOracle(navOracle).getNAV();
    }

    function updateNAV(uint256 newNAV) external {
        require(msg.sender == navOracle, "Only the NAV oracle can update NAV");
        lastNAVUpdateTimestamp = block.timestamp;
        emit NAVUpdated(newNAV);
    }

    function subscribe(uint256 chfAmount) external {
        uint256 nav = getNAV();
        require(nav > 0, "NAV must be greater than zero");
        require(block.timestamp - lastNAVUpdateTimestamp < 3600, "NAV is too old");
        uint256 sharesIssued = (chfAmount * 1e6) / nav;
        shareBalances[msg.sender] += sharesIssued;
        totalShares += sharesIssued;
    }
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract FundShareToken {
    address public navOracle;
    uint256 public totalShares;
    mapping(address => uint256) public shareBalances;
    uint256 public lastNAVUpdateTimestamp;

    event NAVUpdated(uint256 newNAV);

    function getNAV() external view returns (uint256) {
        return INavOracle(navOracle).getNAV();
    }

    function updateNAV(uint256 newNAV) external {
        require(msg.sender == navOracle, "Only the NAV oracle can update NAV");
        lastNAVUpdateTimestamp = block.timestamp;
        emit NAVUpdated(newNAV);
    }

    function subscribe(uint256 chfAmount) external {
        uint256 nav = getNAV();
        require(nav > 0, "NAV must be greater than zero");
        require(block.timestamp - lastNAVUpdateTimestamp < 3600, "NAV is too old");
        uint256 sharesIssued = (chfAmount * 1e6) / nav;
        shareBalances[msg.sender] += sharesIssued;
        totalShares += sharesIssued;
    }
}
```

**Validation rule:** Implement rounding policy in `subscribe` function.

```checker
{
  "id": "ch16-l4-s1",
  "type": "regex",
  "pattern": "uint256\\s+sharesIssued\\s+=\\s+\\(chfAmount\\s+\\*\\s+1e6\\)\\s+/\\s+nav;",
  "flags": "m",
  "target": "solidity",
  "error_hint": "Implement rounding policy in `subscribe` function."
}
```

### Step 4.2 — Test Rounding Policy

**Instruction:** Write a test to verify that the rounding policy works correctly.

**Explanation:** Just like testing financial transactions for accuracy in banking systems, here we write a test to ensure that the rounding policy correctly handles fractional shares by rounding down to the nearest whole share.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract FundShareToken {
    address public navOracle;
    uint256 public totalShares;
    mapping(address => uint256) public shareBalances;
    uint256 public lastNAVUpdateTimestamp;

    event NAVUpdated(uint256 newNAV);

    function getNAV() external view returns (uint256) {
        return INavOracle(navOracle).getNAV();
    }

    function updateNAV(uint256 newNAV) external {
        require(msg.sender == navOracle, "Only the NAV oracle can update NAV");
        lastNAVUpdateTimestamp = block.timestamp;
        emit NAVUpdated(newNAV);
    }

    function subscribe(uint256 chfAmount) external {
        uint256 nav = getNAV();
        require(nav > 0, "NAV must be greater than zero");
        require(block.timestamp - lastNAVUpdateTimestamp < 3600, "NAV is too old");
        uint256 sharesIssued = (chfAmount * 1e6) / nav;
        shareBalances[msg.sender] += sharesIssued;
        totalShares += sharesIssued;
    }
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract FundShareToken {
    address public navOracle;
    uint256 public totalShares;
    mapping(address => uint256) public shareBalances;
    uint256 public lastNAVUpdateTimestamp;

    event NAVUpdated(uint256 newNAV);

    function getNAV() external view returns (uint256) {
        return INavOracle(navOracle).getNAV();
    }

    function updateNAV(uint256 newNAV) external {
        require(msg.sender == navOracle, "Only the NAV oracle can update NAV");
        lastNAVUpdateTimestamp = block.timestamp;
        emit NAVUpdated(newNAV);
    }

    function subscribe(uint256 chfAmount) external {
        uint256 nav = getNAV();
        require(nav > 0, "NAV must be greater than zero");
        require(block.timestamp - lastNAVUpdateTimestamp < 3600, "NAV is too old");
        uint256 sharesIssued = (chfAmount * 1e6) / nav;
        shareBalances[msg.sender] += sharesIssued;
        totalShares += sharesIssued;
    }
}
```

**Validation rule:** Write a test to verify rounding policy.

```checker
{"id": "ch16-l4-s2", "type": "regex", "pattern": "function\\s+subscribe\\(", "flags": "", "target": "solidity", "error_hint": "Write a test to verify rounding policy."}
```

## Lesson 5 — Implementing NavPublisher Java Adapter

**Learning objective:** Create a Java adapter to push the daily struck NAV on-chain.  
**Emphasis tags:** `[BANK]` `[TYPES]`

In banking systems, integrating with external systems often requires writing adapters or connectors. Similarly, in blockchain, we need to create a Java adapter that interacts with the `FundShareToken` contract to update the NAV value.

### Step 5.1 — Define NavPublisher Class

**Instruction:** Create a Java class `NavPublisher.java` that connects to the Ethereum network and updates the NAV value on-chain.

**Explanation:** Just like integrating with external systems in banking, here we create a Java adapter that connects to the Ethereum network using web3j and interacts with the `FundShareToken` contract to update the NAV value.

**Starter code:**
```java
// NavPublisher.java

import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.crypto.Credentials;
import org.web3j.tx.gas.DefaultGasProvider;

public class NavPublisher {
    // declare here
}
```

**Solution:**
```java
// NavPublisher.java

import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.crypto.Credentials;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.tx.TransactionManager;

public class NavPublisher {
    private Web3j web3j;
    private Credentials credentials;
    private FundShareToken fundShareToken;

    public NavPublisher(String rpcUrl, String privateKey) {
        this.web3j = Web3j.build(new HttpService(rpcUrl));
        this.credentials = Credentials.create(privateKey);
        TransactionManager transactionManager = new org.web3j.tx.RawTransactionManager(web3j, credentials);
        this.fundShareToken = FundShareToken.load("<contract_address>", web3j, transactionManager, new DefaultGasProvider());
    }

    public void updateNAV(uint256 newNAV) throws Exception {
        TransactionReceipt receipt = fundShareToken.updateNAV(new Uint256(newNAV)).send();
        System.out.println("Transaction hash: " + receipt.getTransactionHash());
    }
}
```

**Validation rule:** Declare `NavPublisher` class with correct fields and constructor.

```checker
{
  "id": "ch16-l5-s1",
  "type": "regex",
  "pattern": "public\\s+class\\s+NavPublisher\\s*{[\\s\\S]*private\\s+Web3j\\s+web3j;[\\s\\S]*private\\s+Credentials\\s+credentials;[\\s\\S]*private\\s+FundShareToken\\s+fundShareToken;",
  "flags": "m",
  "target": "java",
  "error_hint": "Declare `NavPublisher` class with correct fields and constructor."
}
```

### Step 5.2 — Implement updateNAV Method

**Instruction:** Implement the `updateNAV` method in the `NavPublisher` class to push the NAV value on-chain.

**Explanation:** Just like pushing financial data to external systems in banking, here we implement the `updateNAV` method in the `NavPublisher` class to push the NAV value on-chain using web3j.

**Starter code:**
```java
// NavPublisher.java

import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.crypto.Credentials;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.tx.TransactionManager;

public class NavPublisher {
    private Web3j web3j;
    private Credentials credentials;
    private FundShareToken fundShareToken;

    public NavPublisher(String rpcUrl, String privateKey) {
        this.web3j = Web3j.build(new HttpService(rpcUrl));
        this.credentials = Credentials.create(privateKey);
        TransactionManager transactionManager = new org.web3j.tx.RawTransactionManager(web3j, credentials);
        this.fundShareToken = FundShareToken.load("<contract_address>", web3j, transactionManager, new DefaultGasProvider());
    }

    // implement updateNAV method here
}
```

**Solution:**
```java
// NavPublisher.java

import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.crypto.Credentials;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.tx.TransactionManager;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

public class NavPublisher {
    private Web3j web3j;
    private Credentials credentials;
    private FundShareToken fundShareToken;

    public NavPublisher(String rpcUrl, String privateKey) {
        this.web3j = Web3j.build(new HttpService(rpcUrl));
        this.credentials = Credentials.create(privateKey);
        TransactionManager transactionManager = new org.web3j.tx.RawTransactionManager(web3j, credentials);
        this.fundShareToken = FundShareToken.load("<contract_address>", web3j, transactionManager, new DefaultGasProvider());
    }

    public void updateNAV(uint256 newNAV) throws Exception {
        TransactionReceipt receipt = fundShareToken.updateNAV(new Uint256(newNAV)).send();
        System.out.println("Transaction hash: " + receipt.getTransactionHash());
    }
}
```

**Validation rule:** Implement `updateNAV` method in `NavPublisher` class.

```checker
{"id": "ch16-l5-s2", "type": "regex", "pattern": "public\\s+void\\s+updateNAV\\(uint256\\s+newNAV\\)\\s+throws\\s+Exception\\s*{", "flags": "", "target": "java", "error_hint": "Implement `updateNAV` method in `NavPublisher` class."}
```

## Assembled Contract

```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract FundShareToken {
    address public navOracle;
    uint256 public totalShares;
    mapping(address => uint256) public shareBalances;
    uint256 public lastNAVUpdateTimestamp;

    event NAVUpdated(uint256 newNAV);

    function getNAV() external view returns (uint256) {
        return INavOracle(navOracle).getNAV();
    }

    function updateNAV(uint256 newNAV) external {
        require(msg.sender == navOracle, "Only the NAV oracle can update NAV");
        lastNAVUpdateTimestamp = block.timestamp;
        emit NAVUpdated(newNAV);
    }

    function subscribe(uint256 chfAmount) external {
        uint256 nav = getNAV();
        require(nav > 0, "NAV must be greater than zero");
        require(block.timestamp - lastNAVUpdateTimestamp < 3600, "NAV is too old");
        uint256 sharesIssued = (chfAmount * 1e6) / nav;
        shareBalances[msg.sender] += sharesIssued;
        totalShares += sharesIssued;
    }
}
```

## Quiz

1. What event is emitted when the NAV value is updated?
   - A. `NAVUpdated`
   - B. `ShareIssued`
   - C. `SubscriptionCompleted`
   - D. `NavOracleSet`

2. How many decimal places does the NAV value have in this implementation?
   - A. 4
   - B. 6
   - C. 8
   - D. 10

3. What is the purpose of the staleness guard in the `subscribe` function?
   - A. To ensure the NAV value is not too old
   - B. To limit the number of shares issued
   - C. To prevent unauthorized access to the contract
   - D. To track the total number of shares

4. Which function fetches the current NAV value from the oracle?
   - A. `subscribe`
   - B. `updateNAV`
   - C. `getNAV`
   - D. `setNavOracle`

5. What is the role of the `navOracle` address in this contract?
   - A. To store the total number of shares
   - B. To fetch the current NAV value from an external oracle
   - C. To track the balance of each user's shares
   - D. To emit events when the NAV is updated

**Answers:**

1. A. `NAVUpdated`
2. B. 6
3. A. To ensure the NAV value is not too old
4. C. `getNAV`
5. B. To fetch the current NAV value from an external oracle