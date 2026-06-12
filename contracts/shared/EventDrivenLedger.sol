// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

/// @title EventDrivenLedger — booking entries as an event feed (Course Chapter 03)
/// @notice The event log IS the ledger feed. Contract storage keeps only what the
///         EVM itself must enforce: access control + idempotency dedup. Everything
///         the bank needs for reconciliation travels in the BookingRecorded event.
///
/// Event-feed design rules (the integration contract with core banking):
///  1. Every event carries an idempotency key: `externalRef` (bytes32, bank-assigned,
///     e.g. right-padded ASCII of the core-banking booking reference).
///  2. Amounts are uint256 in smallest units (e.g. rappen / token base units);
///     never floating point, never formatted strings.
///  3. `indexed` fields = what the bank queries by (topics); non-indexed = payload (data).
contract EventDrivenLedger {
    // ------------------------------------------------------------------
    // State — deliberately minimal: full booking data lives in event logs,
    // which are ~10x cheaper than contract storage and replayable forever.
    // ------------------------------------------------------------------
    address public admin;                        // bootstrap principal (cf. system account)
    uint256 public bookingCount;                 // running counter, cheap integrity check
    mapping(address => bool) public isOperator;  // modeled on OZ AccessControl (single-role simplification; full RBAC in Chapter 05)
    mapping(bytes32 => bool) public processed;   // idempotency dedup: externalRef -> seen

    // ------------------------------------------------------------------
    // Entry-type codes — uint8 codes (not an enum) for a stable, explicit ABI
    // that off-chain decoders can hard-code against.
    // ------------------------------------------------------------------
    uint8 public constant ENTRY_DEBIT = 1;
    uint8 public constant ENTRY_CREDIT = 2;

    // ------------------------------------------------------------------
    // Custom errors — cheaper than require strings; decoded off-chain via ABI.
    // ------------------------------------------------------------------
    error LedgerNotAdmin(address caller);
    error LedgerNotOperator(address caller);
    error LedgerZeroRef();
    error LedgerZeroAccount();
    error LedgerZeroAmount();
    error LedgerInvalidEntryType(uint8 entryType);
    error LedgerDuplicateRef(bytes32 externalRef);

    // ------------------------------------------------------------------
    // Events — the integration contract with the bank.
    // topic0 = keccak256("BookingRecorded(bytes32,address,uint256,uint8)")
    // ------------------------------------------------------------------
    event BookingRecorded(
        bytes32 indexed externalRef, // idempotency key, queryable topic
        address indexed account,     // position account, queryable topic
        uint256 amount,              // smallest units, in data section
        uint8 entryType              // ENTRY_DEBIT / ENTRY_CREDIT, in data section
    );
    event OperatorGranted(address indexed account, address indexed grantedBy);
    event OperatorRevoked(address indexed account, address indexed revokedBy);

    // ------------------------------------------------------------------
    // Modifiers — policy gates, cf. Spring Security @PreAuthorize.
    // ------------------------------------------------------------------
    modifier onlyAdmin() {
        if (msg.sender != admin) revert LedgerNotAdmin(msg.sender);
        _; // splice point: the guarded function body runs here
    }

    modifier onlyOperator() {
        if (!isOperator[msg.sender]) revert LedgerNotOperator(msg.sender);
        _;
    }

    constructor() {
        admin = msg.sender;
        isOperator[msg.sender] = true;
        // genesis audit record: even the bootstrap grant is on the feed
        emit OperatorGranted(msg.sender, msg.sender);
    }

    // ------------------------------------------------------------------
    // Role administration (audited via events)
    // ------------------------------------------------------------------
    function grantOperator(address account) external onlyAdmin {
        if (account == address(0)) revert LedgerZeroAccount();
        isOperator[account] = true;
        emit OperatorGranted(account, msg.sender);
    }

    function revokeOperator(address account) external onlyAdmin {
        isOperator[account] = false;
        emit OperatorRevoked(account, msg.sender);
    }

    // ------------------------------------------------------------------
    // Core write path: validate -> dedup -> effects -> emit
    // ------------------------------------------------------------------
    function recordBooking(
        bytes32 externalRef,
        address account,
        uint256 amount,
        uint8 entryType
    ) external onlyOperator {
        // checks (validate at the boundary, like a payment-message parser)
        if (externalRef == bytes32(0)) revert LedgerZeroRef();
        if (account == address(0)) revert LedgerZeroAccount();
        if (amount == 0) revert LedgerZeroAmount();
        if (!_isValidEntryType(entryType)) revert LedgerInvalidEntryType(entryType);
        if (processed[externalRef]) revert LedgerDuplicateRef(externalRef);

        // effects (state first — checks-effects-interactions discipline)
        processed[externalRef] = true;
        unchecked {
            bookingCount += 1; // cannot overflow before the heat death of the universe
        }

        // event last: the booking hits the bank's feed exactly once
        emit BookingRecorded(externalRef, account, amount, entryType);
    }

    // ------------------------------------------------------------------
    // Free read API (eth_call — the bank's no-cost query endpoint)
    // ------------------------------------------------------------------
    function ledgerStatus() external view returns (uint256 count, address currentAdmin) {
        return (bookingCount, admin);
    }

    /// @notice Convert major units to smallest units, e.g. CHF -> rappen with tokenDecimals = 2.
    /// @dev pure: touches neither storage nor environment; reverts on overflow (0.8 checked math).
    function toMinorUnits(uint256 major, uint8 tokenDecimals) public pure returns (uint256) {
        return major * (10 ** uint256(tokenDecimals));
    }

    /// @dev internal: shared validation, invisible in the ABI (cf. a private Java method).
    function _isValidEntryType(uint8 entryType) internal pure returns (bool) {
        return entryType == ENTRY_DEBIT || entryType == ENTRY_CREDIT;
    }
}
