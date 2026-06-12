# Chapter 03 — Functions, Modifiers, and Events as the Integration Contract

**Track:** shared (all learners)
**Emphasis threads:** `[BANK-heavy]` `[TYPES]`
**Chapter learning objective:** Master the three Solidity constructs that define how a bank system talks to a smart contract — functions (the command + query API), modifiers (declarative policy gates, your on-chain Spring Security), and events (the booking feed that core banking reconciles against). You will assemble `EventDrivenLedger`, a contract whose entire design thesis is: *contract storage holds only what the EVM must enforce; everything the bank needs travels in events.*
**Prerequisites:** Chapter 01 (EVM mental model, transactions, gas), Chapter 02 (uint256 money, bytes32 identifiers, mappings).
**You will build:** `contracts/shared/EventDrivenLedger.sol` (on-chain) and `java-adapters/EventLogParser.java` (bank-side web3j adapter).

---

## Lesson 1 — Functions and Visibility: The Contract's API Surface

**Learning objective:** Declare functions with correct visibility (`external`/`public`/`internal`/`private`) and mutability (`view`/`pure`), return multiple values, and replace require-strings with custom errors — understanding the gas cost of each choice.
**Emphasis tags:** `[BANK]` `[TYPES]`
**Track:** shared

A Solidity contract's functions are its entire API. There is no REST layer, no servlet container, no controller annotations — the compiled ABI *is* the OpenAPI spec, and visibility keywords *are* your `public`/`package-private`/`private` access levels, except they also change gas cost and who on the planet can call you.

### Step 1.1 — Lay down the contract skeleton and public state

**Instruction:** Create the file `EventDrivenLedger.sol`. Declare the pragma, the contract, and two state variables: `admin` (an `address`, `public`) and `bookingCount` (a `uint256`, `public`).

**Explanation:** Marking a state variable `public` makes the compiler auto-generate a free getter function with the same name. `admin` becomes a function `admin()` returning the address — exactly like Lombok generating `getAdmin()` from a field, except the "getter" is part of your public network API. Any system in the world (including your bank's Java adapter) can call `admin()` via `eth_call` at zero cost. There is **no** auto-generated setter — writes always need an explicit function and a signed transaction, the way a core-banking posting needs an authenticated message, never a raw DB UPDATE.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract EventDrivenLedger {
    // declare admin and bookingCount here, both public

}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract EventDrivenLedger {
    address public admin;        // public => compiler generates admin() view getter
    uint256 public bookingCount; // running counter, free to read via eth_call
}
```

**Validation rule:** `address\s+public\s+admin\s*;[\s\S]*uint256\s+public\s+bookingCount\s*;` — checks both state variables are declared `public` with correct types.

> **Datatype/parser note:** When your Java adapter calls the generated getter, web3j decodes the return: `address` → web3j `Address` → Java `String` (`"0x..."` hex, 20 bytes); `uint256` → web3j `Uint256` → Java `BigInteger`. Never decode a uint256 into `long` — uint256 max is ~1.16e77, and token amounts in smallest units routinely exceed 2^63.

### Step 1.2 — A `view` function with multiple returns: the free query API

**Instruction:** Add an `external view` function `ledgerStatus()` that returns two named values: `uint256 count` and `address currentAdmin`, populated from the state variables.

**Explanation:** `view` promises the function reads state but never writes it. The killer consequence for a bank integrator: `view` functions are served by `eth_call`, which executes on a node *without* creating a transaction — **no gas paid, no signature, no block inclusion, instant answer**. This is your query API, the equivalent of a read-only replica endpoint. Compare with writes: every state-changing call is a transaction that costs gas and waits for a block, like a payment instruction waiting for the clearing cycle. Solidity also returns multiple values natively (tuples) — no DTO wrapper class needed; web3j decodes the tuple into a `List<Type>` in declaration order. Visibility `external` means "callable only from outside the contract" — marginally cheaper than `public` for functions never called internally, because arguments stay in calldata instead of being copied to memory.

**Starter code:**
```solidity
    // returns (bookingCount, admin) as a tuple — fill in mutability and body
    function ledgerStatus() external ____ returns (uint256 count, address currentAdmin) {

    }
```

**Solution:**
```solidity
    // Free read API: served by eth_call — no gas, no tx, no signature.
    function ledgerStatus() external view returns (uint256 count, address currentAdmin) {
        return (bookingCount, admin); // tuple return, decoded in order off-chain
    }
```

**Validation rule:** `function\s+ledgerStatus\s*\(\s*\)\s+external\s+view\s+returns\s*\(\s*uint256\s+count\s*,\s*address\s+currentAdmin\s*\)` — checks signature with `external view` and the two named returns.

> **Banking integration note:** Your nightly reconciliation job should hammer `view` functions freely — they cost nothing. Here is the Java side calling `ledgerStatus()` via raw `eth_call` with web3j:
> ```java
> Function fn = new Function("ledgerStatus", Collections.emptyList(),
>     Arrays.asList(new TypeReference<Uint256>() {}, new TypeReference<Address>() {}));
> String encoded = FunctionEncoder.encode(fn);
> EthCall resp = web3j.ethCall(
>     Transaction.createEthCallTransaction(null, contractAddress, encoded),
>     DefaultBlockParameterName.LATEST).send();
> List<Type> out = FunctionReturnDecoder.decode(resp.getValue(), fn.getOutputParameters());
> BigInteger count = ((Uint256) out.get(0)).getValue();
> String admin   = ((Address) out.get(1)).getValue();
> ```
> No private key was involved. Reads are anonymous and free — design your monitoring around that.

### Step 1.3 — A `pure` function: deterministic helpers

**Instruction:** Add a `public pure` function `toMinorUnits(uint256 major, uint8 tokenDecimals)` returning `uint256` — multiply `major` by `10 ** tokenDecimals`.

**Explanation:** `pure` is stricter than `view`: the function may touch *neither* storage *nor* environment (`block.timestamp`, `msg.sender`, balances). Input in, output out — a static utility method, `Math.multiplyExact` territory. Why bother annotating? The compiler enforces it (you cannot accidentally read state), callers know it is side-effect-free, and nodes can serve it without any state access. The body demonstrates the Chapter 02 money rule: monetary amounts always live as integers in smallest units (CHF 125.50 → `12550` with `tokenDecimals = 2`). Solidity 0.8 checked arithmetic means the multiplication reverts on overflow instead of silently wrapping — like `Math.multiplyExact` throwing `ArithmeticException`, not like C's silent wrap.

**Starter code:**
```solidity
    // CHF -> rappen style conversion; choose the right mutability keyword
    function toMinorUnits(uint256 major, uint8 tokenDecimals) public ____ returns (uint256) {
        return ____;
    }
```

**Solution:**
```solidity
    /// pure: reads neither storage nor environment — a deterministic utility.
    function toMinorUnits(uint256 major, uint8 tokenDecimals) public pure returns (uint256) {
        return major * (10 ** uint256(tokenDecimals)); // reverts on overflow (0.8 checked math)
    }
