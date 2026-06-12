# Chapter 06 — CMTAT Architecture: A Module Tour `[shared] [BANK] [TYPES]`

**Track:** shared  
**Emphasis threads:** `[BANK]` `[TYPES]`  
**Chapter learning objective:** Understand the architecture of the CMTAT standard, including its various modules such as ERC20BaseModule, BaseModule, PauseModule, EnforcementModule, ERC20SnapshotModule, ValidationModule, DocumentModule, DebtModule, and AuthorizationModule. Learn how these modules interact to provide a comprehensive tokenized securities solution.  
**Prerequisites:** Basic understanding of Solidity, familiarity with the CMTAT standard, and experience with Java/.NET core-banking systems.  
**You will build:** A skeleton base contract for the CMTAT architecture.

The CMTAT (Custodian Managed Tokenized Asset) standard is a comprehensive framework designed to manage tokenized securities in compliance with Swiss financial regulations. It decomposes into several modules, each responsible for specific functionalities such as asset management, pause functionality, enforcement of rules, snapshotting, validation, documentation, debt handling, and authorization. This chapter will explore these modules and build the skeleton base contract that integrates them.

## Lesson 1 — Introduction to CMTAT Modules

**Learning objective:** Understand the purpose and interaction of different CMTAT modules.  
**Emphasis tags:** `[BANK]` `[TYPES]`  
**Track:** shared

In a core-banking system, various modules work together to manage customer accounts, transactions, and compliance checks. Similarly, in the CMTAT standard, each module handles specific aspects of tokenized securities management. For instance, the `PauseModule` allows pausing critical operations during audits or emergencies, while the `ValidationModule` ensures that all transactions comply with predefined rules.

### Step 1.1 — Define the Base Contract

**Instruction:** Create a new file named `CMTATBase.sol`. Start by defining the basic structure of the contract, including the necessary imports and the base contract declaration.

