# Chapter 11 — Coupons: Record Dates and Snapshots `[A] [BANK] [TYPES]`

**Track:** A  
**Emphasis threads:** `[BANK]` `[TYPES]`  
**Chapter learning objective:** Implement a snapshot mechanism for coupon record dates, compute coupons in fixed-point arithmetic, emit CouponDeclared/CouponPaid events, and integrate with a bank-side coupon-payment batch job.  
**Prerequisites:** Understanding of Solidity structs, events, and fixed-point arithmetic. Familiarity with Java for the bank-side integration.  
**You will build:** A `BondToken` contract with snapshot capabilities and coupon computation logic, along with a Java class to handle batch coupon payments.

## Lesson 1 — Snapshot Mechanism

**Learning objective:** Implement a snapshot mechanism to record coupon dates and balances.  
**Emphasis tags:** `[BANK]` `[TYPES]`

In core banking systems, snapshots are crucial for maintaining accurate records of account balances at specific points in time. Similarly, in our `BondToken` contract, we need to record the balance of each bond holder at the time a coupon is declared.

### Step 1.1 — Define Snapshot Struct

**Instruction:** Add a `Snapshot` struct to the `BondToken` contract to store snapshot information.

**Explanation:** Just like how banks maintain snapshots of account balances for auditing and reporting, we need to record the balance of each bond holder at specific times. This will help us compute accurate coupon payments later on.

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
    }

    struct Snapshot {
        uint256 date;
        uint256 balance;
    }

    address public admin;
    DebtInfo public bondMetadata;

    constructor(address _admin, DebtInfo memory _bondMetadata) {
        admin = _admin;
        bondMetadata = _bondMetadata;
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

    function updateBondMetadata(DebtInfo memory _newMetadata) public {
        require(msg.sender == admin, "Only admin can update metadata");
        bondMetadata = _newMetadata;
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
    }

    struct Snapshot {
        uint256 date;
        uint256 balance;
    }

    address public admin;
    DebtInfo public bondMetadata;

    constructor(address _admin, DebtInfo memory _bondMetadata) {
        admin = _admin;
        bondMetadata = _bondMetadata;
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

    function updateBondMetadata(DebtInfo memory _newMetadata) public {
        require(msg.sender == admin, "Only admin can update metadata");
        bondMetadata = _newMetadata;
    }
}
```

**Validation rule:** Define a `Snapshot` struct with `date` and `balance` fields.

```checker
{"id": "ch11-l1-s1", "type": "regex", "pattern": "function\\s+updateBondMetadata\\(DebtInfo\\s+memory\\s+_newMetadata\\)\\s+public\\s+\\{", "flags": "m", "target": "solidity", "error_hint": "Define a `Snapshot` struct with `date` and `balance` fields."}
```

### Step 1.2 — Declare Snapshot Storage

**Instruction:** Add a mapping to store snapshots for each bond holder.

**Explanation:** Similar to how banks maintain transaction histories for each account, we need to keep track of snapshots for each bond holder. This will allow us to compute accurate coupon payments based on the balance at the time of declaration.

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
    }

    struct Snapshot {
        uint256 date;
        uint256 balance;
    }

    address public admin;
    DebtInfo public bondMetadata;

    mapping(address => Snapshot[]) snapshots;

    constructor(address _admin, DebtInfo memory _bondMetadata) {
        admin = _admin;
        bondMetadata = _bondMetadata;
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

    function updateBondMetadata(DebtInfo memory _newMetadata) public {
        require(msg.sender == admin, "Only admin can update metadata");
        bondMetadata = _newMetadata;
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
    }

    struct Snapshot {
        uint256 date;
        uint256 balance;
    }

    address public admin;
    DebtInfo public bondMetadata;

    mapping(address => Snapshot[]) snapshots;

    constructor(address _admin, DebtInfo memory _bondMetadata) {
        admin = _admin;
        bondMetadata = _bondMetadata;
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

    function updateBondMetadata(DebtInfo memory _newMetadata) public {
        require(msg.sender == admin, "Only admin can update metadata");
        bondMetadata = _newMetadata;
    }
}
```

**Validation rule:** Declare a mapping `snapshots` to store snapshots for each bond holder.

```checker
{"id": "ch11-l1-s2", "type": "regex", "pattern": "mapping\\(address\\s+=>\\s+Snapshot\\[\\]\\)\\s+snapshots;", "flags": "m", "target": "solidity", "error_hint": "Declare a `snapshots` mapping to store snapshots for each bond holder."}
```

## Lesson 2 — Coupon Computation

**Learning objective:** Implement coupon computation in fixed-point arithmetic.  
**Emphasis tags:** `[TYPES]`

In core banking systems, computations involving financial instruments often require high precision. Similarly, we need to compute coupons with fixed-point arithmetic to ensure accuracy.

### Step 2.1 — Define Fixed-Point Constants

**Instruction:** Define constants for fixed-point arithmetic in the `BondToken` contract.

**Explanation:** Just like how banks use fixed-point arithmetic to handle fractional values accurately, we need to define constants for our coupon computation. This will allow us to perform precise calculations without losing precision due to floating-point errors.

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
    }

    struct Snapshot {
        uint256 date;
        uint256 balance;
    }

    address public admin;
    DebtInfo public bondMetadata;

    mapping(address => Snapshot[]) snapshots;

    uint256 constant FIXED_POINT_SCALE = 1e18;

    constructor(address _admin, DebtInfo memory _bondMetadata) {
        admin = _admin;
        bondMetadata = _bondMetadata;
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

    function updateBondMetadata(DebtInfo memory _newMetadata) public {
        require(msg.sender == admin, "Only admin can update metadata");
        bondMetadata = _newMetadata;
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
    }

    struct Snapshot {
        uint256 date;
        uint256 balance;
    }

    address public admin;
    DebtInfo public bondMetadata;

    mapping(address => Snapshot[]) snapshots;

    uint256 constant FIXED_POINT_SCALE = 1e18;

    constructor(address _admin, DebtInfo memory _bondMetadata) {
        admin = _admin;
        bondMetadata = _bondMetadata;
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

    function updateBondMetadata(DebtInfo memory _newMetadata) public {
        require(msg.sender == admin, "Only admin can update metadata");
        bondMetadata = _newMetadata;
    }
}
```

**Validation rule:** Define a constant `FIXED_POINT_SCALE` with a value of `1e18`.

```checker
{"id": "ch11-l2-s1", "type": "regex", "pattern": "uint256\\s+constant\\s+FIXED_POINT_SCALE\\s*=\\s*1e18;", "flags": "m", "target": "solidity", "error_hint": "Define a `FIXED_POINT_SCALE` constant with a value of `1e18`."}
```

### Step 2.2 — Implement Coupon Computation

**Instruction:** Add a function to compute the coupon amount for a given snapshot.

**Explanation:** Just like how banks calculate interest on loans, we need to compute the coupon amount based on the balance at the time of declaration. This will involve multiplying the interest rate, par value, and holdings, then dividing by the denominator using fixed-point arithmetic.

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
    }

    struct Snapshot {
        uint256 date;
        uint256 balance;
    }

    address public admin;
    DebtInfo public bondMetadata;

    mapping(address => Snapshot[]) snapshots;

    uint256 constant FIXED_POINT_SCALE = 1e18;

    constructor(address _admin, DebtInfo memory _bondMetadata) {
        admin = _admin;
        bondMetadata = _bondMetadata;
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

    function updateBondMetadata(DebtInfo memory _newMetadata) public {
        require(msg.sender == admin, "Only admin can update metadata");
        bondMetadata = _newMetadata;
    }

    function computeCoupon(address holder, uint256 snapshotIndex) public view returns (uint256) {
        Snapshot storage snap = snapshots[holder][snapshotIndex];
        uint256 interestRate = bondMetadata.interestRate;
        uint256 parValue = bondMetadata.parValue;
        uint256 couponAmount = (snap.balance * interestRate * parValue) / FIXED_POINT_SCALE;
        return couponAmount;
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
    }

    struct Snapshot {
        uint256 date;
        uint256 balance;
    }

    address public admin;
    DebtInfo public bondMetadata;

    mapping(address => Snapshot[]) snapshots;

    uint256 constant FIXED_POINT_SCALE = 1e18;

    constructor(address _admin, DebtInfo memory _bondMetadata) {
        admin = _admin;
        bondMetadata = _bondMetadata;
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

    function updateBondMetadata(DebtInfo memory _newMetadata) public {
        require(msg.sender == admin, "Only admin can update metadata");
        bondMetadata = _newMetadata;
    }

    function computeCoupon(address holder, uint256 snapshotIndex) public view returns (uint256) {
        Snapshot storage snap = snapshots[holder][snapshotIndex];
        uint256 interestRate = bondMetadata.interestRate;
        uint256 parValue = bondMetadata.parValue;
        uint256 couponAmount = (snap.balance * interestRate * parValue) / FIXED_POINT_SCALE;
        return couponAmount;
    }
}
```

**Validation rule:** Implement a `computeCoupon` function that calculates the coupon amount using fixed-point arithmetic.

```checker
{"id": "ch11-l2-s2", "type": "regex", "pattern": "function\\s+computeCoupon\\(address\\s+holder,\\s+uint256\\s+snapshotIndex\\)\\s+public\\s+view\\s+returns\\s+\\(uint256\\)\\s+\\{", "flags": "m", "target": "solidity", "error_hint": "Implement a `computeCoupon` function that calculates the coupon amount using fixed-point arithmetic."}
```

## Lesson 3 — Events for Coupon Declaration and Payment

**Learning objective:** Emit events when coupons are declared and paid.  
**Emphasis tags:** `[BANK]`

In core banking systems, events are crucial for tracking transactions and ensuring transparency. Similarly, we need to emit events when coupons are declared and paid.

### Step 3.1 — Define Coupon Events

**Instruction:** Add `CouponDeclared` and `CouponPaid` events to the `BondToken` contract.

**Explanation:** Just like how banks log every transaction for auditing purposes, we need to emit events when coupons are declared and paid. This will allow us to track the status of each coupon payment and ensure transparency.

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
    }

    struct Snapshot {
        uint256 date;
        uint256 balance;
    }

    address public admin;
    DebtInfo public bondMetadata;

    mapping(address => Snapshot[]) snapshots;

    uint256 constant FIXED_POINT_SCALE = 1e18;

    event CouponDeclared(address indexed holder, uint256 snapshotIndex, uint256 amount);
    event CouponPaid(address indexed holder, uint256 snapshotIndex, uint256 amount);

    constructor(address _admin, DebtInfo memory _bondMetadata) {
        admin = _admin;
        bondMetadata = _bondMetadata;
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

    function updateBondMetadata(DebtInfo memory _newMetadata) public {
        require(msg.sender == admin, "Only admin can update metadata");
        bondMetadata = _newMetadata;
    }

    function computeCoupon(address holder, uint256 snapshotIndex) public view returns (uint256) {
        Snapshot storage snap = snapshots[holder][snapshotIndex];
        uint256 interestRate = bondMetadata.interestRate;
        uint256 parValue = bondMetadata.parValue;
        uint256 couponAmount = (snap.balance * interestRate * parValue) / FIXED_POINT_SCALE;
        return couponAmount;
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
    }

    struct Snapshot {
        uint256 date;
        uint256 balance;
    }

    address public admin;
    DebtInfo public bondMetadata;

    mapping(address => Snapshot[]) snapshots;

    uint256 constant FIXED_POINT_SCALE = 1e18;

    event CouponDeclared(address indexed holder, uint256 snapshotIndex, uint256 amount);
    event CouponPaid(address indexed holder, uint256 snapshotIndex, uint256 amount);

    constructor(address _admin, DebtInfo memory _bondMetadata) {
        admin = _admin;
        bondMetadata = _bondMetadata;
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

    function updateBondMetadata(DebtInfo memory _newMetadata) public {
        require(msg.sender == admin, "Only admin can update metadata");
        bondMetadata = _newMetadata;
    }

    function computeCoupon(address holder, uint256 snapshotIndex) public view returns (uint256) {
        Snapshot storage snap = snapshots[holder][snapshotIndex];
        uint256 interestRate = bondMetadata.interestRate;
        uint256 parValue = bondMetadata.parValue;
        uint256 couponAmount = (snap.balance * interestRate * parValue) / FIXED_POINT_SCALE;
        return couponAmount;
    }
}
```

**Validation rule:** Define `CouponDeclared` and `CouponPaid` events.

```checker
{"id": "ch11-l3-s1", "type": "regex", "pattern": "event\\s+CouponPaid\\(address\\s+indexed\\s+holder,\\s+uint256\\s+snapshotIndex,\\s+uint256\\s+amount\\);", "flags": "", "target": "solidity", "error_hint": "Define `CouponDeclared` and `CouponPaid` events."}
```

### Step 3.2 — Emit Coupon Events

**Instruction:** Modify the `computeCoupon` function to emit `CouponDeclared` and `CouponPaid` events.

**Explanation:** Just like how banks log every transaction for auditing purposes, we need to emit events when coupons are declared and paid. This will allow us to track the status of each coupon payment and ensure transparency.

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
    }

    struct Snapshot {
        uint256 date;
        uint256 balance;
    }

    address public admin;
    DebtInfo public bondMetadata;

    mapping(address => Snapshot[]) snapshots;

    uint256 constant FIXED_POINT_SCALE = 1e18;

    event CouponDeclared(address indexed holder, uint256 snapshotIndex, uint256 amount);
    event CouponPaid(address indexed holder, uint256 snapshotIndex, uint256 amount);

    constructor(address _admin, DebtInfo memory _bondMetadata) {
        admin = _admin;
        bondMetadata = _bondMetadata;
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

    function updateBondMetadata(DebtInfo memory _newMetadata) public {
        require(msg.sender == admin, "Only admin can update metadata");
        bondMetadata = _newMetadata;
    }

    function computeCoupon(address holder, uint256 snapshotIndex) public view returns (uint256) {
        Snapshot storage snap = snapshots[holder][snapshotIndex];
        uint256 interestRate = bondMetadata.interestRate;
        uint256 parValue = bondMetadata.parValue;
        uint256 couponAmount = (snap.balance * interestRate * parValue) / FIXED_POINT_SCALE;
        return couponAmount;
    }

    function payCoupon(address holder, uint256 snapshotIndex) public {
        require(msg.sender == admin, "Only admin can pay coupons");
        Snapshot storage snap = snapshots[holder][snapshotIndex];
        uint256 interestRate = bondMetadata.interestRate;
        uint256 parValue = bondMetadata.parValue;
        uint256 couponAmount = (snap.balance * interestRate * parValue) / FIXED_POINT_SCALE;
        emit CouponPaid(holder, snapshotIndex, couponAmount);
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
    }

    struct Snapshot {
        uint256 date;
        uint256 balance;
    }

    address public admin;
    DebtInfo public bondMetadata;

    mapping(address => Snapshot[]) snapshots;

    uint256 constant FIXED_POINT_SCALE = 1e18;

    event CouponDeclared(address indexed holder, uint256 snapshotIndex, uint256 amount);
    event CouponPaid(address indexed holder, uint256 snapshotIndex, uint256 amount);

    constructor(address _admin, DebtInfo memory _bondMetadata) {
        admin = _admin;
        bondMetadata = _bondMetadata;
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

    function updateBondMetadata(DebtInfo memory _newMetadata) public {
        require(msg.sender == admin, "Only admin can update metadata");
        bondMetadata = _newMetadata;
    }

    function computeCoupon(address holder, uint256 snapshotIndex) public view returns (uint256) {
        Snapshot storage snap = snapshots[holder][snapshotIndex];
        uint256 interestRate = bondMetadata.interestRate;
        uint256 parValue = bondMetadata.parValue;
        uint256 couponAmount = (snap.balance * interestRate * parValue) / FIXED_POINT_SCALE;
        emit CouponDeclared(holder, snapshotIndex, couponAmount);
        return couponAmount;
    }

    function payCoupon(address holder, uint256 snapshotIndex) public {
        require(msg.sender == admin, "Only admin can pay coupons");
        Snapshot storage snap = snapshots[holder][snapshotIndex];
        uint256 interestRate = bondMetadata.interestRate;
        uint256 parValue = bondMetadata.parValue;
        uint256 couponAmount = (snap.balance * interestRate * parValue) / FIXED_POINT_SCALE;
        emit CouponPaid(holder, snapshotIndex, couponAmount);
    }
}
```

**Validation rule:** Modify the `computeCoupon` function to emit `CouponDeclared` and `CouponPaid` events.

```checker
{"id": "ch11-l3-s2", "type": "regex", "pattern": "function\\s+payCoupon\\(", "flags": "", "target": "solidity", "error_hint": "Modify the `computeCoupon` function to emit `CouponDeclared` and `CouponPaid` events."}
```

## Lesson 4 — Bank-Side Coupon Payment Batch Job

**Learning objective:** Implement a Java class to handle batch coupon payments.  
**Emphasis tags:** `[BANK]`

In core banking systems, batch jobs are often used to process large volumes of transactions efficiently. Similarly, we need to implement a Java class to handle batch coupon payments.

### Step 4.1 — Define CouponPaymentJob Class

**Instruction:** Create a `CouponPaymentJob` class in Java to handle batch coupon payments.

**Explanation:** Just like how banks use batch jobs to process large volumes of transactions efficiently, we need to implement a Java class to handle batch coupon payments. This will allow us to automate the payment process and ensure timely distribution of coupons.

**Starter code:**
```java
package com.example.coupon;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.crypto.Credentials;

public class CouponPaymentJob {
    private Web3j web3j;
    private Credentials credentials;
    private String contractAddress;

    public CouponPaymentJob(Web3j web3j, Credentials credentials, String contractAddress) {
        this.web3j = web3j;
        this.credentials = credentials;
        this.contractAddress = contractAddress;
    }

    // Add methods to handle batch coupon payments
}
```

**Solution:**
```java
package com.example.coupon;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.crypto.Credentials;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.core.methods.request.Transaction;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

public class CouponPaymentJob {
    private Web3j web3j;
    private Credentials credentials;
    private String contractAddress;

    public CouponPaymentJob(Web3j web3j, Credentials credentials, String contractAddress) {
        this.web3j = web3j;
        this.credentials = credentials;
        this.contractAddress = contractAddress;
    }

    public TransactionReceipt payCoupons(List<Address> holders, List<Uint256> snapshotIndices) throws Exception {
        Function function = new Function(
            "payCoupon",
            Arrays.asList(new Address(contractAddress), new Uint256(snapshotIndices.get(0))),
            Collections.emptyList()
        );

        String encodedFunction = FunctionEncoder.encode(function);
        Transaction transaction = Transaction.createFunctionCallTransaction(
            credentials.getAddress(),
            contractAddress,
            BigInteger.ZERO,
            DefaultGasProvider.GAS_LIMIT,
            DefaultGasProvider.GAS_PRICE,
            encodedFunction
        );

        return web3j.ethSendRawTransaction(transaction.getRawTransaction()).send().getTransactionReceipt().get();
    }
}
```

**Validation rule:** Define a `CouponPaymentJob` class with methods to handle batch coupon payments.

```checker
{"id": "ch11-l4-s1", "type": "regex", "pattern": "public\\s+class\\s+CouponPaymentJob\\s*{", "flags": "", "target": "java", "error_hint": "Define a `CouponPaymentJob` class with methods to handle batch coupon payments."}
```

### Step 4.2 — Implement Batch Coupon Payment Logic

**Instruction:** Add logic to the `payCoupons` method to process multiple coupon payments in a batch.

**Explanation:** Just like how banks use batch jobs to process large volumes of transactions efficiently, we need to implement logic in the `payCoupons` method to process multiple coupon payments in a batch. This will allow us to automate the payment process and ensure timely distribution of coupons.

**Starter code:**
```java
package com.example.coupon;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.crypto.Credentials;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.core.methods.request.Transaction;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

public class CouponPaymentJob {
    private Web3j web3j;
    private Credentials credentials;
    private String contractAddress;

    public CouponPaymentJob(Web3j web3j, Credentials credentials, String contractAddress) {
        this.web3j = web3j;
        this.credentials = credentials;
        this.contractAddress = contractAddress;
    }

    public TransactionReceipt payCoupons(List<Address> holders, List<Uint256> snapshotIndices) throws Exception {
        // Add logic to process multiple coupon payments in a batch
    }
}
```

**Solution:**
```java
package com.example.coupon;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.crypto.Credentials;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.core.methods.request.Transaction;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

public class CouponPaymentJob {
    private Web3j web3j;
    private Credentials credentials;
    private String contractAddress;

    public CouponPaymentJob(Web3j web3j, Credentials credentials, String contractAddress) {
        this.web3j = web3j;
        this.credentials = credentials;
        this.contractAddress = contractAddress;
    }

    public TransactionReceipt payCoupons(List<Address> holders, List<Uint256> snapshotIndices) throws Exception {
        for (int i = 0; i < holders.size(); i++) {
            Function function = new Function(
                "payCoupon",
                Arrays.asList(holders.get(i), snapshotIndices.get(i)),
                Collections.emptyList()
            );

            String encodedFunction = FunctionEncoder.encode(function);
            Transaction transaction = Transaction.createFunctionCallTransaction(
                credentials.getAddress(),
                contractAddress,
                BigInteger.ZERO,
                DefaultGasProvider.GAS_LIMIT,
                DefaultGasProvider.GAS_PRICE,
                encodedFunction
            );

            web3j.ethSendRawTransaction(transaction.getRawTransaction()).send().getTransactionReceipt().get();
        }
        return null;
    }
}
```

**Validation rule:** Implement logic to process multiple coupon payments in a batch.

```checker
{"id": "ch11-l4-s2", "type": "regex", "pattern": "public\\s+TransactionReceipt\\s+payCoupons\\(List<Address>\\s+holders,\\s+List<Uint256>\\s+snapshotIndices\\)\\s+throws\\s+Exception\\s*{", "flags": "m", "target": "java", "error_hint": "Implement logic to process multiple coupon payments in a batch."}
```

## Quiz

1. **What is the purpose of the `Snapshot` struct in the `BondToken` contract?**
   - A) To store transaction history
   - B) To record bond metadata
   - C) To record snapshot dates and balances
   - D) To compute coupon amounts

