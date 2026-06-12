// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

/*//////////////////////////////////////////////////////////////////////////
    Chapter 05 — Access Control: Mapping Bank Org Structure On-Chain

    RoleControlled
    --------------
    Role-based access control base contract.
    // modeled on OpenZeppelin AccessControl + CMTAT AuthorizationModule

    Self-contained: zero imports. All patterns re-implemented inline.
//////////////////////////////////////////////////////////////////////////*/

contract RoleControlled {
    // ---------------------------------------------------------------
    // The root role. bytes32(0) by convention (OZ AccessControl).
    // Holders of DEFAULT_ADMIN_ROLE administer every role whose
    // adminRole was never changed via _setRoleAdmin.
    // ---------------------------------------------------------------
    bytes32 public constant DEFAULT_ADMIN_ROLE = 0x00;

    // Per-role data: membership set + the role that administers it.
    // modeled on OpenZeppelin AccessControl.RoleData
    struct RoleControlledRoleData {
        mapping(address => bool) members; // who holds the role
        bytes32 adminRole;                // which role may grant/revoke it
    }

    // role id (bytes32) => role data. The on-chain entitlement table.
    mapping(bytes32 => RoleControlledRoleData) private _roles;

    // ---------------------------------------------------------------
    // Events — the access-recertification feed consumed by bank IAM.
    // All three parameters indexed: filterable by role, account, actor.
    // ---------------------------------------------------------------
    event RoleGranted(bytes32 indexed role, address indexed account, address indexed sender);
    event RoleRevoked(bytes32 indexed role, address indexed account, address indexed sender);
    event RoleAdminChanged(bytes32 indexed role, bytes32 indexed previousAdminRole, bytes32 indexed newAdminRole);

    // Custom errors: cheaper than revert strings, ABI-decodable off-chain.
    error RoleControlledMissingRole(address account, bytes32 role);
    error RoleControlledBadConfirmation();

    // Policy gate: caller must hold `role` or the call reverts.
    modifier onlyRole(bytes32 role) {
        _checkRole(role, msg.sender);
        _;
    }

    constructor(address initialAdmin) {
        // Bootstrap: someone must be able to grant the first roles.
        _grantRole(DEFAULT_ADMIN_ROLE, initialAdmin);
    }

    // ------------------------- views -------------------------------

    function hasRole(bytes32 role, address account) public view returns (bool) {
        return _roles[role].members[account];
    }

    function getRoleAdmin(bytes32 role) public view returns (bytes32) {
        // Unset adminRole == bytes32(0) == DEFAULT_ADMIN_ROLE: safe default.
        return _roles[role].adminRole;
    }

    // ------------------------ mutations -----------------------------

    // Only the admin role of `role` may grant it.
    function grantRole(bytes32 role, address account)
        public
        virtual
        onlyRole(getRoleAdmin(role))
    {
        _grantRole(role, account);
    }

    // Only the admin role of `role` may revoke it.
    function revokeRole(bytes32 role, address account)
        public
        virtual
        onlyRole(getRoleAdmin(role))
    {
        _revokeRole(role, account);
    }

    // An account may always drop its own entitlements (leaver process).
    // callerConfirmation guards against fat-finger calls from tooling.
    function renounceRole(bytes32 role, address callerConfirmation) public virtual {
        if (callerConfirmation != msg.sender) revert RoleControlledBadConfirmation();
        _revokeRole(role, msg.sender);
    }

    // ------------------------ internals -----------------------------

    function _checkRole(bytes32 role, address account) internal view {
        if (!hasRole(role, account)) {
            revert RoleControlledMissingRole(account, role);
        }
    }

    // Re-point which role administers `role` (role admin hierarchy).
    function _setRoleAdmin(bytes32 role, bytes32 adminRole) internal {
        bytes32 previousAdminRole = _roles[role].adminRole;
        _roles[role].adminRole = adminRole;
        emit RoleAdminChanged(role, previousAdminRole, adminRole);
    }

    // Idempotent: granting an already-held role is a no-op (no event),
    // so replayed admin jobs cannot pollute the audit trail.
    function _grantRole(bytes32 role, address account) internal returns (bool) {
        if (_roles[role].members[account]) {
            return false;
        }
        _roles[role].members[account] = true;
        emit RoleGranted(role, account, msg.sender);
        return true;
    }

    // Idempotent mirror of _grantRole.
    function _revokeRole(bytes32 role, address account) internal returns (bool) {
        if (!_roles[role].members[account]) {
            return false;
        }
        _roles[role].members[account] = false;
        emit RoleRevoked(role, account, msg.sender);
        return true;
    }
}

