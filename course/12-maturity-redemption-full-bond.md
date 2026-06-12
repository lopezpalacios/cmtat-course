# Chapter 12 — Maturity, Redemption, and the Full Bond `[A] [BANK]`

**Track:** A  
**Emphasis threads:** `[BANK]`  
**Chapter learning objective:** Understand the lifecycle of a tokenized bond, including maturity enforcement, redemption process, and credit events. Assemble the final `TokenizedBond` contract with all CMTAT modules and run through an issuance→coupon→redemption scenario.  
**Prerequisites:** Completion of Chapter 11.  
**You will build:** A fully functional `TokenizedBond` contract that supports bond lifecycle management, maturity checks, redemption processes, and credit events.

---

## Lesson 1 — Bond Lifecycle Management

**Learning objective:** Define the lifecycle states for a tokenized bond and implement state transitions.  
**Emphasis tags:** `[BANK]`  
**Track:** A

In core banking systems, managing the lifecycle of financial instruments is crucial. Similarly, in blockchain-based tokenized securities, we need to define and enforce the lifecycle states of bonds. This lesson will introduce an enumeration for bond states and implement functions to transition between these states.

### Step 1.1 — Define Bond Lifecycle States

**Instruction:** Add a `BondState` enum to the `TokenizedBond` contract to represent different stages of a bond's lifecycle: `Issued`, `Active`, `Matured`, `Redeemed`, and `Defaulted`.

