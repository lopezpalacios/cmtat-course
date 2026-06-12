// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

/// @title CMTATBase — Chapter 06 skeleton of a CMTAT-style security token
/// @notice Teaching re-implementation, self-contained (zero imports).
///         Design goals of the CMTAT standard (CMTA, Switzerland):
///         1. ISSUER CONTROL  — the issuer (or its registrar/transfer agent)
///            retains mint/burn/pause/freeze powers over the register.
///         2. COMPLIANCE HOOKS — every transfer can be vetoed by pluggable
///            rules (ValidationModule + RuleEngine) and by global pause or
///            per-address freeze.
///         3. INSTRUMENT METADATA — the token carries its own legal anchors:
///            tokenId, terms, information fields (BaseModule) and a document
///            registry (DocumentModule).
///         Legal background (ledger-based securities, OR Art. 973d ff.):
///         see Chapter 09.

// ---------------------------------------------------------------------------
// SECTION 1 — Minimal inline access control
// modeled on CMTAT AuthorizationModule (which wraps OZ AccessControl)
// ---------------------------------------------------------------------------

abstract contract CMTATBaseAuthorization {
    // Role identifiers. CMTAT uses the same keccak256("...") convention as
    // OpenZeppelin AccessControl; DEFAULT_ADMIN_ROLE is bytes32(0).
    bytes32 public constant DEFAULT_ADMIN_ROLE = 0x00;
    bytes32 public constant MINTER_ROLE = keccak256("MINTER_ROLE");
    bytes32 public constant BURNER_ROLE = keccak256("BURNER_ROLE");
    bytes32 public constant PAUSER_ROLE = keccak256("PAUSER_ROLE");
    bytes32 public constant ENFORCER_ROLE = keccak256("ENFORCER_ROLE");
    bytes32 public constant SNAPSHOOTER_ROLE = keccak256("SNAPSHOOTER_ROLE");

    // role => account => granted?
    mapping(bytes32 => mapping(address => bool)) private _roles;

    // Audit trail of every role change — the bank's IAM reconciliation
    // job consumes these (see Chapter 05).
    event RoleGranted(bytes32 indexed role, address indexed account, address indexed sender);
    event RoleRevoked(bytes32 indexed role, address indexed account, address indexed sender);

    error CMTATBaseUnauthorized(address account, bytes32 role);

    modifier onlyRole(bytes32 role) {
        if (!_roles[role][msg.sender]) {
            revert CMTATBaseUnauthorized(msg.sender, role);
        }
        _;
    }

    function hasRole(bytes32 role, address account) public view returns (bool) {
        return _roles[role][account];
    }

    function grantRole(bytes32 role, address account) public onlyRole(DEFAULT_ADMIN_ROLE) {
        _grantRole(role, account);
    }

    function revokeRole(bytes32 role, address account) public onlyRole(DEFAULT_ADMIN_ROLE) {
        if (_roles[role][account]) {
            _roles[role][account] = false;
            emit RoleRevoked(role, account, msg.sender);
        }
    }

    // Internal grant used by the constructor (bootstrap, no admin yet).
    function _grantRole(bytes32 role, address account) internal {
        if (!_roles[role][account]) {
            _roles[role][account] = true;
            emit RoleGranted(role, account, msg.sender);
        }
    }
}

// ---------------------------------------------------------------------------
// SECTION 2 — Module surface catalog (interfaces only in this chapter;
// implementations arrive in Chapters 07, 11/14, 10).
// Each interface is modeled on the corresponding CMTAT module.
// ---------------------------------------------------------------------------

/// modeled on CMTAT PauseModule (OZ Pausable underneath)
/// Bank capability: market-wide trading halt on the instrument.
interface ICMTATBasePause {
    event Paused(address account);
    event Unpaused(address account);
    // Assumption: CMTAT >=2.3 also exposes deactivateContract() as a
    // permanent kill switch; signature varies by version, omitted here.
    function pause() external;
    function unpause() external;
    function paused() external view returns (bool);
}

/// modeled on CMTAT EnforcementModule
/// Bank capability: sanctions desk / court-order freeze of one holder.
interface ICMTATBaseEnforcement {
    // Assumption: CMTAT v2.3 emits Freeze/Unfreeze with both an indexed
    // (hashed) and a plain copy of the human-readable reason string.
    event Freeze(address indexed enforcer, address indexed owner, string reasonIndexed, string reason);
    event Unfreeze(address indexed enforcer, address indexed owner, string reasonIndexed, string reason);
    function freeze(address account, string calldata reason) external returns (bool);
    function unfreeze(address account, string calldata reason) external returns (bool);
    function frozen(address account) external view returns (bool);
}

