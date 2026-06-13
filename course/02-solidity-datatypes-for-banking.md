# Chapter 02 — Solidity Datatypes for Banking Integrators `[shared] [TYPES-heavy]`

**Track:** shared  
**Emphasis threads:** `[BANK]` `[TYPES]`  
**Chapter learning objective:** Master every Solidity type that crosses the bank boundary: `uint256` + decimals for monetary amounts, fixed-point money math, `bytes32` for ISIN/LEI/identifiers, `address` validation & checksums, `enum` lifecycle states, `struct` instrument metadata, `mapping` as the position-keeping table. Full Solidity↔web3j Java type-mapping table.  
**Prerequisites:** Basic understanding of Solidity and smart contracts. Familiarity with Java programming.  
**You will build:** A contract that demonstrates various Solidity datatypes relevant to banking integrations, along with a Java class that maps these types.

## Lesson 1 — Numeric Types: `uint256` for Monetary Values

**Learning objective:** Understand how to use `uint256` for representing monetary values in Solidity.  
**Emphasis tags:** `[BANK]` `[TYPES]`
**Track:** shared

In the banking world, precise representation of monetary values is crucial to avoid rounding errors and ensure accurate financial transactions. In Solidity, `uint256` is commonly used to represent these values because it can hold large numbers without any fractional part. This is analogous to using `BigDecimal` in Java for high-precision arithmetic.

### Step 1.1 — Declare a `uint256` variable

**Instruction:** Create the file `InstrumentTypes.sol`. Declare a `uint256` variable named `balance`.

