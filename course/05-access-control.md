# Chapter 05 — Access Control: Mapping Bank Org Structure On-Chain `[shared] [BANK]`

**Track:** shared  
**Emphasis threads:** `[BANK]`  
**Chapter learning objective:** Implement role-based access control in Solidity, mapping it to typical bank organizational roles such as issuer, registrar/transfer agent, compliance officer, and operations. Understand four-eyes patterns, role admin hierarchies, and audit of role grants via events.  
**Prerequisites:** Basic understanding of Solidity contracts, familiarity with the CMTAT tokenized-securities standard.  
**You will build:** A `RoleControlled` contract that defines roles for different bank functions and ensures proper access control.

## Lesson 1 — Defining Roles and Role Hierarchies

**Learning objective:** Define roles such as DEFAULT_ADMIN_ROLE, MINTER_ROLE, BURNER_ROLE, PAUSER_ROLE, and ENFORCER_ROLE. Understand role hierarchies where one role can manage other roles.

### Step 1.1 — Create the RoleControlled contract skeleton

**Instruction:** Create a new file `RoleControlled.sol`. Declare the pragma, contract name, and import necessary libraries.

**Explanation:** In core banking systems, different roles are assigned to various personnel to ensure that only authorized individuals can perform certain actions. Similarly, in blockchain, we need to define these roles and their permissions. Here, we start by setting up the basic structure of our contract.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract RoleControlled {
    // Define roles here
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/access/AccessControl.sol";

contract RoleControlled is AccessControl {
    bytes32 public constant DEFAULT_ADMIN_ROLE = 0x00;
    bytes32 public constant MINTER_ROLE = keccak256("MINTER_ROLE");
    bytes32 public constant BURNER_ROLE = keccak256("BURNER_ROLE");
    bytes32 public constant PAUSER_ROLE = keccak256("PAUSER_ROLE");
    bytes32 public constant ENFORCER_ROLE = keccak256("ENFORCER_ROLE");

    constructor() {
        _grantRole(DEFAULT_ADMIN_ROLE, msg.sender);
    }
}
```

**Validation rule:** Define the roles and grant DEFAULT_ADMIN_ROLE to the contract creator.

```checker
{
  "id": "ch05-l1-s1",
  "type": "regex",
  "pattern": "bytes32\\s+public\\s+constant\\s+DEFAULT_ADMIN_ROLE\\s*=\\s*0x00;[\\s\\S]*bytes32\\s+public\\s+constant\\s+MINTER_ROLE\\s*=\\s*keccak256\\(\"MINTER_ROLE\"\\);[\\s\\S]*bytes32\\s+public\\s+constant\\s+BURNER_ROLE\\s*=\\s*keccak256\\(\"BURNER_ROLE\"\\);[\\s\\S]*bytes32\\s+public\\s+constant\\s+PAUSER_ROLE\\s*=\\s*keccak256\\(\"PAUSER_ROLE\"\\);[\\s\\S]*bytes32\\s+public\\s+constant\\s+ENFORCER_ROLE\\s*=\\s*keccak256\\(\"ENFORCER_ROLE\"\\);",
  "flags": "m",
  "target": "solidity",
  "error_hint": "Define all roles with the correct keccak256 hash."
}
```

### Step 1.2 — Grant and revoke roles

**Instruction:** Add functions to grant and revoke roles.

**Explanation:** In banking systems, roles are often granted or revoked based on organizational changes. Similarly, in our contract, we need to provide functionality to manage these roles dynamically.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/access/AccessControl.sol";

contract RoleControlled is AccessControl {
    bytes32 public constant DEFAULT_ADMIN_ROLE = 0x00;
    bytes32 public constant MINTER_ROLE = keccak256("MINTER_ROLE");
    bytes32 public constant BURNER_ROLE = keccak256("BURNER_ROLE");
    bytes32 public constant PAUSER_ROLE = keccak256("PAUSER_ROLE");
    bytes32 public constant ENFORCER_ROLE = keccak256("ENFORCER_ROLE");

    constructor() {
        _grantRole(DEFAULT_ADMIN_ROLE, msg.sender);
    }

    // Add functions here
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/access/AccessControl.sol";

contract RoleControlled is AccessControl {
    bytes32 public constant DEFAULT_ADMIN_ROLE = 0x00;
    bytes32 public constant MINTER_ROLE = keccak256("MINTER_ROLE");
    bytes32 public constant BURNER_ROLE = keccak256("BURNER_ROLE");
    bytes32 public constant PAUSER_ROLE = keccak256("PAUSER_ROLE");
    bytes32 public constant ENFORCER_ROLE = keccak256("ENFORCER_ROLE");

    constructor() {
        _grantRole(DEFAULT_ADMIN_ROLE, msg.sender);
    }

    function grantRole(bytes32 role, address account) public onlyRole(DEFAULT_ADMIN_ROLE) {
        _grantRole(role, account);
    }

    function revokeRole(bytes32 role, address account) public onlyRole(DEFAULT_ADMIN_ROLE) {
        _revokeRole(role, account);
    }
}
```

**Validation rule:** Add functions to grant and revoke roles.

```checker
{
  "id": "ch05-l1-s2",
  "type": "regex",
  "pattern": "function\\s+grantRole\\(bytes32\\s+role,\\s+address\\s+account\\)\\s+public\\s+onlyRole\\(DEFAULT_ADMIN_ROLE\\)\\s*{[\\s\\S]*}\\s*function\\s+revokeRole\\(bytes32\\s+role,\\s+address\\s+account\\)\\s+public\\s+onlyRole\\(DEFAULT_ADMIN_ROLE\\)\\s*{[\\s\\S]*}",
  "flags": "m",
  "target": "solidity",
  "error_hint": "Define the grantRole and revokeRole functions with the correct parameters and modifiers."
}
```

## Lesson 2 — Role Hierarchies and Four-Eyes Patterns

**Learning objective:** Implement role hierarchies where one role can manage other roles. Understand four-eyes patterns to ensure that critical actions require approval from multiple roles.

### Step 2.1 — Define role admin relationships

**Instruction:** Set up role admin relationships so that DEFAULT_ADMIN_ROLE can manage all other roles.

**Explanation:** In banking, certain roles have the authority to manage other roles. For example, a compliance officer might be able to grant or revoke roles related to regulatory requirements. We need to define these relationships in our contract.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/access/AccessControl.sol";

contract RoleControlled is AccessControl {
    bytes32 public constant DEFAULT_ADMIN_ROLE = 0x00;
    bytes32 public constant MINTER_ROLE = keccak256("MINTER_ROLE");
    bytes32 public constant BURNER_ROLE = keccak256("BURNER_ROLE");
    bytes32 public constant PAUSER_ROLE = keccak256("PAUSER_ROLE");
    bytes32 public constant ENFORCER_ROLE = keccak256("ENFORCER_ROLE");

    constructor() {
        _grantRole(DEFAULT_ADMIN_ROLE, msg.sender);
    }

    function grantRole(bytes32 role, address account) public onlyRole(DEFAULT_ADMIN_ROLE) {
        _grantRole(role, account);
    }

    function revokeRole(bytes32 role, address account) public onlyRole(DEFAULT_ADMIN_ROLE) {
        _revokeRole(role, account);
    }
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/access/AccessControl.sol";

contract RoleControlled is AccessControl {
    bytes32 public constant DEFAULT_ADMIN_ROLE = 0x00;
    bytes32 public constant MINTER_ROLE = keccak256("MINTER_ROLE");
    bytes32 public constant BURNER_ROLE = keccak256("BURNER_ROLE");
    bytes32 public constant PAUSER_ROLE = keccak256("PAUSER_ROLE");
    bytes32 public constant ENFORCER_ROLE = keccak256("ENFORCER_ROLE");

    constructor() {
        _grantRole(DEFAULT_ADMIN_ROLE, msg.sender);
        _setRoleAdmin(MINTER_ROLE, DEFAULT_ADMIN_ROLE);
        _setRoleAdmin(BURNER_ROLE, DEFAULT_ADMIN_ROLE);
        _setRoleAdmin(PAUSER_ROLE, DEFAULT_ADMIN_ROLE);
        _setRoleAdmin(ENFORCER_ROLE, DEFAULT_ADMIN_ROLE);
    }

    function grantRole(bytes32 role, address account) public onlyRole(DEFAULT_ADMIN_ROLE) {
        _grantRole(role, account);
    }

    function revokeRole(bytes32 role, address account) public onlyRole(DEFAULT_ADMIN_ROLE) {
        _revokeRole(role, account);
    }
}
```

**Validation rule:** Set up role admin relationships.

```checker
{
  "id": "ch05-l2-s1",
  "type": "regex",
  "pattern": "_setRoleAdmin\\(MINTER_ROLE,\\s+DEFAULT_ADMIN_ROLE\\);[\\s\\S]*_setRoleAdmin\\(BURNER_ROLE,\\s+DEFAULT_ADMIN_ROLE\\);[\\s\\S]*_setRoleAdmin\\(PAUSER_ROLE,\\s+DEFAULT_ADMIN_ROLE\\);[\\s\\S]*_setRoleAdmin\\(ENFORCER_ROLE,\\s+DEFAULT_ADMIN_ROLE\\);",
  "flags": "m",
  "target": "solidity",
  "error_hint": "Define the role admin relationships correctly."
}
```

### Step 2.2 — Implement four-eyes pattern for critical actions

**Instruction:** Add a function that requires approval from both MINTER_ROLE and ENFORCER_ROLE to mint tokens.

**Explanation:** In banking, certain actions require multiple approvals to ensure compliance and reduce the risk of errors. We need to implement similar logic in our contract to ensure that only authorized personnel can perform critical actions.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/access/AccessControl.sol";

contract RoleControlled is AccessControl {
    bytes32 public constant DEFAULT_ADMIN_ROLE = 0x00;
    bytes32 public constant MINTER_ROLE = keccak256("MINTER_ROLE");
    bytes32 public constant BURNER_ROLE = keccak256("BURNER_ROLE");
    bytes32 public constant PAUSER_ROLE = keccak256("PAUSER_ROLE");
    bytes32 public constant ENFORCER_ROLE = keccak256("ENFORCER_ROLE");

    constructor() {
        _grantRole(DEFAULT_ADMIN_ROLE, msg.sender);
        _setRoleAdmin(MINTER_ROLE, DEFAULT_ADMIN_ROLE);
        _setRoleAdmin(BURNER_ROLE, DEFAULT_ADMIN_ROLE);
        _setRoleAdmin(PAUSER_ROLE, DEFAULT_ADMIN_ROLE);
        _setRoleAdmin(ENFORCER_ROLE, DEFAULT_ADMIN_ROLE);
    }

    function grantRole(bytes32 role, address account) public onlyRole(DEFAULT_ADMIN_ROLE) {
        _grantRole(role, account);
    }

    function revokeRole(bytes32 role, address account) public onlyRole(DEFAULT_ADMIN_ROLE) {
        _revokeRole(role, account);
    }
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/access/AccessControl.sol";

contract RoleControlled is AccessControl {
    bytes32 public constant DEFAULT_ADMIN_ROLE = 0x00;
    bytes32 public constant MINTER_ROLE = keccak256("MINTER_ROLE");
    bytes32 public constant BURNER_ROLE = keccak256("BURNER_ROLE");
    bytes32 public constant PAUSER_ROLE = keccak256("PAUSER_ROLE");
    bytes32 public constant ENFORCER_ROLE = keccak256("ENFORCER_ROLE");

    constructor() {
        _grantRole(DEFAULT_ADMIN_ROLE, msg.sender);
        _setRoleAdmin(MINTER_ROLE, DEFAULT_ADMIN_ROLE);
        _setRoleAdmin(BURNER_ROLE, DEFAULT_ADMIN_ROLE);
        _setRoleAdmin(PAUSER_ROLE, DEFAULT_ADMIN_ROLE);
        _setRoleAdmin(ENFORCER_ROLE, DEFAULT_ADMIN_ROLE);
    }

    function grantRole(bytes32 role, address account) public onlyRole(DEFAULT_ADMIN_ROLE) {
        _grantRole(role, account);
    }

    function revokeRole(bytes32 role, address account) public onlyRole(DEFAULT_ADMIN_ROLE) {
        _revokeRole(role, account);
    }

    function mintTokens(address to, uint256 amount) public onlyRole(MINTER_ROLE) onlyRole(ENFORCER_ROLE) {
        // Mint tokens logic here
    }
}
```

**Validation rule:** Add a function that requires approval from both MINTER_ROLE and ENFORCER_ROLE.

```checker
{
  "id": "ch05-l2-s2",
  "type": "regex",
  "pattern": "function\\s+mintTokens\\(address\\s+to,\\s+uint256\\s+amount\\)\\s+public\\s+onlyRole\\(MINTER_ROLE\\)\\s+onlyRole\\(ENFORCER_ROLE\\)\\s*{[\\s\\S]*}",
  "flags": "m",
  "target": "solidity",
  "error_hint": "Define the mintTokens function with the correct parameters and modifiers."
}
```

## Lesson 3 — Auditing Role Grants

**Learning objective:** Implement an event to log role grants and revokes for auditing purposes.

### Step 3.1 — Define events for role changes

**Instruction:** Add events to log when roles are granted or revoked.

**Explanation:** In banking, it is crucial to keep a record of all transactions and changes made to the system. Similarly, in our contract, we need to log all role changes so that they can be audited later.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/access/AccessControl.sol";

contract RoleControlled is AccessControl {
    bytes32 public constant DEFAULT_ADMIN_ROLE = 0x00;
    bytes32 public constant MINTER_ROLE = keccak256("MINTER_ROLE");
    bytes32 public constant BURNER_ROLE = keccak256("BURNER_ROLE");
    bytes32 public constant PAUSER_ROLE = keccak256("PAUSER_ROLE");
    bytes32 public constant ENFORCER_ROLE = keccak256("ENFORCER_ROLE");

    constructor() {
        _grantRole(DEFAULT_ADMIN_ROLE, msg.sender);
        _setRoleAdmin(MINTER_ROLE, DEFAULT_ADMIN_ROLE);
        _setRoleAdmin(BURNER_ROLE, DEFAULT_ADMIN_ROLE);
        _setRoleAdmin(PAUSER_ROLE, DEFAULT_ADMIN_ROLE);
        _setRoleAdmin(ENFORCER_ROLE, DEFAULT_ADMIN_ROLE);
    }

    function grantRole(bytes32 role, address account) public onlyRole(DEFAULT_ADMIN_ROLE) {
        _grantRole(role, account);
    }

    function revokeRole(bytes32 role, address account) public onlyRole(DEFAULT_ADMIN_ROLE) {
        _revokeRole(role, account);
    }

    function mintTokens(address to, uint256 amount) public onlyRole(MINTER_ROLE) onlyRole(ENFORCER_ROLE) {
        // Mint tokens logic here
    }
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/access/AccessControl.sol";

contract RoleControlled is AccessControl {
    bytes32 public constant DEFAULT_ADMIN_ROLE = 0x00;
    bytes32 public constant MINTER_ROLE = keccak256("MINTER_ROLE");
    bytes32 public constant BURNER_ROLE = keccak256("BURNER_ROLE");
    bytes32 public constant PAUSER_ROLE = keccak256("PAUSER_ROLE");
    bytes32 public constant ENFORCER_ROLE = keccak256("ENFORCER_ROLE");

    event RoleGranted(bytes32 indexed role, address indexed account, address indexed sender);
    event RoleRevoked(bytes32 indexed role, address indexed account, address indexed sender);

    constructor() {
        _grantRole(DEFAULT_ADMIN_ROLE, msg.sender);
        _setRoleAdmin(MINTER_ROLE, DEFAULT_ADMIN_ROLE);
        _setRoleAdmin(BURNER_ROLE, DEFAULT_ADMIN_ROLE);
        _setRoleAdmin(PAUSER_ROLE, DEFAULT_ADMIN_ROLE);
        _setRoleAdmin(ENFORCER_ROLE, DEFAULT_ADMIN_ROLE);
    }

    function grantRole(bytes32 role, address account) public override {
        super.grantRole(role, account);
        emit RoleGranted(role, account, msg.sender);
    }

    function revokeRole(bytes32 role, address account) public override {
        super.revokeRole(role, account);
        emit RoleRevoked(role, account, msg.sender);
    }

    function mintTokens(address to, uint256 amount) public onlyRole(MINTER_ROLE) onlyRole(ENFORCER_ROLE) {
        // Mint tokens logic here
    }
}
```

**Validation rule:** Add events for role changes.

```checker
{
  "id": "ch05-l3-s1",
  "type": "regex",
  "pattern": "event\\s+RoleGranted\\(bytes32\\s+indexed\\s+role,\\s+address\\s+indexed\\s+account,\\s+address\\s+indexed\\s+sender\\);[\\s\\S]*event\\s+RoleRevoked\\(bytes32\\s+indexed\\s+role,\\s+address\\s+indexed\\s+account,\\s+address\\s+indexed\\s+sender\\);",
  "flags": "m",
  "target": "solidity",
  "error_hint": "Define the RoleGranted and RoleRevoked events with the correct parameters."
}
```

### Step 3.2 — Emit events in role management functions

**Instruction:** Ensure that the `grantRole` and `revokeRole` functions emit the appropriate events.

**Explanation:** Logging these events will help us keep track of all changes made to roles, which is essential for auditing purposes. We need to ensure that these events are emitted whenever a role is granted or revoked.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/access/AccessControl.sol";

contract RoleControlled is AccessControl {
    bytes32 public constant DEFAULT_ADMIN_ROLE = 0x00;
    bytes32 public constant MINTER_ROLE = keccak256("MINTER_ROLE");
    bytes32 public constant BURNER_ROLE = keccak256("BURNER_ROLE");
    bytes32 public constant PAUSER_ROLE = keccak256("PAUSER_ROLE");
    bytes32 public constant ENFORCER_ROLE = keccak256("ENFORCER_ROLE");

    event RoleGranted(bytes32 indexed role, address indexed account, address indexed sender);
    event RoleRevoked(bytes32 indexed role, address indexed account, address indexed sender);

    constructor() {
        _grantRole(DEFAULT_ADMIN_ROLE, msg.sender);
        _setRoleAdmin(MINTER_ROLE, DEFAULT_ADMIN_ROLE);
        _setRoleAdmin(BURNER_ROLE, DEFAULT_ADMIN_ROLE);
        _setRoleAdmin(PAUSER_ROLE, DEFAULT_ADMIN_ROLE);
        _setRoleAdmin(ENFORCER_ROLE, DEFAULT_ADMIN_ROLE);
    }

    function grantRole(bytes32 role, address account) public override {
        super.grantRole(role, account);
        emit RoleGranted(role, account, msg.sender);
    }

    function revokeRole(bytes32 role, address account) public override {
        super.revokeRole(role, account);
        emit RoleRevoked(role, account, msg.sender);
    }

    function mintTokens(address to, uint256 amount) public onlyRole(MINTER_ROLE) onlyRole(ENFORCER_ROLE) {
        // Mint tokens logic here
    }
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/access/AccessControl.sol";

contract RoleControlled is AccessControl {
    bytes32 public constant DEFAULT_ADMIN_ROLE = 0x00;
    bytes32 public constant MINTER_ROLE = keccak256("MINTER_ROLE");
    bytes32 public constant BURNER_ROLE = keccak256("BURNER_ROLE");
    bytes32 public constant PAUSER_ROLE = keccak256("PAUSER_ROLE");
    bytes32 public constant ENFORCER_ROLE = keccak256("ENFORCER_ROLE");

    event RoleGranted(bytes32 indexed role, address indexed account, address indexed sender);
    event RoleRevoked(bytes32 indexed role, address indexed account, address indexed sender);

    constructor() {
        _grantRole(DEFAULT_ADMIN_ROLE, msg.sender);
        _setRoleAdmin(MINTER_ROLE, DEFAULT_ADMIN_ROLE);
        _setRoleAdmin(BURNER_ROLE, DEFAULT_ADMIN_ROLE);
        _setRoleAdmin(PAUSER_ROLE, DEFAULT_ADMIN_ROLE);
        _setRoleAdmin(ENFORCER_ROLE, DEFAULT_ADMIN_ROLE);
    }

    function grantRole(bytes32 role, address account) public override {
        super.grantRole(role, account);
        emit RoleGranted(role, account, msg.sender);
    }

    function revokeRole(bytes32 role, address account) public override {
        super.revokeRole(role, account);
        emit RoleRevoked(role, account, msg.sender);
    }

    function mintTokens(address to, uint256 amount) public onlyRole(MINTER_ROLE) onlyRole(ENFORCER_ROLE) {
        // Mint tokens logic here
    }
}
```

**Validation rule:** Ensure that the `grantRole` and `revokeRole` functions emit the appropriate events.

```checker
{
  "id": "ch05-l3-s2",
  "type": "regex",
  "pattern": "super\\.grantRole\\(role,\\s+account\\);[\\s\\S]*emit\\s+RoleGranted\\(role,\\s+account,\\s+msg\\.sender\\);[\\s\\S]*super\\.revokeRole\\(role,\\s+account\\);[\\s\\S]*emit\\s+RoleRevoked\\(role,\\s+account,\\s+msg\\.sender\\);",
  "flags": "m",
  "target": "solidity",
  "error_hint": "Ensure that the grantRole and revokeRole functions emit the appropriate events."
}
```

## Assembled Contract

```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/access/AccessControl.sol";

contract RoleControlled is AccessControl {
    bytes32 public constant DEFAULT_ADMIN_ROLE = 0x00;
    bytes32 public constant MINTER_ROLE = keccak256("MINTER_ROLE");
    bytes32 public constant BURNER_ROLE = keccak256("BURNER_ROLE");
    bytes32 public constant PAUSER_ROLE = keccak256("PAUSER_ROLE");
    bytes32 public constant ENFORCER_ROLE = keccak256("ENFORCER_ROLE");

    event RoleGranted(bytes32 indexed role, address indexed account, address indexed sender);
    event RoleRevoked(bytes32 indexed role, address indexed account, address indexed sender);

    constructor() {
        _grantRole(DEFAULT_ADMIN_ROLE, msg.sender);
        _setRoleAdmin(MINTER_ROLE, DEFAULT_ADMIN_ROLE);
        _setRoleAdmin(BURNER_ROLE, DEFAULT_ADMIN_ROLE);
        _setRoleAdmin(PAUSER_ROLE, DEFAULT_ADMIN_ROLE);
        _setRoleAdmin(ENFORCER_ROLE, DEFAULT_ADMIN_ROLE);
    }

    function grantRole(bytes32 role, address account) public override {
        super.grantRole(role, account);
        emit RoleGranted(role, account, msg.sender);
    }

    function revokeRole(bytes32 role, address account) public override {
        super.revokeRole(role, account);
        emit RoleRevoked(role, account, msg.sender);
    }

    function mintTokens(address to, uint256 amount) public onlyRole(MINTER_ROLE) onlyRole(ENFORCER_ROLE) {
        // Mint tokens logic here
    }
}
```

## Quiz

1. What is the purpose of defining role hierarchies in a smart contract?
   - A) To allow any user to perform any action.
   - B) To ensure that certain actions require approval from multiple roles.
   - C) To make the contract more complex and harder to understand.

2. How can you log role changes for auditing purposes in Solidity?
   - A) Use `console.log` statements.
   - B) Define events and emit them when roles are granted or revoked.
   - C) Store role changes in a mapping.

3. What is the significance of the DEFAULT_ADMIN_ROLE in this contract?
   - A) It can perform any action without restrictions.
   - B) It can manage all other roles.
   - C) It cannot grant or revoke roles.

4. How does the four-eyes pattern enhance security in a smart contract?
   - A) By allowing anyone to approve actions.
   - B) By requiring multiple approvals for critical actions.
   - C) By making it easier to perform actions.

5. What is the role of events in a smart contract?
   - A) To store data permanently on the blockchain.
   - B) To log important changes and actions for auditing purposes.
   - C) To execute specific functions when certain conditions are met.