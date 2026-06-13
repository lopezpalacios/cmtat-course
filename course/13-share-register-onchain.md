# Chapter 13 — The Share Register On-Chain `[B] [BANK] [TYPES]`

**Track:** equity  
**Emphasis threads:** `[BANK]` `[TYPES]`  
**Chapter learning objective:** Understand how to register shares (Namenaktien) as CMTAT tokens, including share metadata struct (ISIN, nominal value, share class), registrar role = transfer agent, shareholder identity binding (on-chain address ↔ off-chain KYC record), and DocumentModule for articles of association / terms.  
**Prerequisites:** Basic understanding of Solidity, familiarity with the CMTAT tokenized-securities standard, and experience with Java/.NET core-banking systems.  
**You will build:** A `ShareToken.sol` contract that models share registration on-chain.

## Lesson 1 — Define Share Metadata

**Learning objective:** Define a struct to hold metadata for each share, including ISIN, nominal value, and share class.  
**Emphasis tags:** `[TYPES]`  
**Track:** equity

In core-banking systems, shares are often represented by various attributes such as ISIN (International Securities Identification Number), nominal value, and share class. Similarly, in Solidity, we can define a struct to encapsulate these details.

### Step 1.1 — Define the ShareMetadata struct

**Instruction:** Inside `ShareToken.sol`, define a struct named `ShareMetadata` with fields for ISIN (string), nominalValue (uint256), and shareClass (string).

**Explanation:** Just as in core-banking systems, where each share has specific attributes like ISIN and nominal value, we need to define these attributes in our Solidity contract. This struct will help us manage the metadata of each share.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract ShareToken {
    // Define the ShareMetadata struct here
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
}
```

**Validation rule:** The `ShareMetadata` struct should have the correct fields and types.

```checker
{"id": "ch13-l1-s1", "type": "regex", "pattern": "struct\\s+ShareMetadata\\s*{[\\s\\S]*string\\s+ISIN;[\\s\\S]*uint256\\s+nominalValue;[\\s\\S]*string\\s+shareClass;", "flags": "m", "target": "solidity", "error_hint": "Define the `ShareMetadata` struct with fields `ISIN`, `nominalValue`, and `shareClass`."}
```

### Step 1.2 — Add a mapping for Share Metadata

**Instruction:** Add a public mapping from address to ShareMetadata named `shares` to store metadata for each shareholder.

**Explanation:** Just as in core-banking systems, where each shareholder has specific attributes like ISIN and nominal value, we need to define this mapping in our Solidity contract. This will help us manage the metadata of each shareholder.

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

    // Define the shares mapping here
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

    mapping(address => ShareMetadata) public shares;
}
```

**Validation rule:** The `shares` mapping should be declared as public.

```checker
{"id": "ch13-l1-s2", "type": "regex", "pattern": "mapping\\(address\\s+=>\\s+ShareMetadata\\)\\s+public\\s+shares;", "flags": "m", "target": "solidity", "error_hint": "Declare `shares` as a public mapping from address to ShareMetadata."}
```

## Lesson 2 — Implement Registrar Role

**Learning objective:** Implement a registrar role that acts as a transfer agent for shares.  
**Emphasis tags:** `[BANK]`  
**Track:** equity

In core-banking systems, the registrar or transfer agent is responsible for managing share transfers and ensuring compliance with regulations. Similarly, in our Solidity contract, we need to define a role that can perform these tasks.

### Step 2.1 — Define the Registrar Role

**Instruction:** Add a public state variable `registrar` of type address to represent the registrar role.

**Explanation:** Just as in core-banking systems, where there is a designated registrar or transfer agent, we need to define this role in our Solidity contract. This will allow us to manage who can perform certain actions related to share transfers.

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

    mapping(address => ShareMetadata) public shares;

    // Define the registrar role here
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

    mapping(address => ShareMetadata) public shares;

    address public registrar;
}
```

**Validation rule:** The `registrar` state variable should be declared as public.

```checker
{"id": "ch13-l2-s1", "type": "regex", "pattern": "address\\s+public\\s+registrar;", "flags": "m", "target": "solidity", "error_hint": "Declare `registrar` as a public address."}
```

### Step 2.2 — Add a modifier for Registrar Only Actions

**Instruction:** Create a modifier named `onlyRegistrar` that restricts certain functions to be called only by the registrar.

**Explanation:** Just as in core-banking systems, where certain actions can only be performed by authorized personnel, we need to define this restriction in our Solidity contract. This will ensure that only the registrar can perform certain actions related to share transfers.

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

    mapping(address => ShareMetadata) public shares;

    address public registrar;

    // Define the onlyRegistrar modifier here
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

    mapping(address => ShareMetadata) public shares;

    address public registrar;

    modifier onlyRegistrar() {
        require(msg.sender == registrar, "Caller is not the registrar");
        _;
    }
}
```

