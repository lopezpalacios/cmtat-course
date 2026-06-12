// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

/// @title HelloLedger — first on-chain ledger for core-banking engineers
/// @notice Chapter 01 teaching contract. Stores a greeting and an append-only
///         entry counter; every recorded entry emits an EntryRecorded event,
///         which is the integration surface a bank's listener consumes.
/// @dev Self-contained: no imports. Patterns modeled on plain Solidity ^0.8
///      idioms (custom errors, immutable deployer) used throughout CMTAT.
contract HelloLedger {
    // ---------------------------------------------------------------
    // State — the contract's slice of the EVM world state (master ledger)
    // ---------------------------------------------------------------

    /// @notice Account that deployed the contract. Set once in the
    ///         constructor, then baked into bytecode-adjacent storage
    ///         (immutable = read-only after construction).
    address public immutable deployer;

    /// @notice Human-readable greeting; mutable system parameter.
    string private greeting;

    /// @notice Monotonic counter of recorded entries. Doubles as the
    ///         idempotency-friendly identifier carried by each event.
    uint256 public entryCount;

    // ---------------------------------------------------------------
    // Events — the integration contract with off-chain systems
    // ---------------------------------------------------------------

    /// @notice Emitted once per recorded entry. The bank-side adapter
    ///         treats (contract address, entryId) as the idempotency key.
    event EntryRecorded(
        uint256 indexed entryId,
        address indexed recordedBy,
        string note,
        uint256 timestamp
    );

    /// @notice Emitted whenever the greeting changes — audit trail of
    ///         parameter changes, like a config-change record in a core system.
    event GreetingChanged(
        string oldGreeting,
        string newGreeting,
        address indexed changedBy
    );

    // ---------------------------------------------------------------
    // Errors — typed reason codes, cheaper than revert strings
    // ---------------------------------------------------------------

    /// @notice Rejects empty notes at the boundary (input validation).
    error HelloLedgerEmptyNote();

    /// @notice Caller is not the deployer (simple owner gate; Chapter 05
    ///         replaces this with full role-based access control).
    error HelloLedgerNotDeployer(address caller);

    // ---------------------------------------------------------------
    // Constructor — runs exactly once, at deployment
    // ---------------------------------------------------------------

    /// @param initialGreeting Greeting stored at deployment time.
    constructor(string memory initialGreeting) {
        deployer = msg.sender; // the deploying account, cryptographically authenticated
        greeting = initialGreeting;
    }

    // ---------------------------------------------------------------
    // State-changing functions — each call is a transaction
    // ---------------------------------------------------------------

    /// @notice Records an entry: bumps the counter and emits the event.
    /// @param note Free-text note; must be non-empty.
    /// @return entryId The sequential id assigned to this entry.
    function recordEntry(string calldata note) external returns (uint256 entryId) {
        if (bytes(note).length == 0) revert HelloLedgerEmptyNote();
        entryCount += 1;          // ^0.8 checked arithmetic: overflow reverts
        entryId = entryCount;
        emit EntryRecorded(entryId, msg.sender, note, block.timestamp);
    }

    /// @notice Changes the greeting. Only the deployer may call.
    /// @param newGreeting Replacement greeting.
    function setGreeting(string calldata newGreeting) external {
        if (msg.sender != deployer) revert HelloLedgerNotDeployer(msg.sender);
        string memory old = greeting;
        greeting = newGreeting;
        emit GreetingChanged(old, newGreeting, msg.sender);
    }

    // ---------------------------------------------------------------
    // Read-only functions — free local queries, no transaction needed
    // ---------------------------------------------------------------

    /// @notice Returns the current greeting.
    function getGreeting() external view returns (string memory) {
        return greeting;
    }

    /// @notice True if `account` has deployed code (contract account),
    ///         false for an externally owned account (EOA).
    /// @dev Note: returns false for a contract while its constructor runs.
    function isContractAccount(address account) external view returns (bool) {
        return account.code.length > 0;
    }
}