/// modeled on CMTAT ERC20SnapshotModule
/// Bank capability: record date — fix the holder register at a point in time
/// for coupons (Track A) or dividends/voting (Track B).
interface ICMTATBaseSnapshot {
    event SnapshotSchedule(uint256 indexed oldTime, uint256 indexed newTime);
    event SnapshotUnschedule(uint256 indexed time);
    function scheduleSnapshot(uint256 time) external;
    function unscheduleSnapshot(uint256 time) external;
    function snapshotBalanceOf(uint256 time, address owner) external view returns (uint256);
    function snapshotTotalSupply(uint256 time) external view returns (uint256);
}

/// modeled on CMTAT IRuleEngine / IEIP1404Wrapper
/// Bank capability: pre-trade compliance — pluggable rule set consulted
/// before every transfer (whitelist, jurisdiction caps, lock-ups...).
interface ICMTATBaseRuleEngine {
    // Assumption: CMTAT versions differ here — v2.x exposes an ERC-1404
    // style wrapper (restriction codes + messages); v2.3+ adds
    // operateOnTransfer for stateful rules. We model the read-only surface.
    function validateTransfer(address from, address to, uint256 amount) external view returns (bool);
    function detectTransferRestriction(address from, address to, uint256 amount) external view returns (uint8 code);
    function messageForTransferRestriction(uint8 code) external view returns (string memory);
}

/// modeled on CMTAT ValidationModule (token-side half of the pair)
interface ICMTATBaseValidation {
    event RuleEngineSet(address indexed newRuleEngine);
    function setRuleEngine(address ruleEngine) external;
    function validateTransfer(address from, address to, uint256 amount) external view returns (bool);
}

/// modeled on CMTAT DocumentModule (ERC-1643 style document registry)
/// Bank capability: legal-document registry — prospectus, terms, KIID —
/// each anchored by hash so the off-chain PDF can be integrity-checked.
interface ICMTATBaseDocument {
    event DocumentUpdated(bytes32 indexed name, string uri, bytes32 documentHash);
    event DocumentRemoved(bytes32 indexed name, string uri, bytes32 documentHash);
    // Assumption: CMTAT v3 delegates storage to an external DocumentEngine;
    // the function surface below matches the ERC-1643 shape both share.
    function setDocument(bytes32 name, string calldata uri, bytes32 documentHash) external;
    function getDocument(bytes32 name) external view returns (string memory uri, bytes32 documentHash, uint256 lastModified);
    function getAllDocuments() external view returns (bytes32[] memory);
}

/// modeled on CMTAT DebtModule (DebtBaseModule)
/// Bank capability: instrument terms for debt — rate, par, maturity.
interface ICMTATBaseDebt {
    // Assumption: real CMTAT DebtBase carries additional string fields
    // (interestScheduleFormat, dayCountConvention, businessDayConvention,
    // publicHolidaysCalendar...). Chapter 10 models the full struct; this
    // chapter keeps the numeric core.
    struct CMTATBaseDebtInfo {
        uint256 interestRateBps;        // basis points, 425 = 4.25%
        uint256 parValue;               // smallest currency unit
        uint256 maturityDate;           // unix seconds
        uint256 couponFrequencySeconds; // e.g. 365 days for annual
        bytes32 isin;                   // right-padded ASCII
    }
    event DebtInfoSet(bytes32 indexed isin, uint256 interestRateBps, uint256 parValue, uint256 maturityDate);
    function debtInfo() external view returns (CMTATBaseDebtInfo memory);
}

// ---------------------------------------------------------------------------
// SECTION 3 — CMTATBase: BaseModule + ERC20BaseModule skeleton
// ---------------------------------------------------------------------------

/// @notice Constructor DTO. Bundling issuance parameters in a struct keeps
///         the deployment ABI flat-proof (no stack-too-deep) and mirrors how
///         a core-banking system passes an "instrument setup" record.
struct CMTATBaseParams {
    address admin;       // issuer admin (root of the role tree)
    string name;         // ERC-20 display name
    string symbol;       // ERC-20 ticker
    uint8 decimals;      // 0 for shares, >0 for divisible instruments
    string tokenId;      // issuer's internal instrument identifier
    string terms;        // URI/hash reference to legally binding terms
    string information;  // free-text information field
}

