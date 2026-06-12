// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

/*//////////////////////////////////////////////////////////////////////////
  Chapter 07 — Compliance Modules: Pause, Freeze, Transfer Rules
  ComplianceToken = ERC-20 share ledger
                  + role-based access control   (modeled on OZ AccessControl)
                  + PauseModule                 (modeled on CMTAT PauseModule / OZ Pausable)
                  + EnforcementModule           (modeled on CMTAT EnforcementModule)
                  + ValidationModule            (modeled on CMTAT ValidationModule + IRuleEngine)
  Self-contained: zero imports. All referenced patterns re-implemented inline.
//////////////////////////////////////////////////////////////////////////*/

/// @title IComplianceRuleEngine
/// @notice Pre-trade compliance plug-in interface.
///         Modeled on CMTAT IRuleEngine, flavored with ERC-1404 restriction
///         codes (uint8 code, 0 = no restriction, human message per code).
/// @dev    Course convention: codes 1..9 are reserved for the token itself
///         (pause/freeze); rule engines return 0 or codes >= 10.
interface IComplianceRuleEngine {
    /// @return true when the transfer passes every rule
    function validateTransfer(address from, address to, uint256 amount)
        external
        view
        returns (bool);

    /// @return restrictionCode 0 = OK, >= 10 = engine-specific restriction
    function detectTransferRestriction(address from, address to, uint256 amount)
        external
        view
        returns (uint8 restrictionCode);

    /// @return human-readable message for a restriction code
    function messageForTransferRestriction(uint8 restrictionCode)
        external
        view
        returns (string memory);
}