**Validation rule:** The `onlyRegistrar` modifier should be correctly defined.

```checker
{"id": "ch13-l2-s2", "type": "regex", "pattern": "modifier\\s+onlyRegistrar\\(\\)\\s*{[\\s\\S]*require\\(msg\\.sender\\s+==\\s+registrar,\\s+\"Caller\\s+is\\s+not\\s+the\\s+registrar\"\\);", "flags": "m", "target": "solidity", "error_hint": "Define the `onlyRegistrar` modifier correctly."}
```

## Lesson 3 — Bind Shareholder Identity

**Learning objective:** Implement a mapping to bind shareholder identity (on-chain address) to off-chain KYC records.  
**Emphasis tags:** `[BANK]` `[TYPES]`  
**Track:** equity

In core-banking systems, it is crucial to ensure that each shareholder's on-chain address corresponds to an off-chain KYC record. Similarly, in our Solidity contract, we need to implement a mapping to achieve this.

### Step 3.1 — Define the Shareholder Identity Mapping

**Instruction:** Add a public mapping from address to bool named `isKYCVerified` to represent whether a shareholder's identity is verified.

**Explanation:** Just as in core-banking systems, where each shareholder's identity must be verified against an off-chain KYC record, we need to implement this verification mechanism in our Solidity contract. This will ensure that only verified shareholders can perform certain actions.

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

    mapping(address => ShareMetadata) public shares;

    address public registrar;

    modifier onlyRegistrar() {
        require(msg.sender == registrar, "Caller is not the registrar");
        _;
    }

    // Define the shareholder identity mapping here
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

    mapping(address => ShareMetadata) public shares;

    address public registrar;

    modifier onlyRegistrar() {
        require(msg.sender == registrar, "Caller is not the registrar");
        _;
    }

    mapping(address => bool) public isKYCVerified;
}
```

**Validation rule:** The `isKYCVerified` mapping should be declared as public.

```checker
{"id": "ch13-l3-s1", "type": "regex", "pattern": "mapping\\(address\\s+=>\\s+bool\\)\\s+public\\s+isKYCVerified;", "flags": "m", "target": "solidity", "error_hint": "Declare `isKYCVerified` as a public mapping from address to bool."}
```

### Step 3.2 — Add a Function to Verify KYC

**Instruction:** Create a function named `verifyKYC` that marks a shareholder's identity as verified.

**Explanation:** Just as in core-banking systems, where each shareholder's identity must be verified against an off-chain KYC record, we need to implement this verification mechanism in our Solidity contract. This will ensure that only verified shareholders can perform certain actions.

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

    mapping(address => ShareMetadata) public shares;

    address public registrar;

    modifier onlyRegistrar() {
        require(msg.sender == registrar, "Caller is not the registrar");
        _;
    }

    mapping(address => bool) public isKYCVerified;

    // Define the verifyKYC function here
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

    mapping(address => ShareMetadata) public shares;

    address public registrar;

    modifier onlyRegistrar() {
        require(msg.sender == registrar, "Caller is not the registrar");
        _;
    }

    mapping(address => bool) public isKYCVerified;

    function verifyKYC(address shareholder) external onlyRegistrar {
        isKYCVerified[shareholder] = true;
    }
}
```

**Validation rule:** The `verifyKYC` function should be correctly defined.

```checker
{"id": "ch13-l3-s2", "type": "regex", "pattern": "function\\s+verifyKYC\\(address\\s+shareholder\\)\\s+external\\s+onlyRegistrar\\s*{[\\s\\S]*isKYCVerified\\[shareholder\\]\\s+=\\s+true;", "flags": "m", "target": "solidity", "error_hint": "Define the `verifyKYC` function correctly."}
```

## Lesson 4 — Implement DocumentModule

**Learning objective:** Implement a DocumentModule for articles of association / terms.  
**Emphasis tags:** `[BANK]` `[TYPES]`  
**Track:** equity

In core-banking systems, documents such as articles of association and terms are crucial for managing the legal aspects of share registration. Similarly, in our Solidity contract, we need to implement a module to handle these documents.

### Step 4.1 — Define the DocumentModule Struct

**Instruction:** Inside `ShareToken.sol`, define a struct named `Document` with fields for documentName (string) and documentHash (bytes32).

