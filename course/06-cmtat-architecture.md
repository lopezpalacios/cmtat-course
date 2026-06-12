# Chapter 06 — CMTAT Architecture: A Module Tour

**Track:** shared  
**Emphasis threads:** `[BANK]` `[TYPES]`  
**Chapter learning objective:** Understand the architecture of the CMTAT standard, including its various modules and their purposes. Learn how to build a basic skeleton for the `CMTATBase.sol` contract.  
**Prerequisites:** Familiarity with Solidity and basic blockchain concepts. Understanding of Java/.NET is beneficial but not required.  
**You will build:** A basic skeleton for the `CMTATBase.sol` contract, incorporating key CMTAT modules.

The CMTAT (Common Modular Token Architecture) standard is a comprehensive framework designed to facilitate the creation and management of tokenized securities in Switzerland. It decomposes complex functionalities into modular components, each serving a specific purpose. These modules include ERC20BaseModule for basic token functionalities, BaseModule for core tokenId/terms/information handling, PauseModule for pausing contract operations, EnforcementModule for freezing tokens, ERC20SnapshotModule for snapshotting token balances, ValidationModule with RuleEngine for rule-based validations, DocumentModule for document management, DebtModule for debt-related functionalities, and AuthorizationModule for managing access controls. This modular approach allows developers to build flexible and secure tokenized securities solutions tailored to their specific needs.

## Lesson 1 — Introduction to CMTAT Modules

**Learning objective:** Understand the purpose and functionality of each CMTAT module.  
**Emphasis tags:** `[BANK]` `[TYPES]`
**Track:** shared

In core banking systems, modules are used to encapsulate specific functionalities such as account management, transaction processing, and risk assessment. Similarly, in blockchain, modular architecture allows for better separation of concerns and easier maintenance. Each CMTAT module serves a distinct purpose, making it easier to manage and extend tokenized securities.

### Step 1.1 — Create the `CMTATBase.sol` contract skeleton

**Instruction:** Create the file `CMTATBase.sol`. Declare the pragma, contract, and import necessary modules.