2. **How is fixed-point arithmetic implemented in the `BondToken` contract?**
   - A) Using floating-point numbers
   - B) Using integers with a scale factor
   - C) Using decimals
   - D) Using fractions

3. **What events are emitted when coupons are declared and paid in the `BondToken` contract?**
   - A) `CouponDeclared`, `CouponPaid`
   - B) `TransactionLogged`, `PaymentProcessed`
   - C) `CouponIssued`, `CouponSettled`
   - D) `CouponRecorded`, `CouponReleased`

4. **What is the purpose of the `CouponPaymentJob` class in Java?**
   - A) To compute coupon amounts
   - B) To handle batch coupon payments
   - C) To record snapshot dates and balances
   - D) To update bond metadata

5. **How are multiple coupon payments processed in a batch using the `CouponPaymentJob` class?**
   - A) By calling the `payCoupon` method once for each payment
   - B) By calling the `payCoupons` method with a list of holders and snapshot indices
   - C) By calling the `updateBondMetadata` method
   - D) By calling the `computeCoupon` method

**Answers:**
1. C) To record snapshot dates and balances
2. B) Using integers with a scale factor
3. A) `CouponDeclared`, `CouponPaid`
4. B) To handle batch coupon payments
5. B) By calling the `payCoupons` method with a list of holders and snapshot indices