contract CMTATBase is CMTATBaseAuthorization {
    // ---- ERC20BaseModule fields (modeled on CMTAT ERC20BaseModule) ----
    string public name;
    string public symbol;
    // CMTAT note: Swiss registered shares are typically indivisible, so
    // CMTAT defaults decimals to 0; bonds/fund shares may use more.
    uint8 public immutable decimals;

    // ---- BaseModule fields (modeled on CMTAT BaseModule) ----
    // Assumption: CMTAT v2.x BaseModule stores tokenId, terms, information
    // as strings plus a uint256 flag bitfield. CMTAT v3 moves `terms` to an
    // ERC-1643 Document struct; we keep the v2 string shape for the skeleton.
    string public tokenId;     // issuer's internal instrument identifier
    string public terms;       // URI/hash reference to legally binding terms
    string public information; // free-text field (e.g. venue, contact)
    uint256 public flag;       // issuer-defined bitfield

    // ---- Minimal ERC-20 register (full version built in Chapter 04) ----
    uint256 private _totalSupply;
    mapping(address => uint256) private _balances;

    // ---- Events ----
    // modeled on CMTAT BaseModule events: each setter emits BOTH an indexed
    // copy (stored as keccak256 hash in the log topic — searchable) and a
    // plain copy (readable in the log data).
    event TokenIdSet(string indexed newTokenIdIndexed, string newTokenId);
    event TermSet(string indexed newTermIndexed, string newTerm);
    event InformationSet(string indexed newInformationIndexed, string newInformation);
    event FlagSet(uint256 indexed newFlag);
    // ERC-20 booking entry (see Chapter 04).
    event Transfer(address indexed from, address indexed to, uint256 value);

    error CMTATBaseInvalidAddress(address account);
    error CMTATBaseInsufficientBalance(address from, uint256 balance, uint256 needed);
    error CMTATBaseSameFlag(uint256 flag);

    constructor(CMTATBaseParams memory p) {
        if (p.admin == address(0)) {
            revert CMTATBaseInvalidAddress(p.admin);
        }
        // Bootstrap the role tree: admin can then grant operational roles
        // to registrar / compliance / operations addresses (Chapter 05).
        _grantRole(DEFAULT_ADMIN_ROLE, p.admin);

        name = p.name;
        symbol = p.symbol;
        decimals = p.decimals;

        tokenId = p.tokenId;
        terms = p.terms;
        information = p.information;

        // Emit metadata events at issuance so indexers see initial values.
        emit TokenIdSet(p.tokenId, p.tokenId);
        emit TermSet(p.terms, p.terms);
        emit InformationSet(p.information, p.information);
    }

    // ---- BaseModule setters (issuer-controlled metadata) ----

    function setTokenId(string calldata tokenId_) external onlyRole(DEFAULT_ADMIN_ROLE) {
        tokenId = tokenId_;
        emit TokenIdSet(tokenId_, tokenId_);
    }

    function setTerms(string calldata terms_) external onlyRole(DEFAULT_ADMIN_ROLE) {
        terms = terms_;
        emit TermSet(terms_, terms_);
    }

    function setInformation(string calldata information_) external onlyRole(DEFAULT_ADMIN_ROLE) {
        information = information_;
        emit InformationSet(information_, information_);
    }

    function setFlag(uint256 flag_) external onlyRole(DEFAULT_ADMIN_ROLE) {
        if (flag_ == flag) {
            revert CMTATBaseSameFlag(flag_);
        }
        flag = flag_;
        emit FlagSet(flag_);
    }

    // ---- Minimal register operations ----

    function totalSupply() external view returns (uint256) {
        return _totalSupply;
    }

    function balanceOf(address account) external view returns (uint256) {
        return _balances[account];
    }

    function mint(address to, uint256 value) external onlyRole(MINTER_ROLE) {
        if (to == address(0)) {
            revert CMTATBaseInvalidAddress(to);
        }
        _beforeTokenTransfer(address(0), to, value);
        _totalSupply += value;
        _balances[to] += value;
        emit Transfer(address(0), to, value);
    }

    function transfer(address to, uint256 value) external returns (bool) {
        if (to == address(0)) {
            revert CMTATBaseInvalidAddress(to);
        }
        uint256 fromBalance = _balances[msg.sender];
        if (fromBalance < value) {
            revert CMTATBaseInsufficientBalance(msg.sender, fromBalance, value);
        }
        _beforeTokenTransfer(msg.sender, to, value);
        unchecked {
            _balances[msg.sender] = fromBalance - value;
        }
        _balances[to] += value;
        emit Transfer(msg.sender, to, value);
        return true;
    }

    // ---- The module seam ----
    // Compliance modules (pause, freeze, validation) plug in here in
    // Chapter 07 by overriding this hook — same pattern CMTAT/OZ use with
    // _beforeTokenTransfer / _update.
    function _beforeTokenTransfer(address from, address to, uint256 value) internal virtual {}
}

// ---------------------------------------------------------------------------
// SECTION 4 — Factory deployment (standalone model)
// modeled on the CMTAT deployment scripts / CMTAT factory contracts
// ---------------------------------------------------------------------------

contract CMTATBaseFactory {
    // Registry of everything this factory issued — the bank's instrument
    // master can reconcile against this list.
    address[] public deployedTokens;

    event CMTATBaseDeployed(
        uint256 indexed deploymentId,
        address indexed token,
        address indexed admin,
        string tokenId
    );

    function deployStandalone(CMTATBaseParams calldata p) external returns (address) {
        CMTATBase token = new CMTATBase(p);
        deployedTokens.push(address(token));
        emit CMTATBaseDeployed(deployedTokens.length - 1, address(token), p.admin, p.tokenId);
        return address(token);
    }

    function deployedCount() external view returns (uint256) {
        return deployedTokens.length;
    }
}