**Explanation:** Just as in core banking systems where each module is a separate component that interacts with others to provide comprehensive services, CMTAT uses modular components to build a robust tokenized securities platform. Start by setting up the basic structure of the `CMTATBase.sol` contract and importing the required modules.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract CMTATBase {
    // import necessary modules here
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";
import "./modules/BaseModule.sol";
import "./modules/PauseModule.sol";

contract CMTATBase is ERC20, BaseModule, PauseModule {
    constructor(string memory name, string memory symbol) ERC20(name, symbol) {}
}
```

**Validation rule:** The contract should inherit from `ERC20`, `BaseModule`, and `PauseModule`.

```checker
{"id": "ch06-l1-s1", "type": "regex", "pattern": "contract\\s+CMTATBase\\s+is\\s+ERC20,\\s+BaseModule,\\s+PauseModule\\s*{", "flags": "m", "target": "solidity", "error_hint": "Ensure the contract inherits from ERC20, BaseModule, and PauseModule."}
```

## Lesson 2 — Implementing Basic Token Functions

**Learning objective:** Learn how to implement basic token functions using the CMTAT modules.  
**Emphasis tags:** `[BANK]` `[TYPES]`
**Track:** shared

In core banking systems, basic account functionalities such as deposit and withdrawal are essential. Similarly, in blockchain, basic token functionalities like minting and transferring tokens are crucial. This lesson will guide you through implementing these functions using the CMTAT modules.

### Step 2.1 — Add a function to mint tokens

**Instruction:** Implement a `mint` function that allows the contract admin to mint new tokens.

**Explanation:** Just as in core banking systems where authorized personnel can deposit funds into accounts, the `mint` function allows the contract admin to issue new tokens. This is a critical functionality for managing token supply and ensuring proper authorization.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";
import "./modules/BaseModule.sol";
import "./modules/PauseModule.sol";

contract CMTATBase is ERC20, BaseModule, PauseModule {
    constructor(string memory name, string memory symbol) ERC20(name, symbol) {}

    // implement mint function here
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";
import "./modules/BaseModule.sol";
import "./modules/PauseModule.sol";

contract CMTATBase is ERC20, BaseModule, PauseModule {
    constructor(string memory name, string memory symbol) ERC20(name, symbol) {}

    function mint(address to, uint256 amount) public onlyAdmin {
        _mint(to, amount);
    }
}
```

**Validation rule:** The `mint` function should be correctly implemented and accessible only by the admin.

```checker
{"id": "ch06-l2-s1", "type": "regex", "pattern": "function\\s+mint\\(address\\s+to,\\s+uint256\\s+amount\\)\\s+public\\s+onlyAdmin\\s*{[^}]*_mint\\(to,\\s+amount\\);", "flags": "m", "target": "solidity", "error_hint": "Ensure the mint function is correctly implemented and accessible only by the admin."}
```

## Lesson 3 — Adding Pause Functionality

**Learning objective:** Learn how to add pause functionality using the CMTAT modules.  
**Emphasis tags:** `[BANK]` `[TYPES]`
**Track:** shared

In core banking systems, there are scenarios where it is necessary to temporarily halt certain operations, such as during system maintenance or in case of an emergency. Similarly, in blockchain, pause functionality allows for temporary suspension of token transfers and other critical operations.

### Step 3.1 — Implement the pause function

**Instruction:** Implement a `pause` function that allows the contract admin to pause all token transfers.

**Explanation:** Just as in core banking systems where authorized personnel can temporarily halt certain operations, the `pause` function allows the contract admin to suspend token transfers and other critical operations. This is essential for maintaining security and preventing unauthorized activities.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";
import "./modules/BaseModule.sol";
import "./modules/PauseModule.sol";

contract CMTATBase is ERC20, BaseModule, PauseModule {
    constructor(string memory name, string memory symbol) ERC20(name, symbol) {}

    function mint(address to, uint256 amount) public onlyAdmin {
        _mint(to, amount);
    }

    // implement pause function here
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";
import "./modules/BaseModule.sol";
import "./modules/PauseModule.sol";

contract CMTATBase is ERC20, BaseModule, PauseModule {
    constructor(string memory name, string memory symbol) ERC20(name, symbol) {}

    function mint(address to, uint256 amount) public onlyAdmin {
        _mint(to, amount);
    }

    function pause() public onlyAdmin {
        _pause();
    }
}
```

**Validation rule:** The `pause` function should be correctly implemented and accessible only by the admin.

```checker
{"id": "ch06-l3-s1", "type": "regex", "pattern": "function\\s+pause\\(\\)\\s+public\\s+onlyAdmin\\s*{[^}]*_pause\\(\\);", "flags": "m", "target": "solidity", "error_hint": "Ensure the pause function is correctly implemented and accessible only by the admin."}
```

## Lesson 4 — Adding Freeze Functionality

**Learning objective:** Learn how to add freeze functionality using the CMTAT modules.  
**Emphasis tags:** `[BANK]` `[TYPES]`
**Track:** shared

In core banking systems, there are scenarios where it is necessary to temporarily prevent certain accounts from performing transactions, such as during account reconciliation or in case of suspicious activity. Similarly, in blockchain, freeze functionality allows for temporary suspension of token transfers and other critical operations.

### Step 4.1 — Implement the freeze function

**Instruction:** Implement a `freeze` function that allows the contract admin to freeze a specific address.

**Explanation:** Just as in core banking systems where authorized personnel can temporarily prevent certain accounts from performing transactions, the `freeze` function allows the contract admin to suspend token transfers and other critical operations for a specific address. This is essential for maintaining security and preventing unauthorized activities.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";
import "./modules/BaseModule.sol";
import "./modules/PauseModule.sol";
import "./modules/EnforcementModule.sol";

contract CMTATBase is ERC20, BaseModule, PauseModule, EnforcementModule {
    constructor(string memory name, string memory symbol) ERC20(name, symbol) {}

    function mint(address to, uint256 amount) public onlyAdmin {
        _mint(to, amount);
    }

    function pause() public onlyAdmin {
        _pause();
    }

    // implement freeze function here
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";
import "./modules/BaseModule.sol";
import "./modules/PauseModule.sol";
import "./modules/EnforcementModule.sol";

contract CMTATBase is ERC20, BaseModule, PauseModule, EnforcementModule {
    constructor(string memory name, string memory symbol) ERC20(name, symbol) {}

    function mint(address to, uint256 amount) public onlyAdmin {
        _mint(to, amount);
    }

    function pause() public onlyAdmin {
        _pause();
    }

    function freeze(address account) public onlyAdmin {
        _freeze(account);
    }
}
```

**Validation rule:** The `freeze` function should be correctly implemented and accessible only by the admin.

```checker
{"id": "ch06-l4-s1", "type": "regex", "pattern": "function\\s+freeze\\(address\\s+account\\)\\s+public\\s+onlyAdmin\\s*{[^}]*_freeze\\(account\\);", "flags": "m", "target": "solidity", "error_hint": "Ensure the freeze function is correctly implemented and accessible only by the admin."}
```

## Assembled Contract

```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";
import "./modules/BaseModule.sol";
import "./modules/PauseModule.sol";
import "./modules/EnforcementModule.sol";

contract CMTATBase is ERC20, BaseModule, PauseModule, EnforcementModule {
    constructor(string memory name, string memory symbol) ERC20(name, symbol) {}

    function mint(address to, uint256 amount) public onlyAdmin {
        _mint(to, amount);
    }

    function pause() public onlyAdmin {
        _pause();
    }

    function freeze(address account) public onlyAdmin {
        _freeze(account);
    }
}
```

## Quiz

1. What is the purpose of the `PauseModule` in CMTAT?
   - A) To handle token snapshots
   - B) To pause all token transfers
   - C) To manage document storage
   - D) To enforce validation rules

2. Which function allows the contract admin to mint new tokens?
   - A) `pause()`
   - B) `mint(address to, uint256 amount)`
   - C) `freeze(address account)`
   - D) `transfer(address to, uint256 amount)`

3. What is the purpose of the `EnforcementModule` in CMTAT?
   - A) To handle token snapshots
   - B) To pause all token transfers
   - C) To freeze specific addresses
   - D) To manage document storage

4. Which function allows the contract admin to freeze a specific address?
   - A) `pause()`
   - B) `mint(address to, uint256 amount)`
   - C) `freeze(address account)`
   - D) `transfer(address to, uint256 amount)`

**Answers:**

1. B) To pause all token transfers
2. B) `mint(address to, uint256 amount)`
3. C) To freeze specific addresses
4. C) `freeze(address account)`