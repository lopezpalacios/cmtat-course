# Chapter 10 — Bond Instrument Modeling with the Debt Module `[A] [TYPES-heavy]`

**Track:** A  
**Emphasis threads:** `[BANK]` `[TYPES]`  
**Chapter learning objective:** Model a CHF fixed-rate bond using the CMTAT DebtModule pattern, including debt metadata such as interest rate, par value, maturity date, coupon frequency, ISIN, and rating fields.  
**Prerequisites:** Understanding of Solidity basics, familiarity with financial instruments, and experience with Java for banking integration.  
**You will build:** A `BondToken.sol` contract that models a CHF fixed-rate bond with the specified metadata fields.

## Lesson 1 — Setting Up the Bond Metadata Structure

**Learning objective:** Define the debt metadata structure for the bond token.  
**Emphasis tags:** `[BANK]` `[TYPES]`

In banking, managing financial instruments like bonds involves maintaining detailed metadata such as interest rates, par values, maturity dates, and ratings. This metadata is crucial for risk assessment, trading, and regulatory compliance. Similarly, in Solidity, we need to define a struct that holds all the necessary information about the bond. This struct will help us manage and access bond data efficiently.

### Step 1.1 — Define the DebtInfo Struct

**Instruction:** Create the file `BondToken.sol` and define a `DebtInfo` struct with fields for interest rate, par value, maturity date, coupon frequency, ISIN, and rating.

