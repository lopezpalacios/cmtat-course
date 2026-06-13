# Chapter 18 — Redemptions, Gates, and the Full MMF Share `[C] [BANK] [TYPES]`

**Track:** C  
**Emphasis threads:** `[BANK]` `[TYPES]`  
**Chapter learning objective:** Implement redemption payout flow with off-chain CHF leg, liquidity gates & redemption suspension (pause + custom gates), fee accrual in integer math. Assemble final MoneyMarketFundShare with full CMTAT module set + complete daily-cycle scenario.  
**Prerequisites:** Completion of Chapter 17. Understanding of Solidity, event-driven programming, and basic financial concepts.  
**You will build:** A fully functional `MoneyMarketFundShare` contract with redemption capabilities, liquidity gates, and suspension mechanisms.

---

## Lesson 1 — Redemption Suspension and Pause Mechanism

**Learning objective:** Implement a mechanism to suspend redemptions either globally or through custom gates.  
**Emphasis tags:** `[BANK]` `[TYPES]`

In core banking systems, there are times when transactions need to be suspended due to system maintenance or regulatory requirements. Similarly, in our `MoneyMarketFundShare` contract, we need a way to suspend redemptions either globally or based on specific conditions.

### Step 1.1 — Add Suspension Flags

**Instruction:** Add two boolean flags to the contract: one for global suspension and another for custom gates.