**Explanation:** Just as in core-banking systems, where documents such as articles of association are stored and managed, we need to define these documents in our Solidity contract. This struct will help us manage the metadata of each document.

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

    mapping(address => ShareMetadata) public shares;

    address public registrar;

    modifier onlyRegistrar() {
        require(msg.sender == registrar, "Caller is not the registrar");
        _;
    }

    mapping(address => bool) public isKYCVerified;

    function verifyKYC(address shareholder) external onlyRegistrar {
        isKYCVerified[shareholder] = true;
    }

    // Define the Document struct here
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

    mapping(address => ShareMetadata) public shares;

    address public registrar;

    modifier onlyRegistrar() {
        require(msg.sender == registrar, "Caller is not the registrar");
        _;
    }

    mapping(address => bool) public isKYCVerified;

    function verifyKYC(address shareholder) external onlyRegistrar {
        isKYCVerified[shareholder] = true;
    }

    struct Document {
        string documentName;
        bytes32 documentHash;
    }
}
```

**Validation rule:** The `Document` struct should have the correct fields and types.

```checker
{"id": "ch13-l4-s1", "type": "regex", "pattern": "struct\\s+Document\\s*{[\\s\\S]*string\\s+documentName;[\\s\\S]*bytes32\\s+documentHash;", "flags": "m", "target": "solidity", "error_hint": "Define the `Document` struct with fields `documentName` and `documentHash`."}
```

### Step 4.2 — Add a Mapping for Documents

**Instruction:** Add a public mapping from string to Document named `documents` to store documents by name.

**Explanation:** Just as in core-banking systems, where documents such as articles of association are stored and managed, we need to define this mapping in our Solidity contract. This will help us manage the metadata of each document.

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

    mapping(address => ShareMetadata) public shares;

    address public registrar;

    modifier onlyRegistrar() {
        require(msg.sender == registrar, "Caller is not the registrar");
        _;
    }

    mapping(address => bool) public isKYCVerified;

    function verifyKYC(address shareholder) external onlyRegistrar {
        isKYCVerified[shareholder] = true;
    }

    struct Document {
        string documentName;
        bytes32 documentHash;
    }

    // Define the documents mapping here
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

    mapping(address => ShareMetadata) public shares;

    address public registrar;

    modifier onlyRegistrar() {
        require(msg.sender == registrar, "Caller is not the registrar");
        _;
    }

    mapping(address => bool) public isKYCVerified;

    function verifyKYC(address shareholder) external onlyRegistrar {
        isKYCVerified[shareholder] = true;
    }

    struct Document {
        string documentName;
        bytes32 documentHash;
    }

    mapping(string => Document) public documents;
}
```

**Validation rule:** The `documents` mapping should be declared as public.

```checker
{"id": "ch13-l4-s2", "type": "regex", "pattern": "mapping\\(string\\s+=>\\s+Document\\)\\s+public\\s+documents;", "flags": "m", "target": "solidity", "error_hint": "Declare `documents` as a public mapping from string to Document."}
```

## Lesson 5 — Add Events for Share Registration

**Learning objective:** Define events to log share registration actions.  
**Emphasis tags:** `[BANK]`  
**Track:** equity

In core-banking systems, it is crucial to maintain an audit trail of all transactions and actions. Similarly, in our Solidity contract, we need to define events to log share registration actions.

### Step 5.1 — Define the ShareRegistered Event

**Instruction:** Inside `ShareToken.sol`, define an event named `ShareRegistered` that logs the shareholder's address and their metadata.