/*//////////////////////////////////////////////////////////////////////////
    RoleControlledShareToken
    ------------------------
    Demo registered-share ledger gated by RoleControlled.
    // ERC-20 register modeled on CMTAT ERC20BaseModule (minimal subset)
    // pause modeled on CMTAT PauseModule
    // freeze modeled on CMTAT EnforcementModule
    Four-eyes (propose/approve) grants for sensitive roles.
//////////////////////////////////////////////////////////////////////////*/

contract RoleControlledShareToken is RoleControlled {
    // ------------------ bank role constants -------------------------
    // bytes32 role ids derived from human-readable names via keccak256.
    // Off-chain (web3j): Hash.sha3String("MINTER_ROLE") gives same id.
    bytes32 public constant MINTER_ROLE = keccak256("MINTER_ROLE");           // registrar / issuance desk
    bytes32 public constant BURNER_ROLE = keccak256("BURNER_ROLE");           // registrar (cancellations)
    bytes32 public constant PAUSER_ROLE = keccak256("PAUSER_ROLE");           // compliance officer
    bytes32 public constant ENFORCER_ROLE = keccak256("ENFORCER_ROLE");       // compliance / legal
    bytes32 public constant SNAPSHOOTER_ROLE = keccak256("SNAPSHOOTER_ROLE"); // corporate actions desk

    // ------------------ minimal share register ----------------------
    string public name;
    string public symbol;
    uint8 public constant decimals = 0; // registered shares: whole units only

    uint256 public totalSupply;
    mapping(address => uint256) public balanceOf;

    bool public paused;                     // market-wide halt
    mapping(address => bool) public frozen; // per-address enforcement

    // ------------------ four-eyes pending grants --------------------
    struct RoleControlledPendingGrant {
        address proposer;   // first pair of eyes
        uint64 proposedAt;  // block timestamp of proposal
        bool exists;
    }
    // role => account => pending proposal
    mapping(bytes32 => mapping(address => RoleControlledPendingGrant)) public pendingGrants;

    // ------------------------- events -------------------------------
    event Transfer(address indexed from, address indexed to, uint256 value);
    event TokenPaused(address indexed sender);
    event TokenUnpaused(address indexed sender);
    event AddressFrozen(address indexed account, address indexed sender);
    event AddressUnfrozen(address indexed account, address indexed sender);
    event GrantProposed(bytes32 indexed role, address indexed account, address indexed proposer);
    event GrantApproved(bytes32 indexed role, address indexed account, address indexed approver);
    event GrantCancelled(bytes32 indexed role, address indexed account, address indexed sender);

    // ------------------------- errors -------------------------------
    error RoleControlledTransfersPaused();
    error RoleControlledAccountFrozen(address account);
    error RoleControlledFourEyesRequired(bytes32 role);
    error RoleControlledNoPendingGrant(bytes32 role, address account);
    error RoleControlledSelfApproval(bytes32 role, address account);
    error RoleControlledGrantAlreadyPending(bytes32 role, address account);
    error RoleControlledInsufficientBalance(address from, uint256 balance, uint256 needed);
    error RoleControlledZeroAddress();

    // Two admins from day one: four-eyes needs two distinct approvers.
    constructor(
        string memory name_,
        string memory symbol_,
        address initialAdmin,
        address coAdmin
    ) RoleControlled(initialAdmin) {
        if (coAdmin == address(0)) revert RoleControlledZeroAddress();
        name = name_;
        symbol = symbol_;
        _grantRole(DEFAULT_ADMIN_ROLE, coAdmin);
    }

    // ------------------ four-eyes grant flow -------------------------

    // Sensitive roles can never be granted in a single call.
    function _isSensitiveRole(bytes32 role) internal pure returns (bool) {
        return role == DEFAULT_ADMIN_ROLE || role == ENFORCER_ROLE;
    }

    // Block the single-step path for sensitive roles.
    function grantRole(bytes32 role, address account)
        public
        override
        onlyRole(getRoleAdmin(role))
    {
        if (_isSensitiveRole(role)) revert RoleControlledFourEyesRequired(role);
        _grantRole(role, account);
    }

    // Step 1 of 2: an admin proposes the grant. Nothing takes effect yet.
    function proposeGrant(bytes32 role, address account)
        external
        onlyRole(getRoleAdmin(role))
    {
        if (account == address(0)) revert RoleControlledZeroAddress();
        if (pendingGrants[role][account].exists) {
            revert RoleControlledGrantAlreadyPending(role, account);
        }
        pendingGrants[role][account] = RoleControlledPendingGrant({
            proposer: msg.sender,
            proposedAt: uint64(block.timestamp),
            exists: true
        });
        emit GrantProposed(role, account, msg.sender);
    }

    // Step 2 of 2: a DIFFERENT admin approves. Enforced on-chain.
    function approveGrant(bytes32 role, address account)
        external
        onlyRole(getRoleAdmin(role))
    {
        RoleControlledPendingGrant memory pending = pendingGrants[role][account];
        if (!pending.exists) revert RoleControlledNoPendingGrant(role, account);
        if (pending.proposer == msg.sender) {
            revert RoleControlledSelfApproval(role, account);
        }
        delete pendingGrants[role][account];
        _grantRole(role, account); // emits RoleGranted
        emit GrantApproved(role, account, msg.sender);
    }

    // Either admin may withdraw a pending proposal.
    function cancelGrant(bytes32 role, address account)
        external
        onlyRole(getRoleAdmin(role))
    {
        if (!pendingGrants[role][account].exists) {
            revert RoleControlledNoPendingGrant(role, account);
        }
        delete pendingGrants[role][account];
        emit GrantCancelled(role, account, msg.sender);
    }

    // ------------------ role-gated operations ------------------------

    // Registrar / issuance desk books new shares into the register.
    function mint(address to, uint256 amount) external onlyRole(MINTER_ROLE) {
        if (to == address(0)) revert RoleControlledZeroAddress();
        totalSupply += amount;
        balanceOf[to] += amount;
        emit Transfer(address(0), to, amount);
    }

    // Registrar cancels shares (buyback cancellation, error correction).
    function burn(address from, uint256 amount) external onlyRole(BURNER_ROLE) {
        uint256 balance = balanceOf[from];
        if (balance < amount) {
            revert RoleControlledInsufficientBalance(from, balance, amount);
        }
        balanceOf[from] = balance - amount;
        totalSupply -= amount;
        emit Transfer(from, address(0), amount);
    }

    // Compliance officer halts the whole market (see Chapter 09).
    function pause() external onlyRole(PAUSER_ROLE) {
        paused = true;
        emit TokenPaused(msg.sender);
    }

    function unpause() external onlyRole(PAUSER_ROLE) {
        paused = false;
        emit TokenUnpaused(msg.sender);
    }

    // Compliance/legal freezes a single address (sanctions, court order).
    function freeze(address account) external onlyRole(ENFORCER_ROLE) {
        frozen[account] = true;
        emit AddressFrozen(account, msg.sender);
    }

    function unfreeze(address account) external onlyRole(ENFORCER_ROLE) {
        frozen[account] = false;
        emit AddressUnfrozen(account, msg.sender);
    }

    // ------------------ investor-facing transfer ---------------------

    function transfer(address to, uint256 amount) external returns (bool) {
        if (paused) revert RoleControlledTransfersPaused();
        if (frozen[msg.sender]) revert RoleControlledAccountFrozen(msg.sender);
        if (frozen[to]) revert RoleControlledAccountFrozen(to);
        if (to == address(0)) revert RoleControlledZeroAddress();
        uint256 balance = balanceOf[msg.sender];
        if (balance < amount) {
            revert RoleControlledInsufficientBalance(msg.sender, balance, amount);
        }
        balanceOf[msg.sender] = balance - amount;
        balanceOf[to] += amount;
        emit Transfer(msg.sender, to, amount);
        return true;
    }
}