```

**Validation rule:** `function\s+toMinorUnits\s*\(\s*uint256\s+major\s*,\s*uint8\s+tokenDecimals\s*\)\s+public\s+pure\s+returns\s*\(\s*uint256\s*\)` — checks the `pure` keyword and exact parameter types.

### Step 1.4 — Custom errors instead of require strings

**Instruction:** Declare seven custom errors at contract level (above the functions): `LedgerNotAdmin(address caller)`, `LedgerNotOperator(address caller)`, `LedgerZeroRef()`, `LedgerZeroAccount()`, `LedgerZeroAmount()`, `LedgerInvalidEntryType(uint8 entryType)`, `LedgerDuplicateRef(bytes32 externalRef)`.

**Explanation:** The legacy pattern `require(ok, "not admin")` embeds the ASCII string in the deployed bytecode and ABI-encodes it on every revert — you pay deployment gas for prose. Custom errors (Solidity ≥0.8.4) compile to a 4-byte selector plus typed parameters: cheaper to deploy, cheaper to revert, and — crucially for the bank — **machine-decodable**. `LedgerDuplicateRef(bytes32)` tells your Java adapter *which* reference collided, structured, not by parsing an English sentence. Think typed exception hierarchy (`DuplicateBookingException(String ref)`) versus `throw new RuntimeException("something broke")`. A revert rolls back the *entire* transaction — every state change, every event — exactly like a database transaction rollback. There are no partially-applied transactions on the EVM; this is your atomicity guarantee.

**Starter code:**
```solidity
    // declare the seven custom errors here, e.g.:
    // error LedgerNotAdmin(address caller);

```

**Solution:**
```solidity
    // Custom errors: 4-byte selector + typed params. Cheaper than require strings,
    // and the bank adapter decodes them like typed exceptions.
    error LedgerNotAdmin(address caller);
    error LedgerNotOperator(address caller);
    error LedgerZeroRef();
    error LedgerZeroAccount();
    error LedgerZeroAmount();
    error LedgerInvalidEntryType(uint8 entryType);
    error LedgerDuplicateRef(bytes32 externalRef);
```

**Validation rule:** `error\s+LedgerDuplicateRef\s*\(\s*bytes32\s+externalRef\s*\)\s*;` plus `error\s+LedgerNotOperator\s*\(\s*address\s+caller\s*\)\s*;` — checks at least the two load-bearing errors are declared with typed parameters.

> **Banking integration note:** When a transaction reverts with a custom error, the revert reason surfaces in the `eth_call` simulation (and in `TransactionReceipt` debugging via trace APIs). web3j gives you the raw revert bytes; the first 4 bytes are `keccak256("LedgerDuplicateRef(bytes32)")[0..4]`. Match the selector, then ABI-decode the payload. Your adapter should treat `LedgerDuplicateRef` as **success-by-idempotency** (the booking already exists — do not retry, do not alert), while `LedgerNotOperator` is a configuration incident (wrong signing key / role not granted) that pages someone.

### Step 1.5 — `internal` helpers: invisible in the ABI

**Instruction:** Add an `internal pure` function `_isValidEntryType(uint8 entryType)` returning `bool` — true when `entryType` is `1` or `2`. (We will replace the magic numbers with named constants in Lesson 3.)

**Explanation:** Four visibility levels, mapped to what you know:
- `external` — callable only from outside. A REST endpoint.
- `public` — callable from outside *and* internally. A public method that also serves remote calls.
- `internal` — this contract and its subclasses. Java `protected`.
- `private` — this contract only, not even children. Java `private`.

`internal`/`private` functions never appear in the ABI — outsiders cannot call them, and they execute as cheap `JUMP`s rather than external calls. Note the leading-underscore convention for internal/private members: it is how OpenZeppelin and CMTAT mark internal API (`_transfer`, `_mint`, `_beforeTokenTransfer`), and we follow it course-wide. CMTAT's modules are an inheritance lattice glued together precisely by `internal` functions — when you read CMTAT's `ValidationModule` calling `_validateTransfer` in Chapter 07, this is the mechanism.

**Starter code:**
```solidity
    // entry types: 1 = debit, 2 = credit. Pick visibility so it is NOT in the ABI.
    function _isValidEntryType(uint8 entryType) ____ pure returns (bool) {
        return ____;
    }
```

**Solution:**
```solidity
    /// internal: shared validation helper, invisible in the ABI (cf. a protected Java method).
    function _isValidEntryType(uint8 entryType) internal pure returns (bool) {
        return entryType == 1 || entryType == 2; // named constants replace these in Lesson 3
    }
```

**Validation rule:** `function\s+_isValidEntryType\s*\(\s*uint8\s+entryType\s*\)\s+internal\s+pure\s+returns\s*\(\s*bool\s*\)` — checks `internal pure` visibility/mutability on the helper.

---

## Lesson 2 — Modifiers as Policy Gates

**Learning objective:** Build `onlyAdmin` and `onlyOperator` modifiers by hand, understand `msg.sender` as the authenticated principal, and map modifier-gated functions to Spring Security method-level authorization.
**Emphasis tags:** `[BANK-heavy]`
**Track:** shared

In your Java stack, authorization is declarative: `@PreAuthorize("hasRole('OPERATOR')")` on the method, an AOP proxy intercepts the call, checks the `SecurityContext`, and either proceeds or throws `AccessDeniedException`. Solidity has the same idea built into the language: **modifiers**. A modifier wraps a function body; the special statement `_;` is the splice point where the body runs — exactly an `@Around` advice's `proceed()`.

One profound difference: there is no session, no JWT, no `SecurityContextHolder`. The EVM gives you `msg.sender` — the address cryptographically recovered from the transaction signature. It cannot be spoofed without the private key. Authentication is free and absolute; *authorization* is what you build.

### Step 2.1 — The constructor: who is the bootstrap principal?

**Instruction:** Add a state variable `mapping(address => bool) public isOperator;` and a constructor that sets `admin = msg.sender;` and `isOperator[msg.sender] = true;`.

**Explanation:** The constructor runs exactly once, during deployment, inside the deployment transaction — like a Flyway baseline migration plus the creation of the first system user. Inside it, `msg.sender` is the deploying address: whoever signs the deployment transaction becomes admin. In a bank context this deployment key is a ceremony artifact — generated in an HSM, used once, and the role is then handed to operational keys (Chapter 05 builds full role hierarchies; this chapter uses a deliberately simplified single-admin model, labeled as such). The `isOperator` mapping is our role table: address → bool, the on-chain equivalent of a `ROLE_ASSIGNMENT` table keyed by principal.

**Starter code:**
```solidity
    mapping(address => bool) public isOperator; // role table: principal -> granted

    constructor() {
        // bootstrap: deployer becomes admin AND first operator

    }
```

**Solution:**
```solidity
    // modeled on OZ AccessControl (single-role simplification; full RBAC in Chapter 05)
    mapping(address => bool) public isOperator;

    constructor() {
        admin = msg.sender;            // deployer = bootstrap principal (key ceremony artifact)
        isOperator[msg.sender] = true; // admin can also operate, until roles are split
    }
```

**Validation rule:** `constructor\s*\(\s*\)\s*\{[\s\S]*admin\s*=\s*msg\.sender\s*;[\s\S]*isOperator\[\s*msg\.sender\s*\]\s*=\s*true\s*;[\s\S]*\}` — checks both bootstrap assignments inside the constructor.

### Step 2.2 — Write `onlyAdmin` by hand

**Instruction:** Write a modifier `onlyAdmin()` that reverts with `LedgerNotAdmin(msg.sender)` when `msg.sender != admin`, then runs the function body via `_;`.

**Explanation:** Read the modifier as an interceptor: check first, `_;` is `joinPoint.proceed()`. If the check reverts, the body never runs and all state is rolled back. Putting `_;` *after* the check is the standard guard pattern; you could put code after `_;` too (post-conditions — an `@AfterReturning` advice), which CMTAT does in places. Reverting with the typed error carries the offending caller address into the revert payload, so the bank's monitoring sees *who* attempted the unauthorized call — your `AccessDeniedException` with the principal attached, except it is also evidence on a public ledger.

**Starter code:**
```solidity
    modifier onlyAdmin() {
        // revert with LedgerNotAdmin(msg.sender) unless caller is admin
        // then splice in the function body

    }