**Explanation:** Just as in core-banking systems, where all transactions are logged for auditing purposes, we need to define this event in our Solidity contract. This will help us maintain an audit trail of share registration actions.

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

    mapping(address => ShareMetadata) public shares;

    address public registrar;

    modifier onlyRegistrar() {
        require(msg.sender == registrar, "Caller is not the registrar");
        _;
    }

    mapping(address => bool) public isKYCVerified;

    function verifyKYC(address shareholder) external onlyRegistrar {
        isKYCVerified[shareholder] = true;
    }

    struct Document {
        string documentName;
        bytes32 documentHash;
    }

    mapping(string => Document) public documents;

    // Define the ShareRegistered event here
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

    mapping(address => ShareMetadata) public shares;

    address public registrar;

    modifier onlyRegistrar() {
        require(msg.sender == registrar, "Caller is not the registrar");
        _;
    }

    mapping(address => bool) public isKYCVerified;

    function verifyKYC(address shareholder) external onlyRegistrar {
        isKYCVerified[shareholder] = true;
    }

    struct Document {
        string documentName;
        bytes32 documentHash;
    }

    mapping(string => Document) public documents;

    event ShareRegistered(address indexed shareholder, ShareMetadata metadata);
}
```

**Validation rule:** The `ShareRegistered` event should be correctly defined.

```checker
{"id": "ch13-l5-s1", "type": "regex", "pattern": "event\\s+ShareRegistered\\(address\\s+indexed\\s+shareholder,\\s+ShareMetadata\\s+metadata\\);", "flags": "m", "target": "solidity", "error_hint": "Define the `ShareRegistered` event correctly."}
```

### Step 5.2 — Emit the ShareRegistered Event

**Instruction:** Modify the `verifyKYC` function to emit the `ShareRegistered` event when a shareholder's identity is verified.

**Explanation:** Just as in core-banking systems, where all transactions are logged for auditing purposes, we need to modify this function in our Solidity contract. This will help us maintain an audit trail of share registration actions.

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

    mapping(address => ShareMetadata) public shares;

    address public registrar;

    modifier onlyRegistrar() {
        require(msg.sender == registrar, "Caller is not the registrar");
        _;
    }

    mapping(address => bool) public isKYCVerified;

    function verifyKYC(address shareholder) external onlyRegistrar {
        isKYCVerified[shareholder] = true;
    }

    struct Document {
        string documentName;
        bytes32 documentHash;
    }

    mapping(string => Document) public documents;

    event ShareRegistered(address indexed shareholder, ShareMetadata metadata);

    // Emit the ShareRegistered event here
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

    mapping(address => ShareMetadata) public shares;

    address public registrar;

    modifier onlyRegistrar() {
        require(msg.sender == registrar, "Caller is not the registrar");
        _;
    }

    mapping(address => bool) public isKYCVerified;

    function verifyKYC(address shareholder) external onlyRegistrar {
        isKYCVerified[shareholder] = true;
        emit ShareRegistered(shareholder, shares[shareholder]);
    }

    struct Document {
        string documentName;
        bytes32 documentHash;
    }

    mapping(string => Document) public documents;

    event ShareRegistered(address indexed shareholder, ShareMetadata metadata);
}
```

**Validation rule:** The `verifyKYC` function should emit the `ShareRegistered` event.

```checker
{"id": "ch13-l5-s2", "type": "regex", "pattern": "emit\\s+ShareRegistered\\(shareholder,\\s+shares\\[shareholder\\]\\);", "flags": "m", "target": "solidity", "error_hint": "Emit the `ShareRegistered` event in the `verifyKYC` function."}
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

    mapping(address => ShareMetadata) public shares;

    address public registrar;

    modifier onlyRegistrar() {
        require(msg.sender == registrar, "Caller is not the registrar");
        _;
    }

    mapping(address => bool) public isKYCVerified;

    function verifyKYC(address shareholder) external onlyRegistrar {
        isKYCVerified[shareholder] = true;
        emit ShareRegistered(shareholder, shares[shareholder]);
    }

    struct Document {
        string documentName;
        bytes32 documentHash;
    }

    mapping(string => Document) public documents;

    event ShareRegistered(address indexed shareholder, ShareMetadata metadata);
}
```

## Quiz

**Q1 (multiple choice).** Which of the following is NOT a component of the Share Metadata struct in CMTAT?
a) ISIN — b) Nominal Value — c) Share Class — d) Bank Account Number
**Answer: d.** The Share Metadata struct includes ISIN, Nominal Value, and Share Class, but not Bank Account Number.

**Q2 (multiple choice).** In the context of CMTAT, which role is responsible for managing the transfer of shares?
a) Issuer — b) Registrar — c) Auditor — d) Shareholder
**Answer: b.** The Registrar role in CMTAT is responsible for managing the transfer of shares.

**Q3 (multiple choice).** What is the purpose of binding shareholder identity in CMTAT?
a) To ensure that only authorized users can access the system — b) To link on-chain addresses with off-chain KYC records — c) To facilitate faster transaction processing — d) To prevent double spending
**Answer: b.** The purpose of binding shareholder identity is to link on-chain addresses with off-chain KYC records.

**Q4 (short answer).** Explain the role of the DocumentModule in CMTAT.
**Answer:** The DocumentModule in CMTAT is used to store and manage important documents such as articles of association and terms, ensuring that they are accessible and immutable within the blockchain system.

**Q5 (short answer).** Why is it important to add events for share registration in a Solidity smart contract?
**Answer:** Adding events for share registration in a Solidity smart contract is important because it provides transparency and allows stakeholders to track changes and updates related to share registrations, enhancing accountability and traceability.
