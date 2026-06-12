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