```

**Solution:**
```solidity
    // cf. Spring's @PreAuthorize("hasRole('ADMIN')") — but enforced by every node on earth
    modifier onlyAdmin() {
        if (msg.sender != admin) revert LedgerNotAdmin(msg.sender);
        _; // splice point: the guarded function body executes here
    }
```

**Validation rule:** `modifier\s+onlyAdmin\s*\(\s*\)\s*\{\s*if\s*\(\s*msg\.sender\s*!=\s*admin\s*\)\s*revert\s+LedgerNotAdmin\s*\(\s*msg\.sender\s*\)\s*;\s*_\s*;\s*\}` — checks guard-then-splice structure with the custom error.

### Step 2.3 — Write `onlyOperator`

**Instruction:** Write a modifier `onlyOperator()` that reverts with `LedgerNotOperator(msg.sender)` unless `isOperator[msg.sender]` is true.

**Explanation:** Same pattern, different role check — a mapping lookup instead of an equality test. Mapping reads of an unset key return the zero value (`false` for bool), so unknown addresses are denied by default: **default-deny**, the posture every bank security review demands. This hand-rolled pair is the essence of what OpenZeppelin's `AccessControl` generalizes (roles as `bytes32` identifiers, per-role admin roles) and what CMTAT's **AuthorizationModule** packages for tokenized securities. Assumption: CMTAT v2.x AuthorizationModule wraps OZ AccessControl — we re-derive the primitive here and build the full version in Chapter 05.

**Starter code:**
```solidity
    modifier onlyOperator() {

    }
```

**Solution:**
```solidity
    modifier onlyOperator() {
        if (!isOperator[msg.sender]) revert LedgerNotOperator(msg.sender);
        _;
    }
```

**Validation rule:** `modifier\s+onlyOperator\s*\(\s*\)\s*\{\s*if\s*\(\s*!\s*isOperator\[\s*msg\.sender\s*\]\s*\)\s*revert\s+LedgerNotOperator\s*\(\s*msg\.sender\s*\)\s*;\s*_\s*;\s*\}` — checks negated mapping lookup guarding the splice.

### Step 2.4 — Role administration: grant and revoke

**Instruction:** Add `grantOperator(address account)` and `revokeOperator(address account)`, both `external` and gated by `onlyAdmin`. `grantOperator` must reject the zero address with `LedgerZeroAccount()`.

**Explanation:** Applying a modifier is just naming it in the signature — declarative, like stacking annotations. Only admin can change the role table: in bank terms, only the IAM team mutates `ROLE_ASSIGNMENT`, and every mutation is itself an authorized operation. The zero-address check matters because `address(0)` is the EVM's `null` — granting it a role is always a fat-finger (a typo'd config value decoding to zero), so we validate at the boundary exactly as you validate inbound payment messages. Note `revokeOperator` does *not* zero-check: revoking a role from address zero is harmless and idempotent.

**Starter code:**
```solidity
    function grantOperator(address account) external ____ {
        // reject address(0), then grant

    }

    function revokeOperator(address account) external ____ {

    }
```

**Solution:**
```solidity
    function grantOperator(address account) external onlyAdmin {
        if (account == address(0)) revert LedgerZeroAccount(); // null-principal guard
        isOperator[account] = true;
    }

    function revokeOperator(address account) external onlyAdmin {
        isOperator[account] = false; // idempotent: revoking twice is a no-op
    }
```

**Validation rule:** `function\s+grantOperator\s*\(\s*address\s+account\s*\)\s+external\s+onlyAdmin[\s\S]*function\s+revokeOperator\s*\(\s*address\s+account\s*\)\s+external\s+onlyAdmin` — checks both functions exist and carry the `onlyAdmin` modifier.

### Step 2.5 — Map the gates to the bank's org chart

**Instruction:** Above the modifiers, add a comment block documenting the role mapping: admin → issuer/security-officer function, operator → booking/settlement operations. Include the words `four-eyes` and `Chapter 05`.

**Explanation:** On-chain roles are meaningless until mapped to your bank's organizational reality. The admin key belongs with whoever owns issuer authority (and in production is a multisig — the on-chain form of **four-eyes approval**, where a transaction needs M-of-N signatures before execution, like dual-control payment release). Operator keys live in the settlement system's HSM and sign the day-to-day booking traffic. Writing this mapping into the source is not decoration: in a FINMA audit (see Chapter 09), the reviewer reads the contract and must be able to trace each capability to an accountable human function. CMTAT formalizes this with distinct roles per module — `PAUSER_ROLE` for compliance halts, `ENFORCER_ROLE` for freezes — all built on the primitive you just wrote.

**Starter code:**
```solidity
    // ROLE MAPPING (bank org chart):
    // admin    -> ____
    // operator -> ____
```

**Solution:**
```solidity
    // ROLE MAPPING (bank org chart):
    // admin    -> issuer / security officer function; production key = multisig (four-eyes)
    // operator -> booking & settlement operations (HSM-held signing key)
    // Full RBAC with role hierarchies and per-role admins: Chapter 05.
```

**Validation rule:** `(?i)//[\s\S]*four-eyes[\s\S]*Chapter\s*05` — checks the role-mapping comment mentions four-eyes control and the forward reference.

> **Banking integration note:** Your Java adapter should *pre-flight* authorization before spending gas: call the public `isOperator(address)` getter via `eth_call` with the adapter's own signing address at startup. If it returns `false`, fail fast at boot — do not discover the missing role grant by burning gas on reverted transactions at 09:00 on settlement day.
> ```java
> Function fn = new Function("isOperator",
>     Collections.singletonList(new Address(adapterSigningAddress)),
>     Collections.singletonList(new TypeReference<Bool>() {}));
> EthCall resp = web3j.ethCall(Transaction.createEthCallTransaction(
>     null, contractAddress, FunctionEncoder.encode(fn)),
>     DefaultBlockParameterName.LATEST).send();
> Boolean granted = ((Bool) FunctionReturnDecoder.decode(
>     resp.getValue(), fn.getOutputParameters()).get(0)).getValue();
> if (!granted) throw new IllegalStateException("Adapter key lacks operator role");
> ```

---

## Lesson 3 — Events Are the Integration Contract

**Learning objective:** Design events as the bank's booking feed: choose indexed vs non-indexed parameters, carry idempotency keys and amounts in smallest units, understand log storage vs contract storage, and know why contracts can never read events.
**Emphasis tags:** `[BANK-heavy]` `[TYPES]`
**Track:** shared

This is the thesis lesson of the course. Hold this model: **the contract is the posting engine; the event log is the booking feed your bank consumes.** When a core-banking system integrates with anything — SWIFT, SIC, an internal GL — it consumes a message feed and reconciles. The EVM's message feed is the **event log**: an append-only, cryptographically anchored stream of structured records, written by contracts, readable by everyone, replayable from any point in history. You do not integrate with a smart contract by polling its storage. You integrate by consuming its events.

Three facts to internalize:

1. **Logs are not contract storage.** `emit` writes to a separate log data structure in the transaction receipt. Log data costs roughly 8 gas/byte vs storage's 20,000 gas per 32-byte slot — events are ~10× cheaper than the cheapest storage write. That asymmetry *is* the design driver: store the minimum, emit the maximum.
2. **Contracts cannot read events.** Not their own, not anyone's. There is no EVM opcode for it (logs exist for the outside world; nodes index them, contracts never see them). Events are strictly **write-only from inside, read-only from outside** — a one-way outbound queue, like a system that can publish to Kafka but has no consumer API. Anything the contract must *check* later (our dedup flag) must live in storage; anything only the *bank* needs can live in the log.
3. **Indexed parameters become topics.** Each log record has up to 4 *topics* (32-byte words the node indexes for filtering — your queryable columns) and a *data* section (ABI-encoded blob — your payload columns). `topic[0]` is always `keccak256` of the canonical event signature; up to 3 `indexed` parameters fill topics 1–3. Filters like "all bookings for account X" hit topics and are fast, server-side, native. Filtering on non-indexed data means downloading and decoding every log yourself.