**Explanation:** Just like in banking where each bond has specific attributes that need to be tracked, we define a struct in Solidity to encapsulate all the relevant information about the bond. This struct will help us manage and access bond data efficiently. The `DebtInfo` struct should include fields for interest rate (in basis points), par value, maturity date (as a timestamp), coupon frequency (in days), ISIN (as bytes32), and rating.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract BondToken {
    // Define DebtInfo struct here
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract BondToken {
    struct DebtInfo {
        uint256 interestRate; // in basis points
        uint256 parValue;
        uint256 maturityDate; // timestamp
        uint256 couponFrequency; // in days
        bytes32 ISIN;
        string rating;
    }
}
```

**Validation rule:** The `DebtInfo` struct should have the correct fields with the specified types.

```checker
{"id": "ch10-l1-s1", "type": "regex", "pattern": "uint256\\s+couponFrequency;", "flags": "m", "target": "solidity", "error_hint": "Ensure the DebtInfo struct has all required fields with correct types."}
```

### Step 1.2 — Define Day-Count Conventions

**Instruction:** Add two additional fields to the `DebtInfo` struct for day-count conventions: `dayCountConvention` and `basisPointRate`.

**Explanation:** In banking, day-count conventions are crucial for calculating interest accruals accurately. Similarly, in Solidity, we need to include these conventions in our bond metadata. The `dayCountConvention` field will store the convention as an integer (e.g., 1 for ACT/365, 2 for 30/360), and the `basisPointRate` field will store the basis point rate as a uint256.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract BondToken {
    struct DebtInfo {
        uint256 interestRate; // in basis points
        uint256 parValue;
        uint256 maturityDate; // timestamp
        uint256 couponFrequency; // in days
        bytes32 ISIN;
        string rating;
        // Add dayCountConvention and basisPointRate here
    }
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract BondToken {
    struct DebtInfo {
        uint256 interestRate; // in basis points
        uint256 parValue;
        uint256 maturityDate; // timestamp
        uint256 couponFrequency; // in days
        bytes32 ISIN;
        string rating;
        uint8 dayCountConvention; // 1 for ACT/365, 2 for 30/360
        uint256 basisPointRate; // basis point rate
    }
}
```

**Validation rule:** The `DebtInfo` struct should include the `dayCountConvention` and `basisPointRate` fields.

```checker
{"id": "ch10-l1-s2", "type": "regex", "pattern": "uint8\\s+dayCountConvention;", "flags": "m", "target": "solidity", "error_hint": "Ensure the DebtInfo struct includes dayCountConvention and basisPointRate fields."}
```

## Lesson 2 — Initializing Bond Metadata

**Learning objective:** Initialize the bond metadata using the `DebtInfo` struct.  
**Emphasis tags:** `[BANK]` `[TYPES]`

In banking, when a new bond is issued, its metadata must be initialized and stored securely. Similarly, in Solidity, we need to initialize our bond metadata and store it within the contract.

### Step 2.1 — Declare State Variables

**Instruction:** Add state variables to hold the bond's metadata and an admin address for managing the bond.

**Explanation:** Just like a bank would have a system to manage different bonds with their respective data, we need to declare state variables in our Solidity contract to store this information. The `admin` variable will allow us to control who can update the bond metadata.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract BondToken {
    struct DebtInfo {
        uint256 interestRate; // in basis points
        uint256 parValue;
        uint256 maturityDate; // timestamp
        uint256 couponFrequency; // in days
        bytes32 ISIN;
        string rating;
        uint8 dayCountConvention; // 1 for ACT/365, 2 for 30/360
        uint256 basisPointRate; // basis point rate
    }

    // Declare state variables here
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract BondToken {
    struct DebtInfo {
        uint256 interestRate; // in basis points
        uint256 parValue;
        uint256 maturityDate; // timestamp
        uint256 couponFrequency; // in days
        bytes32 ISIN;
        string rating;
        uint8 dayCountConvention; // 1 for ACT/365, 2 for 30/360
        uint256 basisPointRate; // basis point rate
    }

    address public admin;
    DebtInfo public bondMetadata;

    constructor(address _admin, DebtInfo memory _bondMetadata) {
        admin = _admin;
        bondMetadata = _bondMetadata;
    }
}
```

**Validation rule:** The contract should have an `admin` address and a `bondMetadata` state variable of type `DebtInfo`.

```checker
{"id": "ch10-l2-s1", "type": "regex", "pattern": "address\\s+public\\s+admin;", "flags": "m", "target": "solidity", "error_hint": "Ensure the contract has an admin address and a bondMetadata state variable."}
```

### Step 2.2 — Initialize Bond Metadata

**Instruction:** Modify the constructor to initialize the `bondMetadata` with specific values.

**Explanation:** In banking, when a new bond is issued, its metadata must be initialized with specific values such as interest rate, par value, maturity date, etc. Similarly, in Solidity, we need to ensure that the `bondMetadata` is properly initialized when the contract is deployed.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract BondToken {
    struct DebtInfo {
        uint256 interestRate; // in basis points
        uint256 parValue;
        uint256 maturityDate; // timestamp
        uint256 couponFrequency; // in days
        bytes32 ISIN;
        string rating;
        uint8 dayCountConvention; // 1 for ACT/365, 2 for 30/360
        uint256 basisPointRate; // basis point rate
    }

    address public admin;
    DebtInfo public bondMetadata;

    constructor(address _admin) {
        admin = _admin;
        // Initialize bondMetadata here
    }
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract BondToken {
    struct DebtInfo {
        uint256 interestRate; // in basis points
        uint256 parValue;
        uint256 maturityDate; // timestamp
        uint256 couponFrequency; // in days
        bytes32 ISIN;
        string rating;
        uint8 dayCountConvention; // 1 for ACT/365, 2 for 30/360
        uint256 basisPointRate; // basis point rate
    }

    address public admin;
    DebtInfo public bondMetadata;

    constructor(address _admin) {
        admin = _admin;
        bondMetadata = DebtInfo({
            interestRate: 100, // 1% in basis points
            parValue: 1000000, // CHF 1,000,000
            maturityDate: block.timestamp + 365 days,
            couponFrequency: 90, // every 3 months
            ISIN: "CH0038742062", // example ISIN
            rating: "AAA",
            dayCountConvention: 1, // ACT/365
            basisPointRate: 100 // 1% in basis points
        });
    }
}
```

**Validation rule:** The contract should initialize the `bondMetadata` with specific values.

```checker
{"id": "ch10-l2-s2", "type": "regex", "pattern": "interestRate:\\s+100,", "flags": "m", "target": "solidity", "error_hint": "Ensure the bondMetadata is initialized with correct values."}
```

## Lesson 3 — Accessing Bond Metadata

**Learning objective:** Implement functions to access the bond metadata.  
**Emphasis tags:** `[BANK]` `[TYPES]`

In banking, accessing bond metadata is crucial for various operations such as trading, valuation, and reporting. Similarly, in Solidity, we need to implement getter functions to retrieve the bond's metadata.

### Step 3.1 — Implement Getter Functions

**Instruction:** Add getter functions for each field in the `DebtInfo` struct.

**Explanation:** Just like a bank would provide APIs or methods to access different pieces of information about a bond, we need to implement getter functions in our Solidity contract to allow external contracts or users to retrieve the bond's metadata. Each getter function should return the corresponding field from the `bondMetadata`.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract BondToken {
    struct DebtInfo {
        uint256 interestRate; // in basis points
        uint256 parValue;
        uint256 maturityDate; // timestamp
        uint256 couponFrequency; // in days
        bytes32 ISIN;
        string rating;
        uint8 dayCountConvention; // 1 for ACT/365, 2 for 30/360
        uint256 basisPointRate; // basis point rate
    }

    address public admin;
    DebtInfo public bondMetadata;

    constructor(address _admin) {
        admin = _admin;
        bondMetadata = DebtInfo({
            interestRate: 100, // 1% in basis points
            parValue: 1000000, // CHF 1,000,000
            maturityDate: block.timestamp + 365 days,
            couponFrequency: 90, // every 3 months
            ISIN: "CH0038742062", // example ISIN
            rating: "AAA",
            dayCountConvention: 1, // ACT/365
            basisPointRate: 100 // 1% in basis points
        });
    }

    // Implement getter functions here
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract BondToken {
    struct DebtInfo {
        uint256 interestRate; // in basis points
        uint256 parValue;
        uint256 maturityDate; // timestamp
        uint256 couponFrequency; // in days
        bytes32 ISIN;
        string rating;
        uint8 dayCountConvention; // 1 for ACT/365, 2 for 30/360
        uint256 basisPointRate; // basis point rate
    }

    address public admin;
    DebtInfo public bondMetadata;

    constructor(address _admin) {
        admin = _admin;
        bondMetadata = DebtInfo({
            interestRate: 100, // 1% in basis points
            parValue: 1000000, // CHF 1,000,000
            maturityDate: block.timestamp + 365 days,
            couponFrequency: 90, // every 3 months
            ISIN: "CH0038742062", // example ISIN
            rating: "AAA",
            dayCountConvention: 1, // ACT/365
            basisPointRate: 100 // 1% in basis points
        });
    }

    function getInterestRate() public view returns (uint256) {
        return bondMetadata.interestRate;
    }

    function getParValue() public view returns (uint256) {
        return bondMetadata.parValue;
    }

    function getMaturityDate() public view returns (uint256) {
        return bondMetadata.maturityDate;
    }

    function getCouponFrequency() public view returns (uint256) {
        return bondMetadata.couponFrequency;
    }

    function getISIN() public view returns (bytes32) {
        return bondMetadata.ISIN;
    }

    function getRating() public view returns (string memory) {
        return bondMetadata.rating;
    }

    function getDayCountConvention() public view returns (uint8) {
        return bondMetadata.dayCountConvention;
    }

    function getBasisPointRate() public view returns (uint256) {
        return bondMetadata.basisPointRate;
    }
}
```

**Validation rule:** The contract should have getter functions for each field in the `DebtInfo` struct.

```checker
{"id": "ch10-l3-s1", "type": "regex", "pattern": "function\\s+getCouponFrequency\\(\\)\\s+public\\s+view\\s+returns\\s+\\(uint256\\)\\s+\\{", "flags": "m", "target": "solidity", "error_hint": "Ensure the contract has getter functions for each field in the DebtInfo struct."}
```

### Step 3.2 — Decode ISIN in Java

**Instruction:** Write a Java method to decode the ISIN from bytes32 format using web3j.

**Explanation:** In banking, ISINs are often stored as bytes32 for efficient storage and retrieval on the blockchain. When integrating with Java systems, we need to convert these bytes32 values back into human-readable strings. The `Bytes32ToStringDecoder` class in web3j can be used to achieve this.

**Starter code:**
```java
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.utils.Numeric;

public class BondMetadataReader {
    // Implement ISIN decoding method here
}
```

**Solution:**
```java
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.utils.Numeric;

public class BondMetadataReader {
    public static String decodeISIN(Bytes32 isinBytes) {
        return Numeric.toHexString(isinBytes.getValue()).substring(2).toUpperCase();
    }
}
```

**Validation rule:** The Java method should correctly decode the ISIN from bytes32 format.

```checker
{"id": "ch10-l3-s2", "type": "regex", "pattern": "public\\s+static\\s+String\\s+decodeISIN\\(Bytes32\\s+isinBytes\\)\\s*{", "flags": "", "target": "java", "error_hint": "Ensure the Java method correctly decodes the ISIN from bytes32 format."}
```

## Lesson 4 — Updating Bond Metadata

**Learning objective:** Implement a function to update the bond metadata.  
**Emphasis tags:** `[BANK]` `[TYPES]`

In banking, updating bond metadata might be necessary due to changes in interest rates, ratings, or other factors. Similarly, in Solidity, we need to implement a function that allows authorized users (like the admin) to update the bond's metadata.

### Step 4.1 — Implement Update Function

**Instruction:** Add a function `updateBondMetadata` that allows the admin to update the bond metadata.

**Explanation:** Just like a bank would have procedures for updating bond information, we need to implement a secure function in our Solidity contract that only the admin can use to modify the bond's metadata. This ensures data integrity and prevents unauthorized changes.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract BondToken {
    struct DebtInfo {
        uint256 interestRate; // in basis points
        uint256 parValue;
        uint256 maturityDate; // timestamp
        uint256 couponFrequency; // in days
        bytes32 ISIN;
        string rating;
        uint8 dayCountConvention; // 1 for ACT/365, 2 for 30/360
        uint256 basisPointRate; // basis point rate
    }

    address public admin;
    DebtInfo public bondMetadata;

    constructor(address _admin) {
        admin = _admin;
        bondMetadata = DebtInfo({
            interestRate: 100, // 1% in basis points
            parValue: 1000000, // CHF 1,000,000
            maturityDate: block.timestamp + 365 days,
            couponFrequency: 90, // every 3 months
            ISIN: "CH0038742062", // example ISIN
            rating: "AAA",
            dayCountConvention: 1, // ACT/365
            basisPointRate: 100 // 1% in basis points
        });
    }

    function getInterestRate() public view returns (uint256) {
        return bondMetadata.interestRate;
    }

    function getParValue() public view returns (uint256) {
        return bondMetadata.parValue;
    }

    function getMaturityDate() public view returns (uint256) {
        return bondMetadata.maturityDate;
    }

    function getCouponFrequency() public view returns (uint256) {
        return bondMetadata.couponFrequency;
    }

    function getISIN() public view returns (bytes32) {
        return bondMetadata.ISIN;
    }

    function getRating() public view returns (string memory) {
        return bondMetadata.rating;
    }

    function getDayCountConvention() public view returns (uint8) {
        return bondMetadata.dayCountConvention;
    }

    function getBasisPointRate() public view returns (uint256) {
        return bondMetadata.basisPointRate;
    }

    // Implement updateBondMetadata function here
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract BondToken {
    struct DebtInfo {
        uint256 interestRate; // in basis points
        uint256 parValue;
        uint256 maturityDate; // timestamp
        uint256 couponFrequency; // in days
        bytes32 ISIN;
        string rating;
        uint8 dayCountConvention; // 1 for ACT/365, 2 for 30/360
        uint256 basisPointRate; // basis point rate
    }

    address public admin;
    DebtInfo public bondMetadata;

    constructor(address _admin) {
        admin = _admin;
        bondMetadata = DebtInfo({
            interestRate: 100, // 1% in basis points
            parValue: 1000000, // CHF 1,000,000
            maturityDate: block.timestamp + 365 days,
            couponFrequency: 90, // every 3 months
            ISIN: "CH0038742062", // example ISIN
            rating: "AAA",
            dayCountConvention: 1, // ACT/365
            basisPointRate: 100 // 1% in basis points
        });
    }

    function getInterestRate() public view returns (uint256) {
        return bondMetadata.interestRate;
    }

    function getParValue() public view returns (uint256) {
        return bondMetadata.parValue;
    }

    function getMaturityDate() public view returns (uint256) {
        return bondMetadata.maturityDate;
    }

    function getCouponFrequency() public view returns (uint256) {
        return bondMetadata.couponFrequency;
    }

    function getISIN() public view returns (bytes32) {
        return bondMetadata.ISIN;
    }

    function getRating() public view returns (string memory) {
        return bondMetadata.rating;
    }

    function getDayCountConvention() public view returns (uint8) {
        return bondMetadata.dayCountConvention;
    }

    function getBasisPointRate() public view returns (uint256) {
        return bondMetadata.basisPointRate;
    }

    event DebtInfoUpdated(DebtInfo newMetadata);

    function updateBondMetadata(DebtInfo memory _newMetadata) public {
        require(msg.sender == admin, "Only admin can update metadata");
        bondMetadata = _newMetadata;
        emit DebtInfoUpdated(_newMetadata);
    }
}
```

**Validation rule:** The contract should have an `updateBondMetadata` function that only the admin can call.

```checker
{"id": "ch10-l4-s1", "type": "regex", "pattern": "function\\s+updateBondMetadata\\(DebtInfo\\s+memory\\s+_newMetadata\\)\\s+public\\s*{", "flags": "", "target": "solidity", "error_hint": "Ensure the updateBondMetadata function is only callable by the admin."}
```

### Step 4.2 — Emit DebtInfoUpdated Event

**Instruction:** Add an event `DebtInfoUpdated` to log changes to the bond metadata.

**Explanation:** In banking, it's important to keep a record of changes made to bond metadata for auditing and compliance purposes. Similarly, in Solidity, we can use events to log changes to the bond metadata. This will help us track updates and ensure transparency.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract BondToken {
    struct DebtInfo {
        uint256 interestRate; // in basis points
        uint256 parValue;
        uint256 maturityDate; // timestamp
        uint256 couponFrequency; // in days
        bytes32 ISIN;
        string rating;
        uint8 dayCountConvention; // 1 for ACT/365, 2 for 30/360
        uint256 basisPointRate; // basis point rate
    }

    address public admin;
    DebtInfo public bondMetadata;

    constructor(address _admin) {
        admin = _admin;
        bondMetadata = DebtInfo({
            interestRate: 100, // 1% in basis points
            parValue: 1000000, // CHF 1,000,000
            maturityDate: block.timestamp + 365 days,
            couponFrequency: 90, // every 3 months
            ISIN: "CH0038742062", // example ISIN
            rating: "AAA",
            dayCountConvention: 1, // ACT/365
            basisPointRate: 100 // 1% in basis points
        });
    }

    function getInterestRate() public view returns (uint256) {
        return bondMetadata.interestRate;
    }

    function getParValue() public view returns (uint256) {
        return bondMetadata.parValue;
    }

    function getMaturityDate() public view returns (uint256) {
        return bondMetadata.maturityDate;
    }

    function getCouponFrequency() public view returns (uint256) {
        return bondMetadata.couponFrequency;
    }

    function getISIN() public view returns (bytes32) {
        return bondMetadata.ISIN;
    }

    function getRating() public view returns (string memory) {
        return bondMetadata.rating;
    }

    function getDayCountConvention() public view returns (uint8) {
        return bondMetadata.dayCountConvention;
    }

    function getBasisPointRate() public view returns (uint256) {
        return bondMetadata.basisPointRate;
    }

    // Implement DebtInfoUpdated event and updateBondMetadata function here
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract BondToken {
    struct DebtInfo {
        uint256 interestRate; // in basis points
        uint256 parValue;
        uint256 maturityDate; // timestamp
        uint256 couponFrequency; // in days
        bytes32 ISIN;
        string rating;
        uint8 dayCountConvention; // 1 for ACT/365, 2 for 30/360
        uint256 basisPointRate; // basis point rate
    }

    address public admin;
    DebtInfo public bondMetadata;

    constructor(address _admin) {
        admin = _admin;
        bondMetadata = DebtInfo({
            interestRate: 100, // 1% in basis points
            parValue: 1000000, // CHF 1,000,000
            maturityDate: block.timestamp + 365 days,
            couponFrequency: 90, // every 3 months
            ISIN: "CH0038742062", // example ISIN
            rating: "AAA",
            dayCountConvention: 1, // ACT/365
            basisPointRate: 100 // 1% in basis points
        });
    }

    function getInterestRate() public view returns (uint256) {
        return bondMetadata.interestRate;
    }

    function getParValue() public view returns (uint256) {
        return bondMetadata.parValue;
    }

    function getMaturityDate() public view returns (uint256) {
        return bondMetadata.maturityDate;
    }

    function getCouponFrequency() public view returns (uint256) {
        return bondMetadata.couponFrequency;
    }

    function getISIN() public view returns (bytes32) {
        return bondMetadata.ISIN;
    }

    function getRating() public view returns (string memory) {
        return bondMetadata.rating;
    }

    function getDayCountConvention() public view returns (uint8) {
        return bondMetadata.dayCountConvention;
    }

    function getBasisPointRate() public view returns (uint256) {
        return bondMetadata.basisPointRate;
    }

    event DebtInfoUpdated(DebtInfo newMetadata);

    function updateBondMetadata(DebtInfo memory _newMetadata) public {
        require(msg.sender == admin, "Only admin can update metadata");
        bondMetadata = _newMetadata;
        emit DebtInfoUpdated(_newMetadata);
    }
}
```

**Validation rule:** The contract should emit a `DebtInfoUpdated` event when the bond metadata is updated.

```checker
{"id": "ch10-l4-s2", "type": "regex", "pattern": "event\\s+DebtInfoUpdated\\(DebtInfo\\s+newMetadata\\);", "flags": "", "target": "solidity", "error_hint": "Ensure the contract emits a DebtInfoUpdated event when the bond metadata is updated."}
```

## Assembled Contract

```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract BondToken {
    struct DebtInfo {
        uint256 interestRate; // in basis points
        uint256 parValue;
        uint256 maturityDate; // timestamp
        uint256 couponFrequency; // in days
        bytes32 ISIN;
        string rating;
        uint8 dayCountConvention; // 1 for ACT/365, 2 for 30/360
        uint256 basisPointRate; // basis point rate
    }

    address public admin;
    DebtInfo public bondMetadata;

    constructor(address _admin) {
        admin = _admin;
        bondMetadata = DebtInfo({
            interestRate: 100, // 1% in basis points
            parValue: 1000000, // CHF 1,000,000
            maturityDate: block.timestamp + 365 days,
            couponFrequency: 90, // every 3 months
            ISIN: "CH0038742062", // example ISIN
            rating: "AAA",
            dayCountConvention: 1, // ACT/365
            basisPointRate: 100 // 1% in basis points
        });
    }

    function getInterestRate() public view returns (uint256) {
        return bondMetadata.interestRate;
    }

    function getParValue() public view returns (uint256) {
        return bondMetadata.parValue;
    }

    function getMaturityDate() public view returns (uint256) {
        return bondMetadata.maturityDate;
    }

    function getCouponFrequency() public view returns (uint256) {
        return bondMetadata.couponFrequency;
    }

    function getISIN() public view returns (bytes32) {
        return bondMetadata.ISIN;
    }

    function getRating() public view returns (string memory) {
        return bondMetadata.rating;
    }

    function getDayCountConvention() public view returns (uint8) {
        return bondMetadata.dayCountConvention;
    }

    function getBasisPointRate() public view returns (uint256) {
        return bondMetadata.basisPointRate;
    }

    event DebtInfoUpdated(DebtInfo newMetadata);

    function updateBondMetadata(DebtInfo memory _newMetadata) public {
        require(msg.sender == admin, "Only admin can update metadata");
        bondMetadata = _newMetadata;
        emit DebtInfoUpdated(_newMetadata);
    }
}
```

## Quiz

1. **What is the purpose of the `admin` address in the BondToken contract?**
   - A) To store the bond's par value.
   - B) To allow authorized updates to the bond metadata.
   - C) To track the bond's maturity date.

2. **How many getter functions are implemented for the `DebtInfo` struct in this chapter?**
   - A) 3
   - B) 5
   - C) 6

3. **What is the role of the `updateBondMetadata` function in the BondToken contract?**
   - A) To initialize the bond metadata.
   - B) To allow authorized updates to the bond metadata.
   - C) To retrieve the bond's interest rate.

4. **Which of the following statements is true regarding the `DebtInfo` struct?**
   - A) It contains only financial data fields.
   - B) It includes both financial and non-financial data fields.
   - C) It is used to store transaction history.

5. **What does the `require` statement in the `updateBondMetadata` function ensure?**
   - A) The bond metadata can be updated by anyone.
   - B) Only the admin address can update the bond metadata.
   - C) The bond metadata cannot be changed after initialization.