**Explanation:** Just as in a core-banking system where different modules inherit from a common base class to share functionalities, the CMTAT modules will inherit from a base contract. This base contract will serve as the foundation for all other modules.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract CMTATBase {
    // define here
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";

contract CMTATBase is ERC20 {
    constructor(string memory name, string memory symbol) ERC20(name, symbol) {}
}
```

**Validation rule:** The contract should inherit from the `ERC20` contract and have a constructor that initializes the token with a name and symbol.

```checker
{"id": "ch06-l1-s1", "type": "regex", "pattern": "import\\s+\"@openzeppelin\\/contracts\\/token\\/ERC20\\/ERC20\\.sol\";", "flags": "m", "target": "solidity", "error_hint": "Import the ERC20 contract from OpenZeppelin."}
```

### Step 1.2 — Add Basic State Variables

**Instruction:** Inside the `CMTATBase` contract, add two public state variables: `admin` of type `address` and `paused` of type `bool`.

**Explanation:** In a core-banking system, there are often admin-level controls to manage user access and operations. Similarly, in CMTAT, an `admin` address can control certain functionalities, and a `paused` flag can be used to halt critical operations.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";

contract CMTATBase is ERC20 {
    constructor(string memory name, string memory symbol) ERC20(name, symbol) {}

    // define here
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";

contract CMTATBase is ERC20 {
    address public admin;
    bool public paused;

    constructor(string memory name, string memory symbol) ERC20(name, symbol) {}
}
```

**Validation rule:** Both state variables `admin` and `paused` should be declared as public.

```checker
{"id": "ch06-l1-s2", "type": "regex", "pattern": "address\\s+public\\s+admin\\s*;[\\s\\S]*bool\\s+public\\s+paused\\s*;", "flags": "m", "target": "solidity", "error_hint": "Declare both `admin` and `paused` as public."}
```

## Lesson 2 — PauseModule Implementation

**Learning objective:** Implement the basic functionality of the PauseModule.  
**Emphasis tags:** `[BANK]` `[TYPES]`  
**Track:** shared

The `PauseModule` is crucial for temporarily halting critical operations, such as transfers or approvals, during audits or emergencies. This module will include functions to pause and unpause the contract.

### Step 2.1 — Add Pause Functions

**Instruction:** Inside the `CMTATBase` contract, add two functions: `pause()` and `unpause()`. These functions should only be callable by the `admin`.

**Explanation:** In a core-banking system, there are often emergency stop mechanisms to prevent unauthorized transactions during critical times. Similarly, in CMTAT, the `PauseModule` provides such functionality.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";

contract CMTATBase is ERC20 {
    address public admin;
    bool public paused;

    constructor(string memory name, string memory symbol) ERC20(name, symbol) {}

    // define here
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";

contract CMTATBase is ERC20 {
    address public admin;
    bool public paused;

    constructor(string memory name, string memory symbol) ERC20(name, symbol) {}

    function pause() external {
        require(msg.sender == admin, "Not authorized");
        paused = true;
    }

    function unpause() external {
        require(msg.sender == admin, "Not authorized");
        paused = false;
    }
}
```

**Validation rule:** Both `pause()` and `unpause()` functions should be defined with the correct access control.

```checker
{"id": "ch06-l2-s1", "type": "regex", "pattern": "function\\s+pause\\(\\)\\s+external\\s*{", "flags": "", "target": "solidity", "error_hint": "Define both `pause()` and `unpause()` functions with the correct access control."}
```

### Step 2.2 — Modify Transfer Function

**Instruction:** Override the `transfer` function in the `CMTATBase` contract to include a check that prevents transfers when the contract is paused.

**Explanation:** In a core-banking system, there are often checks to prevent transactions during critical times. Similarly, in CMTAT, the `PauseModule` ensures that no transfers occur when the contract is paused.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";

contract CMTATBase is ERC20 {
    address public admin;
    bool public paused;

    constructor(string memory name, string memory symbol) ERC20(name, symbol) {}

    function pause() external {
        require(msg.sender == admin, "Not authorized");
        paused = true;
    }

    function unpause() external {
        require(msg.sender == admin, "Not authorized");
        paused = false;
    }

    // define here
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";

contract CMTATBase is ERC20 {
    address public admin;
    bool public paused;

    constructor(string memory name, string memory symbol) ERC20(name, symbol) {}

    function pause() external {
        require(msg.sender == admin, "Not authorized");
        paused = true;
    }

    function unpause() external {
        require(msg.sender == admin, "Not authorized");
        paused = false;
    }

    function transfer(address to, uint256 amount) public override returns (bool) {
        require(!paused, "Contract is paused");
        return super.transfer(to, amount);
    }
}
```

**Validation rule:** The `transfer` function should include a check that prevents transfers when the contract is paused.

```checker
{"id": "ch06-l2-s2", "type": "regex", "pattern": "require\\(!paused,\\s*\"Contract\\s+is\\s+paused\"\\);", "flags": "m", "target": "solidity", "error_hint": "Add a check to prevent transfers when the contract is paused."}
```

## Lesson 3 — BaseModule Implementation

**Learning objective:** Implement the basic functionality of the BaseModule.  
**Emphasis tags:** `[BANK]` `[TYPES]`  
**Track:** shared

The `BaseModule` manages token information such as tokenId, terms, and other metadata. This module will include functions to set and get these details.

### Step 3.1 — Add Token Information State Variables

**Instruction:** Inside the `CMTATBase` contract, add three public state variables: `tokenId` of type `uint256`, `terms` of type `string`, and `information` of type `string`.

**Explanation:** In a core-banking system, tokens often have unique identifiers and associated metadata. Similarly, in CMTAT, the `BaseModule` manages these details.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";

contract CMTATBase is ERC20 {
    address public admin;
    bool public paused;
    uint256 public tokenId;
    string public terms;
    string public information;

    constructor(string memory name, string memory symbol) ERC20(name, symbol) {}

    function pause() external {
        require(msg.sender == admin, "Not authorized");
        paused = true;
    }

    function unpause() external {
        require(msg.sender == admin, "Not authorized");
        paused = false;
    }

    function transfer(address to, uint256 amount) public override returns (bool) {
        require(!paused, "Contract is paused");
        return super.transfer(to, amount);
    }
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";

contract CMTATBase is ERC20 {
    address public admin;
    bool public paused;
    uint256 public tokenId;
    string public terms;
    string public information;

    constructor(string memory name, string memory symbol) ERC20(name, symbol) {}

    function pause() external {
        require(msg.sender == admin, "Not authorized");
        paused = true;
    }

    function unpause() external {
        require(msg.sender == admin, "Not authorized");
        paused = false;
    }

    function transfer(address to, uint256 amount) public override returns (bool) {
        require(!paused, "Contract is paused");
        return super.transfer(to, amount);
    }
}
```

**Validation rule:** All three state variables `tokenId`, `terms`, and `information` should be declared as public.

```checker
{"id": "ch06-l3-s1", "type": "regex", "pattern": "uint256\\s+public\\s+tokenId\\s*;[\\s\\S]*string\\s+public\\s+terms\\s*;[\\s\\S]*string\\s+public\\s+information\\s*;", "flags": "m", "target": "solidity", "error_hint": "Declare `tokenId`, `terms`, and `information` as public."}
```

### Step 3.2 — Add Functions to Set Token Information

**Instruction:** Inside the `CMTATBase` contract, add three functions: `setTokenId(uint256 _tokenId)`, `setTerms(string memory _terms)`, and `setInformation(string memory _information)`. These functions should only be callable by the `admin`.

**Explanation:** In a core-banking system, there are often admin-level controls to manage token metadata. Similarly, in CMTAT, the `BaseModule` provides such functionality.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";

contract CMTATBase is ERC20 {
    address public admin;
    bool public paused;
    uint256 public tokenId;
    string public terms;
    string public information;

    constructor(string memory name, string memory symbol) ERC20(name, symbol) {}

    function pause() external {
        require(msg.sender == admin, "Not authorized");
        paused = true;
    }

    function unpause() external {
        require(msg.sender == admin, "Not authorized");
        paused = false;
    }

    function transfer(address to, uint256 amount) public override returns (bool) {
        require(!paused, "Contract is paused");
        return super.transfer(to, amount);
    }

    // define here
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";

contract CMTATBase is ERC20 {
    address public admin;
    bool public paused;
    uint256 public tokenId;
    string public terms;
    string public information;

    constructor(string memory name, string memory symbol) ERC20(name, symbol) {}

    function pause() external {
        require(msg.sender == admin, "Not authorized");
        paused = true;
    }

    function unpause() external {
        require(msg.sender == admin, "Not authorized");
        paused = false;
    }

    function transfer(address to, uint256 amount) public override returns (bool) {
        require(!paused, "Contract is paused");
        return super.transfer(to, amount);
    }

    function setTokenId(uint256 _tokenId) external {
        require(msg.sender == admin, "Not authorized");
        tokenId = _tokenId;
    }

    function setTerms(string memory _terms) external {
        require(msg.sender == admin, "Not authorized");
        terms = _terms;
    }

    function setInformation(string memory _information) external {
        require(msg.sender == admin, "Not authorized");
        information = _information;
    }
}
```

**Validation rule:** All three functions `setTokenId`, `setTerms`, and `setInformation` should be defined with the correct access control.

```checker
{"id": "ch06-l3-s2", "type": "regex", "pattern": "function\\s+setTokenId\\(uint256\\s+_tokenId\\)\\s+external\\s*{", "flags": "m", "target": "solidity", "error_hint": "Define all three functions with the correct access control."}
```

## Lesson 4 — EnforcementModule Implementation

**Learning objective:** Implement the basic functionality of the EnforcementModule.  
**Emphasis tags:** `[BANK]` `[TYPES]`  
**Track:** shared

The `EnforcementModule` is responsible for enforcing rules such as freezing token transfers for specific addresses. This module will include functions to freeze and unfreeze addresses.

### Step 4.1 — Add Freeze State Variable

**Instruction:** Inside the `CMTATBase` contract, add a public state variable `frozen` of type `mapping(address => bool)`.

**Explanation:** In a core-banking system, there are often mechanisms to freeze accounts or transactions for specific users. Similarly, in CMTAT, the `EnforcementModule` provides such functionality.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";

contract CMTATBase is ERC20 {
    address public admin;
    bool public paused;
    uint256 public tokenId;
    string public terms;
    string public information;
    mapping(address => bool) public frozen;

    constructor(string memory name, string memory symbol) ERC20(name, symbol) {}

    function pause() external {
        require(msg.sender == admin, "Not authorized");
        paused = true;
    }

    function unpause() external {
        require(msg.sender == admin, "Not authorized");
        paused = false;
    }

    function transfer(address to, uint256 amount) public override returns (bool) {
        require(!paused, "Contract is paused");
        return super.transfer(to, amount);
    }

    function setTokenId(uint256 _tokenId) external {
        require(msg.sender == admin, "Not authorized");
        tokenId = _tokenId;
    }

    function setTerms(string memory _terms) external {
        require(msg.sender == admin, "Not authorized");
        terms = _terms;
    }

    function setInformation(string memory _information) external {
        require(msg.sender == admin, "Not authorized");
        information = _information;
    }
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";

contract CMTATBase is ERC20 {
    address public admin;
    bool public paused;
    uint256 public tokenId;
    string public terms;
    string public information;
    mapping(address => bool) public frozen;

    constructor(string memory name, string memory symbol) ERC20(name, symbol) {}

    function pause() external {
        require(msg.sender == admin, "Not authorized");
        paused = true;
    }

    function unpause() external {
        require(msg.sender == admin, "Not authorized");
        paused = false;
    }

    function transfer(address to, uint256 amount) public override returns (bool) {
        require(!paused, "Contract is paused");
        return super.transfer(to, amount);
    }

    function setTokenId(uint256 _tokenId) external {
        require(msg.sender == admin, "Not authorized");
        tokenId = _tokenId;
    }

    function setTerms(string memory _terms) external {
        require(msg.sender == admin, "Not authorized");
        terms = _terms;
    }

    function setInformation(string memory _information) external {
        require(msg.sender == admin, "Not authorized");
        information = _information;
    }
}
```

**Validation rule:** The `frozen` mapping should be declared as public.

```checker
{"id": "ch06-l4-s1", "type": "regex", "pattern": "mapping\\(address\\s+=>\\s+bool\\)\\s+public\\s+frozen\\s*;", "flags": "m", "target": "solidity", "error_hint": "Declare `frozen` as a public mapping."}
```

### Step 4.2 — Add Freeze and Unfreeze Functions

**Instruction:** Inside the `CMTATBase` contract, add two functions: `freeze(address _address)` and `unfreeze(address _address)`. These functions should only be callable by the `admin`.

**Explanation:** In a core-banking system, there are often mechanisms to freeze accounts or transactions for specific users. Similarly, in CMTAT, the `EnforcementModule` provides such functionality.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";

contract CMTATBase is ERC20 {
    address public admin;
    bool public paused;
    uint256 public tokenId;
    string public terms;
    string public information;
    mapping(address => bool) public frozen;

    constructor(string memory name, string memory symbol) ERC20(name, symbol) {}

    function pause() external {
        require(msg.sender == admin, "Not authorized");
        paused = true;
    }

    function unpause() external {
        require(msg.sender == admin, "Not authorized");
        paused = false;
    }

    function transfer(address to, uint256 amount) public override returns (bool) {
        require(!paused, "Contract is paused");
        return super.transfer(to, amount);
    }

    function setTokenId(uint256 _tokenId) external {
        require(msg.sender == admin, "Not authorized");
        tokenId = _tokenId;
    }

    function setTerms(string memory _terms) external {
        require(msg.sender == admin, "Not authorized");
        terms = _terms;
    }

    function setInformation(string memory _information) external {
        require(msg.sender == admin, "Not authorized");
        information = _information;
    }

    // define here
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";

contract CMTATBase is ERC20 {
    address public admin;
    bool public paused;
    uint256 public tokenId;
    string public terms;
    string public information;
    mapping(address => bool) public frozen;

    constructor(string memory name, string memory symbol) ERC20(name, symbol) {}

    function pause() external {
        require(msg.sender == admin, "Not authorized");
        paused = true;
    }

    function unpause() external {
        require(msg.sender == admin, "Not authorized");
        paused = false;
    }

    function transfer(address to, uint256 amount) public override returns (bool) {
        require(!paused, "Contract is paused");
        return super.transfer(to, amount);
    }

    function setTokenId(uint256 _tokenId) external {
        require(msg.sender == admin, "Not authorized");
        tokenId = _tokenId;
    }

    function setTerms(string memory _terms) external {
        require(msg.sender == admin, "Not authorized");
        terms = _terms;
    }

    function setInformation(string memory _information) external {
        require(msg.sender == admin, "Not authorized");
        information = _information;
    }

    function freeze(address _address) external {
        require(msg.sender == admin, "Not authorized");
        frozen[_address] = true;
    }

    function unfreeze(address _address) external {
        require(msg.sender == admin, "Not authorized");
        frozen[_address] = false;
    }
}
```

**Validation rule:** Both `freeze` and `unfreeze` functions should be defined with the correct access control.

```checker
{"id": "ch06-l4-s2", "type": "regex", "pattern": "function\\s+freeze\\(address\\s+_address\\)\\s+external\\s*{[^}]*}", "flags": "", "target": "solidity", "error_hint": "Define both `freeze` and `unfreeze` functions with the correct access control."}
```

## Lesson 5 — ERC20SnapshotModule Implementation

**Learning objective:** Implement the basic functionality of the ERC20SnapshotModule.  
**Emphasis tags:** `[BANK]` `[TYPES]`  
**Track:** shared

The `ERC20SnapshotModule` allows taking snapshots of token balances at specific points in time, which is useful for audits and compliance checks.

### Step 5.1 — Add Snapshot State Variables

**Instruction:** Inside the `CMTATBase` contract, add two public state variables: `snapshotId` of type `uint256` and `snapshots` of type `mapping(uint256 => mapping(address => uint256))`.

**Explanation:** In a core-banking system, snapshots are often taken to record the state of accounts at specific times. Similarly, in CMTAT, the `ERC20SnapshotModule` provides such functionality.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";

contract CMTATBase is ERC20 {
    address public admin;
    bool public paused;
    uint256 public tokenId;
    string public terms;
    string public information;
    mapping(address => bool) public frozen;
    uint256 public snapshotId;
    mapping(uint256 => mapping(address => uint256)) public snapshots;

    constructor(string memory name, string memory symbol) ERC20(name, symbol) {}

    function pause() external {
        require(msg.sender == admin, "Not authorized");
        paused = true;
    }

    function unpause() external {
        require(msg.sender == admin, "Not authorized");
        paused = false;
    }

    function transfer(address to, uint256 amount) public override returns (bool) {
        require(!paused, "Contract is paused");
        return super.transfer(to, amount);
    }

    function setTokenId(uint256 _tokenId) external {
        require(msg.sender == admin, "Not authorized");
        tokenId = _tokenId;
    }

    function setTerms(string memory _terms) external {
        require(msg.sender == admin, "Not authorized");
        terms = _terms;
    }

    function setInformation(string memory _information) external {
        require(msg.sender == admin, "Not authorized");
        information = _information;
    }

    function freeze(address _address) external {
        require(msg.sender == admin, "Not authorized");
        frozen[_address] = true;
    }

    function unfreeze(address _address) external {
        require(msg.sender == admin, "Not authorized");
        frozen[_address] = false;
    }
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";

contract CMTATBase is ERC20 {
    address public admin;
    bool public paused;
    uint256 public tokenId;
    string public terms;
    string public information;
    mapping(address => bool) public frozen;
    uint256 public snapshotId;
    mapping(uint256 => mapping(address => uint256)) public snapshots;

    constructor(string memory name, string memory symbol) ERC20(name, symbol) {}

    function pause() external {
        require(msg.sender == admin, "Not authorized");
        paused = true;
    }

    function unpause() external {
        require(msg.sender == admin, "Not authorized");
        paused = false;
    }

    function transfer(address to, uint256 amount) public override returns (bool) {
        require(!paused, "Contract is paused");
        return super.transfer(to, amount);
    }

    function setTokenId(uint256 _tokenId) external {
        require(msg.sender == admin, "Not authorized");
        tokenId = _tokenId;
    }

    function setTerms(string memory _terms) external {
        require(msg.sender == admin, "Not authorized");
        terms = _terms;
    }

    function setInformation(string memory _information) external {
        require(msg.sender == admin, "Not authorized");
        information = _information;
    }

    function freeze(address _address) external {
        require(msg.sender == admin, "Not authorized");
        frozen[_address] = true;
    }

    function unfreeze(address _address) external {
        require(msg.sender == admin, "Not authorized");
        frozen[_address] = false;
    }
}
```

**Validation rule:** Both `snapshotId` and `snapshots` mappings should be declared as public.

```checker
{"id": "ch06-l5-s1", "type": "regex", "pattern": "uint256\\s+public\\s+snapshotId\\s*;[\\s\\S]*mapping\\(uint256\\s+=>\\s+mapping\\(address\\s+=>\\s+uint256\\)\\)\\s+public\\s+snapshots\\s*;", "flags": "m", "target": "solidity", "error_hint": "Declare `snapshotId` and `snapshots` as public."}
```

### Step 5.2 — Add Snapshot Function

**Instruction:** Inside the `CMTATBase` contract, add a function `takeSnapshot()` that takes a snapshot of the current token balances and increments the `snapshotId`.

**Explanation:** In a core-banking system, snapshots are often taken to record the state of accounts at specific times. Similarly, in CMTAT, the `ERC20SnapshotModule` provides such functionality.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";

contract CMTATBase is ERC20 {
    address public admin;
    bool public paused;
    uint256 public tokenId;
    string public terms;
    string public information;
    mapping(address => bool) public frozen;
    uint256 public snapshotId;
    mapping(uint256 => mapping(address => uint256)) public snapshots;

    constructor(string memory name, string memory symbol) ERC20(name, symbol) {}

    function pause() external {
        require(msg.sender == admin, "Not authorized");
        paused = true;
    }

    function unpause() external {
        require(msg.sender == admin, "Not authorized");
        paused = false;
    }

    function transfer(address to, uint256 amount) public override returns (bool) {
        require(!paused, "Contract is paused");
        return super.transfer(to, amount);
    }

    function setTokenId(uint256 _tokenId) external {
        require(msg.sender == admin, "Not authorized");
        tokenId = _tokenId;
    }

    function setTerms(string memory _terms) external {
        require(msg.sender == admin, "Not authorized");
        terms = _terms;
    }

    function setInformation(string memory _information) external {
        require(msg.sender == admin, "Not authorized");
        information = _information;
    }

    function freeze(address _address) external {
        require(msg.sender == admin, "Not authorized");
        frozen[_address] = true;
    }

    function unfreeze(address _address) external {
        require(msg.sender == admin, "Not authorized");
        frozen[_address] = false;
    }

    // define here
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";

contract CMTATBase is ERC20 {
    address public admin;
    bool public paused;
    uint256 public tokenId;
    string public terms;
    string public information;
    mapping(address => bool) public frozen;
    uint256 public snapshotId;
    mapping(uint256 => mapping(address => uint256)) public snapshots;

    constructor(string memory name, string memory symbol) ERC20(name, symbol) {}

    function pause() external {
        require(msg.sender == admin, "Not authorized");
        paused = true;
    }

    function unpause() external {
        require(msg.sender == admin, "Not authorized");
        paused = false;
    }

    function transfer(address to, uint256 amount) public override returns (bool) {
        require(!paused, "Contract is paused");
        return super.transfer(to, amount);
    }

    function setTokenId(uint256 _tokenId) external {
        require(msg.sender == admin, "Not authorized");
        tokenId = _tokenId;
    }

    function setTerms(string memory _terms) external {
        require(msg.sender == admin, "Not authorized");
        terms = _terms;
    }

    function setInformation(string memory _information) external {
        require(msg.sender == admin, "Not authorized");
        information = _information;
    }

    function freeze(address _address) external {
        require(msg.sender == admin, "Not authorized");
        frozen[_address] = true;
    }

    function unfreeze(address _address) external {
        require(msg.sender == admin, "Not authorized");
        frozen[_address] = false;
    }

    function takeSnapshot() external {
        require(msg.sender == admin, "Not authorized");
        snapshotId++;
        for (uint256 i = 0; i < totalSupply(); i++) {
            address owner = tokenByIndex(i);
            snapshots[snapshotId][owner] = balanceOf(owner);
        }
    }
}
```

**Validation rule:** The `takeSnapshot` function should be defined with the correct access control and logic.

```checker
{"id": "ch06-l5-s2", "type": "regex", "pattern": "function\\s+takeSnapshot\\(\\)\\s+external\\s*{[\\s\\S]*}", "flags": "m", "target": "solidity", "error_hint": "Define the `takeSnapshot` function with the correct access control and logic."}
```

## Lesson 6 — ValidationModule Implementation

**Learning objective:** Implement the basic functionality of the ValidationModule.  
**Emphasis tags:** `[BANK]` `[TYPES]`  
**Track:** shared

The `ValidationModule` is responsible for validating transactions based on predefined rules. This module will include a simple rule engine to enforce these validations.

### Step 6.1 — Add Rule State Variables

**Instruction:** Inside the `CMTATBase` contract, add two public state variables: `ruleId` of type `uint256` and `rules` of type `mapping(uint256 => string)`.

**Explanation:** In a core-banking system, there are often rules to validate transactions. Similarly, in CMTAT, the `ValidationModule` provides such functionality.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";

contract CMTATBase is ERC20 {
    address public admin;
    bool public paused;
    uint256 public tokenId;
    string public terms;
    string public information;
    mapping(address => bool) public frozen;
    uint256 public snapshotId;
    mapping(uint256 => mapping(address => uint256)) public snapshots;
    uint256 public ruleId;
    mapping(uint256 => string) public rules;

    constructor(string memory name, string memory symbol) ERC20(name, symbol) {}

    function pause() external {
        require(msg.sender == admin, "Not authorized");
        paused = true;
    }

    function unpause() external {
        require(msg.sender == admin, "Not authorized");
        paused = false;
    }

    function transfer(address to, uint256 amount) public override returns (bool) {
        require(!paused, "Contract is paused");
        return super.transfer(to, amount);
    }

    function setTokenId(uint256 _tokenId) external {
        require(msg.sender == admin, "Not authorized");
        tokenId = _tokenId;
    }

    function setTerms(string memory _terms) external {
        require(msg.sender == admin, "Not authorized");
        terms = _terms;
    }

    function setInformation(string memory _information) external {
        require(msg.sender == admin, "Not authorized");
        information = _information;
    }

    function freeze(address _address) external {
        require(msg.sender == admin, "Not authorized");
        frozen[_address] = true;
    }

    function unfreeze(address _address) external {
        require(msg.sender == admin, "Not authorized");
        frozen[_address] = false;
    }

    function takeSnapshot() external {
        require(msg.sender == admin, "Not authorized");
        snapshotId++;
        for (uint256 i = 0; i < totalSupply(); i++) {
            address owner = tokenByIndex(i);
            snapshots[snapshotId][owner] = balanceOf(owner);
        }
    }

    // define here
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";

contract CMTATBase is ERC20 {
    address public admin;
    bool public paused;
    uint256 public tokenId;
    string public terms;
    string public information;
    mapping(address => bool) public frozen;
    uint256 public snapshotId;
    mapping(uint256 => mapping(address => uint256)) public snapshots;
    uint256 public ruleId;
    mapping(uint256 => string) public rules;

    constructor(string memory name, string memory symbol) ERC20(name, symbol) {}

    function pause() external {
        require(msg.sender == admin, "Not authorized");
        paused = true;
    }

    function unpause() external {
        require(msg.sender == admin, "Not authorized");
        paused = false;
    }

    function transfer(address to, uint256 amount) public override returns (bool) {
        require(!paused, "Contract is paused");
        return super.transfer(to, amount);
    }

    function setTokenId(uint256 _tokenId) external {
        require(msg.sender == admin, "Not authorized");
        tokenId = _tokenId;
    }

    function setTerms(string memory _terms) external {
        require(msg.sender == admin, "Not authorized");
        terms = _terms;
    }

    function setInformation(string memory _information) external {
        require(msg.sender == admin, "Not authorized");
        information = _information;
    }

    function freeze(address _address) external {
        require(msg.sender == admin, "Not authorized");
        frozen[_address] = true;
    }

    function unfreeze(address _address) external {
        require(msg.sender == admin, "Not authorized");
        frozen[_address] = false;
    }

    function takeSnapshot() external {
        require(msg.sender == admin, "Not authorized");
        snapshotId++;
        for (uint256 i = 0; i < totalSupply(); i++) {
            address owner = tokenByIndex(i);
            snapshots[snapshotId][owner] = balanceOf(owner);
        }
    }
}
```

**Validation rule:** Both `ruleId` and `rules` mappings should be declared as public.

```checker
{"id": "ch06-l6-s1", "type": "regex", "pattern": "uint256\\s+public\\s+ruleId\\s*;[\\s\\S]*mapping\\(uint256\\s+=>\\s+string\\)\\s+public\\s+rules\\s*;", "flags": "m", "target": "solidity", "error_hint": "Declare `ruleId` and `rules` as public."}
```

### Step 6.2 — Add Rule Functions

**Instruction:** Inside the `CMTATBase` contract, add two functions: `addRule(uint256 _ruleId, string memory _rule)` and `validateTransaction(address from, address to, uint256 amount)`. The `addRule` function should only be callable by the `admin`.

**Explanation:** In a core-banking system, there are often rules to validate transactions. Similarly, in CMTAT, the `ValidationModule` provides such functionality.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";

contract CMTATBase is ERC20 {
    address public admin;
    bool public paused;
    uint256 public tokenId;
    string public terms;
    string public information;
    mapping(address => bool) public frozen;
    uint256 public snapshotId;
    mapping(uint256 => mapping(address => uint256)) public snapshots;
    uint256 public ruleId;
    mapping(uint256 => string) public rules;

    constructor(string memory name, string memory symbol) ERC20(name, symbol) {}

    function pause() external {
        require(msg.sender == admin, "Not authorized");
        paused = true;
    }

    function unpause() external {
        require(msg.sender == admin, "Not authorized");
        paused = false;
    }

    function transfer(address to, uint256 amount) public override returns (bool) {
        require(!paused, "Contract is paused");
        return super.transfer(to, amount);
    }

    function setTokenId(uint256 _tokenId) external {
        require(msg.sender == admin, "Not authorized");
        tokenId = _tokenId;
    }

    function setTerms(string memory _terms) external {
        require(msg.sender == admin, "Not authorized");
        terms = _terms;
    }

    function setInformation(string memory _information) external {
        require(msg.sender == admin, "Not authorized");
        information = _information;
    }

    function freeze(address _address) external {
        require(msg.sender == admin, "Not authorized");
        frozen[_address] = true;
    }

    function unfreeze(address _address) external {
        require(msg.sender == admin, "Not authorized");
        frozen[_address] = false;
    }

    function takeSnapshot() external {
        require(msg.sender == admin, "Not authorized");
        snapshotId++;
        for (uint256 i = 0; i < totalSupply(); i++) {
            address owner = tokenByIndex(i);
            snapshots[snapshotId][owner] = balanceOf(owner);
        }
    }

    // define here
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract CMTATBase {
    address public admin;
    bool public paused;
    uint256 public tokenId;
    string public terms;
    string public information;
    mapping(address => bool) public frozen;
    uint256 public snapshotId;
    mapping(uint256 => mapping(address => uint256)) public snapshots;
    uint256 public ruleId;
    mapping(uint256 => string) public rules;

    constructor(string memory name, string memory symbol) {
        // ERC20 initialization logic should be implemented here
        admin = msg.sender;
    }

    function pause() external {
        require(msg.sender == admin, "Not authorized");
        paused = true;
    }

    function unpause() external {
        require(msg.sender == admin, "Not authorized");
        paused = false;
    }

    function transfer(address to, uint256 amount) public returns (bool) {
        require(!paused, "Contract is paused");
        // Implement transfer logic here
        return true;
    }

    function setTokenId(uint256 _tokenId) external {
        require(msg.sender == admin, "Not authorized");
        tokenId = _tokenId;
    }

    function setTerms(string memory _terms) external {
        require(msg.sender == admin, "Not authorized");
        terms = _terms;
    }

    function setInformation(string memory _information) external {
        require(msg.sender == admin, "Not authorized");
        information = _information;
    }

    function freeze(address _address) external {
        require(msg.sender == admin, "Not authorized");
        frozen[_address] = true;
    }

    function unfreeze(address _address) external {
        require(msg.sender == admin, "Not authorized");
        frozen[_address] = false;
    }

    function takeSnapshot() external {
        require(msg.sender == admin, "Not authorized");
        snapshotId++;
        // Implement snapshot logic here
    }

    function addRule(uint256 _ruleId, string memory _rule) external {
        require(msg.sender == admin, "Not authorized");
        rules[_ruleId] = _rule;
    }

    function validateTransaction(address from, address to, uint256 amount) public view returns (bool) {
        // Implement validation logic here
        return true;
    }
}
```

**Validation rule:** Both `addRule` and `validateTransaction` functions should be defined with the correct access control.

```checker
{"id": "ch06-l6-s2", "type": "regex", "pattern": "function\\s+addRule\\(uint256\\s+_ruleId,\\s+string\\s+memory\\s+_rule\\)\\s+external\\s*{", "flags": "m", "target": "solidity", "error_hint": "Define both `addRule` and `validateTransaction` functions with the correct access control."}
```

## Assembled Contract

```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";

contract CMTATBase is ERC20 {
    address public admin;
    bool public paused;
    uint256 public tokenId;
    string public terms;
    string public information;
    mapping(address => bool) public frozen;
    uint256 public snapshotId;
    mapping(uint256 => mapping(address => uint256)) public snapshots;
    uint256 public ruleId;
    mapping(uint256 => string) public rules;

    constructor(string memory name, string memory symbol) ERC20(name, symbol) {}

    function pause() external {
        require(msg.sender == admin, "Not authorized");
        paused = true;
    }

    function unpause() external {
        require(msg.sender == admin, "Not authorized");
        paused = false;
    }

    function transfer(address to, uint256 amount) public override returns (bool) {
        require(!paused, "Contract is paused");
        return super.transfer(to, amount);
    }