### Step 3.1 — Declare `BookingRecorded`

**Instruction:** Declare the event: `event BookingRecorded(bytes32 indexed externalRef, address indexed account, uint256 amount, uint8 entryType);`

**Explanation:** Every field placement is a deliberate integration decision. `externalRef` indexed: the bank queries "did booking ref X land on-chain?" by topic — the reconciliation primary key. `account` indexed: "give me all bookings for this position account" — the statement query. `amount` NOT indexed: you never filter by exact amount, and for value types like uint256 it makes no query sense; worse, indexing a *dynamic* type (string/bytes) stores only its keccak hash in the topic — the actual value would be unrecoverable. `entryType` rides in data next to the amount it qualifies. Discipline: **3 topic slots are scarce — spend them on identity and party, never on quantities.** This mirrors CMTAT/ERC-20's `Transfer(address indexed from, address indexed to, uint256 value)`: parties indexed, amount in data.

**Starter code:**
```solidity
    // the booking feed record — choose which two params are indexed
    event BookingRecorded(
        bytes32 ____ externalRef,
        address ____ account,
        uint256 amount,
        uint8 entryType
    );
```

**Solution:**
```solidity
    // The integration contract with core banking. Indexed = queryable topics.
    event BookingRecorded(
        bytes32 indexed externalRef, // idempotency key — reconciliation lookups by topic
        address indexed account,     // position account — statement queries by topic
        uint256 amount,              // smallest units, data section
        uint8 entryType              // ENTRY_DEBIT / ENTRY_CREDIT, data section
    );
```

**Validation rule:** `event\s+BookingRecorded\s*\(\s*bytes32\s+indexed\s+externalRef\s*,\s*address\s+indexed\s+account\s*,\s*uint256\s+amount\s*,\s*uint8\s+entryType\s*\)\s*;` — checks exact event signature with exactly the first two parameters indexed.

> **Datatype/parser note:** How web3j sees this log: `externalRef` arrives as `topics[1]`, decoded with `Bytes32` → Java `byte[32]` (right-padded ASCII per course convention — trim trailing zeros to recover the String). `account` is `topics[2]`, decoded with `Address` → `String`. The data section ABI-encodes `(uint256, uint8)` together: `Uint256` → `BigInteger`, `Uint8` → `BigInteger` (narrow with `intValueExact()`). Indexed and non-indexed fields are decoded by *different* web3j calls — `decodeIndexedValue` per topic vs `FunctionReturnDecoder.decode` for the whole data blob — you will write both in Lesson 4.

### Step 3.2 — Named entry-type codes

**Instruction:** Add two public constants: `uint8 public constant ENTRY_DEBIT = 1;` and `uint8 public constant ENTRY_CREDIT = 2;`. Update `_isValidEntryType` to use them.

**Explanation:** Why `uint8` codes instead of a Solidity `enum`? An enum would *work* (it ABI-encodes as uint8), but explicit codes make the cross-system contract brutally unambiguous: the Java adapter, the Kotlin reporting job, and the auditor's Python script all hard-code `1 = DEBIT, 2 = CREDIT` against documentation, with no dependency on Solidity declaration order — renumber an enum by inserting a member and every off-chain decoder breaks silently. We also deliberately skip `0`: an uninitialized uint8 is `0`, so "zero means invalid" turns missing data into a loud revert instead of a silently-booked debit. Same reason your ISO 20022 code tables never assign meaning to empty. `constant` values are baked into bytecode at compile time — reading them costs no storage access, and `public` exposes free getters so off-chain systems can verify the code table on-chain.

**Starter code:**
```solidity
    uint8 public constant ENTRY_DEBIT = ____;
    uint8 public constant ENTRY_CREDIT = ____;

    function _isValidEntryType(uint8 entryType) internal pure returns (bool) {
        return ____;
    }
```

**Solution:**
```solidity
    // Explicit uint8 code table (not an enum): stable ABI for off-chain decoders.
    // 0 intentionally unassigned — uninitialized data must fail validation, not book a debit.
    uint8 public constant ENTRY_DEBIT = 1;
    uint8 public constant ENTRY_CREDIT = 2;

    function _isValidEntryType(uint8 entryType) internal pure returns (bool) {
        return entryType == ENTRY_DEBIT || entryType == ENTRY_CREDIT;
    }
```

**Validation rule:** `uint8\s+public\s+constant\s+ENTRY_DEBIT\s*=\s*1\s*;[\s\S]*uint8\s+public\s+constant\s+ENTRY_CREDIT\s*=\s*2\s*;[\s\S]*entryType\s*==\s*ENTRY_DEBIT\s*\|\|\s*entryType\s*==\s*ENTRY_CREDIT` — checks constants declared and used in the helper.

### Step 3.3 — Audit events for role changes

**Instruction:** Declare `event OperatorGranted(address indexed account, address indexed grantedBy);` and `event OperatorRevoked(address indexed account, address indexed revokedBy);`. Emit them at the end of `grantOperator` and `revokeOperator` respectively, passing `msg.sender` as the second argument.

**Explanation:** Course rule (Chapter Map convention): **every state change emits an event** — no exceptions, especially not for security-relevant changes. A role grant without an event is a permission change missing from the audit log; no bank control framework tolerates that. Both parties are indexed: "when did this address gain operator?" and "what did this admin grant?" are both topic queries. Emitting `grantedBy = msg.sender` puts the *acting* principal in the record — who did it, not just what happened. This is the on-chain twin of your IAM system's mandatory audit events, except append-only by physics rather than by policy. Convention also visible throughout CMTAT/OZ: `RoleGranted(role, account, sender)` carries the grantor for exactly this reason.

**Starter code:**
```solidity
    event OperatorGranted(____);
    event OperatorRevoked(____);

    function grantOperator(address account) external onlyAdmin {
        if (account == address(0)) revert LedgerZeroAccount();
        isOperator[account] = true;
        // emit here
    }

    function revokeOperator(address account) external onlyAdmin {
        isOperator[account] = false;
        // emit here
    }
```

**Solution:**
```solidity
    // Role-change audit feed — cf. OZ AccessControl's RoleGranted/RoleRevoked
    event OperatorGranted(address indexed account, address indexed grantedBy);
    event OperatorRevoked(address indexed account, address indexed revokedBy);

    function grantOperator(address account) external onlyAdmin {
        if (account == address(0)) revert LedgerZeroAccount();
        isOperator[account] = true;
        emit OperatorGranted(account, msg.sender); // acting principal in the record
    }

    function revokeOperator(address account) external onlyAdmin {
        isOperator[account] = false;
        emit OperatorRevoked(account, msg.sender);
    }
```

**Validation rule:** `emit\s+OperatorGranted\s*\(\s*account\s*,\s*msg\.sender\s*\)\s*;[\s\S]*emit\s+OperatorRevoked\s*\(\s*account\s*,\s*msg\.sender\s*\)\s*;` — checks both emits with the acting principal as second argument.

### Step 3.4 — Emit the genesis grant from the constructor

**Instruction:** Add `emit OperatorGranted(msg.sender, msg.sender);` as the last line of the constructor.

**Explanation:** Without this, the deployer's operator role exists in storage but is invisible to any system that reconstructs the role table from the event feed — your bank's access-review job would report a phantom operator it never saw granted. Rule: **if a consumer replays your events from block 0, it must reach the exact current state.** That property — the event stream as the source of truth from which state is derivable — is event sourcing, and it is precisely how your adapter will rebuild its mirror after a restart. Self-grant (`grantedBy == account`) is honest: the genesis record documents the bootstrap, the way the first row in an audit table is the system installer creating itself.

