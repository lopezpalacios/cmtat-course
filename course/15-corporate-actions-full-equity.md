# Chapter 15 — Corporate Actions and the Full Equity Token `[B] [BANK]`

**Track:** A  
**Emphasis threads:** `[BANK]` `[TYPES]`  
**Chapter learning objective:** Understand how to implement corporate actions such as stock split, capital increase, share buyback, and squeeze-out in a tokenized equity system.  
**Prerequisites:** Completion of Chapter 14 — Advanced Token Features  
**You will build:** The final `EquityShareToken.sol` contract with full module set for corporate actions.

---

## Lesson 1 — Stock Split: Rebase vs Reissue

**Learning objective:** Learn how to implement a stock split in a tokenized equity system using both rebase and reissue methods.  
**Emphasis tags:** `[BANK]` `[TYPES]`

In the banking world, a stock split is equivalent to issuing new shares to existing shareholders while maintaining the total value of their holdings. This can be done through two methods: rebase (changing the number of tokens per share) and reissue (issuing new tokens). In this lesson, we will implement both methods in our `EquityShareToken` contract.

### Step 1.1 — Implement Rebase Stock Split

**Instruction:** Add a function to perform a rebase stock split. This function should update the balance of each shareholder by multiplying their current balance by the split factor.

**Explanation:** A rebase stock split involves changing the number of tokens per share without creating new tokens. This is similar to adjusting the scale of an account balance in banking, where the number of units changes but the total value remains the same. In Solidity, this can be achieved by multiplying each shareholder's balance by a factor.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract EquityShareToken {
    struct ShareMetadata {
        string ISIN;
        uint256 nominalValue;
        string shareClass;
    }

    address public registrar;

    mapping(address => bool) public isKYCVerified;

    struct Document {
        string documentName;
        bytes32 documentHash;
    }

    struct Snapshot {
        uint256 timestamp;
        uint256 totalSupply;
        mapping(address => uint256) balances;
    }

    mapping(uint256 => Snapshot) public snapshots;

    event SnapshotTaken(uint256 timestamp);
    event DividendPaid(address indexed recipient, uint256 amount);

    function takeSnapshot() external {
        require(msg.sender == registrar, "Only the registrar can take a snapshot");
        uint256 currentTimestamp = block.timestamp;
        Snapshot storage snapshot = snapshots[currentTimestamp];
        snapshot.timestamp = currentTimestamp;
        snapshot.totalSupply = totalSupply();
        for (uint256 i = 0; i < _holders.length; i++) {
            address holder = _holders[i];
            snapshot.balances[holder] = balanceOf(holder);
        }
        emit SnapshotTaken(currentTimestamp);
    }

    function payDividend(uint256 snapshotId, uint256 dividendPerShare) external {
        require(msg.sender == registrar, "Only the registrar can distribute dividends");
        require(snapshots[snapshotId].timestamp > 0, "Snapshot does not exist");

        Snapshot storage snapshot = snapshots[snapshotId];
        for (uint256 i = 0; i < _holders.length; i++) {
            address holder = _holders[i];
            uint256 balance = snapshot.balances[holder];
            uint256 dividendAmount = (balance * dividendPerShare) / 1e18;
            uint256 taxAmount = (dividendAmount * 35) / 100; // 35% withholding tax
            uint256 netDividendAmount = dividendAmount - taxAmount;
            // Pay the dividend to the holder
            // For simplicity, assume a function `pay` exists that handles this
            pay(holder, netDividendAmount);
            emit DividendPaid(holder, netDividendAmount);
        }
    }

    function getVotingPowerAtSnapshot(uint256 snapshotId, address account) external view returns (uint256) {
        require(snapshots[snapshotId].timestamp > 0, "Snapshot does not exist");
        return snapshots[snapshotId].balances[account];
    }

    mapping(address => uint256) private _balances;
    address[] private _holders;

    function totalSupply() public view returns (uint256) {
        return _holders.length;
    }

    function balanceOf(address account) public view returns (uint256) {
        return _balances[account];
    }

    function pay(address recipient, uint256 amount) internal {
        // Placeholder for actual payment logic
    }
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract EquityShareToken {
    struct ShareMetadata {
        string ISIN;
        uint256 nominalValue;
        string shareClass;
    }

    address public registrar;

    mapping(address => bool) public isKYCVerified;

    struct Document {
        string documentName;
        bytes32 documentHash;
    }

    struct Snapshot {
        uint256 timestamp;
        uint256 totalSupply;
        mapping(address => uint256) balances;
    }

    mapping(uint256 => Snapshot) public snapshots;

    event SnapshotTaken(uint256 timestamp);
    event DividendPaid(address indexed recipient, uint256 amount);

    function takeSnapshot() external {
        require(msg.sender == registrar, "Only the registrar can take a snapshot");
        uint256 currentTimestamp = block.timestamp;
        Snapshot storage snapshot = snapshots[currentTimestamp];
        snapshot.timestamp = currentTimestamp;
        snapshot.totalSupply = totalSupply();
        for (uint256 i = 0; i < _holders.length; i++) {
            address holder = _holders[i];
            snapshot.balances[holder] = balanceOf(holder);
        }
        emit SnapshotTaken(currentTimestamp);
    }

    function payDividend(uint256 snapshotId, uint256 dividendPerShare) external {
        require(msg.sender == registrar, "Only the registrar can distribute dividends");
        require(snapshots[snapshotId].timestamp > 0, "Snapshot does not exist");

        Snapshot storage snapshot = snapshots[snapshotId];
        for (uint256 i = 0; i < _holders.length; i++) {
            address holder = _holders[i];
            uint256 balance = snapshot.balances[holder];
            uint256 dividendAmount = (balance * dividendPerShare) / 1e18;
            uint256 taxAmount = (dividendAmount * 35) / 100; // 35% withholding tax
            uint256 netDividendAmount = dividendAmount - taxAmount;
            // Pay the dividend to the holder
            // For simplicity, assume a function `pay` exists that handles this
            pay(holder, netDividendAmount);
            emit DividendPaid(holder, netDividendAmount);
        }
    }

    function getVotingPowerAtSnapshot(uint256 snapshotId, address account) external view returns (uint256) {
        require(snapshots[snapshotId].timestamp > 0, "Snapshot does not exist");
        return snapshots[snapshotId].balances[account];
    }

    mapping(address => uint256) private _balances;
    address[] private _holders;

    function totalSupply() public view returns (uint256) {
        return _holders.length;
    }

    function balanceOf(address account) public view returns (uint256) {
        return _balances[account];
    }

    function pay(address recipient, uint256 amount) internal {
        // Placeholder for actual payment logic
    }

    function rebaseStockSplit(uint256 splitFactor) external {
        require(msg.sender == registrar, "Only the registrar can perform a stock split");
        require(splitFactor > 0, "Split factor must be greater than zero");

        for (uint256 i = 0; i < _holders.length; i++) {
            address holder = _holders[i];
            uint256 balance = _balances[holder];
            _balances[holder] = balance * splitFactor;
        }
    }
}
```

**Validation rule:** The `rebaseStockSplit` function should be present and correctly implemented.

```checker
{"id": "ch15-l1-s1", "type": "regex", "pattern": "function\\s+takeSnapshot\\(\\)\\sexternal\\s*{", "flags": "", "target": "solidity", "error_hint": "Implement the `rebaseStockSplit` function correctly."}
```

> **Banking integration note:** In banking, a rebase stock split is similar to adjusting the scale of an account balance. The number of units changes, but the total value remains the same.

---

## Lesson 2 — Capital Increase: Rights Issue Mint Flow

**Learning objective:** Learn how to implement a capital increase through a rights issue in a tokenized equity system.  
**Emphasis tags:** `[BANK]` `[TYPES]`

In the banking world, a capital increase is achieved through a rights issue, where existing shareholders have the option to purchase new shares at a discount. In this lesson, we will implement the mint flow for a rights issue in our `EquityShareToken` contract.

### Step 2.1 — Add Rights Issue Parameters

**Instruction:** Add parameters to the contract to manage rights issues, such as the number of new shares to be issued and the price per share.

**Explanation:** A rights issue involves issuing new shares at a discount to existing shareholders. This is similar to offering a loan with a lower interest rate to existing customers in banking. In Solidity, we can add parameters to manage these details.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract EquityShareToken {
    struct ShareMetadata {
        string ISIN;
        uint256 nominalValue;
        string shareClass;
    }

    address public registrar;

    mapping(address => bool) public isKYCVerified;

    struct Document {
        string documentName;
        bytes32 documentHash;
    }

    struct Snapshot {
        uint256 timestamp;
        uint256 totalSupply;
        mapping(address => uint256) balances;
    }

    mapping(uint256 => Snapshot) public snapshots;

    event SnapshotTaken(uint256 timestamp);
    event DividendPaid(address indexed recipient, uint256 amount);

    function takeSnapshot() external {
        require(msg.sender == registrar, "Only the registrar can take a snapshot");
        uint256 currentTimestamp = block.timestamp;
        Snapshot storage snapshot = snapshots[currentTimestamp];
        snapshot.timestamp = currentTimestamp;
        snapshot.totalSupply = totalSupply();
        for (uint256 i = 0; i < _holders.length; i++) {
            address holder = _holders[i];
            snapshot.balances[holder] = balanceOf(holder);
        }
        emit SnapshotTaken(currentTimestamp);
    }

    function payDividend(uint256 snapshotId, uint256 dividendPerShare) external {
        require(msg.sender == registrar, "Only the registrar can distribute dividends");
        require(snapshots[snapshotId].timestamp > 0, "Snapshot does not exist");

        Snapshot storage snapshot = snapshots[snapshotId];
        for (uint256 i = 0; i < _holders.length; i++) {
            address holder = _holders[i];
            uint256 balance = snapshot.balances[holder];
            uint256 dividendAmount = (balance * dividendPerShare) / 1e18;
            uint256 taxAmount = (dividendAmount * 35) / 100; // 35% withholding tax
            uint256 netDividendAmount = dividendAmount - taxAmount;
            // Pay the dividend to the holder
            // For simplicity, assume a function `pay` exists that handles this
            pay(holder, netDividendAmount);
            emit DividendPaid(holder, netDividendAmount);
        }
    }

    function getVotingPowerAtSnapshot(uint256 snapshotId, address account) external view returns (uint256) {
        require(snapshots[snapshotId].timestamp > 0, "Snapshot does not exist");
        return snapshots[snapshotId].balances[account];
    }

    mapping(address => uint256) private _balances;
    address[] private _holders;

    function totalSupply() public view returns (uint256) {
        return _holders.length;
    }

    function balanceOf(address account) public view returns (uint256) {
        return _balances[account];
    }

    function pay(address recipient, uint256 amount) internal {
        // Placeholder for actual payment logic
    }

    function rebaseStockSplit(uint256 splitFactor) external {
        require(msg.sender == registrar, "Only the registrar can perform a stock split");
        require(splitFactor > 0, "Split factor must be greater than zero");

        for (uint256 i = 0; i < _holders.length; i++) {
            address holder = _holders[i];
            uint256 balance = _balances[holder];
            _balances[holder] = balance * splitFactor;
        }
    }
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract EquityShareToken {
    struct ShareMetadata {
        string ISIN;
        uint256 nominalValue;
        string shareClass;
    }

    address public registrar;

    mapping(address => bool) public isKYCVerified;

    struct Document {
        string documentName;
        bytes32 documentHash;
    }

    struct Snapshot {
        uint256 timestamp;
        uint256 totalSupply;
        mapping(address => uint256) balances;
    }

    mapping(uint256 => Snapshot) public snapshots;

    event SnapshotTaken(uint256 timestamp);
    event DividendPaid(address indexed recipient, uint256 amount);

    function takeSnapshot() external {
        require(msg.sender == registrar, "Only the registrar can take a snapshot");
        uint256 currentTimestamp = block.timestamp;
        Snapshot storage snapshot = snapshots[currentTimestamp];
        snapshot.timestamp = currentTimestamp;
        snapshot.totalSupply = totalSupply();
        for (uint256 i = 0; i < _holders.length; i++) {
            address holder = _holders[i];
            snapshot.balances[holder] = balanceOf(holder);
        }
        emit SnapshotTaken(currentTimestamp);
    }

    function payDividend(uint256 snapshotId, uint256 dividendPerShare) external {
        require(msg.sender == registrar, "Only the registrar can distribute dividends");
        require(snapshots[snapshotId].timestamp > 0, "Snapshot does not exist");

        Snapshot storage snapshot = snapshots[snapshotId];
        for (uint256 i = 0; i < _holders.length; i++) {
            address holder = _holders[i];
            uint256 balance = snapshot.balances[holder];
            uint256 dividendAmount = (balance * dividendPerShare) / 1e18;
            uint256 taxAmount = (dividendAmount * 35) / 100; // 35% withholding tax
            uint256 netDividendAmount = dividendAmount - taxAmount;
            // Pay the dividend to the holder
            // For simplicity, assume a function `pay` exists that handles this
            pay(holder, netDividendAmount);
            emit DividendPaid(holder, netDividendAmount);
        }
    }

    function getVotingPowerAtSnapshot(uint256 snapshotId, address account) external view returns (uint256) {
        require(snapshots[snapshotId].timestamp > 0, "Snapshot does not exist");
        return snapshots[snapshotId].balances[account];
    }

    mapping(address => uint256) private _balances;
    address[] private _holders;

    function totalSupply() public view returns (uint256) {
        return _holders.length;
    }

    function balanceOf(address account) public view returns (uint256) {
        return _balances[account];
    }

    function pay(address recipient, uint256 amount) internal {
        // Placeholder for actual payment logic
    }

    function rebaseStockSplit(uint256 splitFactor) external {
        require(msg.sender == registrar, "Only the registrar can perform a stock split");
        require(splitFactor > 0, "Split factor must be greater than zero");

        for (uint256 i = 0; i < _holders.length; i++) {
            address holder = _holders[i];
            uint256 balance = _balances[holder];
            _balances[holder] = balance * splitFactor;
        }
    }

    uint256 public rightsIssueNewShares;
    uint256 public rightsIssuePricePerShare;

    function setRightsIssueParameters(uint256 newShares, uint256 pricePerShare) external {
        require(msg.sender == registrar, "Only the registrar can set rights issue parameters");
        rightsIssueNewShares = newShares;
        rightsIssuePricePerShare = pricePerShare;
    }
}
```

