// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

/*//////////////////////////////////////////////////////////////////////////
    BankERC20 — a minimal ERC-20 built as a securities register.
    Course Chapter 04. Self-contained: zero imports.
    // modeled on OpenZeppelin ERC20 and CMTAT ERC20BaseModule
//////////////////////////////////////////////////////////////////////////*/

/// @title IBankERC20 — the ERC-20 standard interface (EIP-20)
/// @notice The on-chain equivalent of an ISO 20022 message standard:
///         every wallet, exchange, and custodian integrates against
///         exactly these function selectors and event topics.
interface IBankERC20 {
    /// @notice Emitted on every balance movement — the booking entry.
    ///         Mint = Transfer(address(0), to, value).
    ///         Burn = Transfer(from, address(0), value).
    event Transfer(address indexed from, address indexed to, uint256 value);

    /// @notice Emitted when an owner sets a spending mandate for a spender.
    event Approval(address indexed owner, address indexed spender, uint256 value);

    // --- read-only register queries (like account-balance inquiries) ---
    function name() external view returns (string memory);
    function symbol() external view returns (string memory);
    function decimals() external view returns (uint8);
    function totalSupply() external view returns (uint256);
    function balanceOf(address account) external view returns (uint256);
    function allowance(address owner, address spender) external view returns (uint256);

    // --- state-changing booking instructions ---
    function transfer(address to, uint256 value) external returns (bool);
    function approve(address spender, uint256 value) external returns (bool);
    function transferFrom(address from, address to, uint256 value) external returns (bool);
}

