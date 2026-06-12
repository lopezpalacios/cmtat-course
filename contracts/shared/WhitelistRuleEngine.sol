// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

/*//////////////////////////////////////////////////////////////////////////
  Chapter 07 — Compliance Modules
  WhitelistRuleEngine: KYC-whitelist pre-trade rule engine.
  Modeled on CMTAT RuleEngine + RuleWhitelist, ERC-1404 restriction codes.

  Self-contained: zero imports. Structurally implements the
  IComplianceRuleEngine interface declared in ComplianceToken.sol
  (validateTransfer / detectTransferRestriction /
  messageForTransferRestriction) — Solidity matches on function
  selectors, not on declared inheritance, so the token can call this
  contract through the interface without any import here.
//////////////////////////////////////////////////////////////////////////*/

/// @title WhitelistRuleEngine
/// @notice Only KYC-whitelisted addresses may send or receive.
///         List managed by the bank's compliance/onboarding desk
///         holding ENFORCER_ROLE. address(0) is always allowed so that
///         mint and burn legs pass the rule.
contract WhitelistRuleEngine {
    /*////////////////////////////////////////////////////////////////////
                  ROLES — modeled on OZ AccessControl (Chapter 05)
    ////////////////////////////////////////////////////////////////////*/

    bytes32 public constant DEFAULT_ADMIN_ROLE = 0x00;
    bytes32 public constant ENFORCER_ROLE = keccak256("ENFORCER_ROLE");

    mapping(bytes32 => mapping(address => bool)) private _roles;

    event WhitelistRoleGranted(bytes32 indexed role, address indexed account, address indexed sender);
    event WhitelistRoleRevoked(bytes32 indexed role, address indexed account, address indexed sender);

    error WhitelistUnauthorized(address account, bytes32 neededRole);
    error WhitelistInvalidAddress(address account);
    error WhitelistLengthMismatch(uint256 accountsLength, uint256 refsLength);

    modifier onlyWhitelistRole(bytes32 role) {
        if (!_roles[role][msg.sender]) {
            revert WhitelistUnauthorized(msg.sender, role);
        }
        _;
    }

    function hasRole(bytes32 role, address account) public view returns (bool) {
        return _roles[role][account];
    }

    function grantRole(bytes32 role, address account)
        external
        onlyWhitelistRole(DEFAULT_ADMIN_ROLE)
    {
        if (!_roles[role][account]) {
            _roles[role][account] = true;
            emit WhitelistRoleGranted(role, account, msg.sender);
        }
    }

    function revokeRole(bytes32 role, address account)
        external
        onlyWhitelistRole(DEFAULT_ADMIN_ROLE)
    {
        if (_roles[role][account]) {
            _roles[role][account] = false;
            emit WhitelistRoleRevoked(role, account, msg.sender);
        }
    }

    /*////////////////////////////////////////////////////////////////////
                            WHITELIST STORAGE
    ////////////////////////////////////////////////////////////////////*/

    mapping(address => bool) private _whitelisted;
    // Off-chain KYC dossier reference (right-padded ASCII), e.g.
    // bytes32("KYC-CH-2026-008842"). Lets reconciliation join the chain
    // record back to the bank's KYC system without putting PII on-chain.
    mapping(address => bytes32) private _kycRef;

    event AddressWhitelisted(address indexed account, bytes32 kycRef, address indexed operator);
    event AddressDelisted(address indexed account, bytes32 reasonCode, address indexed operator);

    /*////////////////////////////////////////////////////////////////////
            RESTRICTION CODES — engine namespace is >= 10 (course
            convention; token-level codes 1..9 live in ComplianceToken)
    ////////////////////////////////////////////////////////////////////*/

    uint8 public constant CODE_OK = 0;
    uint8 public constant CODE_FROM_NOT_WHITELISTED = 10;
    uint8 public constant CODE_TO_NOT_WHITELISTED = 11;

    /*////////////////////////////////////////////////////////////////////
                            LIST MANAGEMENT
    ////////////////////////////////////////////////////////////////////*/

    /// @notice Admit an investor after KYC/AML onboarding completes.
    function addToWhitelist(address account, bytes32 kycRef)
        external
        onlyWhitelistRole(ENFORCER_ROLE)
    {
        if (account == address(0)) revert WhitelistInvalidAddress(account);
        _whitelisted[account] = true;
        _kycRef[account] = kycRef;
        emit AddressWhitelisted(account, kycRef, msg.sender);
    }

    /// @notice Batch onboarding — one transaction for a nightly KYC sync job.
    function batchAddToWhitelist(address[] calldata accounts, bytes32[] calldata kycRefs)
        external
        onlyWhitelistRole(ENFORCER_ROLE)
    {
        if (accounts.length != kycRefs.length) {
            revert WhitelistLengthMismatch(accounts.length, kycRefs.length);
        }
        for (uint256 i = 0; i < accounts.length; i++) {
            if (accounts[i] == address(0)) revert WhitelistInvalidAddress(accounts[i]);
            _whitelisted[accounts[i]] = true;
            _kycRef[accounts[i]] = kycRefs[i];
            emit AddressWhitelisted(accounts[i], kycRefs[i], msg.sender);
        }
    }

    /// @notice Remove an investor (KYC expired, relationship closed, sanction).
    function removeFromWhitelist(address account, bytes32 reasonCode)
        external
        onlyWhitelistRole(ENFORCER_ROLE)
    {
        _whitelisted[account] = false;
        _kycRef[account] = bytes32(0);
        emit AddressDelisted(account, reasonCode, msg.sender);
    }

    function isWhitelisted(address account) public view returns (bool) {
        return _whitelisted[account];
    }

    function kycReference(address account) external view returns (bytes32) {
        return _kycRef[account];
    }

    /*////////////////////////////////////////////////////////////////////
        RULE ENGINE SURFACE — IComplianceRuleEngine signatures
    ////////////////////////////////////////////////////////////////////*/

    /// @dev Shared detection logic for both ERC-1404-style entry points.
    function _detect(address from, address to) internal view returns (uint8) {
        if (from != address(0) && !_whitelisted[from]) return CODE_FROM_NOT_WHITELISTED;
        if (to != address(0) && !_whitelisted[to]) return CODE_TO_NOT_WHITELISTED;
        return CODE_OK;
    }

    /// @notice ERC-1404-style detection. address(0) legs (mint/burn)
    ///         always pass — issuance gating is the token's MINTER_ROLE job.
    function detectTransferRestriction(address from, address to, uint256 /* amount */)
        external
        view
        returns (uint8)
    {
        return _detect(from, to);
    }

    function validateTransfer(address from, address to, uint256 /* amount */)
        external
        view
        returns (bool)
    {
        return _detect(from, to) == CODE_OK;
    }

    function messageForTransferRestriction(uint8 restrictionCode)
        external
        pure
        returns (string memory)
    {
        if (restrictionCode == CODE_OK) return "No restriction";
        if (restrictionCode == CODE_FROM_NOT_WHITELISTED) {
            return "Sender not on KYC whitelist";
        }
        if (restrictionCode == CODE_TO_NOT_WHITELISTED) {
            return "Receiver not on KYC whitelist";
        }
        return "Unknown restriction code";
    }

    /*////////////////////////////////////////////////////////////////////
                                CONSTRUCTOR
    ////////////////////////////////////////////////////////////////////*/

    constructor(address admin) {
        if (admin == address(0)) revert WhitelistInvalidAddress(admin);
        _roles[DEFAULT_ADMIN_ROLE][admin] = true;
        emit WhitelistRoleGranted(DEFAULT_ADMIN_ROLE, admin, msg.sender);
    }
}
