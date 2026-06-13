# Chapter 14 — Snapshots for Dividends and Voting `[B] [BANK-heavy]`

**Track:** equity  
**Emphasis threads:** `[BANK]` `[BANK-heavy]`  
**Chapter learning objective:** Understand and implement the ERC20SnapshotModule pattern, including scheduled snapshots, record-date semantics for dividends and general-assembly voting, dividend computation with withholding tax (35% Swiss) in integer math, and exporting voting power to off-chain GA systems.  
**Prerequisites:** Chapters 1–13  
**You will build:** `contracts/equity/ShareToken.sol` (v2: + snapshots), `java-adapters/DividendDistributionJob.java`

In this chapter, we will dive deep into the ERC20SnapshotModule pattern, which is crucial for managing dividends and voting rights in tokenized securities. This pattern allows us to take snapshots of token holdings at specific points in time, ensuring that dividend distributions and voting power are accurately calculated based on the record date.

## Lesson 1 — Snapshot Management

**Learning objective:** Learn how to implement snapshot management within a Solidity contract.  
**Emphasis tags:** `[BANK]` `[TYPES]`

### Step 1.1 — Define the Snapshot Structure

**Instruction:** Add a `Snapshot` struct to the `ShareToken` contract.

**Explanation:** In core banking systems, snapshots are taken at specific points in time to record the state of accounts for various purposes such as dividend calculations or voting rights. Similarly, we need to define a `Snapshot` struct in our Solidity contract to capture the total supply and the balances at a given point in time. This will help us calculate dividends accurately.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract ShareToken {
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
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract ShareToken {
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
}
```

**Validation rule:** Define the `Snapshot` struct with `timestamp`, `totalSupply`, and `balances`.

```checker
{"id": "ch14-l1-s1", "type": "regex", "pattern": "struct\\s+Snapshot\\s*{[\\s\\S]*uint256\\s+timestamp;[\\s\\S]*uint256\\s+totalSupply;[\\s\\S]*mapping\\(address\\s+=>\\s+uint256\\)\\s+balances;", "flags": "m", "target": "solidity", "error_hint": "Define the Snapshot struct with timestamp, totalSupply, and balances."}
```

### Step 1.2 — Create a Snapshots Mapping

**Instruction:** Add a `snapshots` mapping to store all snapshots.

**Explanation:** Just like in banking systems where multiple snapshots are taken over time for different purposes, we need a way to store all the snapshots of our token holdings. This will allow us to look up the state of the contract at any given point in the past.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract ShareToken {
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
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract ShareToken {
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
}
```

**Validation rule:** Define the `snapshots` mapping.

```checker
{"id": "ch14-l1-s2", "type": "regex", "pattern": "mapping\\(uint256\\s+=>\\s+Snapshot\\)\\s+public\\s+snapshots;", "flags": "m", "target": "solidity", "error_hint": "Define the snapshots mapping."}
```

## Lesson 2 — Taking Snapshots

**Learning objective:** Implement a function to take snapshots of token holdings.  
**Emphasis tags:** `[BANK]` `[TYPES]`

### Step 2.1 — Add Snapshot Functionality

**Instruction:** Implement the `takeSnapshot` function.

**Explanation:** In banking systems, taking a snapshot involves recording the state of accounts at a specific time. Similarly, we need to implement a function in our Solidity contract that records the total supply and balances of all token holders at a given point in time. This will be used for calculating dividends and voting power.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract ShareToken {
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
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract ShareToken {
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
    }

    mapping(address => uint256) private _balances;
    address[] private _holders;

    function totalSupply() public view returns (uint256) {
        return _holders.length;
    }

    function balanceOf(address account) public view returns (uint256) {
        return _balances[account];
    }
}
```

**Validation rule:** Implement the `takeSnapshot` function.

```checker
{"id": "ch14-l2-s1", "type": "regex", "pattern": "function\\s+takeSnapshot\\(\\)\\s+external\\s*{[\\s\\S]*require\\(msg\\.sender\\s==\\sregistrar,\\s\"Only\\s+the\\s+registrar\\s+can\\s+take\\sa\\ssnapshot\"\\);", "flags": "m", "target": "solidity", "error_hint": "Implement the takeSnapshot function with the correct access control."}
```

### Step 2.2 — Add Event for Snapshot

**Instruction:** Emit an event when a snapshot is taken.

**Explanation:** In core banking systems, events are logged whenever important actions occur, such as taking a snapshot. Similarly, we should emit an event in our Solidity contract to log the creation of a new snapshot. This will help us track and audit the snapshots over time.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract ShareToken {
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
    }

    mapping(address => uint256) private _balances;
    address[] private _holders;

    function totalSupply() public view returns (uint256) {
        return _holders.length;
    }

    function balanceOf(address account) public view returns (uint256) {
        return _balances[account];
    }
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract ShareToken {
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

    mapping(address => uint256) private _balances;
    address[] private _holders;

    function totalSupply() public view returns (uint256) {
        return _holders.length;
    }

    function balanceOf(address account) public view returns (uint256) {
        return _balances[account];
    }
}
```

**Validation rule:** Emit the `SnapshotTaken` event.

```checker
{"id": "ch14-l2-s2", "type": "regex", "pattern": "event\\s+SnapshotTaken\\(uint256\\s+timestamp\\);", "flags": "m", "target": "solidity", "error_hint": "Emit the SnapshotTaken event."}
```

## Lesson 3 — Dividend Distribution

**Learning objective:** Implement dividend distribution using snapshots.  
**Emphasis tags:** `[BANK]` `[TYPES]`

### Step 3.1 — Add Dividend Functionality

**Instruction:** Implement the `payDividend` function.

**Explanation:** In core banking systems, dividends are distributed based on the snapshot of token holdings taken at a specific record date. Similarly, we need to implement a function in our Solidity contract that calculates and distributes dividends based on the balances recorded in a snapshot. This will ensure that each token holder receives their fair share of the dividend.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract ShareToken {
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

    mapping(address => uint256) private _balances;
    address[] private _holders;

    function totalSupply() public view returns (uint256) {
        return _holders.length;
    }

    function balanceOf(address account) public view returns (uint256) {
        return _balances[account];
    }
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract ShareToken {
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
            // Pay the dividend to the holder
            // For simplicity, assume a function `pay` exists that handles this
            pay(holder, dividendAmount);
            emit DividendPaid(holder, dividendAmount);
        }
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

**Validation rule:** Implement the `payDividend` function.

```checker
{"id": "ch14-l3-s1", "type": "regex", "pattern": "function\\s+payDividend\\(uint256\\s+snapshotId,\\s+uint256\\s+dividendPerShare\\)\\s+external\\s*{[\\s\\S]*require\\(msg\\.sender\\s==\\sregistrar,\\s\"Only\\s+the\\s+registrar\\s+can\\sdistribute\\sdividends\"\\);", "flags": "m", "target": "solidity", "error_hint": "Implement the payDividend function with the correct access control."}
```

### Step 3.2 — Add Withholding Tax

**Instruction:** Implement withholding tax for dividends.

**Explanation:** In Switzerland, a withholding tax of 35% is applied to dividends paid out to token holders. We need to implement this in our Solidity contract by calculating the tax amount and subtracting it from the dividend payment before sending it to the holder.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract ShareToken {
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
            // Pay the dividend to the holder
            // For simplicity, assume a function `pay` exists that handles this
            pay(holder, dividendAmount);
            emit DividendPaid(holder, dividendAmount);
        }
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

contract ShareToken {
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

**Validation rule:** Implement the withholding tax calculation.

```checker
{"id": "ch14-l3-s2", "type": "regex", "pattern": "uint256\\s+taxAmount\\s=\\s\\(dividendAmount\\s*\\*\\s+35\\)\\s/\\s100;", "flags": "m", "target": "solidity", "error_hint": "Implement the withholding tax calculation."}
```

## Lesson 4 — Voting Power Export

**Learning objective:** Implement functionality to export voting power to off-chain systems.  
**Emphasis tags:** `[BANK]` `[TYPES]`

### Step 4.1 — Add Voting Power Functionality

**Instruction:** Implement the `getVotingPowerAtSnapshot` function.

**Explanation:** In core banking systems, voting power is often exported to off-chain systems for general-assembly voting. Similarly, we need to implement a function in our Solidity contract that allows us to query the voting power of a token holder at a specific snapshot. This will enable off-chain systems to accurately tally votes based on the record date.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract ShareToken {
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

contract ShareToken {
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

**Validation rule:** Implement the `getVotingPowerAtSnapshot` function.

```checker
{"id": "ch14-l4-s1", "type": "regex", "pattern": "function\\s+getVotingPowerAtSnapshot\\(uint256\\s+snapshotId,\\s+address\\s+account\\)\\s+external\\s+view\\s+returns\\s+\\(uint256\\)\\s+\\{", "flags": "m", "target": "solidity", "error_hint": "Implement the getVotingPowerAtSnapshot function."}
```

## Assembled Contract

```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract ShareToken {
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

## Quiz

**Q1 (multiple choice).** Which of the following is a key advantage of using snapshots in dividend distribution for a blockchain-based financial system?
a) Reduces transaction fees — b) Increases transaction speed — c) Ensures accurate record dates — d) Simplifies smart contract deployment
**Answer: c.** Snapshots ensure that dividends are distributed based on the correct number of tokens held by each shareholder at the record date.

**Q2 (multiple choice).** In the context of Solidity and CMTAT, how is the withholding tax for dividends typically handled?
a) By adjusting the dividend amount before distribution — b) Through a separate transaction after distribution — c) Automatically deducted by the blockchain network — d) Exempted from tax in all cases
**Answer: a.** The withholding tax for dividends is typically handled by adjusting the dividend amount before it is distributed to shareholders.

**Q3 (multiple choice).** When exporting voting power to off-chain GA systems, what is the primary purpose of doing so?
a) To increase on-chain transaction speed — b) To allow for more complex voting mechanisms — c) To comply with regulatory requirements — d) To reduce smart contract complexity
**Answer: c.** The primary purpose of exporting voting power to off-chain GA systems is to comply with regulatory requirements.

**Q4 (short answer).** Explain how scheduled snapshots can be beneficial in the context of dividend distribution.
**Answer:** Scheduled snapshots ensure that dividends are distributed based on a consistent and accurate count of tokens held by shareholders at the record date, which helps prevent disputes and ensures fair distribution.

**Q5 (short answer).** Describe one potential challenge when implementing integer math for dividend computation with withholding tax in Solidity.
**Answer:** One potential challenge is ensuring precision and accuracy in calculations to avoid rounding errors that could lead to incorrect dividend amounts or tax deductions.