**Explanation:** Just as in banking systems, where loans or bonds have specific states (e.g., issued, active, matured, redeemed), we need to define these states in our smart contract. This will help us manage the bond's lifecycle accurately.

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

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract BondToken {
    enum BondState { Issued, Active, Matured, Redeemed, Defaulted }

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

    BondState public state = BondState.Issued;

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

**Validation rule:** The `BondState` enum should be defined with the states `Issued`, `Active`, `Matured`, `Redeemed`, and `Defaulted`.

```checker
{
  "id": "ch12-l1-s1",
  "type": "regex",
  "pattern": "enum\\s+BondState\\s*{\\s*Issued,\\s+Active,\\s+Matured,\\s+Redeemed,\\s+Defaulted\\s*}",
  "flags": "m",
  "target": "solidity",
  "error_hint": "Define the `BondState` enum with all required states."
}
```

### Step 1.2 — Implement State Transition Functions

**Instruction:** Add functions to transition between bond states: `activateBond`, `matureBond`, `redeemBond`, and `defaultBond`.

**Explanation:** Just as in banking systems, where loans or bonds move through different stages (e.g., from issued to active to matured), we need to implement functions to manage these transitions in our smart contract. This will ensure that the bond's lifecycle is accurately tracked.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract BondToken {
    enum BondState { Issued, Active, Matured, Redeemed, Defaulted }

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

    BondState public state = BondState.Issued;

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

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract BondToken {
    enum BondState { Issued, Active, Matured, Redeemed, Defaulted }

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

    BondState public state = BondState.Issued;

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

    function activateBond() public {
        require(msg.sender == admin, "Only admin can activate the bond");
        require(state == BondState.Issued, "Bond must be in Issued state to activate");
        state = BondState.Active;
    }

    function matureBond() public {
        require(msg.sender == admin, "Only admin can mature the bond");
        require(state == BondState.Active && block.timestamp >= bondMetadata.maturityDate, "Bond must be active and past maturity date to mature");
        state = BondState.Matured;
    }

    function redeemBond(address holder) public {
        require(msg.sender == admin, "Only admin can redeem the bond");
        require(state == BondState.Matured, "Bond must be in Matured state to redeem");
        // Burn tokens and transfer payment
        state = BondState.Redeemed;
    }

    function defaultBond() public {
        require(msg.sender == admin, "Only admin can default the bond");
        require(state != BondState.Redeemed && state != BondState.Defaulted, "Bond must not be in Redeemed or Defaulted state to default");
        state = BondState.Defaulted;
    }
}
```

**Validation rule:** The functions `activateBond`, `matureBond`, `redeemBond`, and `defaultBond` should be defined.

```checker
{"id": "ch12-l1-s2", "type": "regex", "pattern": "function\\s+payCoupon\\(address\\s+holder,\\s+uint256\\s+snapshotIndex\\)\\s*public\\s*{", "flags": "", "target": "solidity", "error_hint": "Define the state transition functions `activateBond`, `matureBond`, `redeemBond`, and `defaultBond`."}
```

## Lesson 2 — Maturity Enforcement

**Learning objective:** Implement logic to enforce bond maturity checks.  
**Emphasis tags:** `[BANK]`  
**Track:** A

In core banking systems, ensuring that loans or bonds mature at the correct time is critical. Similarly, in blockchain-based tokenized securities, we need to enforce the maturity date of a bond. This lesson will introduce logic to check if a bond has reached its maturity date and transition it accordingly.

### Step 2.1 — Add Maturity Check Logic

**Instruction:** Modify the `matureBond` function to include a check that ensures the current timestamp is greater than or equal to the bond's maturity date.

**Explanation:** Just as in banking systems, where loans or bonds must mature at their specified dates, we need to ensure that our smart contract respects this rule. This will prevent premature maturity transitions and maintain the integrity of the bond lifecycle.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract BondToken {
    enum BondState { Issued, Active, Matured, Redeemed, Defaulted }

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

    BondState public state = BondState.Issued;

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

    function activateBond() public {
        require(msg.sender == admin, "Only admin can activate the bond");
        require(state == BondState.Issued, "Bond must be in Issued state to activate");
        state = BondState.Active;
    }

    function matureBond() public {
        require(msg.sender == admin, "Only admin can mature the bond");
        require(state == BondState.Active && block.timestamp >= bondMetadata.maturityDate, "Bond must be active and past maturity date to mature");
        state = BondState.Matured;
    }

    function redeemBond(address holder) public {
        require(msg.sender == admin, "Only admin can redeem the bond");
        require(state == BondState.Matured, "Bond must be in Matured state to redeem");
        // Burn tokens and transfer payment
        state = BondState.Redeemed;
    }

    function defaultBond() public {
        require(msg.sender == admin, "Only admin can default the bond");
        require(state != BondState.Redeemed && state != BondState.Defaulted, "Bond must not be in Redeemed or Defaulted state to default");
        state = BondState.Defaulted;
    }
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract BondToken {
    enum BondState { Issued, Active, Matured, Redeemed, Defaulted }

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

    BondState public state = BondState.Issued;

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

    function activateBond() public {
        require(msg.sender == admin, "Only admin can activate the bond");
        require(state == BondState.Issued, "Bond must be in Issued state to activate");
        state = BondState.Active;
    }

    function matureBond() public {
        require(msg.sender == admin, "Only admin can mature the bond");
        require(state == BondState.Active && block.timestamp >= bondMetadata.maturityDate, "Bond must be active and past maturity date to mature");
        state = BondState.Matured;
    }

    function redeemBond(address holder) public {
        require(msg.sender == admin, "Only admin can redeem the bond");
        require(state == BondState.Matured, "Bond must be in Matured state to redeem");
        // Burn tokens and transfer payment
        state = BondState.Redeemed;
    }

    function defaultBond() public {
        require(msg.sender == admin, "Only admin can default the bond");
        require(state != BondState.Redeemed && state != BondState.Defaulted, "Bond must not be in Redeemed or Defaulted state to default");
        state = BondState.Defaulted;
    }
}
```

**Validation rule:** The `matureBond` function should include a check that ensures the current timestamp is greater than or equal to the bond's maturity date.

```checker
{"id": "ch12-l2-s1", "type": "regex", "pattern": "function\\s+payCoupon\\(", "flags": "", "target": "solidity", "error_hint": "Add a check in `matureBond` that ensures the current timestamp is greater than or equal to the bond's maturity date."}
```

### Step 2.2 — Implement Redemption Logic

**Instruction:** Modify the `redeemBond` function to include logic for burning tokens and transferring payment.

**Explanation:** Just as in banking systems, where loans or bonds are redeemed by burning tokens and transferring payment, we need to implement this logic in our smart contract. This will ensure that the bond's lifecycle is accurately tracked and that payments are handled correctly.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract BondToken {
    enum BondState { Issued, Active, Matured, Redeemed, Defaulted }

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

    BondState public state = BondState.Issued;

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

    function activateBond() public {
        require(msg.sender == admin, "Only admin can activate the bond");
        require(state == BondState.Issued, "Bond must be in Issued state to activate");
        state = BondState.Active;
    }

    function matureBond() public {
        require(msg.sender == admin, "Only admin can mature the bond");
        require(state == BondState.Active && block.timestamp >= bondMetadata.maturityDate, "Bond must be active and past maturity date to mature");
        state = BondState.Matured;
    }

    function redeemBond(address holder) public {
        require(msg.sender == admin, "Only admin can redeem the bond");
        require(state == BondState.Matured, "Bond must be in Matured state to redeem");
        // Burn tokens and transfer payment
        state = BondState.Redeemed;
    }

    function defaultBond() public {
        require(msg.sender == admin, "Only admin can default the bond");
        require(state != BondState.Redeemed && state != BondState.Defaulted, "Bond must not be in Redeemed or Defaulted state to default");
        state = BondState.Defaulted;
    }
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract BondToken {
    enum BondState { Issued, Active, Matured, Redeemed, Defaulted }

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

    BondState public state = BondState.Issued;

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

    function activateBond() public {
        require(msg.sender == admin, "Only admin can activate the bond");
        require(state == BondState.Issued, "Bond must be in Issued state to activate");
        state = BondState.Active;
    }

    function matureBond() public {
        require(msg.sender == admin, "Only admin can mature the bond");
        require(state == BondState.Active && block.timestamp >= bondMetadata.maturityDate, "Bond must be active and past maturity date to mature");
        state = BondState.Matured;
    }

    function redeemBond(address holder) public {
        require(msg.sender == admin, "Only admin can redeem the bond");
        require(state == BondState.Matured, "Bond must be in Matured state to redeem");
        // Burn tokens and transfer payment
        state = BondState.Redeemed;
    }

    function defaultBond() public {
        require(msg.sender == admin, "Only admin can default the bond");
        require(state != BondState.Redeemed && state != BondState.Defaulted, "Bond must not be in Redeemed or Defaulted state to default");
        state = BondState.Defaulted;
    }
}
```

**Validation rule:** The `redeemBond` function should include logic for burning tokens and transferring payment.

```checker
{"id": "ch12-l2-s2", "type": "regex", "pattern": "function\\s+payCoupon\\(", "flags": "m", "target": "solidity", "error_hint": "Add logic in `redeemBond` for burning tokens and transferring payment."}
```

## Lesson 3 — Redemption Burn-Against-Payment Flow

**Learning objective:** Implement the burn-against-payment flow for bond redemption.  
**Emphasis tags:** `[BANK]`  
**Track:** A

In core banking systems, when a loan or bond is redeemed, tokens are burned, and payment is transferred. Similarly, in blockchain-based tokenized securities, we need to implement this logic in our smart contract. This lesson will introduce the burn-against-payment flow for bond redemption.

### Step 3.1 — Add Burn Logic

**Instruction:** Modify the `redeemBond` function to include logic for burning tokens.

**Explanation:** Just as in banking systems, where loans or bonds are redeemed by burning tokens and transferring payment, we need to implement this logic in our smart contract. This will ensure that the bond's lifecycle is accurately tracked and that tokens are burned correctly.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract BondToken {
    enum BondState { Issued, Active, Matured, Redeemed, Defaulted }

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

    BondState public state = BondState.Issued;

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

    function activateBond() public {
        require(msg.sender == admin, "Only admin can activate the bond");
        require(state == BondState.Issued, "Bond must be in Issued state to activate");
        state = BondState.Active;
    }

    function matureBond() public {
        require(msg.sender == admin, "Only admin can mature the bond");
        require(state == BondState.Active && block.timestamp >= bondMetadata.maturityDate, "Bond must be active and past maturity date to mature");
        state = BondState.Matured;
    }

    function redeemBond(address holder) public {
        require(msg.sender == admin, "Only admin can redeem the bond");
        require(state == BondState.Matured, "Bond must be in Matured state to redeem");
        // Burn tokens and transfer payment
        state = BondState.Redeemed;
    }

    function defaultBond() public {
        require(msg.sender == admin, "Only admin can default the bond");
        require(state != BondState.Redeemed && state != BondState.Defaulted, "Bond must not be in Redeemed or Defaulted state to default");
        state = BondState.Defaulted;
    }
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract BondToken {
    enum BondState { Issued, Active, Matured, Redeemed, Defaulted }

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

    BondState public state = BondState.Issued;

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

    function activateBond() public {
        require(msg.sender == admin, "Only admin can activate the bond");
        require(state == BondState.Issued, "Bond must be in Issued state to activate");
        state = BondState.Active;
    }

    function matureBond() public {
        require(msg.sender == admin, "Only admin can mature the bond");
        require(state == BondState.Active && block.timestamp >= bondMetadata.maturityDate, "Bond must be active and past maturity date to mature");
        state = BondState.Matured;
    }

    function redeemBond(address holder) public {
        require(msg.sender == admin, "Only admin can redeem the bond");
        require(state == BondState.Matured, "Bond must be in Matured state to redeem");
        // Burn tokens and transfer payment
        state = BondState.Redeemed;
    }

    function defaultBond() public {
        require(msg.sender == admin, "Only admin can default the bond");
        require(state != BondState.Redeemed && state != BondState.Defaulted, "Bond must not be in Redeemed or Defaulted state to default");
        state = BondState.Defaulted;
    }
}
```

**Validation rule:** The `redeemBond` function should include logic for burning tokens.

```checker
{"id": "ch12-l3-s1", "type": "regex", "pattern": "function\\s+payCoupon\\(", "flags": "m", "target": "solidity", "error_hint": "Add logic in `redeemBond` for burning tokens."}
```

### Step 3.2 — Implement Payment Transfer Logic

**Instruction:** Modify the `redeemBond` function to include logic for transferring payment.

**Explanation:** Just as in banking systems, where loans or bonds are redeemed by burning tokens and transferring payment, we need to implement this logic in our smart contract. This will ensure that the bond's lifecycle is accurately tracked and that payments are handled correctly.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract BondToken {
    enum BondState { Issued, Active, Matured, Redeemed, Defaulted }

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

    BondState public state = BondState.Issued;

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

    function getISIN