**Starter code:**
```solidity
    constructor() {
        admin = msg.sender;
        isOperator[msg.sender] = true;
        // make the bootstrap grant visible on the feed

    }
```

**Solution:**
```solidity
    constructor() {
        admin = msg.sender;
        isOperator[msg.sender] = true;
        emit OperatorGranted(msg.sender, msg.sender); // genesis audit record: replay-from-0 completeness
    }
```

**Validation rule:** `constructor\s*\(\s*\)\s*\{[\s\S]*emit\s+OperatorGranted\s*\(\s*msg\.sender\s*,\s*msg\.sender\s*\)\s*;[\s\S]*\}` — checks the constructor emits the bootstrap grant.

### Step 3.5 — Write the event-design rules into the contract header

**Instruction:** Above the contract declaration, add a NatSpec comment block stating the three event-feed design rules: (1) every event carries an idempotency key (`externalRef`), (2) amounts are uint256 in smallest units, (3) indexed = query keys, non-indexed = payload.

**Explanation:** These three rules are the deliverable of this lesson — they govern every event you will design in the bond, equity, and MMF tracks. Rule 1: an event without a bank-side reference cannot be reconciled; `externalRef` is the join key between the chain feed and the core-banking booking store (and because the contract dedups on it, it doubles as the idempotency key). Rule 2: amounts as smallest-unit integers — formatting is a display concern; the feed carries `BigInteger`-safe raw values, never `1255.50`. Rule 3: spend topics on identity and party. Writing the rules into the source makes them part of the reviewed artifact; the next engineer extending this contract inherits the policy with the code, like an ADR embedded where it cannot be lost.

**Starter code:**
```solidity
/// @title EventDrivenLedger — booking entries as an event feed
/// Event-feed design rules:
///  1. ____
///  2. ____
///  3. ____
contract EventDrivenLedger {
```

**Solution:**
```solidity
/// @title EventDrivenLedger — booking entries as an event feed (Course Chapter 03)
/// @notice The event log IS the ledger feed. Storage keeps only what the EVM
///         must enforce: access control + idempotency dedup.
/// Event-feed design rules (the integration contract with core banking):
///  1. Every event carries an idempotency key: `externalRef` (bytes32, bank-assigned).
///  2. Amounts are uint256 in smallest units — never floats, never formatted strings.
///  3. `indexed` fields = what the bank queries by (topics); non-indexed = payload (data).
contract EventDrivenLedger {
```

**Validation rule:** `(?i)///[\s\S]*idempotency[\s\S]*smallest\s+units[\s\S]*indexed` — checks the header comment covers all three design rules.

### Step 3.6 — Compute topic0: the event's wire identity

**Instruction:** Above the `BookingRecorded` declaration, add the comment: `// topic0 = keccak256("BookingRecorded(bytes32,address,uint256,uint8)")`.

**Explanation:** Off-chain filtering starts from `topic0` — the keccak256 hash of the **canonical signature**: event name, parenthesized parameter *types* only, no spaces, no parameter names, no `indexed` keyword, no `uint` aliases (always `uint256`, `uint8`). Get one character wrong and your filter matches nothing, *silently* — the single most common "the adapter sees no events" incident, the EVM version of binding a JMS consumer to a misspelled topic name. You never compute the hash by hand: web3j's `EventEncoder.encode(event)` derives it from the typed `Event` declaration, which is why the Java mirror in Lesson 4 must replicate the parameter types exactly. Pinning the canonical string in a comment next to the declaration gives the off-chain team a copy-paste-safe reference and makes signature drift visible in code review.

**Starter code:**
```solidity
    // topic0 = keccak256("____")
    event BookingRecorded(
```

**Solution:**
```solidity
    // topic0 = keccak256("BookingRecorded(bytes32,address,uint256,uint8)")
    event BookingRecorded(
```

**Validation rule:** `keccak256\("BookingRecorded\(bytes32,address,uint256,uint8\)"\)` — checks the canonical signature string is exact: no spaces, no names, full-width types.

> **Banking integration note:** Treat each event signature like an ISO 20022 message-type identifier: versioned, documented, and frozen once a downstream consumer exists. *Changing an event's parameters changes topic0* — old filters go quiet without any error. If you must evolve the schema, add a new event (`BookingRecordedV2`) and emit both during a migration window, exactly like running MT and MX message formats in parallel during a SWIFT migration.

---

## Lesson 4 — Build EventDrivenLedger End to End

**Learning objective:** Implement the idempotent `recordBooking` write path (checks → effects → emit) and build the web3j `EventLogParser` that replays, decodes, and reorg-safely consumes the feed.
**Emphasis tags:** `[BANK-heavy]` `[TYPES]`
**Track:** shared

Everything converges. On-chain: one write function that validates at the boundary, dedups on the bank's reference, and emits exactly one feed record per booking. Off-chain: a Java adapter that can rebuild the entire feed from any block — because logs, unlike Kafka topics with retention limits, never expire.

### Step 4.1 — The idempotency store

**Instruction:** Add the state variable `mapping(bytes32 => bool) public processed;`.

**Explanation:** The one piece of booking data that *must* live in expensive storage rather than the cheap log: the dedup flag. Why? Lesson 3, fact 2 — **the contract cannot read its own events**, so "have I seen this externalRef?" cannot be answered from the log; it must be answerable in storage at execution time. This mapping is the on-chain twin of the unique constraint on your booking table's external-reference column. Cost analysis: one storage slot (20,000 gas first write) buys you exactly-once semantics against a retrying adapter — cheap insurance compared to a double-booked CHF position. The split rule generalizes: *storage = what the contract must enforce; events = what the bank must know.*

**Starter code:**
```solidity
    // idempotency: which externalRefs have already been booked?
    mapping(____ => ____) public processed;
```

**Solution:**
```solidity
    // Idempotency dedup — the contract's UNIQUE constraint on the bank's booking reference.
    // Must be storage (not events): contracts cannot read their own logs.
    mapping(bytes32 => bool) public processed;
```

**Validation rule:** `mapping\s*\(\s*bytes32\s*=>\s*bool\s*\)\s+public\s+processed\s*;` — checks the dedup mapping type and visibility.

### Step 4.2 — `recordBooking`: validate at the boundary

**Instruction:** Declare `function recordBooking(bytes32 externalRef, address account, uint256 amount, uint8 entryType) external onlyOperator` and write the five guard checks: zero ref → `LedgerZeroRef()`, zero account → `LedgerZeroAccount()`, zero amount → `LedgerZeroAmount()`, invalid type (use `_isValidEntryType`) → `LedgerInvalidEntryType(entryType)`, already processed → `LedgerDuplicateRef(externalRef)`.

**Explanation:** This is your inbound message parser: every field validated before any state changes, like an MT/MX syntax-and-semantics check before a payment enters the posting engine. Order matters for diagnosis quality: structural validity first (ref, account, amount, type), business-state check last (duplicate). Each failure mode gets its own typed error so the adapter can branch — remember from Lesson 1: `LedgerDuplicateRef` means "already booked, stand down" (success-by-idempotency), the others mean "bad message or bad config, investigate." A revert refunds remaining gas and rolls back everything; failing fast on the cheapest checks first also minimizes the gas burned on garbage input.

**Starter code:**
```solidity
    function recordBooking(
        bytes32 externalRef,
        address account,
        uint256 amount,
        uint8 entryType
    ) external ____ {
        // five guard checks, each with its typed error

    }
```

