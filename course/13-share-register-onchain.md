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

    address public registrar;
}
```

**Validation rule:** The `registrar` state variable should be declared as public.

```checker
{"id": "ch13-l2-s1", "type": "regex", "pattern": "address\\s+public\\s+registrar;", "flags": "m", "target": "solidity", "error_hint": "Declare `registrar` as a public address."}
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

    address public registrar;

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

    address public registrar;

    mapping(address => bool) public isKYCVerified;
}
```

**Validation rule:** The `isKYCVerified` mapping should be declared as public.

```checker
{"id": "ch13-l3-s1", "type": "regex", "pattern": "mapping\\(address\\s+=>\\s+bool\\)\\s+public\\s+isKYCVerified;", "flags": "m", "target": "solidity", "error_hint": "Declare `isKYCVerified` as a public mapping from address to bool."}
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

    address public registrar;

    mapping(address => bool) public isKYCVerified;

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

    address public registrar;

    mapping(address => bool) public isKYCVerified;

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
}
```

## Quiz

1. What is the purpose of the `ShareMetadata` struct in this contract?
   - [ ] To manage share transfers
   - [ ] To store shareholder identity
   - [x] To hold metadata for each share

2. Which state variable represents the registrar role in this contract?
   - [ ] `admin`
   - [x] `registrar`
   - [ ] `shareholder`

3. What is the purpose of the `isKYCVerified` mapping in this contract?
   - [ ] To store document hashes
   - [x] To bind shareholder identity to off-chain KYC records
   - [ ] To manage share metadata

4. Which struct is used to represent documents such as articles of association in this contract?
   - [ ] `ShareMetadata`
   - [ ] `DocumentModule`
   - [x] `Document`

5. What is the purpose of the `registrar` role in this contract?
   - [ ] To manage shareholder identity
   - [x] To act as a transfer agent for shares
   - [ ] To store document hashes