**Explanation:** In Solidity, `uint256` is an unsigned integer type that can hold values from 0 to 2^256-1. This is similar to using `long` in Java for large integers, but with the added benefit of being able to handle very large numbers without overflow issues.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract InstrumentTypes {
    // declare here
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract InstrumentTypes {
    uint256 public balance;
}
```

**Validation rule:** Declare a `uint256` variable named `balance`.

```checker
{"id": "ch02-l1-s1", "type": "regex", "pattern": "uint256\\s+public\\s+balance\\s*;", "flags": "m", "target": "solidity", "error_hint": "Declare a `uint256` variable named `balance`."}
```

### Step 1.2 — Initialize the `uint256` variable

**Instruction:** Set the initial value of `balance` to 1000000.

**Explanation:** Initializing variables is important to ensure that they have a known starting state. In Java, you would initialize a variable like this: `long balance = 1000000L;`. Similarly, in Solidity, you can set the initial value of a variable at the point of declaration.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract InstrumentTypes {
    uint256 public balance;
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract InstrumentTypes {
    uint256 public balance = 1000000;
}
```

**Validation rule:** Initialize the `uint256` variable `balance` to 1000000.

```checker
{"id": "ch02-l1-s2", "type": "regex", "pattern": "uint256\\s+public\\s+balance\\s*=\\s*1000000;", "flags": "m", "target": "solidity", "error_hint": "Initialize the `uint256` variable `balance` to 1000000."}
```

## Lesson 2 — Fixed-Point Arithmetic for Monetary Values

**Learning objective:** Understand how to perform fixed-point arithmetic in Solidity using `uint256`.  
**Emphasis tags:** `[BANK]` `[TYPES]`
**Track:** shared

In banking, monetary values often require fractional parts. While Solidity does not have a built-in decimal type, you can simulate fixed-point arithmetic by scaling your numbers. For example, if you want to represent dollars and cents, you could use `uint256` where 1 unit represents one cent.

### Step 2.1 — Declare a `uint256` variable for price

**Instruction:** Add a `uint256` variable named `price` to the contract.

**Explanation:** The `price` variable will represent the price of an instrument in cents. This is similar to using `BigDecimal` in Java, where you can scale your numbers to achieve fractional precision.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract InstrumentTypes {
    uint256 public balance = 1000000;
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract InstrumentTypes {
    uint256 public balance = 1000000;
    uint256 public price;
}
```

**Validation rule:** Declare a `uint256` variable named `price`.

```checker
{"id": "ch02-l2-s1", "type": "regex", "pattern": "uint256\\s+public\\s+balance\\s*=\\s*1000000;[\\s\\S]*uint256\\s+public\\s+price;", "flags": "m", "target": "solidity", "error_hint": "Declare a `uint256` variable named `price`."}
```

### Step 2.2 — Initialize the `price` variable

**Instruction:** Set the initial value of `price` to 100.

**Explanation:** Setting the initial price to 100 means that each unit represents one cent, so a price of 100 would be equivalent to $1.00. This is similar to initializing a `BigDecimal` in Java with a scale of two decimal places.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract InstrumentTypes {
    uint256 public balance = 1000000;
    uint256 public price;
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract InstrumentTypes {
    uint256 public balance = 1000000;
    uint256 public price = 100;
}
```

**Validation rule:** Initialize the `uint256` variable `price` to 100.

```checker
{"id": "ch02-l2-s2", "type": "regex", "pattern": "uint256\\s+public\\s+balance\\s*=\\s*1000000;[\\s\\S]*uint256\\s+public\\s+price\\s*=\\s*100;", "flags": "m", "target": "solidity", "error_hint": "Initialize the `uint256` variable `price` to 100."}
```

## Lesson 3 — String and Identifier Types: `bytes32`

**Learning objective:** Understand how to use `bytes32` for storing identifiers like ISIN, LEI, etc.  
**Emphasis tags:** `[BANK]` `[TYPES]`
**Track:** shared

In banking, instruments often have unique identifiers such as ISIN (International Securities Identification Number) or LEI (Legal Entity Identifier). These identifiers are typically represented as strings in Java, but in Solidity, `bytes32` is a fixed-size byte array that can be used to store these identifiers efficiently.

### Step 3.1 — Declare a `bytes32` variable for ISIN

**Instruction:** Add a `bytes32` variable named `isin`.

**Explanation:** The `isin` variable will store the ISIN of an instrument. In Java, you would use a `String` to represent this identifier. In Solidity, `bytes32` is a fixed-size byte array that can hold up to 32 bytes of data.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract InstrumentTypes {
    uint256 public balance = 1000000;
    uint256 public price = 100;
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract InstrumentTypes {
    uint256 public balance = 1000000;
    uint256 public price = 100;
    bytes32 public isin;
}
```

**Validation rule:** Declare a `bytes32` variable named `isin`.

```checker
{"id": "ch02-l3-s1", "type": "regex", "pattern": "uint256\\s+public\\s+balance\\s*=\\s*1000000;[\\s\\S]*uint256\\s+public\\s+price\\s*=\\s*100;[\\s\\S]*bytes32\\s+public\\s+isin;", "flags": "m", "target": "solidity", "error_hint": "Declare a `bytes32` variable named `isin`."}
```

### Step 3.2 — Initialize the `isin` variable

**Instruction:** Set the initial value of `isin` to "US0378331005".

**Explanation:** The ISIN "US0378331005" is a valid example of an ISIN identifier. In Java, you would initialize this as a `String`: `String isin = "US0378331005";`. In Solidity, you can use the `bytes32` type to store this identifier.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract InstrumentTypes {
    uint256 public balance = 1000000;
    uint256 public price = 100;
    bytes32 public isin;
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract InstrumentTypes {
    uint256 public balance = 1000000;
    uint256 public price = 100;
    bytes32 public isin = "US0378331005";
}
```

**Validation rule:** Initialize the `bytes32` variable `isin` to "US0378331005".

```checker
{"id": "ch02-l3-s2", "type": "regex", "pattern": "uint256\\s+public\\s+balance\\s*=\\s*1000000;[\\s\\S]*uint256\\s+public\\s+price\\s*=\\s*100;[\\s\\S]*bytes32\\s+public\\s+isin\\s*=\\s*\"US0378331005\";", "flags": "m", "target": "solidity", "error_hint": "Initialize the `bytes32` variable `isin` to \"US0378331005\"."}
```

## Lesson 4 — Enumerations for Lifecycle States

**Learning objective:** Understand how to use enums in Solidity to represent lifecycle states.  
**Emphasis tags:** `[BANK]` `[TYPES]`
**Track:** shared

In banking, instruments often have different lifecycle states such as "Active", "Inactive", or "Expired". Enums can be used in Solidity to represent these states clearly and concisely.

### Step 4.1 — Declare an `enum` for instrument states

**Instruction:** Add an `enum` named `InstrumentState` with values `Active`, `Inactive`, and `Expired`.

**Explanation:** The `InstrumentState` enum will represent the lifecycle state of an instrument. In Java, you would use an `enum` to achieve similar functionality. Enums in Solidity provide a way to define a set of named constants.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract InstrumentTypes {
    uint256 public balance = 1000000;
    uint256 public price = 100;
    bytes32 public isin;
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract InstrumentTypes {
    enum InstrumentState { Active, Inactive, Expired }

    uint256 public balance = 1000000;
    uint256 public price = 100;
    bytes32 public isin;
}
```

**Validation rule:** Declare an `enum` named `InstrumentState` with values `Active`, `Inactive`, and `Expired`.

```checker
{"id": "ch02-l4-s1", "type": "regex", "pattern": "enum\\s+InstrumentState\\s*{\\s*Active,\\s*Inactive,\\s*Expired\\s*}[\\s\\S]*uint256\\s+public\\s+balance\\s*=\\s*1000000;[\\s\\S]*uint256\\s+public\\s+price\\s*=\\s*100;[\\s\\S]*bytes32\\s+public\\s+isin;", "flags": "m", "target": "solidity", "error_hint": "Declare an `enum` named `InstrumentState` with values `Active`, `Inactive`, and `Expired`."}
```

### Step 4.2 — Declare a state variable using the enum

**Instruction:** Add a state variable of type `InstrumentState` named `state`.

**Explanation:** The `state` variable will store the current lifecycle state of an instrument. In Java, you would use an instance of the `enum` to represent this state.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract InstrumentTypes {
    enum InstrumentState { Active, Inactive, Expired }

    uint256 public balance = 1000000;
    uint256 public price = 100;
    bytes32 public isin;
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract InstrumentTypes {
    enum InstrumentState { Active, Inactive, Expired }

    uint256 public balance = 1000000;
    uint256 public price = 100;
    bytes32 public isin;
    InstrumentState public state;
}
```

**Validation rule:** Declare a state variable of type `InstrumentState` named `state`.

```checker
{"id": "ch02-l4-s2", "type": "regex", "pattern": "enum\\s+InstrumentState\\s*{\\s*Active,\\s*Inactive,\\s*Expired\\s*}[\\s\\S]*uint256\\s+public\\s+balance\\s*=\\s*1000000;[\\s\\S]*uint256\\s+public\\s+price\\s*=\\s*100;[\\s\\S]*bytes32\\s+public\\s+isin;[\\s\\S]*InstrumentState\\s+public\\s+state;", "flags": "m", "target": "solidity", "error_hint": "Declare a state variable of type `InstrumentState` named `state`."}
```

## Lesson 5 — Structs for Instrument Metadata

**Learning objective:** Understand how to use structs in Solidity to represent complex data structures.  
**Emphasis tags:** `[BANK]` `[TYPES]`
**Track:** shared

In banking, instruments often have multiple attributes that need to be stored together. Structs in Solidity provide a way to group these attributes into a single entity.

### Step 5.1 — Declare a `struct` for instrument metadata

**Instruction:** Add a `struct` named `InstrumentMetadata` with fields `name`, `isin`, and `price`.

**Explanation:** The `InstrumentMetadata` struct will represent the metadata of an instrument, including its name, ISIN, and price. In Java, you would use a class to achieve similar functionality.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract InstrumentTypes {
    enum InstrumentState { Active, Inactive, Expired }

    uint256 public balance = 1000000;
    uint256 public price = 100;
    bytes32 public isin;
    InstrumentState public state;
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract InstrumentTypes {
    struct InstrumentMetadata {
        string name;
        bytes32 isin;
        uint256 price;
    }

    enum InstrumentState { Active, Inactive, Expired }

    uint256 public balance = 1000000;
    uint256 public price = 100;
    bytes32 public isin;
    InstrumentState public state;
}
```

**Validation rule:** Declare a `struct` named `InstrumentMetadata` with fields `name`, `isin`, and `price`.

```checker
{"id": "ch02-l5-s1", "type": "regex", "pattern": "struct\\s+InstrumentMetadata\\s*{\\s*string\\s+name;[\\s\\S]*bytes32\\s+isin;[\\s\\S]*uint256\\s+price;\\s*}[\\s\\S]*enum\\s+InstrumentState\\s*{\\s*Active,\\s*Inactive,\\s*Expired\\s*}[\\s\\S]*uint256\\s+public\\s+balance\\s*=\\s*1000000;[\\s\\S]*uint256\\s+public\\s+price\\s*=\\s*100;[\\s\\S]*bytes32\\s+public\\s+isin;[\\s\\S]*InstrumentState\\s+public\\s+state;", "flags": "m", "target": "solidity", "error_hint": "Declare a `struct` named `InstrumentMetadata` with fields `name`, `isin`, and `price`."}
```

### Step 5.2 — Declare a variable of the struct type

**Instruction:** Add a variable of type `InstrumentMetadata` named `metadata`.

**Explanation:** The `metadata` variable will store an instance of the `InstrumentMetadata` struct. In Java, you would create an instance of a class to achieve similar functionality.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract InstrumentTypes {
    struct InstrumentMetadata {
        string name;
        bytes32 isin;
        uint256 price;
    }

    enum InstrumentState { Active, Inactive, Expired }

    uint256 public balance = 1000000;
    uint256 public price = 100;
    bytes32 public isin;
    InstrumentState public state;
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract InstrumentTypes {
    struct InstrumentMetadata {
        string name;
        bytes32 isin;
        uint256 price;
    }

    enum InstrumentState { Active, Inactive, Expired }

    uint256 public balance = 1000000;
    uint256 public price = 100;
    bytes32 public isin;
    InstrumentState public state;
    InstrumentMetadata public metadata;
}
```

**Validation rule:** Declare a variable of type `InstrumentMetadata` named `metadata`.

```checker
{"id": "ch02-l5-s2", "type": "regex", "pattern": "struct\\s+InstrumentMetadata\\s*{\\s*string\\s+name;[\\s\\S]*bytes32\\s+isin;[\\s\\S]*uint256\\s+price;\\s*}[\\s\\S]*enum\\s+InstrumentState\\s*{\\s*Active,\\s*Inactive,\\s*Expired\\s*}[\\s\\S]*uint256\\s+public\\s+balance\\s*=\\s*1000000;[\\s\\S]*uint256\\s+public\\s+price\\s*=\\s*100;[\\s\\S]*bytes32\\s+public\\s+isin;[\\s\\S]*InstrumentState\\s+public\\s+state;[\\s\\S]*InstrumentMetadata\\s+public\\s+metadata;", "flags": "m", "target": "solidity", "error_hint": "Declare a variable of type `InstrumentMetadata` named `metadata`."}
```

## Lesson 6 — Mappings for Position Keeping

**Learning objective:** Understand how to use mappings in Solidity to keep track of positions.  
**Emphasis tags:** `[BANK]` `[TYPES]`
**Track:** shared

In banking, keeping track of positions (e.g., the number of shares held by an account) is crucial. Mappings in Solidity provide a way to associate keys with values efficiently.

### Step 6.1 — Declare a mapping for positions

**Instruction:** Add a mapping named `positions` that maps addresses to `uint256`.

**Explanation:** The `positions` mapping will store the number of shares held by each address. In Java, you would use a `HashMap` to achieve similar functionality.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract InstrumentTypes {
    struct InstrumentMetadata {
        string name;
        bytes32 isin;
        uint256 price;
    }

    enum InstrumentState { Active, Inactive, Expired }

    uint256 public balance = 1000000;
    uint256 public price = 100;
    bytes32 public isin;
    InstrumentState public state;
    InstrumentMetadata public metadata;
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract InstrumentTypes {
    struct InstrumentMetadata {
        string name;
        bytes32 isin;
        uint256 price;
    }

    enum InstrumentState { Active, Inactive, Expired }

    uint256 public balance = 1000000;
    uint256 public price = 100;
    bytes32 public isin;
    InstrumentState public state;
    InstrumentMetadata public metadata;

    mapping(address => uint256) public positions;
}
```

**Validation rule:** Declare a mapping named `positions` that maps addresses to `uint256`.

```checker
{"id": "ch02-l6-s1", "type": "regex", "pattern": "struct\\s+InstrumentMetadata\\s*{\\s*string\\s+name;[\\s\\S]*bytes32\\s+isin;[\\s\\S]*uint256\\s+price;\\s*}[\\s\\S]*enum\\s+InstrumentState\\s*{\\s*Active,\\s*Inactive,\\s*Expired\\s*}[\\s\\S]*uint256\\s+public\\s+balance\\s*=\\s*1000000;[\\s\\S]*uint256\\s+public\\s+price\\s*=\\s*100;[\\s\\S]*bytes32\\s+public\\s+isin;[\\s\\S]*InstrumentState\\s+public\\s+state;[\\s\\S]*InstrumentMetadata\\s+public\\s+metadata;[\\s\\S]*mapping\\(address\\s*=>\\s*uint256\\)\\s+public\\s+positions;", "flags": "m", "target": "solidity", "error_hint": "Declare a mapping named `positions` that maps addresses to `uint256`."}
```

### Step 6.2 — Initialize the mapping

**Instruction:** Set the initial value of the position for address `0x1234567890123456789012345678901234567890` to 100.

**Explanation:** Initializing the mapping is important to ensure that each account has a known starting position. In Java, you would initialize a `HashMap` like this: `map.put(address, 100);`. Similarly, in Solidity, you can set the initial value of a mapping using the key.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract InstrumentTypes {
    struct InstrumentMetadata {
        string name;
        bytes32 isin;
        uint256 price;
    }

    enum InstrumentState { Active, Inactive, Expired }

    uint256 public balance = 1000000;
    uint256 public price = 100;
    bytes32 public isin;
    InstrumentState public state;
    InstrumentMetadata public metadata;

    mapping(address => uint256) public positions;
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract InstrumentTypes {
    struct InstrumentMetadata {
        string name;
        bytes32 isin;
        uint256 price;
    }

    enum InstrumentState { Active, Inactive, Expired }

    uint256 public balance = 1000000;
    uint256 public price = 100;
    bytes32 public isin;
    InstrumentState public state;
    InstrumentMetadata public metadata;

    mapping(address => uint256) public positions;

    constructor() {
        positions[0x1234567890123456789012345678901234567890] = 100;
    }
}
```

**Validation rule:** Set the initial value of the position for address `0x1234567890123456789012345678901234567890` to 100.

```checker
{"id": "ch02-l6-s2", "type": "regex", "pattern": "struct\\s+InstrumentMetadata\\s*{\\s*string\\s+name;[\\s\\S]*bytes32\\s+isin;[\\s\\S]*uint256\\s+price;\\s*}[\\s\\S]*enum\\s+InstrumentState\\s*{\\s*Active,\\s*Inactive,\\s*Expired\\s*}[\\s\\S]*uint256\\s+public\\s+balance\\s*=\\s*1000000;[\\s\\S]*uint256\\s+public\\s+price\\s*=\\s*100;[\\s\\S]*bytes32\\s+public\\s+isin;[\\s\\S]*InstrumentState\\s+public\\s+state;[\\s\\S]*InstrumentMetadata\\s+public\\s+metadata;[\\s\\S]*mapping\\(address\\s*=>\\s*uint256\\)\\s+public\\s+positions;[\\s\\S]*constructor\\(\\)\\s*{[\\s\\S]*positions\\[0x1234567890123456789012345678901234567890\\]\\s*=\\s*100;", "flags": "m", "target": "solidity", "error_hint": "Set the initial value of the position for address `0x1234567890123456789012345678901234567890` to 100."}
```

## Assembled Contract

```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract InstrumentTypes {
    struct InstrumentMetadata {
        string name;
        bytes32 isin;
        uint256 price;
    }

    enum InstrumentState { Active, Inactive, Expired }

    uint256 public balance = 1000000;
    uint256 public price = 100;
    bytes32 public isin = "US0378331005";
    InstrumentState public state;
    InstrumentMetadata public metadata;

    mapping(address => uint256) public positions;

    constructor() {
        positions[0x1234567890123456789012345678901234567890] = 100;
    }
}
```

## Quiz

**Q1 (multiple choice).** Which Solidity type is best suited for representing monetary values in a smart contract, ensuring precision without the need for floating-point arithmetic?
a) `uint256` — b) `int256` — c) `fixed128x18` — d) `bytes32`
**Answer: c.** The `fixed128x18` type provides fixed-point arithmetic, which is suitable for monetary values requiring precision without the pitfalls of floating-point calculations.

**Q2 (multiple choice).** When integrating a smart contract with a banking system using web3j in Java, what is the equivalent Java type for Solidity's `bytes32`?
a) `String` — b) `byte[]` — c) `BigInteger` — d) `Address`
**Answer: b.** The equivalent Java type for Solidity's `bytes32` when using web3j is `byte[]`.

**Q3 (multiple choice).** In a smart contract, which Solidity construct is used to define a set of named constants that represent different lifecycle states of an instrument?
a) `uint256` — b) `enum` — c) `struct` — d) `mapping`
**Answer: b.** The `enum` construct in Solidity is used to define a set of named constants representing different lifecycle states.

**Q4 (short answer).** Explain how you would use a `mapping` in a smart contract to keep track of positions for multiple users.
**Answer:** A `mapping` can be used to associate user addresses with their positions. For example, `mapping(address => uint256) public positions;` allows you to store and retrieve the position amount for each user by their address.

**Q5 (short answer).** Describe how fixed-point arithmetic in Solidity can help manage monetary values more accurately than floating-point arithmetic.
**Answer:** Fixed-point arithmetic in Solidity uses integer types with a fixed number of decimal places, allowing precise representation and manipulation of monetary values without the rounding errors common in floating-point arithmetic.