/// @title BankERC20 — minimal ERC-20 share ledger
/// @notice The balances mapping IS the securities register. The contract
///         enforces the core invariant: sum(_balances) == _totalSupply.
contract BankERC20 is IBankERC20 {
    // ------------------------------------------------------------------
    // Register storage
    // ------------------------------------------------------------------

    /// @dev The register: holder address => position (in base units).
    ///      Equivalent of the position-keeping table in a core-banking DB,
    ///      keyed by account number instead of address.
    mapping(address => uint256) private _balances;

    /// @dev Issued capital in base units. Invariant: equals the sum of
    ///      all entries in _balances at all times.
    uint256 private _totalSupply;

    /// @dev Delegated-authority register:
    ///      owner => (spender => remaining mandate in base units).
    ///      The on-chain power of attorney / standing-order table.
    mapping(address => mapping(address => uint256)) private _allowances;

    // ------------------------------------------------------------------
    // Instrument metadata
    // ------------------------------------------------------------------

    /// @dev Human-readable instrument name, e.g. "Helvetia AG Registered Shares".
    string private _name;

    /// @dev Ticker-style short code, e.g. "HELV".
    string private _symbol;

    /// @dev Number of base units per whole token. 0 for indivisible
    ///      registered shares; 18 for cash-like or fractionalized legs.
    ///      Immutable: fixed at issuance, cheap to read (no SLOAD).
    uint8 private immutable _decimals;

    /// @dev Single privileged issuance/cancellation address.
    ///      Stand-in for the transfer agent; replaced by full role-based
    ///      access control (CMTAT AuthorizationModule pattern) in Chapter 05.
    address public immutable registrar;

    // ------------------------------------------------------------------
    // Issuance/cancellation events (bank-side audit + idempotency)
    // ------------------------------------------------------------------

    /// @notice Issuance booking. operationId = idempotency key assigned by
    ///         the bank's issuance system (e.g. hash of the corporate-action
    ///         reference), so the reconciliation job can match 1:1.
    event Issued(address indexed to, uint256 value, bytes32 indexed operationId);

    /// @notice Cancellation booking (capital reduction, redemption, buyback).
    event Cancelled(address indexed from, uint256 value, bytes32 indexed operationId);

    // ------------------------------------------------------------------
    // Custom errors (cheaper than revert strings; ABI-decodable off-chain)
    // ------------------------------------------------------------------

    error BankERC20InvalidSender(address sender);
    error BankERC20InvalidReceiver(address receiver);
    error BankERC20InsufficientBalance(address sender, uint256 balance, uint256 needed);
    error BankERC20InvalidApprover(address approver);
    error BankERC20InvalidSpender(address spender);
    error BankERC20InsufficientAllowance(address spender, uint256 currentAllowance, uint256 needed);
    error BankERC20UnauthorizedRegistrar(address caller);

    // ------------------------------------------------------------------
    // Modifiers
    // ------------------------------------------------------------------

    /// @dev Policy gate: only the registrar may issue or cancel.
    modifier onlyRegistrar() {
        if (msg.sender != registrar) {
            revert BankERC20UnauthorizedRegistrar(msg.sender);
        }
        _;
    }

    // ------------------------------------------------------------------
    // Construction
    // ------------------------------------------------------------------

    /// @param name_     instrument name
    /// @param symbol_   short code
    /// @param decimals_ base-unit precision (0 for whole-share registers)
    constructor(string memory name_, string memory symbol_, uint8 decimals_) {
        _name = name_;
        _symbol = symbol_;
        _decimals = decimals_;
        registrar = msg.sender; // deployer acts as transfer agent until Ch. 05
    }

    // ------------------------------------------------------------------
    // Read-only register queries
    // ------------------------------------------------------------------

    function name() public view returns (string memory) {
        return _name;
    }

    function symbol() public view returns (string memory) {
        return _symbol;
    }

    function decimals() public view returns (uint8) {
        return _decimals;
    }

    function totalSupply() public view returns (uint256) {
        return _totalSupply;
    }

    function balanceOf(address account) public view returns (uint256) {
        return _balances[account];
    }

    function allowance(address owner, address spender) public view returns (uint256) {
        return _allowances[owner][spender];
    }

    // ------------------------------------------------------------------
    // Booking instructions
    // ------------------------------------------------------------------

    /// @notice Move `value` base units from the caller to `to`.
    /// @return success — always true; failures revert (atomic booking).
    function transfer(address to, uint256 value) public returns (bool) {
        _transfer(msg.sender, to, value);
        return true;
    }

    /// @notice Grant `spender` a mandate over `value` base units of the
    ///         caller's position. Overwrites any previous mandate.
    function approve(address spender, uint256 value) public returns (bool) {
        _approve(msg.sender, spender, value);
        return true;
    }

    /// @notice Spender executes a transfer on behalf of `from`, consuming
    ///         the mandate. The on-chain power-of-attorney execution.
    function transferFrom(address from, address to, uint256 value) public returns (bool) {
        _spendAllowance(from, msg.sender, value);
        _transfer(from, to, value);
        return true;
    }

    // ------------------------------------------------------------------
    // Issuance / cancellation (registrar only — full RBAC in Chapter 05)
    // ------------------------------------------------------------------

    /// @notice Issue `value` base units to `to`. Primary-market booking.
    /// @param operationId bank-assigned idempotency key for reconciliation.
    function mint(address to, uint256 value, bytes32 operationId) external onlyRegistrar {
        _mint(to, value);
        emit Issued(to, value, operationId);
    }

    /// @notice Cancel `value` base units held by `from` (capital reduction).
    /// @param operationId bank-assigned idempotency key for reconciliation.
    function burn(address from, uint256 value, bytes32 operationId) external onlyRegistrar {
        _burn(from, value);
        emit Cancelled(from, value, operationId);
    }

    // ------------------------------------------------------------------
    // Internal booking engine
    // ------------------------------------------------------------------

    /// @dev Holder-to-holder transfer with zero-address guards.
    function _transfer(address from, address to, uint256 value) internal {
        if (from == address(0)) revert BankERC20InvalidSender(address(0));
        if (to == address(0)) revert BankERC20InvalidReceiver(address(0));
        _update(from, to, value);
    }

    /// @dev Single booking engine for transfer, mint, and burn.
    ///      from == address(0): issuance (supply grows).
    ///      to   == address(0): cancellation (supply shrinks).
    ///      Preserves the invariant sum(_balances) == _totalSupply because
    ///      every debit has a matching credit (double-entry on-chain).
    function _update(address from, address to, uint256 value) internal {
        if (from == address(0)) {
            // Issuance: credit total supply. Checked add — reverts on
            // overflow, which caps supply at type(uint256).max.
            _totalSupply += value;
        } else {
            uint256 fromBalance = _balances[from];
            if (fromBalance < value) {
                revert BankERC20InsufficientBalance(from, fromBalance, value);
            }
            unchecked {
                // Safe: fromBalance >= value just checked.
                _balances[from] = fromBalance - value;
            }
        }

        if (to == address(0)) {
            unchecked {
                // Safe: value was already deducted from a real balance,
                // and sum(balances) <= _totalSupply at all times.
                _totalSupply -= value;
            }
        } else {
            unchecked {
                // Safe: sum of all balances can never exceed _totalSupply,
                // whose checked add above bounds the total.
                _balances[to] += value;
            }
        }

        // THE booking entry — the integration contract with the bank.
        emit Transfer(from, to, value);
    }

    /// @dev Issuance primitive: Transfer event from the zero address.
    function _mint(address to, uint256 value) internal {
        if (to == address(0)) revert BankERC20InvalidReceiver(address(0));
        _update(address(0), to, value);
    }

    /// @dev Cancellation primitive: Transfer event to the zero address.
    function _burn(address from, uint256 value) internal {
        if (from == address(0)) revert BankERC20InvalidSender(address(0));
        _update(from, address(0), value);
    }

    /// @dev Records the mandate and emits Approval for the audit trail.
    function _approve(address owner, address spender, uint256 value) internal {
        if (owner == address(0)) revert BankERC20InvalidApprover(address(0));
        if (spender == address(0)) revert BankERC20InvalidSpender(address(0));
        _allowances[owner][spender] = value;
        emit Approval(owner, spender, value);
    }

    /// @dev Consumes the mandate. type(uint256).max = unlimited standing
    ///      order, never decremented (saves one SSTORE per transferFrom).
    function _spendAllowance(address owner, address spender, uint256 value) internal {
        uint256 currentAllowance = _allowances[owner][spender];
        if (currentAllowance != type(uint256).max) {
            if (currentAllowance < value) {
                revert BankERC20InsufficientAllowance(spender, currentAllowance, value);
            }
            unchecked {
                _allowances[owner][spender] = currentAllowance - value;
            }
        }
    }
}