**Explanation:** In banking systems, there are often flags that indicate whether certain transactions are allowed. We'll implement similar flags in our contract to control redemptions. The `globalSuspension` flag will suspend all redemptions, while the `customGates` flag can be used to implement more complex suspension logic.

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
        // Reconciliation logic here
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

    bool public globalSuspension = false;
    bool public customGates = false;

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
        // Reconciliation logic here
    }
}
```

**Validation rule:** Declare `globalSuspension` and `customGates` as public boolean flags.

```checker
{"id": "ch18-l1-s1", "type": "regex", "pattern": "bool\\s+public\\s+globalSuspension\\s*=\\s*false;", "flags": "m", "target": "solidity", "error_hint": "Declare `globalSuspension` as a public boolean flag initialized to false."}
```

```checker
{"id": "ch18-l1-s2", "type": "regex", "pattern": "bool\\s+public\\s+customGates\\s*=\\s*false;", "flags": "m", "target": "solidity", "error_hint": "Declare `customGates` as a public boolean flag initialized to false."}
```

> **Banking integration note:** In banking systems, suspension flags are often used to control transaction flows. Similarly, in our contract, these flags will help manage redemptions.

---

## Lesson 2 — Implementing Redemption Suspension Logic

**Learning objective:** Implement logic to check and enforce redemption suspension based on the flags.  
**Emphasis tags:** `[BANK]` `[TYPES]`

In banking systems, transactions are often subject to various checks before being processed. We need to implement similar checks in our contract to ensure that redemptions are only processed when allowed.

### Step 2.1 — Modify `processOrder` to Check Suspension Flags

**Instruction:** Update the `processOrder` function to check both `globalSuspension` and `customGates` before processing a redemption order.

**Explanation:** In banking systems, transactions are often subject to various checks before being processed. We need to implement similar checks in our contract to ensure that redemptions are only processed when allowed. The `processOrder` function should now check the suspension flags before proceeding with any redemption logic.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract FundShareToken {
    address public navOracle;
    uint256 public totalShares;
    mapping(address => uint256) public shareBalances;
    uint256 public lastNAVUpdateTimestamp; // New state variable

    bool public globalSuspension = false;
    bool public customGates = false;

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
        // Reconciliation logic here
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

    bool public globalSuspension = false;
    bool public customGates = false;

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
            require(!globalSuspension && !customGates, "Redemptions are suspended");
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

**Validation rule:** Ensure `processOrder` checks both `globalSuspension` and `customGates`.

```checker
{"id": "ch18-l2-s1", "type": "regex", "pattern": "require\\(!globalSuspension\\s*&&\\s!*customGates,\\s*\"Redemptions are suspended\"\\);", "flags": "m", "target": "solidity", "error_hint": "Add a check to ensure redemptions are not processed when either `globalSuspension` or `customGates` is true."}
```

> **Banking integration note:** In banking systems, transactions are often subject to various checks before being processed. Similarly, in our contract, these checks will help manage redemptions.

---

## Lesson 3 — Implementing Fee Accrual

**Learning objective:** Implement a mechanism to accrue fees during the redemption process using integer math.  
**Emphasis tags:** `[BANK]` `[TYPES]`

In banking systems, fees are often accrued and deducted from transactions. We need to implement similar logic in our contract to ensure that fees are correctly calculated and deducted during redemptions.

### Step 3.1 — Add Fee Parameters

**Instruction:** Add two parameters to the contract: `redemptionFeeRate` (in basis points) and `feeAccrued`.

**Explanation:** In banking systems, fees are often accrued and deducted from transactions. We need to implement similar logic in our contract to ensure that fees are correctly calculated and deducted during redemptions. The `redemptionFeeRate` will determine the percentage of the redemption amount that is charged as a fee, and `feeAccrued` will keep track of the total fees accrued.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract FundShareToken {
    address public navOracle;
    uint256 public totalShares;
    mapping(address => uint256) public shareBalances;
    uint256 public lastNAVUpdateTimestamp; // New state variable

    bool public globalSuspension = false;
    bool public customGates = false;

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
            require(!globalSuspension && !customGates, "Redemptions are suspended");
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

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract FundShareToken {
    address public navOracle;
    uint256 public totalShares;
    mapping(address => uint256) public shareBalances;
    uint256 public lastNAVUpdateTimestamp; // New state variable

    bool public globalSuspension = false;
    bool public customGates = false;

    uint256 public redemptionFeeRate = 100; // 1% fee
    uint256 public feeAccrued = 0;

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
            require(!globalSuspension && !customGates, "Redemptions are suspended");
            uint256 sharesToRedeem = (order.amount * 1e6) / nav;
            if (shareBalances[order.user] < sharesToRedeem) {
                sharesToRedeem = shareBalances[order.user];
            }
            uint256 feeAmount = (sharesToRedeem * redemptionFeeRate) / 10000; // Calculate fee in integer math
            feeAccrued += feeAmount;
            sharesToRedeem -= feeAmount;
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

**Validation rule:** Declare `redemptionFeeRate` and `feeAccrued` as public state variables.

```checker
{"id": "ch18-l3-s1", "type": "regex", "pattern": "uint256\\s+public\\s+redemptionFeeRate\\s*=\\s*100;", "flags": "m", "target": "solidity", "error_hint": "Declare `redemptionFeeRate` as a public uint256 variable initialized to 100 (1%)."}
```

```checker
{"id": "ch18-l3-s2", "type": "regex", "pattern": "uint256\\s+public\\s+feeAccrued\\s*=\\s*0;", "flags": "m", "target": "solidity", "error_hint": "Declare `feeAccrued` as a public uint256 variable initialized to 0."}
```

> **Banking integration note:** In banking systems, fees are often accrued and deducted from transactions. Similarly, in our contract, these checks will help manage redemptions.

---

## Lesson 4 — Implementing Liquidity Gates

**Learning objective:** Implement a mechanism to check liquidity before processing redemptions.  
**Emphasis tags:** `[BANK]` `[TYPES]`

In banking systems, there are often liquidity checks to ensure that transactions can be processed without causing insolvency. We need to implement similar logic in our contract to ensure that redemptions do not deplete the fund's liquidity below a certain threshold.

### Step 4.1 — Add Liquidity Threshold Parameter

**Instruction:** Add a parameter `liquidityThreshold` to the contract.

**Explanation:** In banking systems, there are often liquidity checks to ensure that transactions can be processed without causing insolvency. We need to implement similar logic in our contract to ensure that redemptions do not deplete the fund's liquidity below a certain threshold. The `liquidityThreshold` will determine the minimum amount of CHF that must be maintained in the fund.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract FundShareToken {
    address public navOracle;
    uint256 public totalShares;
    mapping(address => uint256) public shareBalances;
    uint256 public lastNAVUpdateTimestamp; // New state variable

    bool public globalSuspension = false;
    bool public customGates = false;

    uint256 public redemptionFeeRate = 100; // 1% fee
    uint256 public feeAccrued = 0;

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
            require(!globalSuspension && !customGates, "Redemptions are suspended");
            uint256 sharesToRedeem = (order.amount * 1e6) / nav;
            if (shareBalances[order.user] < sharesToRedeem) {
                sharesToRedeem = shareBalances[order.user];
            }
            uint256 feeAmount = (sharesToRedeem * redemptionFeeRate) / 10000; // Calculate fee in integer math
            feeAccrued += feeAmount;
            sharesToRedeem -= feeAmount;
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

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract FundShareToken {
    address public navOracle;
    uint256 public totalShares;
    mapping(address => uint256) public shareBalances;
    uint256 public lastNAVUpdateTimestamp; // New state variable

    bool public globalSuspension = false;
    bool public customGates = false;

    uint256 public redemptionFeeRate = 100; // 1% fee
    uint256 public feeAccrued = 0;
    uint256 public liquidityThreshold = 1000000; // Minimum CHF to maintain

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
            require(!globalSuspension && !customGates, "Redemptions are suspended");
            uint256 sharesToRedeem = (order.amount * 1e6) / nav;
            if (shareBalances[order.user] < sharesToRedeem) {
                sharesToRedeem = shareBalances[order.user];
            }
            uint256 feeAmount = (sharesToRedeem * redemptionFeeRate) / 10000; // Calculate fee in integer math
            feeAccrued += feeAmount;
            sharesToRedeem -= feeAmount;
            require(totalShares - sharesToRedeem >= liquidityThreshold, "Liquidity threshold breached");
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

**Validation rule:** Declare `liquidityThreshold` as a public state variable.

```checker
{"id": "ch18-l4-s1", "type": "regex", "pattern": "uint256\\s+public\\s+liquidityThreshold\\s*=\\s*1000000;", "flags": "m", "target": "solidity", "error_hint": "Declare `liquidityThreshold` as a public uint256 variable initialized to 1000000 (CHF)."}
```

> **Banking integration note:** In banking systems, liquidity checks are crucial to ensure that transactions can be processed without causing insolvency. Similarly, in our contract, these checks will help manage redemptions.

---

## Lesson 5 — Implementing Daily Cut-Off Logic

**Learning objective:** Implement logic to mark the end of the trading day and perform reconciliation.  
**Emphasis tags:** `[BANK]` `[TYPES]`

In banking systems, there is a daily cut-off process where all transactions for the day are settled and reconciled. We need to implement similar logic in our contract to ensure that redemptions are processed correctly at the end of each trading day.

### Step 5.1 — Implement `dailyCutOff` Function

**Instruction:** Implement the `dailyCutOff` function to mark the end of the trading day and perform any necessary reconciliation.

**Explanation:** In banking systems, there is a daily cut-off process where all transactions for the day are settled and reconciled. We need to implement similar logic in our contract to ensure that redemptions are processed correctly at the end of each trading day. The `dailyCutOff` function will reset any necessary state variables and perform any required reconciliation.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract FundShareToken {
    address public navOracle;
    uint256 public totalShares;
    mapping(address => uint256) public shareBalances;
    uint256 public lastNAVUpdateTimestamp; // New state variable

    bool public globalSuspension = false;
    bool public customGates = false;

    uint256 public redemptionFeeRate = 100; // 1% fee
    uint256 public feeAccrued = 0;
    uint256 public liquidityThreshold = 1000000; // Minimum CHF to maintain

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
            require(!globalSuspension && !customGates, "Redemptions are suspended");
            uint256 sharesToRedeem = (order.amount * 1e6) / nav;
            if (shareBalances[order.user] < sharesToRedeem) {
                sharesToRedeem = shareBalances[order.user];
            }
            uint256 feeAmount = (sharesToRedeem * redemptionFeeRate) / 10000; // Calculate fee in integer math
            feeAccrued += feeAmount;
            sharesToRedeem -= feeAmount;
            require(totalShares - sharesToRedeem >= liquidityThreshold, "Liquidity threshold breached");
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

    bool public globalSuspension = false;
    bool public customGates = false;

    uint256 public redemptionFeeRate = 100; // 1% fee
    uint256 public feeAccrued = 0;
    uint256 public liquidityThreshold = 1000000; // Minimum CHF to maintain

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
            require(!globalSuspension && !customGates, "Redemptions are suspended");
            uint256 sharesToRedeem = (order.amount * 1e6) / nav;
            if (shareBalances[order.user] < sharesToRedeem) {
                sharesToRedeem = shareBalances[order.user];
            }
            uint256 feeAmount = (sharesToRedeem * redemptionFeeRate) / 10000; // Calculate fee in integer math
            feeAccrued += feeAmount;
            sharesToRedeem -= feeAmount;
            require(totalShares - sharesToRedeem >= liquidityThreshold, "Liquidity threshold breached");
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
        // Reset any necessary state variables
        globalSuspension = false;
        customGates = false;
        feeAccrued = 0;

        // Perform reconciliation logic here
        // For example, distribute accrued fees to the fund manager or other stakeholders
    }
}
```

