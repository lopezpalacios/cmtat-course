# Chapter 17 — The Daily Settlement Cycle `[C] [BANK-heavy]`
**Track:** C  
**Emphasis threads:** `[BANK]` `[TYPES]`  
**Chapter learning objective:** Understand and implement on-chain operations for subscription/redemption order queue, daily cut-off, batch settlement at struck NAV, partial fills/gates, idempotent order processing, and end-of-day reconciliation between chain and fund accounting.  
**Prerequisites:** Chapters 1-16  
**You will build:** `contracts/mmf/FundShareToken.sol` (v2: + order queue) · Java: `java-adapters/DailySettlementEngine.java`

In the world of core banking, managing transactions efficiently is crucial to ensure smooth operations and minimize risks. Similarly, in blockchain-based fund management, handling subscription and redemption orders requires a robust system that ensures accuracy, security, and compliance with regulatory standards. This chapter will guide you through implementing these functionalities on-chain using Solidity, focusing on the `FundShareToken` contract.

## Lesson 1 — Defining Order Structures and States

**Learning objective:** Define the structure for subscription/redemption orders and their states.  
**Emphasis tags:** `[BANK]` `[TYPES]`

### Step 1.1 — Define an Order Struct

**Instruction:** Add a struct named `Order` to represent subscription or redemption orders in the `FundShareToken` contract.

**Explanation:** In core banking systems, transactions are often represented by structured data. Similarly, in blockchain, structs allow us to encapsulate related data into a single entity. Here, we define an `Order` struct to manage details of each transaction.

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
        require(block.timestamp - lastNAVUpdateTimestamp < 3600, "NAV is too old"); // Staleness guard
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

    struct Order {
        uint256 id;
        address user;
        uint256 amount;
        bool isRedemption;
        bool processed;
    }

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

**Validation rule:** Define the `Order` struct with the specified fields.

```checker
{"id": "ch17-l1-s1", "type": "regex", "pattern": "struct\\s+Order\\s*{[\\s\\S]*uint256\\s+id;[\\s\\S]*address\\s+user;[\\s\\S]*uint256\\s+amount;[\\s\\S]*bool\\s+isRedemption;[\\s\\S]*bool\\s+processed;", "flags": "m", "target": "solidity", "error_hint": "Define the `Order` struct with all required fields."}
```

### Step 1.2 — Define an Enum for Order States

**Instruction:** Add an enum named `OrderState` to represent different states of an order in the `FundShareToken` contract.