**Solution:**
```solidity
    function recordBooking(
        bytes32 externalRef,
        address account,
        uint256 amount,
        uint8 entryType
    ) external onlyOperator {
        // checks — validate at the boundary, like a payment-message parser
        if (externalRef == bytes32(0)) revert LedgerZeroRef();
        if (account == address(0)) revert LedgerZeroAccount();
        if (amount == 0) revert LedgerZeroAmount();
        if (!_isValidEntryType(entryType)) revert LedgerInvalidEntryType(entryType);
        if (processed[externalRef]) revert LedgerDuplicateRef(externalRef); // idempotency gate
    }
```

**Validation rule:** `function\s+recordBooking\s*\([\s\S]*?\)\s+external\s+onlyOperator[\s\S]*LedgerZeroRef\(\)[\s\S]*LedgerZeroAccount\(\)[\s\S]*LedgerZeroAmount\(\)[\s\S]*LedgerInvalidEntryType\s*\(\s*entryType\s*\)[\s\S]*LedgerDuplicateRef\s*\(\s*externalRef\s*\)` — checks the modifier and all five typed-error guards in order.

### Step 4.3 — Effects, then emit

**Instruction:** Complete `recordBooking`: set `processed[externalRef] = true;`, increment `bookingCount` inside an `unchecked` block, then `emit BookingRecorded(externalRef, account, amount, entryType);`.

**Explanation:** **Checks → effects → interactions/emit** — Solidity's canonical ordering (the discipline that prevents reentrancy bugs once external calls appear in later chapters; we adopt it now as habit). Mark the ref processed *before* anything else can observe state. The `unchecked` block disables 0.8's overflow check for the increment — justified and documented because a uint256 counter incremented once per booking cannot realistically overflow (10^77 bookings), and it shaves a small, *auditable* amount of gas; never use `unchecked` on arithmetic with user-supplied operands. The emit goes last: by the time the record hits the bank's feed, all invariants hold. One booking, one event, exactly once — enforced by the dedup gate above.

**Starter code:**
```solidity
        // effects
        ____;
        unchecked {
            ____;
        }

        // event last: the booking hits the bank's feed exactly once
        emit ____;
```

**Solution:**
```solidity
        // effects (state first — checks-effects-interactions discipline)
        processed[externalRef] = true;
        unchecked {
            bookingCount += 1; // counter can't overflow uint256 in any realistic timeline
        }

        // event last: the booking hits the bank's feed exactly once
        emit BookingRecorded(externalRef, account, amount, entryType);
    }
```

**Validation rule:** `processed\[\s*externalRef\s*\]\s*=\s*true\s*;[\s\S]*unchecked\s*\{\s*bookingCount\s*\+=\s*1\s*;[\s\S]*\}[\s\S]*emit\s+BookingRecorded\s*\(\s*externalRef\s*,\s*account\s*,\s*amount\s*,\s*entryType\s*\)\s*;` — checks effects-before-emit ordering and the exact emit arguments.

The contract is complete — the full assembled source is at the end of this chapter and in `contracts/shared/EventDrivenLedger.sol`. The remaining steps build the bank side.

### Step 4.4 — Java: mirror the event and derive topic0

**Instruction:** In `EventLogParser.java`, declare the web3j `Event` mirror of `BookingRecorded` — `Bytes32` (indexed), `Address` (indexed), `Uint256`, `Uint8` — and compute its topic with `EventEncoder.encode(...)`.