**Validation rule:** Implement `dailyCutOff` function with reset and reconciliation logic.

```checker
{"id": "ch18-l5-s1", "type": "regex", "pattern": "function\\s+dailyCutOff\\(\\)\\s+

## Quiz

**Q1 (multiple choice).** Which of the following is a key component in implementing liquidity gates to ensure that redemptions do not deplete the fund's reserves below a certain threshold?
a) Redemption Suspension — b) Fee Accrual — c) Liquidity Gate Threshold — d) Daily Cut-Off Logic
**Answer: c.** The liquidity gate threshold ensures that the fund maintains sufficient reserves by preventing redemptions when they would fall below this predefined level.

**Q2 (multiple choice).** In the context of implementing a redemption payout flow with an off-chain CHF leg, which of the following is essential to ensure accurate fee accrual?
a) Integer Math — b) Floating Point Arithmetic — c) Off-Chain Oracle — d) Redemption Pause
**Answer: a.** Using integer math for fee accrual ensures precise calculations without the rounding errors associated with floating point arithmetic.

**Q3 (multiple choice).** When implementing redemption suspension logic, which of the following mechanisms would you use to temporarily halt redemptions?
a) Liquidity Gate — b) Daily Cut-Off Logic — c) Redemption Pause — d) Fee Accrual
**Answer: c.** The redemption pause mechanism is specifically designed to temporarily halt redemptions when necessary.

**Q4 (short answer).** Explain how implementing a daily cut-off logic can affect the redemption process in a MoneyMarketFundShare.
**Answer:** Implementing daily cut-off logic ensures that all redemption requests are processed up to a specific time each day, preventing last-minute redemptions that could disrupt the fund's liquidity or exposure management. This helps maintain consistent operations and risk control.

**Q5 (short answer).** Describe how fee accrual in integer math can be beneficial for a MoneyMarketFundShare.
**Answer:** Fee accrual in integer math provides precise and predictable calculations, avoiding the rounding errors that can occur with floating point arithmetic. This ensures accurate tracking of fees, which is crucial for maintaining transparency and compliance in financial operations.