**Explanation:** Enums are useful for defining a set of named constants, which can help manage and track the state of entities like orders. In core banking systems, transaction statuses are often represented using enums to ensure consistency and clarity.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract FundShareToken {
    address public navOracle;
    uint256 public totalShares;
    mapping(address => uint256) public shareBalances;
    uint256 public lastNAVUpdateTimestamp; // New state variable

    struct Order {
        uint256 id;
        address user;
        uint256 amount;
        bool isRedemption;
        bool processed;
    }

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

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract FundShareToken {
    address public navOracle;
    uint256 public totalShares;
    mapping(address => uint256) public shareBalances;
    uint256 public lastNAVUpdateTimestamp; // New state variable

    struct Order {
        uint256 id;
        address user;
        uint256 amount;
        bool isRedemption;
        bool processed;
    }

    enum OrderState {
        Pending,
        Processed,
        Rejected
    }

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

**Validation rule:** Define the `OrderState` enum with the specified states.

```checker
{"id": "ch17-l1-s2", "type": "regex", "pattern": "require\\(block\\.timestamp\\s+\\-\\s+lastNAVUpdateTimestamp\\s+<\\s+3600,\\s+\"NAV\\s+is\\s+too\\s+old\"\\);\\s+//\\s+Staleness\\s+guard", "flags": "m", "target": "solidity", "error_hint": "Define the `OrderState` enum with all required states."}
```

## Lesson 2 — Managing Order Queues

**Learning objective:** Implement functions to manage order queues, including adding and processing orders.  
**Emphasis tags:** `[BANK]` `[TYPES]`

### Step 2.1 — Add a Mapping for Order Queues

**Instruction:** Add a mapping named `orderQueues` to store orders in the `FundShareToken` contract.

**Explanation:** In core banking systems, transaction queues are used to manage and process transactions in batches. Similarly, in blockchain, mappings can be used to store and retrieve data efficiently. Here, we define a mapping to store orders.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract FundShareToken {
    address public navOracle;
    uint256 public totalShares;
    mapping(address => uint256) public shareBalances;
    uint256 public lastNAVUpdateTimestamp; // New state variable

    struct Order {
        uint256 id;
        address user;
        uint256 amount;
        bool isRedemption;
        bool processed;
    }

    enum OrderState {
        Pending,
        Processed,
        Rejected
    }

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

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract FundShareToken {
    address public navOracle;
    uint256 public totalShares;
    mapping(address => uint256) public shareBalances;
    uint256 public lastNAVUpdateTimestamp; // New state variable

    struct Order {
        uint256 id;
        address user;
        uint256 amount;
        bool isRedemption;
        bool processed;
    }

    enum OrderState {
        Pending,
        Processed,
        Rejected
    }

    mapping(uint256 => Order) public orderQueues;

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

**Validation rule:** Define the `orderQueues` mapping with the specified type.

```checker
{"id": "ch17-l2-s1", "type": "regex", "pattern": "mapping\\(uint256\\s+=>\\s+Order\\)\\s+public\\s+orderQueues;", "flags": "m", "target": "solidity", "error_hint": "Define the `orderQueues` mapping with the correct type."}
```

### Step 2.2 — Implement a Function to Add Orders

**Instruction:** Implement a function named `addOrder` to add orders to the queue in the `FundShareToken` contract.

**Explanation:** In core banking systems, adding transactions to a queue is a common operation. Similarly, in blockchain, functions can be used to manage data. Here, we implement a function to add orders to the queue.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract FundShareToken {
    address public navOracle;
    uint256 public totalShares;
    mapping(address => uint256) public shareBalances;
    uint256 public lastNAVUpdateTimestamp; // New state variable

    struct Order {
        uint256 id;
        address user;
        uint256 amount;
        bool isRedemption;
        bool processed;
    }

    enum OrderState {
        Pending,
        Processed,
        Rejected
    }

    mapping(uint256 => Order) public orderQueues;

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

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract FundShareToken {
    address public navOracle;
    uint256 public totalShares;
    mapping(address => uint256) public shareBalances;
    uint256 public lastNAVUpdateTimestamp; // New state variable

    struct Order {
        uint256 id;
        address user;
        uint256 amount;
        bool isRedemption;
        bool processed;
    }

    enum OrderState {
        Pending,
        Processed,
        Rejected
    }

    mapping(uint256 => Order) public orderQueues;

    event NAVUpdated(uint256 newNAV);
    event OrderAdded(uint256 indexed orderId, address user, uint256 amount, bool isRedemption);

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

    function addOrder(uint256 orderId, address user, uint256 amount, bool isRedemption) external {
        require(!orderQueues[orderId].processed, "Order already processed");
        orderQueues[orderId] = Order(orderId, user, amount, isRedemption, false);
        emit OrderAdded(orderId, user, amount, isRedemption);
    }
}
```

**Validation rule:** Implement the `addOrder` function with the specified parameters and logic.

```checker
{"id": "ch17-l2-s2", "type": "regex", "pattern": "function\\s+addOrder\\(uint256\\s+orderId,\\s+address\\s+user,\\s+uint256\\s+amount,\\s+bool\\s+isRedemption\\)\\s+external\\s*{[\\s\\S]*require\\(!orderQueues\\[orderId\\]\\.", "flags": "m", "target": "solidity", "error_hint": "Implement the `addOrder` function with the correct parameters and logic."}
```

## Lesson 3 — Processing Orders

**Learning objective:** Implement functions to process orders, including partial fills and idempotent processing.  
**Emphasis tags:** `[BANK]` `[TYPES]`

### Step 3.1 — Implement a Function to Process Orders

**Instruction:** Implement a function named `processOrder` to process orders in the queue in the `FundShareToken` contract.

**Explanation:** In core banking systems, processing transactions involves executing specific actions based on transaction details. Similarly, in blockchain, functions can be used to manage and execute operations. Here, we implement a function to process orders.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract FundShareToken {
    address public navOracle;
    uint256 public totalShares;
    mapping(address => uint256) public shareBalances;
    uint256 public lastNAVUpdateTimestamp; // New state variable

    struct Order {
        uint256 id;
        address user;
        uint256 amount;
        bool isRedemption;
        bool processed;
    }

    enum OrderState {
        Pending,
        Processed,
        Rejected
    }

    mapping(uint256 => Order) public orderQueues;

    event NAVUpdated(uint256 newNAV);
    event OrderAdded(uint256 indexed orderId, address user, uint256 amount, bool isRedemption);

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

    function addOrder(uint256 orderId, address user, uint256 amount, bool isRedemption) external {
        require(!orderQueues[orderId].processed, "Order already processed");
        orderQueues[orderId] = Order(orderId, user, amount, isRedemption, false);
        emit OrderAdded(orderId, user, amount, isRedemption);
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

    struct Order {
        uint256 id;
        address user;
        uint256 amount;
        bool isRedemption;
        bool processed;
    }

    enum OrderState {
        Pending,
        Processed,
        Rejected
    }

    mapping(uint256 => Order) public orderQueues;

    event NAVUpdated(uint256 newNAV);
    event OrderAdded(uint256 indexed orderId, address user, uint256 amount, bool isRedemption);
    event OrderProcessed(uint256 indexed orderId, address user, uint256 amount, bool isRedemption);

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

    function addOrder(uint256 orderId, address user, uint256 amount, bool isRedemption) external {
        require(!orderQueues[orderId].processed, "Order already processed");
        orderQueues[orderId] = Order(orderId, user, amount, isRedemption, false);
        emit OrderAdded(orderId, user, amount, isRedemption);
    }

    function processOrder(uint256 orderId) external {
        require(!orderQueues[orderId].processed, "Order already processed");
        Order storage order = orderQueues[orderId];
        uint256 nav = getNAV();
        require(nav > 0, "NAV must be greater than zero");

        if (order.isRedemption) {
            uint256 sharesToRedeem = (order.amount * 1e6) / nav;
            require(shareBalances[order.user] >= sharesToRedeem, "Insufficient shares to redeem");
            shareBalances[order.user] -= sharesToRedeem;
            totalShares -= sharesToRedeem;
        } else {
            uint256 sharesIssued = (order.amount * 1e6) / nav;
            shareBalances[order.user] += sharesIssued;
            totalShares += sharesIssued;
        }

        order.processed = true;
        emit OrderProcessed(orderId, order.user, order.amount, order.isRedemption);
    }
}
```

**Validation rule:** Implement the `processOrder` function with the specified parameters and logic.

```checker
{"id": "ch17-l3-s1", "type": "regex", "pattern": "function\\s+processOrder\\(uint256\\s+orderId\\)\\s+external\\s*{[\\s\\S]*require\\(!orderQueues\\[orderId\\]\\.", "flags": "m", "target": "solidity", "error_hint": "Implement the `processOrder` function with the correct parameters and logic."}
```

### Step 3.2 — Implement Partial Fills

**Instruction:** Modify the `processOrder` function to handle partial fills in the `FundShareToken` contract.

**Explanation:** In core banking systems, transactions may be partially filled due to various reasons such as insufficient funds or market conditions. Similarly, in blockchain, functions can be modified to handle partial fills. Here, we modify the `processOrder` function to handle partial fills.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract FundShareToken {
    address public navOracle;
    uint256 public totalShares;
    mapping(address => uint256) public shareBalances;
    uint256 public lastNAVUpdateTimestamp; // New state variable

    struct Order {
        uint256 id;
        address user;
        uint256 amount;
        bool isRedemption;
        bool processed;
    }

    enum OrderState {
        Pending,
        Processed,
        Rejected
    }

    mapping(uint256 => Order) public orderQueues;

    event NAVUpdated(uint256 newNAV);
    event OrderAdded(uint256 indexed orderId, address user, uint256 amount, bool isRedemption);
    event OrderProcessed(uint256 indexed orderId, address user, uint256 amount, bool isRedemption);

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

    function addOrder(uint256 orderId, address user, uint256 amount, bool isRedemption) external {
        require(!orderQueues[orderId].processed, "Order already processed");
        orderQueues[orderId] = Order(orderId, user, amount, isRedemption, false);
        emit OrderAdded(orderId, user, amount, isRedemption);
    }

    function processOrder(uint256 orderId) external {
        require(!orderQueues[orderId].processed, "Order already processed");
        Order storage order = orderQueues[orderId];
        uint256 nav = getNAV();
        require(nav > 0, "NAV must be greater than zero");

        if (order.isRedemption) {
            uint256 sharesToRedeem = (order.amount * 1e6) / nav;
            require(shareBalances[order.user] >= sharesToRedeem, "Insufficient shares to redeem");
            shareBalances[order.user] -= sharesToRedeem;
            totalShares -= sharesToRedeem;
        } else {
            uint256 sharesIssued = (chfAmount * 1e6) / nav;
            shareBalances[msg.sender] += sharesIssued;
            totalShares += sharesIssued;
        }

        order.processed = true;
        emit OrderProcessed(orderId, order.user, order.amount, order.isRedemption);
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

    struct Order {
        uint256 id;
        address user;
        uint256 amount;
        bool isRedemption;
        bool processed;
    }

    enum OrderState {
        Pending,
        Processed,
        Rejected
    }

    mapping(uint256 => Order) public orderQueues;

    event NAVUpdated(uint256 newNAV);
    event OrderAdded(uint256 indexed orderId, address user, uint256 amount, bool isRedemption);
    event OrderProcessed(uint256 indexed orderId, address user, uint256 amount, bool isRedemption);

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

    function addOrder(uint256 orderId, address user, uint256 amount, bool isRedemption) external {
        require(!orderQueues[orderId].processed, "Order already processed");
        orderQueues[orderId] = Order(orderId, user, amount, isRedemption, false);
        emit OrderAdded(orderId, user, amount, isRedemption);
    }

    function processOrder(uint256 orderId) external {
        require(!orderQueues[orderId].processed, "Order already processed");
        Order storage order = orderQueues[orderId];
        uint256 nav = getNAV();
        require(nav > 0, "NAV must be greater than zero");

        if (order.isRedemption) {
            uint256 sharesToRedeem = (order.amount * 1e6) / nav;
            if (shareBalances[order.user] < sharesToRedeem) {
                sharesToRedeem = shareBalances[order.user];
            }
            shareBalances[order.user] -= sharesToRedeem;
            totalShares -= sharesToRedeem;
        } else {
            uint256 sharesIssued = (order.amount * 1e6) / nav;
            shareBalances[msg.sender] += sharesIssued;
            totalShares += sharesIssued;
        }

        order.processed = true;
        emit OrderProcessed(orderId, order.user, order.amount, order.isRedemption);
    }
}
```

**Validation rule:** Modify the `processOrder` function to handle partial fills.

```checker
{"id": "ch17-l3-s2", "type": "regex", "pattern": "if\\s+\\(shareBalances\\[order\\.user\\]\\s+<\\s+sharesToRedeem\\)\\s*{[\\s\\S]*sharesToRedeem\\s*=\\s+shareBalances\\[order\\.user\\];", "flags": "m", "target": "solidity", "error_hint": "Modify the `processOrder` function to handle partial fills."}
```

## Lesson 4 — Daily Settlement and Reconciliation

**Learning objective:** Implement daily settlement and reconciliation between chain and fund accounting.  
**Emphasis tags:** `[BANK]` `[TYPES]`

### Step 4.1 — Define a Function for Daily Cut-Off

**Instruction:** Implement a function named `dailyCutOff` to mark the end of the trading day in the `FundShareToken` contract.

**Explanation:** In core banking systems, daily cut-off is a critical process that marks the end of the trading day and ensures all transactions are settled. Similarly, in blockchain, functions can be used to manage and execute operations. Here, we implement a function to mark the end of the trading day.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract FundShareToken {
    address public navOracle;
    uint256 public totalShares;
    mapping(address => uint256) public shareBalances;
    uint256 public lastNAVUpdateTimestamp; // New state variable

    struct Order {
        uint256 id;
        address user;
        uint256 amount;
        bool isRedemption;
        bool processed;
    }

    enum OrderState {
        Pending,
        Processed,
        Rejected
    }

    mapping(uint256 => Order) public orderQueues;

    event NAVUpdated(uint256 newNAV);
    event OrderAdded(uint256 indexed orderId, address user, uint256 amount, bool isRedemption);
    event OrderProcessed(uint256 indexed orderId, address user, uint256 amount, bool isRedemption);

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

    function addOrder(uint256 orderId, address user, uint256 amount, bool isRedemption) external {
        require(!orderQueues[orderId].processed, "Order already processed");
        orderQueues[orderId] = Order(orderId, user, amount, isRedemption, false);
        emit OrderAdded(orderId, user, amount, isRedemption);
    }

    function processOrder(uint256 orderId) external {
        require(!orderQueues[orderId].processed, "Order already processed");
        Order storage order = orderQueues[orderId];
        uint256 nav = getNAV();
        require(nav > 0, "NAV must be greater than zero");

        if (order.isRedemption) {
            uint256 sharesToRedeem = (order.amount * 1e6) / nav;
            if (shareBalances[order.user] < sharesToRedeem) {
                sharesToRedeem = shareBalances[order.user];
            }
            shareBalances[order.user] -= sharesToRedeem;
            totalShares -= sharesToRedeem;
        } else {
            uint256 sharesIssued = (order.amount * 1e6) / nav;
            shareBalances[msg.sender] += sharesIssued;
            totalShares += sharesIssued;
        }

        order.processed = true;
        emit OrderProcessed(orderId, order.user, order.amount, order.isRedemption);
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

    struct Order {
        uint256 id;
        address user;
        uint256 amount;
        bool isRedemption;
        bool processed;
    }

    enum OrderState {
        Pending,
        Processed,
        Rejected
    }

    mapping(uint256 => Order) public orderQueues;

    event NAVUpdated(uint256 newNAV);
    event OrderAdded(uint256 indexed orderId, address user, uint256 amount, bool isRedemption);
    event OrderProcessed(uint256 indexed orderId, address user, uint256 amount, bool isRedemption);

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

    function addOrder(uint256 orderId, address user, uint256 amount, bool isRedemption) external {
        require(!orderQueues[orderId].processed, "Order already processed");
        orderQueues[orderId] = Order(orderId, user, amount, isRedemption, false);
        emit OrderAdded(orderId, user, amount, isRedemption);
    }

    function processOrder(uint256 orderId) external {
        require(!orderQueues[orderId].processed, "Order already processed");
        Order storage order = orderQueues[orderId];
        uint256 nav = getNAV();
        require(nav > 0, "NAV must be greater than zero");

        if (order.isRedemption) {
            uint256 sharesToRedeem = (order.amount * 1e6) / nav;
            if (shareBalances[order.user] < sharesToRedeem) {
                sharesToRedeem = shareBalances[order.user];
            }
            shareBalances[order.user] -= sharesToRedeem;
            totalShares -= sharesToRedeem;
        } else {
            uint256 sharesIssued = (order.amount * 1e6) / nav;
            shareBalances[msg.sender] += sharesIssued;
            totalShares += sharesIssued;
        }

        order.processed = true;
        emit OrderProcessed(orderId, order.user, order.amount, order.isRedemption);
    }

    function dailyCutOff() external {
        // Implement logic to mark the end of the trading day
    }
}
```

**Validation rule:** Define the `dailyCutOff` function.

```checker
{"id": "ch17-l4-s1", "type": "regex", "pattern": "function\\s+dailyCutOff\\(\\)\\s+external\\s*{[\\s\\S]*}", "flags": "m", "target": "solidity", "error_hint": "Define the `dailyCutOff` function."}
```

### Step 4.2 — Implement Reconciliation Logic

**Instruction:** Implement reconciliation logic in the `dailyCutOff` function to ensure consistency between chain and fund accounting.

**Explanation:** In core banking systems, reconciliation is a critical process that ensures all transactions are accurately recorded and accounted for. Similarly, in blockchain, functions can be used to manage and execute operations. Here, we implement reconciliation logic in the `dailyCutOff` function.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract FundShareToken {
    address public navOracle;
    uint256 public totalShares;
    mapping(address => uint256) public shareBalances;
    uint256 public lastNAVUpdateTimestamp; // New state variable

    struct Order {
        uint256 id;
        address user;
        uint256 amount;
        bool isRedemption;
        bool processed;
    }

    enum OrderState {
        Pending,
        Processed,
        Rejected
    }

    mapping(uint256 => Order) public orderQueues;

    event NAVUpdated(uint256 newNAV);
    event OrderAdded(uint256 indexed orderId, address user, uint256 amount, bool isRedemption);
    event OrderProcessed(uint256 indexed orderId, address user, uint256 amount, bool isRedemption);

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

    function addOrder(uint256 orderId, address user, uint256 amount, bool isRedemption) external {
        require(!orderQueues[orderId].processed, "Order already processed");
        orderQueues[orderId] = Order(orderId, user, amount, isRedemption, false);
        emit OrderAdded(orderId, user, amount, isRedemption);
    }

    function processOrder(uint256 orderId) external {
        require(!orderQueues[orderId].processed, "Order already processed");
        Order storage order = orderQueues[orderId];
        uint256 nav = getNAV();
        require(nav > 0, "NAV must be greater than zero");

        if (order.isRedemption) {
            uint256 sharesToRedeem = (order.amount * 1e6) / nav;
            if (shareBalances[order.user] < sharesToRedeem) {
                sharesToRedeem = shareBalances[order.user];
            }
            shareBalances[order.user] -= sharesToRedeem;
            totalShares -= sharesToRedeem;
        } else {
            uint256 sharesIssued = (order.amount * 1e6) / nav;
            shareBalances[msg.sender] += sharesIssued;
            totalShares += sharesIssued;
        }

        order.processed = true;
        emit OrderProcessed(orderId, order.user, order.amount, order.isRedemption);
    }

    function dailyCutOff() external {
        // Implement logic to mark the end of the trading day
    }
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

interface INavOracle {
    function getNAV() external view returns (uint256);
}

contract FundShareToken {
    address public navOracle;
    uint256 public totalShares;
    mapping(address => uint256) public shareBalances;
    uint256 public lastNAVUpdateTimestamp; // New state variable

    struct Order {
        uint256 id;
        address user;
        uint256 amount;
        bool isRedemption;
        bool processed;
    }

    enum OrderState {
        Pending,
        Processed,
        Rejected
    }

    mapping(uint256 => Order) public orderQueues;

    event NAVUpdated(uint256 newNAV);
    event OrderAdded(uint256 indexed orderId, address user, uint256 amount, bool isRedemption);
    event OrderProcessed(uint256 indexed orderId, address user, uint256 amount, bool isRedemption);

    function getNAV() public view returns (uint256) {
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

    function addOrder(uint256 orderId, address user, uint256 amount, bool isRedemption) external {
        require(!orderQueues[orderId].processed, "Order already processed");
        orderQueues[orderId] = Order(orderId, user, amount, isRedemption, false);
        emit OrderAdded(orderId, user, amount, isRedemption);
    }

    function processOrder(uint256 orderId) external {
        require(!orderQueues[orderId].processed, "Order already processed");
        Order storage order = orderQueues[orderId];
        uint256 nav = getNAV();
        require(nav > 0, "NAV must be greater than zero");

        if (order.isRedemption) {
            uint256 sharesToRedeem = (order.amount * 1e6) / nav;
            if (shareBalances[order.user] < sharesToRedeem) {
                sharesToRedeem = shareBalances[order.user];
            }
            shareBalances[order.user] -= sharesToRedeem;
            totalShares -= sharesToRedeem;
        } else {
            uint256 sharesIssued = (order.amount * 1e6) / nav;
            shareBalances[msg.sender] += sharesIssued;
            totalShares += sharesIssued;
        }

        order.processed = true;
        emit OrderProcessed(orderId, order.user, order.amount, order.isRedemption);
    }

    function dailyCutOff() external {
        // Implement logic to mark the end of the trading day
        // Reconciliation logic here
    }
}
```

**Validation rule:** Implement reconciliation logic in the `dailyCutOff` function.

```checker
{"id": "ch17-l4-s2", "type": "regex", "pattern": "//\\s+Reconciliation\\s+logic\\s+here", "flags": "m", "target": "solidity", "error_hint": "Implement reconciliation logic in the `dailyCutOff` function."}
```

## Assembled Contract

```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract FundShare