**Explanation:** web3j has no compiler checking your `Event` against the deployed contract — the mirror is a *convention contract*, like a hand-written JAXB binding for a message schema. The boolean in `new TypeReference<Bytes32>(true) {}` marks the parameter indexed; the **order and indexed flags must match the Solidity declaration exactly**, because web3j uses them to decide which values come from topics and which from the data blob. A wrong flag does not throw — it mis-slices the data and hands you garbage that often *looks* plausible (an amount decoded from an address's bytes). `EventEncoder.encode` canonicalizes the signature and keccak-hashes it: the same `topic0` you pinned in the Solidity comment in Step 3.6 — assert they match in a unit test.

**Starter code:**
```java
    /** Mirror of: event BookingRecorded(bytes32 indexed, address indexed, uint256, uint8) */
    public static final Event BOOKING_RECORDED = new Event(
            "BookingRecorded",
            Arrays.<TypeReference<?>>asList(
                    new TypeReference<Bytes32>(____) {},   // externalRef
                    new TypeReference<Address>(____) {},   // account
                    new TypeReference<Uint256>(____) {},   // amount
                    new TypeReference<Uint8>(____) {}      // entryType
            ));

    public static final String BOOKING_RECORDED_TOPIC = ____;
```

**Solution:**
```java
    /** Mirror of: event BookingRecorded(bytes32 indexed, address indexed, uint256, uint8) */
    public static final Event BOOKING_RECORDED = new Event(
            "BookingRecorded",
            Arrays.<TypeReference<?>>asList(
                    new TypeReference<Bytes32>(true) {},   // externalRef  (indexed -> topic[1])
                    new TypeReference<Address>(true) {},   // account      (indexed -> topic[2])
                    new TypeReference<Uint256>(false) {},  // amount       (data)
                    new TypeReference<Uint8>(false) {}     // entryType    (data)
            ));

    /** topic0 = keccak256("BookingRecorded(bytes32,address,uint256,uint8)") */
    public static final String BOOKING_RECORDED_TOPIC = EventEncoder.encode(BOOKING_RECORDED);
```

**Validation rule:** `new\s+TypeReference<Bytes32>\(true\)[\s\S]*new\s+TypeReference<Address>\(true\)[\s\S]*new\s+TypeReference<Uint256>\(false\)[\s\S]*new\s+TypeReference<Uint8>\(false\)[\s\S]*EventEncoder\.encode\(BOOKING_RECORDED\)` — checks indexed flags match the Solidity declaration and topic0 is derived, not hand-typed.

### Step 4.5 — Java: filter and fetch with `EthFilter`

**Instruction:** Write `fetchRange(BigInteger fromBlock, BigInteger toBlock)`: build an `EthFilter` over the contract address, add `BOOKING_RECORDED_TOPIC` with `addSingleTopic`, call `web3j.ethGetLogs(filter).send()`, and skip any log where `isRemoved()` is true.

**Explanation:** `EthFilter` = the WHERE clause executed server-side by the node: `address = contract AND topics[0] = topic0 AND block BETWEEN from AND to`. The node's log index does the work; you receive only matching records — never download all blocks and grep, the way you would never full-table-scan when an index exists. `addSingleTopic` pins topic0 (the event type); later, reconciliation queries can add `topics[1] = externalRef` to point-look-up one booking. The `isRemoved()` check: streaming filters can deliver a log and *later* deliver it again flagged removed when its block was reorged out — consuming a removed log as a booking is how phantom positions are born. For `eth_getLogs` over already-final ranges it should never trigger, but the guard is defense-in-depth and costs one branch.

**Starter code:**
```java
    private List<BookingEntry> fetchRange(BigInteger fromBlock, BigInteger toBlock) throws IOException {
        EthFilter filter = new EthFilter(
                DefaultBlockParameter.valueOf(____),
                DefaultBlockParameter.valueOf(____),
                ____);
        filter.____(BOOKING_RECORDED_TOPIC);

        EthLog response = web3j.ethGetLogs(filter).send();
        if (response.hasError()) {
            throw new IOException("eth_getLogs failed: " + response.getError().getMessage());
        }
        List<BookingEntry> out = new ArrayList<>();
        for (EthLog.LogResult<?> result : response.getLogs()) {
            Log log = (Log) result.get();
            // skip reorged-away logs
            ____
            out.add(decode(log));
        }
        return out;
    }
```

**Solution:**
```java
    /** One eth_getLogs call: filter = address + topic0, range [fromBlock, toBlock]. */
    private List<BookingEntry> fetchRange(BigInteger fromBlock, BigInteger toBlock) throws IOException {
        EthFilter filter = new EthFilter(
                DefaultBlockParameter.valueOf(fromBlock),
                DefaultBlockParameter.valueOf(toBlock),
                contractAddress);
        filter.addSingleTopic(BOOKING_RECORDED_TOPIC); // node-side filtering on topic0

        EthLog response = web3j.ethGetLogs(filter).send();
        if (response.hasError()) {
            throw new IOException("eth_getLogs failed: " + response.getError().getMessage());
        }
        List<BookingEntry> out = new ArrayList<>();
        for (EthLog.LogResult<?> result : response.getLogs()) {
            Log log = (Log) result.get();
            if (Boolean.TRUE.equals(log.isRemoved())) {
                continue; // reorged-away log — never book it
            }
            out.add(decode(log));
        }
        return out;
    }
```

**Validation rule:** `addSingleTopic\(BOOKING_RECORDED_TOPIC\)[\s\S]*ethGetLogs\(filter\)[\s\S]*isRemoved\(\)` — checks topic filtering, the getLogs call, and the removed-log guard.

### Step 4.6 — Java: decode indexed vs non-indexed fields

**Instruction:** Write `decode(Log log)`: extract `externalRef` from `topics[1]` and `account` from `topics[2]` via `FunctionReturnDecoder.decodeIndexedValue`, then decode `amount` and `entryType` together from `log.getData()` via `FunctionReturnDecoder.decode(...)` with `BOOKING_RECORDED.getNonIndexedParameters()`.

**Explanation:** Two physically different decode paths, because the log stores them differently. Indexed params: each is one standalone 32-byte topic — decode each topic individually (`topics[0]` is the signature hash; *your* params start at index 1). Non-indexed params: ABI-encoded *together* into the data blob, decoded in one shot into an ordered `List<Type>` — order is declaration order, position is meaning, exactly like a fixed-position MT field sequence. Type landings: `Bytes32.getValue()` → `byte[32]`, trim trailing zeros to recover the ASCII booking ref (course bytes32 convention); `Address.getValue()` → checksummed-hex `String`; `Uint256.getValue()` → `BigInteger`, kept raw in smallest units; `Uint8.getValue()` → `BigInteger`, narrowed with `intValueExact()` which *throws* on out-of-range rather than truncating — always prefer the exploding conversion at a money boundary.

**Starter code:**
```java
    public BookingEntry decode(Log log) {
        // indexed: one topic each, starting at topics[1]
        Bytes32 refRaw = (Bytes32) FunctionReturnDecoder.decodeIndexedValue(
                log.getTopics().get(____), new TypeReference<Bytes32>() {});
        Address accountRaw = (Address) FunctionReturnDecoder.decodeIndexedValue(
                log.getTopics().get(____), new TypeReference<Address>() {});

        // non-indexed: decoded together from the data blob, in declaration order
        List<Type> data = FunctionReturnDecoder.decode(
                ____, BOOKING_RECORDED.getNonIndexedParameters());
        BigInteger amount = ((Uint256) data.get(____)).getValue();
        int entryType = ((Uint8) data.get(____)).getValue().____();

        return new BookingEntry(bytes32ToAscii(refRaw.getValue()), accountRaw.getValue(),
                amount, entryType, log.getBlockNumber(), log.getTransactionHash(), log.getLogIndex());
    }
```

**Solution:**
```java
    public BookingEntry decode(Log log) {
        // --- indexed params: each one is its own topic; topics[0] = signature hash ---
        Bytes32 refRaw = (Bytes32) FunctionReturnDecoder.decodeIndexedValue(
                log.getTopics().get(1), new TypeReference<Bytes32>() {});
        Address accountRaw = (Address) FunctionReturnDecoder.decodeIndexedValue(
                log.getTopics().get(2), new TypeReference<Address>() {});

        // --- non-indexed params: one blob, decoded in declaration order ---
        List<Type> data = FunctionReturnDecoder.decode(
                log.getData(), BOOKING_RECORDED.getNonIndexedParameters());
        BigInteger amount = ((Uint256) data.get(0)).getValue();          // smallest units, raw
        int entryType = ((Uint8) data.get(1)).getValue().intValueExact(); // throws if out of range

        return new BookingEntry(bytes32ToAscii(refRaw.getValue()), accountRaw.getValue(),
                amount, entryType, log.getBlockNumber(), log.getTransactionHash(), log.getLogIndex());
    }
```

**Validation rule:** `decodeIndexedValue\([\s\S]*getTopics\(\)\.get\(1\)[\s\S]*decodeIndexedValue\([\s\S]*getTopics\(\)\.get\(2\)[\s\S]*FunctionReturnDecoder\.decode\(\s*log\.getData\(\)\s*,\s*BOOKING_RECORDED\.getNonIndexedParameters\(\)\)[\s\S]*intValueExact\(\)` — checks topics decoded from indices 1 and 2, data blob decoded against non-indexed params, and exploding narrowing for uint8.

> **Datatype/parser note:** Full mapping for this event, one line per field:
> | Solidity | log location | web3j type | Java landing | hazard |
> |---|---|---|---|---|
> | `bytes32 indexed externalRef` | `topics[1]` | `Bytes32` | `byte[32]` → trimmed `String` | right-padded ASCII; trim trailing `0x00` |
> | `address indexed account` | `topics[2]` | `Address` | `String` (0x-hex) | compare case-insensitively or checksum-normalize |
> | `uint256 amount` | data, slot 0 | `Uint256` | `BigInteger` | never `long`; format only at display edge |
> | `uint8 entryType` | data, slot 1 | `Uint8` | `BigInteger` → `int` | use `intValueExact()`, not `intValue()` |

### Step 4.7 — Java: replay for backfill with a reorg-safe head

**Instruction:** Write `safeHead()` (latest block minus `CONFIRMATION_DEPTH = 12`, floored at 0) and `replayFrom(BigInteger fromBlock)` — loop `fetchRange` in `CHUNK_SIZE = 5000`-block chunks up to the safe head.

**Explanation:** Two production necessities. **Reorg depth:** the newest blocks can be replaced if the network briefly forks — a log in them can vanish. Treat depth < 12 blocks as *provisional*, like an unconfirmed payment message before settlement finality; only book entries at or below `head − 12`. (12 is a mainnet-conservative default; tune per chain and risk appetite — Chapter 08 covers finality in depth, Chapter 09 the legal-finality view.) **Chunking:** RPC providers cap `eth_getLogs` ranges/response sizes; bounded chunks make backfill restartable and provider-friendly, exactly like paging a large database extract instead of one giant cursor. The replay loop is your disaster-recovery story: adapter down for a week? Read the last booked block from the bank's store, `replayFrom(lastBooked + 1)`, and dedup on `txHash:logIndex` (each log's globally unique identity) during upsert. Logs never expire — the chain *is* the durable message archive.

**Starter code:**
```java
    public static final BigInteger CONFIRMATION_DEPTH = BigInteger.valueOf(____);
    private static final BigInteger CHUNK_SIZE = BigInteger.valueOf(5_000);

    public BigInteger safeHead() throws IOException {
        BigInteger head = web3j.____().send().getBlockNumber();
        BigInteger safe = head.subtract(____);
        return safe.signum() < 0 ? BigInteger.ZERO : safe;
    }

    public List<BookingEntry> replayFrom(BigInteger fromBlock) throws IOException {
        BigInteger safeHead = safeHead();
        List<BookingEntry> entries = new ArrayList<>();
        BigInteger start = fromBlock;
        while (start.compareTo(safeHead) <= 0) {
            BigInteger end = ____;
            entries.addAll(fetchRange(start, end));
            start = ____;
        }
        return entries;
    }
```

