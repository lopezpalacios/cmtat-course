// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

interface INavOracle {
    function getNAV() external view returns (uint256);
}

contract FundShareToken {
    address public navOracle;
    uint256 public totalShares;
    mapping(address => uint256) public shareBalances;
    uint256 public lastNAVUpdateTimestamp; // New state variable

    bool public globalSuspension = false;
    bool public customGates = false;

    uint256 public redemptionFeeRate = 100; // 1% fee
    uint256 public feeAccrued = 0;
    uint256 public liquidityThreshold = 1000000; // Minimum CHF to maintain

    struct Order {
        uint256 id;
        address user;
        uint256 amount;
        bool isRedemption;
        bool processed;
    }

    enum OrderState {
        Pending,
        Processed,
        Rejected
    }

    mapping(uint256 => Order) public orderQueues;

    event NAVUpdated(uint256 newNAV);
    event OrderAdded(uint256 indexed orderId, address user, uint256 amount, bool isRedemption);
    event OrderProcessed(uint256 indexed orderId, address user, uint256 amount, bool isRedemption);

    function getNAV() public view returns (uint256) {
        return INavOracle(navOracle).getNAV();
    }

    function updateNAV(uint256 newNAV) external {
        require(msg.sender == navOracle, "Only the NAV oracle can update NAV");
        lastNAVUpdateTimestamp = block.timestamp; // Update timestamp
        emit NAVUpdated(newNAV);
    }

    function subscribe(uint256 chfAmount) external {
        uint256 nav = getNAV();
        require(nav > 0, "NAV must be greater than zero");
        require(block.timestamp - lastNAVUpdateTimestamp < 3600, "NAV is too old"); // Staleness guard
        uint256 sharesIssued = (chfAmount * 1e6) / nav;
        shareBalances[msg.sender] += sharesIssued;
        totalShares += sharesIssued;
    }

    function addOrder(uint256 orderId, address user, uint256 amount, bool isRedemption) external {
        require(!orderQueues[orderId].processed, "Order already processed");
        orderQueues[orderId] = Order(orderId, user, amount, isRedemption, false);
        emit OrderAdded(orderId, user, amount, isRedemption);
    }

    function processOrder(uint256 orderId) external {
        require(!orderQueues[orderId].processed, "Order already processed");
        Order storage order = orderQueues[orderId];
        uint256 nav = getNAV();
        require(nav > 0, "NAV must be greater than zero");

        if (order.isRedemption) {
            require(!globalSuspension && !customGates, "Redemptions are suspended");
            uint256 sharesToRedeem = (order.amount * 1e6) / nav;
            if (shareBalances[order.user] < sharesToRedeem) {
                sharesToRedeem = shareBalances[order.user];
            }
            uint256 feeAmount = (sharesToRedeem * redemptionFeeRate) / 10000; // Calculate fee in integer math
            feeAccrued += feeAmount;
            sharesToRedeem -= feeAmount;
            require(totalShares - sharesToRedeem >= liquidityThreshold, "Liquidity threshold breached");
            shareBalances[order.user] -= sharesToRedeem;
            totalShares -= sharesToRedeem;
        } else {
            uint256 sharesIssued = (order.amount * 1e6) / nav;
            shareBalances[msg.sender] += sharesIssued;
            totalShares += sharesIssued;
        }

        order.processed = true;
        emit OrderProcessed(orderId, order.user, order.amount, order.isRedemption);
    }

    function dailyCutOff() external {
        // Reset any necessary state variables
        globalSuspension = false;
        customGates = false;
        feeAccrued = 0;

        // Perform reconciliation logic here
        // For example, distribute accrued fees to the fund manager or other stakeholders
    }
}