/// @title ComplianceToken
/// @notice ERC-20 securities ledger with the CMTAT compliance surface:
///         market-wide pause, address-level freeze, pluggable rule engine.
contract ComplianceToken {
    /*////////////////////////////////////////////////////////////////////
                  ROLES — modeled on OZ AccessControl (Chapter 05)
    ////////////////////////////////////////////////////////////////////*/

    bytes32 public constant DEFAULT_ADMIN_ROLE = 0x00; // issuer / board mandate
    bytes32 public constant PAUSER_ROLE = keccak256("PAUSER_ROLE"); // compliance duty desk
    bytes32 public constant ENFORCER_ROLE = keccak256("ENFORCER_ROLE"); // sanctions / legal ops
    bytes32 public constant MINTER_ROLE = keccak256("MINTER_ROLE"); // registrar / issuance desk
    bytes32 public constant BURNER_ROLE = keccak256("BURNER_ROLE"); // registrar / redemption desk

    mapping(bytes32 => mapping(address => bool)) private _roles;

    event RoleGranted(bytes32 indexed role, address indexed account, address indexed sender);
    event RoleRevoked(bytes32 indexed role, address indexed account, address indexed sender);

    error ComplianceUnauthorized(address account, bytes32 neededRole);

    modifier onlyRole(bytes32 role) {
        if (!_roles[role][msg.sender]) {
            revert ComplianceUnauthorized(msg.sender, role);
        }
        _;
    }

    function hasRole(bytes32 role, address account) public view returns (bool) {
        return _roles[role][account];
    }

    function grantRole(bytes32 role, address account) external onlyRole(DEFAULT_ADMIN_ROLE) {
        if (!_roles[role][account]) {
            _roles[role][account] = true;
            emit RoleGranted(role, account, msg.sender);
        }
    }

    function revokeRole(bytes32 role, address account) external onlyRole(DEFAULT_ADMIN_ROLE) {
        if (_roles[role][account]) {
            _roles[role][account] = false;
            emit RoleRevoked(role, account, msg.sender);
        }
    }

    /*////////////////////////////////////////////////////////////////////
            ERC-20 CORE — modeled on CMTAT ERC20BaseModule (Chapter 04)
    ////////////////////////////////////////////////////////////////////*/

    string public name;
    string public symbol;
    uint8 public immutable decimals;
    uint256 public totalSupply;

    mapping(address => uint256) private _balances;
    mapping(address => mapping(address => uint256)) private _allowances;

    event Transfer(address indexed from, address indexed to, uint256 value);
    event Approval(address indexed owner, address indexed spender, uint256 value);

    error ComplianceInvalidAddress(address account);
    error ComplianceInsufficientBalance(address account, uint256 balance, uint256 needed);
    error ComplianceInsufficientAllowance(address spender, uint256 allowance_, uint256 needed);

    /*////////////////////////////////////////////////////////////////////
        PAUSE — modeled on CMTAT PauseModule (which wraps OZ Pausable)
    ////////////////////////////////////////////////////////////////////*/

    bool private _paused;

    event Paused(address account); // account = the pauser who triggered the halt
    event Unpaused(address account);

    error ComplianceEnforcedPause(); // pause() while already paused
    error ComplianceExpectedPause(); // unpause() while not paused

    function paused() public view returns (bool) {
        return _paused;
    }

    /// @notice Market-wide halt: blocks every transfer, mint and burn.
    function pause() external onlyRole(PAUSER_ROLE) {
        if (_paused) revert ComplianceEnforcedPause();
        _paused = true;
        emit Paused(msg.sender);
    }

    /// @notice Lift the halt after the incident / regulatory action ends.
    function unpause() external onlyRole(PAUSER_ROLE) {
        if (!_paused) revert ComplianceExpectedPause();
        _paused = false;
        emit Unpaused(msg.sender);
    }

    /*////////////////////////////////////////////////////////////////////
        FREEZE — modeled on CMTAT EnforcementModule (address-level)
        Assumption: CMTAT v2 emits Freeze(enforcer, owner, string reason);
        this course uses a bytes32 reasonCode (cheaper, fixed-width,
        machine-parseable by the bank's monitoring) and notes the delta.
    ////////////////////////////////////////////////////////////////////*/

    mapping(address => bool) private _frozen;

    event Freeze(address indexed enforcer, address indexed account, bytes32 reasonCode);
    event Unfreeze(address indexed enforcer, address indexed account, bytes32 reasonCode);
    // Forced transfer for legal seizure — modeled on CMTAT Enforcement event
    event Enforcement(
        address indexed enforcer,
        address indexed from,
        address indexed to,
        uint256 amount,
        bytes32 reasonCode
    );

    function frozen(address account) public view returns (bool) {
        return _frozen[account];
    }

    /// @notice Freeze a single address (sanctions hit, court order, fraud hold).
    /// @param reasonCode right-padded ASCII, e.g. bytes32("SECO-SANCTION") or
    ///        a case reference like bytes32("COURT-ZH-2026-117").
    function freeze(address account, bytes32 reasonCode) external onlyRole(ENFORCER_ROLE) {
        if (account == address(0)) revert ComplianceInvalidAddress(account);
        _frozen[account] = true;
        emit Freeze(msg.sender, account, reasonCode);
    }

    /// @notice Lift an address-level freeze (order rescinded, case closed).
    function unfreeze(address account, bytes32 reasonCode) external onlyRole(ENFORCER_ROLE) {
        _frozen[account] = false;
        emit Unfreeze(msg.sender, account, reasonCode);
    }

    /// @notice Enforcer-initiated transfer for legal seizure / squeeze-out.
    /// @dev    Deliberately BYPASSES pause, freeze and rule-engine checks:
    ///         a court-ordered seizure must execute even while the source
    ///         address is frozen. Assumption: mirrors CMTAT v3
    ///         ERC20EnforcementModule.forcedTransfer semantics.
    function forcedTransfer(address from, address to, uint256 amount, bytes32 reasonCode)
        external
        onlyRole(ENFORCER_ROLE)
    {
        if (to == address(0)) revert ComplianceInvalidAddress(to);
        uint256 fromBalance = _balances[from];
        if (fromBalance < amount) {
            revert ComplianceInsufficientBalance(from, fromBalance, amount);
        }
        unchecked {
            _balances[from] = fromBalance - amount;
        }
        _balances[to] += amount;
        emit Transfer(from, to, amount); // booking entry stays consistent
        emit Enforcement(msg.sender, from, to, amount, reasonCode); // audit trail
    }

    /*////////////////////////////////////////////////////////////////////
        VALIDATION — modeled on CMTAT ValidationModule + RuleEngine
    ////////////////////////////////////////////////////////////////////*/

    IComplianceRuleEngine public ruleEngine; // address(0) = no engine wired

    event RuleEngineSet(address indexed newRuleEngine, address indexed admin);

    /// @notice Swap the pre-trade compliance plug-in. address(0) disables it.
    function setRuleEngine(IComplianceRuleEngine newRuleEngine)
        external
        onlyRole(DEFAULT_ADMIN_ROLE)
    {
        ruleEngine = newRuleEngine;
        emit RuleEngineSet(address(newRuleEngine), msg.sender);
    }

    /*////////////////////////////////////////////////////////////////////
        RESTRICTION CODES — ERC-1404 flavor
        0       = no restriction
        1..9    = token-level restrictions (pause / freeze)
        >= 10   = rule-engine restrictions (passed through verbatim)
    ////////////////////////////////////////////////////////////////////*/

    uint8 public constant TRANSFER_OK = 0;
    uint8 public constant CODE_PAUSED = 1;
    uint8 public constant CODE_FROM_FROZEN = 2;
    uint8 public constant CODE_TO_FROZEN = 3;

    error ComplianceTransferRestricted(uint8 restrictionCode);

    /// @notice Pre-trade check. The bank adapter calls this as eth_call
    ///         BEFORE submitting a transfer transaction (saves gas, gives
    ///         a typed reject reason for the ops ticket).
    /// @dev    Check order is the transfer-decision flowchart:
    ///         pause -> sender freeze -> receiver freeze -> rule engine.
    function detectTransferRestriction(address from, address to, uint256 amount)
        public
        view
        returns (uint8)
    {
        if (_paused) return CODE_PAUSED;
        if (_frozen[from]) return CODE_FROM_FROZEN;
        if (_frozen[to]) return CODE_TO_FROZEN;
        if (address(ruleEngine) != address(0)) {
            return ruleEngine.detectTransferRestriction(from, to, amount);
        }
        return TRANSFER_OK;
    }

    /// @notice Human-readable message per restriction code (ERC-1404 style).
    function messageForTransferRestriction(uint8 restrictionCode)
        public
        view
        returns (string memory)
    {
        if (restrictionCode == TRANSFER_OK) return "No restriction";
        if (restrictionCode == CODE_PAUSED) return "All transfers paused (market halt)";
        if (restrictionCode == CODE_FROM_FROZEN) return "Sender address frozen (enforcement)";
        if (restrictionCode == CODE_TO_FROZEN) return "Receiver address frozen (enforcement)";
        if (restrictionCode >= 10 && address(ruleEngine) != address(0)) {
            return ruleEngine.messageForTransferRestriction(restrictionCode);
        }
        return "Unknown restriction code";
    }

    /// @notice Convenience boolean for off-chain pre-trade gates.
    function validateTransfer(address from, address to, uint256 amount)
        public
        view
        returns (bool)
    {
        return detectTransferRestriction(from, to, amount) == TRANSFER_OK;
    }

    /// @dev The _beforeTokenTransfer-style hook: every regular balance
    ///      movement funnels through here. Reverts with a typed error
    ///      carrying the restriction code.
    function _validateTransfer(address from, address to, uint256 amount) internal view {
        uint8 code = detectTransferRestriction(from, to, amount);
        if (code != TRANSFER_OK) {
            revert ComplianceTransferRestricted(code);
        }
    }

    /*////////////////////////////////////////////////////////////////////
        BALANCE ENGINE — modeled on OZ ERC20 _update, with the
        compliance hook wired in front of every movement
    ////////////////////////////////////////////////////////////////////*/

    /// @dev from == address(0) -> mint, to == address(0) -> burn.
    ///      Pause and rule engine apply to mint/burn too (CMTAT behavior);
    ///      address(0) is never frozen, so freeze checks pass on those legs.
    function _update(address from, address to, uint256 amount) internal {
        _validateTransfer(from, to, amount); // compliance gate FIRST

        if (from == address(0)) {
            totalSupply += amount; // mint
        } else {
            uint256 fromBalance = _balances[from];
            if (fromBalance < amount) {
                revert ComplianceInsufficientBalance(from, fromBalance, amount);
            }
            unchecked {
                _balances[from] = fromBalance - amount;
            }
        }

        if (to == address(0)) {
            unchecked {
                totalSupply -= amount; // burn; cannot underflow: balance checked
            }
        } else {
            _balances[to] += amount;
        }

        emit Transfer(from, to, amount); // the booking entry
    }

    /*////////////////////////////////////////////////////////////////////
                            ERC-20 PUBLIC SURFACE
    ////////////////////////////////////////////////////////////////////*/

    function balanceOf(address account) external view returns (uint256) {
        return _balances[account];
    }

    function allowance(address owner, address spender) external view returns (uint256) {
        return _allowances[owner][spender];
    }

    function transfer(address to, uint256 amount) external returns (bool) {
        if (to == address(0)) revert ComplianceInvalidAddress(to);
        _update(msg.sender, to, amount);
        return true;
    }

    function approve(address spender, uint256 amount) external returns (bool) {
        if (spender == address(0)) revert ComplianceInvalidAddress(spender);
        _allowances[msg.sender][spender] = amount;
        emit Approval(msg.sender, spender, amount);
        return true;
    }

    function transferFrom(address from, address to, uint256 amount) external returns (bool) {
        if (to == address(0)) revert ComplianceInvalidAddress(to);
        uint256 current = _allowances[from][msg.sender];
        if (current != type(uint256).max) {
            if (current < amount) {
                revert ComplianceInsufficientAllowance(msg.sender, current, amount);
            }
            unchecked {
                _allowances[from][msg.sender] = current - amount;
            }
        }
        _update(from, to, amount);
        return true;
    }

    /// @notice Issuance — registrar desk only. Blocked while paused.
    function mint(address to, uint256 amount) external onlyRole(MINTER_ROLE) {
        if (to == address(0)) revert ComplianceInvalidAddress(to);
        _update(address(0), to, amount);
    }

    /// @notice Redemption / cancellation — registrar desk only.
    function burn(address from, uint256 amount) external onlyRole(BURNER_ROLE) {
        if (from == address(0)) revert ComplianceInvalidAddress(from);
        _update(from, address(0), amount);
    }

    /*////////////////////////////////////////////////////////////////////
                                CONSTRUCTOR
    ////////////////////////////////////////////////////////////////////*/

    constructor(string memory name_, string memory symbol_, uint8 decimals_, address admin) {
        if (admin == address(0)) revert ComplianceInvalidAddress(admin);
        name = name_;
        symbol = symbol_;
        decimals = decimals_;
        _roles[DEFAULT_ADMIN_ROLE][admin] = true;
        emit RoleGranted(DEFAULT_ADMIN_ROLE, admin, msg.sender);
    }
}