**Solution:**
```java
    /** Blocks below the chain head we treat as final. Tune per chain (see Chapter 08). */
    public static final BigInteger CONFIRMATION_DEPTH = BigInteger.valueOf(12);
    /** eth_getLogs range per request — keeps RPC responses bounded during backfill. */
    private static final BigInteger CHUNK_SIZE = BigInteger.valueOf(5_000);

    /** Chain head minus CONFIRMATION_DEPTH — logs above this line may still reorg away. */
    public BigInteger safeHead() throws IOException {
        BigInteger head = web3j.ethBlockNumber().send().getBlockNumber();
        BigInteger safe = head.subtract(CONFIRMATION_DEPTH);
        return safe.signum() < 0 ? BigInteger.ZERO : safe;
    }

    /** Backfill: replay the feed from fromBlock to the reorg-safe head, in bounded chunks. */
    public List<BookingEntry> replayFrom(BigInteger fromBlock) throws IOException {
        BigInteger safeHead = safeHead();
        List<BookingEntry> entries = new ArrayList<>();
        BigInteger start = fromBlock;
        while (start.compareTo(safeHead) <= 0) {
            BigInteger end = start.add(CHUNK_SIZE).min(safeHead); // bounded page
            entries.addAll(fetchRange(start, end));
            start = end.add(BigInteger.ONE); // next page starts after this one
        }
        return entries;
    }
```

**Validation rule:** `CONFIRMATION_DEPTH\s*=\s*BigInteger\.valueOf\(12\)[\s\S]*ethBlockNumber\(\)[\s\S]*subtract\(CONFIRMATION_DEPTH\)[\s\S]*start\.add\(CHUNK_SIZE\)\.min\(safeHead\)[\s\S]*end\.add\(BigInteger\.ONE\)` — checks reorg margin subtraction and correct non-overlapping chunk advancement.

> **Banking integration note:** The complete adapter is in `java-adapters/EventLogParser.java`. The reconciliation contract between chain and bank, summarized: **on-chain dedup key** = `externalRef` (the contract refuses double bookings); **off-chain dedup key** = `txHash:logIndex` (the adapter's upsert key, immune to replays and restarts); **join key** between the two worlds = `externalRef` again, matching the chain feed against the core-banking booking store. Reconciliation breaks (refs on one side only) are not exceptions to swallow — they are the daily control report. Chapter 08 builds the full `ReconciliationJob` around exactly this triad.

---

## The Full Assembled Contract

File: `contracts/shared/EventDrivenLedger.sol` — compiles standalone with `solc ^0.8.20` / `forge build`.

```solidity
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
```

The complete bank-side adapter is in `java-adapters/EventLogParser.java`.

---

## Chapter 03 Quiz

**Q1 (multiple choice).** Your reconciliation job calls `ledgerStatus()` every 30 seconds. What does this cost the bank in gas?
a) 21,000 gas per call b) Gas proportional to state read c) Nothing — `view` functions execute via `eth_call`, off-transaction d) Nothing, but only on archive nodes

**Answer: c.** `view`/`pure` functions invoked via `eth_call` run locally on a node without creating a transaction: no gas, no signature, no block inclusion. Gas costs apply only when a `view` function is called *inside* a state-changing transaction.

**Q2 (multiple choice).** Why can `recordBooking` NOT check for duplicates by reading its own past `BookingRecorded` events?
a) Events are pruned after 128 blocks b) Reading events costs too much gas c) The EVM has no opcode for contracts to read logs — events are write-only from inside d) Only indexed parameters are readable by contracts

**Answer: c.** Logs live in transaction receipts, outside the state a contract can access; no opcode exposes them to EVM execution. Anything a contract must check later (the dedup flag) must live in storage; events serve external consumers only.

**Q3 (short answer).** The Java adapter's filter uses topic0 computed from `"BookingRecorded(bytes32, address, uint256, uint8)"` and sees zero events, although bookings are confirmed on-chain. What is wrong, and what failure mode does it produce?

**Answer:** The canonical signature must contain no spaces: `"BookingRecorded(bytes32,address,uint256,uint8)"`. The keccak256 of the wrong string yields a different topic0, so the node-side filter matches nothing — *silently*: no error, no exception, just an empty feed. Always derive topic0 with `EventEncoder.encode(event)` from the typed declaration instead of hand-typing the string.

**Q4 (multiple choice).** Which parameter set is the better design for a dividend event consumed by the bank?
a) `DividendPaid(uint256 indexed amount, address account, string isin)`
b) `DividendPaid(bytes32 indexed paymentRef, address indexed account, uint256 amount)`
c) `DividendPaid(string indexed isin, uint256 amount)`
d) `DividendPaid(address account, uint256 amount)`

**Answer: b.** It carries an idempotency/external reference, indexes the two identity fields the bank queries by (ref, account), and keeps the amount in data as smallest-unit uint256. (a) wastes a topic indexing an amount; (c) indexing a `string` stores only its keccak hash — the ISIN value would be unrecoverable from the topic — and there is no idempotency key; (d) has no reference at all, so the event cannot be reconciled or deduplicated.

**Q5 (short answer).** The adapter retries a timed-out `recordBooking` transaction that had actually succeeded. What happens on-chain, and how must the adapter classify the result?

**Answer:** The retry reverts with `LedgerDuplicateRef(externalRef)` because `processed[externalRef]` is already `true` — no state changes, no second event, the first booking stands. The adapter must decode the typed error and classify it as *success-by-idempotency*: the booking exists, stop retrying, no alert. (Contrast `LedgerNotOperator`, which signals a key/role misconfiguration and should page operations.)

**Q6 (multiple choice).** In the modifier `onlyOperator() { if (!isOperator[msg.sender]) revert LedgerNotOperator(msg.sender); _; }`, what is the Java-world equivalent of `_;`?
a) `super.invoke()` in an overridden method b) `joinPoint.proceed()` in an `@Around` AOP advice c) `finally` block execution d) `this.run()` in a `Runnable`

**Answer: b.** A modifier is an interceptor wrapped around the function; `_;` is the splice point where the guarded body executes — exactly `proceed()` in Spring AOP. Code placed after `_;` would run post-execution, like an `@AfterReturning` advice.

**Q7 (short answer).** Name the two different dedup keys used in this chapter's design — one on-chain, one off-chain — and state why the off-chain side cannot simply rely on the on-chain one.

**Answer:** On-chain: `externalRef` (the contract reverts on a duplicate, guaranteeing at most one `BookingRecorded` per ref). Off-chain: `txHash:logIndex`, the globally unique identity of each log record, used as the upsert key in the bank's store. The adapter needs its own key because *it* can observe the same (perfectly valid, emitted-once) log multiple times — during replay/backfill, after restarts, or across overlapping filter windows — and must upsert idempotently regardless of how often it reads the feed.

**Q8 (multiple choice).** Why does `replayFrom` stop at `head − 12` instead of the latest block?
a) `eth_getLogs` cannot query the latest block b) The newest blocks may be replaced in a chain reorg, taking their logs with them — entries there are provisional, like pre-settlement payment messages c) Nodes only index logs after 12 blocks d) web3j caches block numbers for 12 blocks

**Answer: b.** Logs in very recent blocks can disappear if the network reorganizes; booking them risks phantom entries the bank would have to reverse. Treating `head − CONFIRMATION_DEPTH` as the bookable frontier mirrors waiting for settlement finality before posting. Depth is a per-chain risk parameter (Chapter 08; legal finality in Chapter 09).