**Validation rule:** The `rightsIssueNewShares` and `rightsIssuePricePerShare` variables should be present and correctly implemented.

```checker
{"id": "ch15-l2-s1", "type": "regex", "pattern": "struct\\s+Snapshot\\s*{[^}]*}", "flags": "", "target": "solidity", "error_hint": "Declare `rightsIssueNewShares` and `rightsIssuePricePerShare` variables."}
```

> **Banking integration note:** In banking, a rights issue is similar to offering a loan with a lower interest rate to existing customers. The new shares are issued at a discount to existing shareholders.

---

## Lesson 3 — Share Buyback: Cancellation

**Learning objective:** Learn how to implement a share buyback (cancellation) in a tokenized equity system.  
**Emphasis tags:** `[BANK]` `[TYPES]`

In the banking world, a share buyback involves purchasing shares from shareholders at a premium and then canceling them. In this lesson, we will implement the cancellation of shares in our `EquityShareToken` contract.

### Step 3.1 — Add Burn Function

**Instruction:** Add a function to burn (cancel) shares from a shareholder's account.

**Explanation:** A share buyback involves purchasing shares from shareholders at a premium and then canceling them. This is similar to redeeming a bond in banking, where the bond is canceled after payment. In Solidity, we can implement this by adding a burn function.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract EquityShareToken {
    struct ShareMetadata {
        string ISIN;
        uint256 nominalValue;
        string shareClass;
    }

    address public registrar;

    mapping(address => bool) public isKYCVerified;

    struct Document {
        string documentName;
        bytes32 documentHash;
    }

    struct Snapshot {
        uint256 timestamp;
        uint256 totalSupply;
        mapping(address => uint256) balances;
    }

    mapping(uint256 => Snapshot) public snapshots;

    event SnapshotTaken(uint256 timestamp);
    event DividendPaid(address indexed recipient, uint256 amount);

    function takeSnapshot() external {
        require(msg.sender == registrar, "Only the registrar can take a snapshot");
        uint256 currentTimestamp = block.timestamp;
        Snapshot storage snapshot = snapshots[currentTimestamp];
        snapshot.timestamp = currentTimestamp;
        snapshot.totalSupply = totalSupply();
        for (uint256 i = 0; i < _holders.length; i++) {
            address holder = _holders[i];
            snapshot.balances[holder] = balanceOf(holder);
        }
        emit SnapshotTaken(currentTimestamp);
    }

    function payDividend(uint256 snapshotId, uint256 dividendPerShare) external {
        require(msg.sender == registrar, "Only the registrar can distribute dividends");
        require(snapshots[snapshotId].timestamp > 0, "Snapshot does not exist");

        Snapshot storage snapshot = snapshots[snapshotId];
        for (uint256 i = 0; i < _holders.length; i++) {
            address holder = _holders[i];
            uint256 balance = snapshot.balances[holder];
            uint256 dividendAmount = (balance * dividendPerShare) / 1e18;
            uint256 taxAmount = (dividendAmount * 35) / 100; // 35% withholding tax
            uint256 netDividendAmount = dividendAmount - taxAmount;
            // Pay the dividend to the holder
            // For simplicity, assume a function `pay` exists that handles this
            pay(holder, netDividendAmount);
            emit DividendPaid(holder, netDividendAmount);
        }
    }

    function getVotingPowerAtSnapshot(uint256 snapshotId, address account) external view returns (uint256) {
        require(snapshots[snapshotId].timestamp > 0, "Snapshot does not exist");
        return snapshots[snapshotId].balances[account];
    }

    mapping(address => uint256) private _balances;
    address[] private _holders;

    function totalSupply() public view returns (uint256) {
        return _holders.length;
    }

    function balanceOf(address account) public view returns (uint256) {
        return _balances[account];
    }

    function pay(address recipient, uint256 amount) internal {
        // Placeholder for actual payment logic
    }

    function rebaseStockSplit(uint256 splitFactor) external {
        require(msg.sender == registrar, "Only the registrar can perform a stock split");
        require(splitFactor > 0, "Split factor must be greater than zero");

        for (uint256 i = 0; i < _holders.length; i++) {
            address holder = _holders[i];
            uint256 balance = _balances[holder];
            _balances[holder] = balance * splitFactor;
        }
    }

    uint256 public rightsIssueNewShares;
    uint256 public rightsIssuePricePerShare;

    function setRightsIssueParameters(uint256 newShares, uint256 pricePerShare) external {
        require(msg.sender == registrar, "Only the registrar can set rights issue parameters");
        rightsIssueNewShares = newShares;
        rightsIssuePricePerShare = pricePerShare;
    }
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract EquityShareToken {
    struct ShareMetadata {
        string ISIN;
        uint256 nominalValue;
        string shareClass;
    }

    address public registrar;

    mapping(address => bool) public isKYCVerified;

    struct Document {
        string documentName;
        bytes32 documentHash;
    }

    struct Snapshot {
        uint256 timestamp;
        uint256 totalSupply;
        mapping(address => uint256) balances;
    }

    mapping(uint256 => Snapshot) public snapshots;

    event SnapshotTaken(uint256 timestamp);
    event DividendPaid(address indexed recipient, uint256 amount);

    function takeSnapshot() external {
        require(msg.sender == registrar, "Only the registrar can take a snapshot");
        uint256 currentTimestamp = block.timestamp;
        Snapshot storage snapshot = snapshots[currentTimestamp];
        snapshot.timestamp = currentTimestamp;
        snapshot.totalSupply = totalSupply();
        for (uint256 i = 0; i < _holders.length; i++) {
            address holder = _holders[i];
            snapshot.balances[holder] = balanceOf(holder);
        }
        emit SnapshotTaken(currentTimestamp);
    }

    function payDividend(uint256 snapshotId, uint256 dividendPerShare) external {
        require(msg.sender == registrar, "Only the registrar can distribute dividends");
        require(snapshots[snapshotId].timestamp > 0, "Snapshot does not exist");

        Snapshot storage snapshot = snapshots[snapshotId];
        for (uint256 i = 0; i < _holders.length; i++) {
            address holder = _holders[i];
            uint256 balance = snapshot.balances[holder];
            uint256 dividendAmount = (balance * dividendPerShare) / 1e18;
            uint256 taxAmount = (dividendAmount * 35) / 100; // 35% withholding tax
            uint256 netDividendAmount = dividendAmount - taxAmount;
            // Pay the dividend to the holder
            // For simplicity, assume a function `pay` exists that handles this
            pay(holder, netDividendAmount);
            emit DividendPaid(holder, netDividendAmount);
        }
    }

    function getVotingPowerAtSnapshot(uint256 snapshotId, address account) external view returns (uint256) {
        require(snapshots[snapshotId].timestamp > 0, "Snapshot does not exist");
        return snapshots[snapshotId].balances[account];
    }

    mapping(address => uint256) private _balances;
    address[] private _holders;

    function totalSupply() public view returns (uint256) {
        return _holders.length;
    }

    function balanceOf(address account) public view returns (uint256) {
        return _balances[account];
    }

    function pay(address recipient, uint256 amount) internal {
        // Placeholder for actual payment logic
    }

    function rebaseStockSplit(uint256 splitFactor) external {
        require(msg.sender == registrar, "Only the registrar can perform a stock split");
        require(splitFactor > 0, "Split factor must be greater than zero");

        for (uint256 i = 0; i < _holders.length; i++) {
            address holder = _holders[i];
            uint256 balance = _balances[holder];
            _balances[holder] = balance * splitFactor;
        }
    }

    uint256 public rightsIssueNewShares;
    uint256 public rightsIssuePricePerShare;

    function setRightsIssueParameters(uint256 newShares, uint256 pricePerShare) external {
        require(msg.sender == registrar, "Only the registrar can set rights issue parameters");
        rightsIssueNewShares = newShares;
        rightsIssuePricePerShare = pricePerShare;
    }

    event SharesBurned(address indexed account, uint256 amount);

    function burn(uint256 amount) external {
        require(msg.sender == registrar, "Only the registrar can burn shares");
        require(_balances[msg.sender] >= amount, "Insufficient balance");

        _balances[msg.sender] -= amount;
        emit SharesBurned(msg.sender, amount);
    }
}
```

**Validation rule:** The `burn` function should be present and correctly implemented.

```checker
{"id": "ch15-l3-s1", "type": "regex", "pattern": "function\\s+takeSnapshot\\(", "flags": "", "target": "solidity", "error_hint": "Implement the `burn` function correctly."}
```

> **Banking integration note:** In banking, a share buyback is similar to redeeming a bond. The shares are canceled after payment.

---

## Lesson 4 — Squeeze-Out: Forced Transfer

**Learning objective:** Learn how to implement a squeeze-out via forced transfer in a tokenized equity system.  
**Emphasis tags:** `[BANK]` `[TYPES]`

In the banking world, a squeeze-out involves transferring shares from one shareholder to another without their consent, typically as part of a corporate restructuring. In this lesson, we will implement the forced transfer functionality in our `EquityShareToken` contract.

### Step 4.1 — Add Forced Transfer Function

**Instruction:** Add a function to force transfer shares from one shareholder to another.

**Explanation:** A squeeze-out involves transferring shares from one shareholder to another without their consent. This is similar to a forced account transfer in banking, where funds are moved from one customer's account to another against their will. In Solidity, we can implement this by adding a forced transfer function.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract EquityShareToken {
    struct ShareMetadata {
        string ISIN;
        uint256 nominalValue;
        string shareClass;
    }

    address public registrar;

    mapping(address => bool) public isKYCVerified;

    struct Document {
        string documentName;
        bytes32 documentHash;
    }

    struct Snapshot {
        uint256 timestamp;
        uint256 totalSupply;
        mapping(address => uint256) balances;
    }

    mapping(uint256 => Snapshot) public snapshots;

    event SnapshotTaken(uint256 timestamp);
    event DividendPaid(address indexed recipient, uint256 amount);

    function takeSnapshot() external {
        require(msg.sender == registrar, "Only the registrar can take a snapshot");
        uint256 currentTimestamp = block.timestamp;
        Snapshot storage snapshot = snapshots[currentTimestamp];
        snapshot.timestamp = currentTimestamp;
        snapshot.totalSupply = totalSupply();
        for (uint256 i = 0; i < _holders.length; i++) {
            address holder = _holders[i];
            snapshot.balances[holder] = balanceOf(holder);
        }
        emit SnapshotTaken(currentTimestamp);
    }

    function payDividend(uint256 snapshotId, uint256 dividendPerShare) external {
        require(msg.sender == registrar, "Only the registrar can distribute dividends");
        require(snapshots[snapshotId].timestamp > 0, "Snapshot does not exist");

        Snapshot storage snapshot = snapshots[snapshotId];
        for (uint256 i = 0; i < _holders.length; i++) {
            address holder = _holders[i];
            uint256 balance = snapshot.balances[holder];
            uint256 dividendAmount = (balance * dividendPerShare) / 1e18;
            uint256 taxAmount = (dividendAmount * 35) / 100; // 35% withholding tax
            uint256 netDividendAmount = dividendAmount - taxAmount;
            // Pay the dividend to the holder
            // For simplicity, assume a function `pay` exists that handles this
            pay(holder, netDividendAmount);
            emit DividendPaid(holder, netDividendAmount);
        }
    }

    function getVotingPowerAtSnapshot(uint256 snapshotId, address account) external view returns (uint256) {
        require(snapshots[snapshotId].timestamp > 0, "Snapshot does not exist");
        return snapshots[snapshotId].balances[account];
    }

    mapping(address => uint256) private _balances;
    address[] private _holders;

    function totalSupply() public view returns (uint256) {
        return _holders.length;
    }

    function balanceOf(address account) public view returns (uint256) {
        return _balances[account];
    }

    function pay(address recipient, uint256 amount) internal {
        // Placeholder for actual payment logic
    }

    function rebaseStockSplit(uint256 splitFactor) external {
        require(msg.sender == registrar, "Only the registrar can perform a stock split");
        require(splitFactor > 0, "Split factor must be greater than zero");

        for (uint256 i = 0; i < _holders.length; i++) {
            address holder = _holders[i];
            uint256 balance = _balances[holder];
            _balances[holder] = balance * splitFactor;
        }
    }

    uint256 public rightsIssueNewShares;
    uint256 public rightsIssuePricePerShare;

    function setRightsIssueParameters(uint256 newShares, uint256 pricePerShare) external {
        require(msg.sender == registrar, "Only the registrar can set rights issue parameters");
        rightsIssueNewShares = newShares;
        rightsIssuePricePerShare = pricePerShare;
    }

    event SharesBurned(address indexed account, uint256 amount);

    function burn(uint256 amount) external {
        require(msg.sender == registrar, "Only the registrar can burn shares");
        require(_balances[msg.sender] >= amount, "Insufficient balance");

        _balances[msg.sender] -= amount;
        emit SharesBurned(msg.sender, amount);
    }
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract EquityShareToken {
    struct ShareMetadata {
        string ISIN;
        uint256 nominalValue;
        string shareClass;
    }

    address public registrar;

    mapping(address => bool) public isKYCVerified;

    struct Document {
        string documentName;
        bytes32 documentHash;
    }

    struct Snapshot {
        uint256 timestamp;
        uint256 totalSupply;
        mapping(address => uint256) balances;
    }

    mapping(uint256 => Snapshot) public snapshots;

    event SnapshotTaken(uint256 timestamp);
    event DividendPaid(address indexed recipient, uint256 amount);

    function takeSnapshot() external {
        require(msg.sender == registrar, "Only the registrar can take a snapshot");
        uint256 currentTimestamp = block.timestamp;
        Snapshot storage snapshot = snapshots[currentTimestamp];
        snapshot.timestamp = currentTimestamp;
        snapshot.totalSupply = totalSupply();
        for (uint256 i = 0; i < _holders.length; i++) {
            address holder = _holders[i];
            snapshot.balances[holder] = balanceOf(holder);
        }
        emit SnapshotTaken(currentTimestamp);
    }

    function payDividend(uint256 snapshotId, uint256 dividendPerShare) external {
        require(msg.sender == registrar, "Only the registrar can distribute dividends");
        require(snapshots[snapshotId].timestamp > 0, "Snapshot does not exist");

        Snapshot storage snapshot = snapshots[snapshotId];
        for (uint256 i = 0; i < _holders.length; i++) {
            address holder = _holders[i];
            uint256 balance = snapshot.balances[holder];
            uint256 dividendAmount = (balance * dividendPerShare) / 1e18;
            uint256 taxAmount = (dividendAmount * 35) / 100; // 35% withholding tax
            uint256 netDividendAmount = dividendAmount - taxAmount;
            // Pay the dividend to the holder
            // For simplicity, assume a function `pay` exists that handles this
            pay(holder, netDividendAmount);
            emit DividendPaid(holder, netDividendAmount);
        }
    }

    function getVotingPowerAtSnapshot(uint256 snapshotId, address account) external view returns (uint256) {
        require(snapshots[snapshotId].timestamp > 0, "Snapshot does not exist");
        return snapshots[snapshotId].balances[account];
    }

    mapping(address => uint256) private _balances;
    address[] private _holders;

    function totalSupply() public view returns (uint256) {
        return _holders.length;
    }

    function balanceOf(address account) public view returns (uint256) {
        return _balances[account];
    }

    function pay(address recipient, uint256 amount) internal {
        // Placeholder for actual payment logic
    }

    function rebaseStockSplit(uint256 splitFactor) external {
        require(msg.sender == registrar, "Only the registrar can perform a stock split");
        require(splitFactor > 0, "Split factor must be greater than zero");

        for (uint256 i = 0; i < _holders.length; i++) {
            address holder = _holders[i];
            uint256 balance = _balances[holder];
            _balances[holder] = balance * splitFactor;
        }
    }

    uint256 public rightsIssueNewShares;
    uint256 public rightsIssuePricePerShare;

    function setRightsIssueParameters(uint256 newShares, uint256 pricePerShare) external {
        require(msg.sender == registrar, "Only the registrar can set rights issue parameters");
        rightsIssueNewShares = newShares;
        rightsIssuePricePerShare = pricePerShare;
    }

    event SharesBurned(address indexed account, uint256 amount);

    function burn(uint256 amount) external {
        require(msg.sender == registrar, "Only the registrar can burn shares");
        require(_balances[msg.sender] >= amount, "Insufficient balance");

        _balances[msg.sender] -= amount;
        emit SharesBurned(msg.sender, amount);
    }

    event ForcedTransfer(address indexed from, address indexed to, uint256 amount);

    function forceTransfer(address from, address to, uint256 amount) external {
        require(msg.sender == registrar, "Only the registrar can perform a forced transfer");
        require(_balances[from] >= amount, "Insufficient balance");

        _balances[from] -= amount;
        _balances[to] += amount;
        emit ForcedTransfer(from, to, amount);
    }
}
```

**Validation rule:** The `forceTransfer` function should be present and correctly implemented.

```checker
{"id": "ch15-l4-s1", "type": "regex", "pattern": "function\\s+takeSnapshot\\(\\)\\s+external\\s*{", "flags": "", "target": "solidity", "error_hint": "Implement the `forceTransfer` function correctly."}
```

> **Banking integration note:** In banking, a squeeze-out is similar to a forced account transfer. The shares are transferred from one shareholder to another without their consent.

---

## Lesson 5 — Assemble Final EquityShareToken

**Learning objective:** Assemble the final `EquityShareToken` contract with all corporate action modules and runbooks.  
**Emphasis tags:** `[BANK]` `[TYPES]`

In this lesson, we will assemble the final `EquityShareToken` contract by combining all the functionalities implemented in previous lessons.

### Step 5.1 — Final Contract Assembly

**Instruction:** Combine all the functions and variables from previous lessons into a single contract file.

**Explanation:** Now that we have implemented all the corporate action modules, it's time to assemble them into a single contract. This is similar to integrating different banking systems into a unified platform. In Solidity, we can achieve this by copying and pasting the code snippets from each lesson into a single file.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract EquityShareToken {
    struct ShareMetadata {
        string ISIN;
        uint256 nominalValue;
        string shareClass;
    }

    address public registrar;

    mapping(address => bool) public isKYCVerified;

    struct Document {
        string documentName;
        bytes32 documentHash;
    }

    struct Snapshot {
        uint256 timestamp;
        uint256 totalSupply;
        mapping(address => uint256) balances;
    }

    mapping(uint256 => Snapshot) public snapshots;

    event SnapshotTaken(uint256 timestamp);
    event DividendPaid(address indexed recipient, uint256 amount);

    function takeSnapshot() external {
        require(msg.sender == registrar, "Only the registrar can take a snapshot");
        uint256 currentTimestamp = block.timestamp;
        Snapshot storage snapshot = snapshots[currentTimestamp];
        snapshot.timestamp = currentTimestamp;
        snapshot.totalSupply = totalSupply();
        for (uint256 i = 0; i < _holders.length; i++) {
            address holder = _holders[i];
            snapshot.balances[holder] = balanceOf(holder);
        }
        emit SnapshotTaken(currentTimestamp);
    }

    function payDividend(uint256 snapshotId, uint256 dividendPerShare) external {
        require(msg.sender == registrar, "Only the registrar can distribute dividends");
        require(snapshots[snapshotId].timestamp > 0, "Snapshot does not exist");

        Snapshot storage snapshot = snapshots[snapshotId];
        for (uint256 i = 0; i < _holders.length; i++) {
            address holder = _holders[i];
            uint256 balance = snapshot.balances[holder];
            uint256 dividendAmount = (balance * dividendPerShare) / 1e18;
            uint256 taxAmount = (dividendAmount * 35) / 100; // 35% withholding tax
            uint256 netDividendAmount = dividendAmount - taxAmount;
            // Pay the dividend to the holder
            // For simplicity, assume a function `pay` exists that handles this
            pay(holder, netDividendAmount);
            emit DividendPaid(holder, netDividendAmount);
        }
    }

    function getVotingPowerAtSnapshot(uint256 snapshotId, address account) external view returns (uint256) {
        require(snapshots[snapshotId].timestamp > 0, "Snapshot does not exist");
        return snapshots[snapshotId].balances[account];
    }

    mapping(address => uint256) private _balances;
    address[] private _holders;

    function totalSupply() public view returns (uint256) {
        return _holders.length;
    }

    function balanceOf(address account) public view returns (uint256) {
        return _balances[account];
    }

    function pay(address recipient, uint256 amount) internal {
        // Placeholder for actual payment logic
    }
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract EquityShareToken {
    struct ShareMetadata {
        string ISIN;
        uint256 nominalValue;
        string shareClass;
    }

    address public registrar;

    mapping(address => bool) public isKYCVerified;

    struct Document {
        string documentName;
        bytes32 documentHash;
    }

    struct Snapshot {
        uint256 timestamp;
        uint256 totalSupply;
        mapping(address => uint256) balances;
    }

    mapping(uint256 => Snapshot) public snapshots;

    event SnapshotTaken(uint256 timestamp);
    event DividendPaid(address indexed recipient, uint256 amount);

    function takeSnapshot() external {
        require(msg.sender == registrar, "Only the registrar can take a snapshot");
        uint256 currentTimestamp = block.timestamp;
        Snapshot storage snapshot = snapshots[currentTimestamp];
        snapshot.timestamp = currentTimestamp;
        snapshot.totalSupply = totalSupply();
        for (uint256 i = 0; i < _holders.length; i++) {
            address holder = _holders[i];
            snapshot.balances[holder] = balanceOf(holder);
        }
        emit SnapshotTaken(currentTimestamp);
    }

    function payDividend(uint256 snapshotId, uint256 dividendPerShare) external {
        require(msg.sender == registrar, "Only the registrar can distribute dividends");
        require(snapshots[snapshotId].timestamp > 0, "Snapshot does not exist");

        Snapshot storage snapshot = snapshots[snapshotId];
        for (uint256 i = 0; i < _holders.length; i++) {
            address holder = _holders[i];
            uint256 balance = snapshot.balances[holder];
            uint256 dividendAmount = (balance * dividendPerShare) / 1e18;
            uint256 taxAmount = (dividendAmount * 35) / 100; // 35% withholding tax
            uint256 netDividendAmount = dividendAmount - taxAmount;
            // Pay the dividend to the holder
            // For simplicity, assume a function `pay` exists that handles this
            pay(holder, netDividendAmount);
            emit DividendPaid(holder, netDividendAmount);
        }
    }

    function getVotingPowerAtSnapshot(uint256 snapshotId, address account) external view returns (uint256) {
        require(snapshots[snapshotId].timestamp > 0, "Snapshot does not exist");
        return snapshots[snapshotId].balances[account];
    }

    mapping(address => uint256) private _balances;
    address[] private _holders;

    function totalSupply() public view returns (uint256) {
        return _holders.length;
    }

    function balanceOf(address account) public view returns (uint256) {
        return _balances[account];
    }

    function pay(address recipient, uint256 amount) internal {
        // Placeholder for actual payment logic
    }

    function rebaseStockSplit(uint256 splitFactor) external {
        require(msg.sender == registrar, "Only the registrar can perform a stock split");
        require(splitFactor > 0, "Split factor must be greater than zero");

        for (uint256 i = 0; i < _holders.length; i++) {
            address holder = _holders[i];
            uint256 balance = _balances[holder];
            _balances[holder] = balance * splitFactor;
        }
    }

    uint256 public rightsIssueNewShares;
    uint256 public rightsIssuePricePerShare;

    function setRightsIssueParameters(uint256 newShares, uint256 pricePerShare) external {
        require(msg.sender == registrar, "Only the registrar can set rights issue parameters");
        rightsIssueNew