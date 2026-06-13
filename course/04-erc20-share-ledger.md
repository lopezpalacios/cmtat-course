# Chapter 04 — Building the ERC-20 Share Ledger

**Track:** `[shared]` · **Emphasis threads:** `[TYPES]` `[BANK]`
**Chapter learning objective:** Build a complete, minimal ERC-20 token from scratch — and understand it not as a "cryptocurrency" but as a **securities register**: the balances mapping is the position-keeping table, `Transfer` events are booking entries, `approve`/`transferFrom` is a power of attorney, and mint/burn are issuance and cancellation of capital.
**Prerequisites:** Chapter 01 (EVM mental model), Chapter 02 (Solidity datatypes), Chapter 03 (functions, modifiers, events).
**Contract built:** `contracts/shared/BankERC20.sol` · **Java adapter:** `java-adapters/Erc20Operations.java`

Everything in CMTAT sits on top of an ERC-20 core (CMTAT's `ERC20BaseModule`). Before you can reason about pause, freeze, snapshots, or rule engines, you must know — line by line — what the ERC-20 underneath actually does. So in this chapter you write one yourself.

---

## Lesson 4.1 — The ERC-20 Interface: A Standard Like ISO 20022

**Learning objective:** Understand ERC-20 (EIP-20) as a *message standard*: a fixed set of function signatures and event topics that every counterparty system integrates against — exactly the role ISO 20022 plays for payment messages. Declare the interface and the register's core storage.
**Emphasis tags:** `[TYPES]` `[BANK]` · **Track:** shared

Your bank does not invent a new wire format for every correspondent; it speaks ISO 20022 (`pacs.008` for credit transfers, `camt.053` for statements). EVM tokens work the same way: ERC-20 fixes the *function selectors* (the first 4 bytes of `keccak256` of the signature — think of it as the message type identifier) and the *event topics*. Any wallet, exchange, custodian, or CMTAT module that "supports ERC-20" hard-codes exactly these signatures. Deviate by one parameter type and the selector changes — like renaming a `pacs.008` tag: every counterparty parser breaks silently.

### Step 1.1 — Create the file and pragma
**Instruction:** Create `contracts/shared/BankERC20.sol`. Add the SPDX license identifier `MIT` and a pragma pinning the compiler to `^0.8.20`.
**Explanation:** The pragma is your compiler version constraint — same idea as `<maven.compiler.release>17</maven.compiler.release>` in a POM. `^0.8.20` means "0.8.20 or later, but below 0.9.0". Solidity 0.8.x matters specifically because arithmetic overflow *reverts by default* — before 0.8, `uint256` silently wrapped around like an unchecked Java `int` overflow. For a securities register, silent wraparound is an instant career-ending bug, so we require 0.8+.
**Starter code:**
```solidity
// SPDX-License-Identifier: ____
pragma solidity ____;
```
**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20; // 0.8+ => checked arithmetic by default (no silent wraparound)
```
**Validation rule:** `pragma\s+solidity\s+\^0\.8\.20\s*;` — checks the pragma pins `^0.8.20`.

### Step 1.2 — Declare the standard events in the interface
**Instruction:** Declare `interface IBankERC20` containing the two mandatory ERC-20 events: `Transfer(address indexed from, address indexed to, uint256 value)` and `Approval(address indexed owner, address indexed spender, uint256 value)`.
**Explanation:** An `interface` is exactly a Java `interface`: signatures, no bodies, no state. The two events are part of the standard — their *topic hashes* are what every block explorer and custody system filters on. `indexed` puts a parameter into the log's topics array so off-chain consumers can filter by it server-side (like an indexed column in your bookings table: `WHERE debtor_account = ?` without a full scan). `from`/`to` are indexed because the bank's most common query is "all movements for holder X"; `value` stays in the data section because nobody filters by exact amount.
**Starter code:**
```solidity
interface IBankERC20 {
    // booking entry: every balance movement emits this
    event Transfer(address ____ from, address ____ to, uint256 value);
    // mandate entry: owner grants spender a spending limit
    event ____(address indexed owner, address indexed spender, uint256 value);
}
```
**Solution:**
```solidity
interface IBankERC20 {
    /// The booking entry. Mint = Transfer(address(0), to, v); burn = Transfer(from, address(0), v).
    event Transfer(address indexed from, address indexed to, uint256 value);
    /// The mandate entry — the on-chain power-of-attorney record.
    event Approval(address indexed owner, address indexed spender, uint256 value);
}
```
**Validation rule:** `event\s+Transfer\(address\s+indexed\s+from,\s*address\s+indexed\s+to,\s*uint256\s+value\)` — checks the Transfer event matches the EIP-20 signature exactly (selector compatibility).



```checker
{"id": "ch04-l1-s2", "type": "regex", "pattern": "event\\s+Approval\\(address\\s+indexed\\s+owner,\\s+address\\s+indexed\\s+spender,\\s+uint256\\s+value\\);", "flags": "m", "target": "solidity", "error_hint": "Your code should match the solution for this step."}
```
### Step 1.3 — Declare the read-only register queries
**Instruction:** Inside the interface, add the six `view` functions: `name()`, `symbol()`, `decimals()`, `totalSupply()`, `balanceOf(address account)`, `allowance(address owner, address spender)` with their standard return types.
**Explanation:** These are your balance-inquiry messages — `camt.052`-style reads that change nothing. `view` is the compiler-enforced promise of no state mutation (like a `@Transactional(readOnly = true)` repository method, except the EVM *rejects* writes instead of trusting you). Note the return types: `string` for metadata, `uint8` for decimals (a count 0–255, no need for 32 bytes), `uint256` for every amount. Get a type wrong and the function selector — derived from the full signature — no longer matches what wallets call.
**Starter code:**
```solidity
    function name() external view returns (string memory);
    function symbol() external view returns (string memory);
    function decimals() external view returns (____);
    function totalSupply() external view returns (____);
    function balanceOf(address account) external view returns (uint256);
    function allowance(address owner, address spender) external view returns (____);
```
**Solution:**
```solidity
    function name() external view returns (string memory);
    function symbol() external view returns (string memory);
    function decimals() external view returns (uint8);     // precision indicator, fits in 1 byte
    function totalSupply() external view returns (uint256); // issued capital, base units
    function balanceOf(address account) external view returns (uint256);
    function allowance(address owner, address spender) external view returns (uint256);
```
**Validation rule:** `function\s+decimals\(\)\s+external\s+view\s+returns\s*\(uint8\)` — checks decimals() returns uint8 per EIP-20.



```checker
{"id": "ch04-l1-s3", "type": "regex", "pattern": "function\\s+allowance\\(address\\s+owner,\\s+address\\s+spender\\)\\s+external\\s+view\\s+returns\\s+\\(uint256\\);", "flags": "m", "target": "solidity", "error_hint": "Your code should match the solution for this step."}
```
### Step 1.4 — Declare the state-changing booking instructions
**Instruction:** Add the three mutating functions to the interface: `transfer(address to, uint256 value)`, `approve(address spender, uint256 value)`, `transferFrom(address from, address to, uint256 value)` — each returning `bool`.
**Explanation:** These are your booking instructions — the `pacs.008` equivalents. All three return `bool` per the standard. Historical quirk: early tokens returned `false` on failure instead of reverting; modern practice (OpenZeppelin, CMTAT) is *revert on failure, return true on success* — like throwing an exception versus returning an error code. The `bool` stays in the signature for selector compatibility, the way a legacy field stays in a message schema. Off-chain callers must still check it, because they may interact with old non-reverting tokens.
**Starter code:**
```solidity
    function transfer(address to, uint256 value) external returns (____);
    function approve(address spender, uint256 value) external returns (bool);
    function transferFrom(address ____, address ____, uint256 value) external returns (bool);
```
**Solution:**
```solidity
    function transfer(address to, uint256 value) external returns (bool);
    function approve(address spender, uint256 value) external returns (bool);
    // spender (msg.sender) moves value from `from` to `to` under a prior mandate
    function transferFrom(address from, address to, uint256 value) external returns (bool);
```
**Validation rule:** `function\s+transferFrom\(address\s+from,\s*address\s+to,\s*uint256\s+value\)\s+external\s+returns\s*\(bool\)` — checks transferFrom keeps the standard three-argument bool-returning signature.



```checker
{"id": "ch04-l1-s4", "type": "regex", "pattern": "function\\s+transferFrom\\(address\\s+from,\\s+address\\s+to,\\s+uint256\\s+value\\)\\s+external\\s+returns\\s+\\(bool\\);", "flags": "m", "target": "solidity", "error_hint": "Your code should match the solution for this step."}
```
### Step 1.5 — The register: balances mapping and total supply
**Instruction:** Declare `contract BankERC20 is IBankERC20` with two private state variables: `mapping(address => uint256) private _balances;` and `uint256 private _totalSupply;`. Implement `totalSupply()` and `balanceOf(address)` as `public view` functions returning them.
**Explanation:** This mapping IS the share register. In your core-banking system the register is a `positions` table keyed by account number; here it is a key-value store keyed by a 20-byte address, living in the contract's storage trie. Two crucial differences from a DB table: (1) you cannot iterate a mapping — there is no `SELECT * FROM positions`; if the bank needs the full holder list, it rebuilds it off-chain from `Transfer` events (Lesson 4.2's Java adapter does exactly this); (2) every key "exists" with default value `0` — no null rows, no `Optional`. The core invariant you will maintain through the whole chapter: **`_totalSupply` equals the sum of all `_balances` entries at all times** — the on-chain version of "the register reconciles to issued capital."
**Starter code:**
```solidity
contract BankERC20 is IBankERC20 {
    // the register: holder => position in base units
    mapping(____ => ____) private _balances;
    // issued capital; invariant: == sum of all _balances
    uint256 private _totalSupply;

    function totalSupply() public view returns (uint256) {
        return ____;
    }

    function balanceOf(address account) public view returns (uint256) {
        return ____;
    }
}
```
**Solution:**
```solidity
contract BankERC20 is IBankERC20 {
    /// The register: holder address => position (base units).
    /// Core-banking analogue: the position-keeping table, keyed by address.
    mapping(address => uint256) private _balances;

    /// Issued capital. Invariant: == sum(_balances) at all times.
    uint256 private _totalSupply;

    function totalSupply() public view returns (uint256) {
        return _totalSupply;
    }

    function balanceOf(address account) public view returns (uint256) {
        return _balances[account]; // unknown holder => 0, never null
    }
}
```
**Validation rule:** `mapping\(address\s*=>\s*uint256\)\s+private\s+_balances\s*;` — checks the register mapping is declared address-to-uint256 and private.

> **Banking integration note:** The bank never trusts its cached copy of the register — `eth_call` against `balanceOf` is the authoritative read, like querying the CSD rather than your mirror. In web3j 4.x, with no generated wrapper:
> ```java
> Function fn = new Function("balanceOf",
>         List.of(new Address(holderAddress)),
>         List.of(new TypeReference<Uint256>() {}));
> EthCall resp = web3j.ethCall(
>         Transaction.createEthCallTransaction(bankAddress, contractAddress,
>                 FunctionEncoder.encode(fn)),
>         DefaultBlockParameterName.LATEST).send();
> BigInteger baseUnits = ((Uint256) FunctionReturnDecoder
>         .decode(resp.getValue(), fn.getOutputParameters()).get(0)).getValue();
> ```
> `eth_call` is free (no gas, no transaction) — it is the read-only inquiry channel.

> **Datatype/parser note:** Solidity `uint256` ⟷ web3j `Uint256` ⟷ Java `BigInteger`. Never `long`: `uint256` tops out near 1.16×10⁷⁷, while `Long.MAX_VALUE` ≈ 9.2×10¹⁸. Solidity `address` ⟷ web3j `Address` ⟷ Java `String` ("0x" + 40 hex chars). Solidity `uint8` (from `decimals()`) ⟷ web3j `Uint8` ⟷ `BigInteger`, safely narrowed with `intValueExact()`.

---

## Lesson 4.2 — `transfer` and the `Transfer` Event: Booking Semantics

**Learning objective:** Implement the internal booking engine `_update` and the public `transfer` function; understand zero-address rules, atomic debit/credit, and the `Transfer` event as the booking entry the bank reconciles against.
**Emphasis tags:** `[BANK]` · **Track:** shared

A transfer is a two-legged booking: debit the sender, credit the receiver, same amount, atomically. In your core-banking system that is a DB transaction around two row updates plus a journal entry. On the EVM, atomicity is free — a reverted transaction rolls back *all* state changes, like an automatic `ROLLBACK` — and the journal entry is the `Transfer` event.



```checker
{"id": "ch04-l1-s5", "type": "regex", "pattern": "mapping\\(address\\s+=>\\s+uint256\\)\\s+private\\s+_balances;", "flags": "m", "target": "solidity", "error_hint": "Your code should match the solution for this step."}
```
### Step 2.1 — Declare the transfer errors
**Instruction:** Inside the contract, declare three custom errors: `BankERC20InvalidSender(address sender)`, `BankERC20InvalidReceiver(address receiver)`, and `BankERC20InsufficientBalance(address sender, uint256 balance, uint256 needed)`.
**Explanation:** Custom errors (Chapter 03) are typed exceptions — like defining `InsufficientFundsException(account, balance, requested)` instead of throwing `RuntimeException("oops")`. They are ABI-encoded on revert, so the Java side can decode *which* check failed and with what values, and they cost far less gas than revert strings. Naming convention: prefix with the contract name (`BankERC20...`) — same reason your bank prefixes internal error codes by subsystem, and it keeps names unique when all course chapters compile together.
**Starter code:**
```solidity
    error BankERC20InvalidSender(address sender);
    error ____(address receiver);
    error BankERC20InsufficientBalance(address sender, uint256 ____, uint256 needed);
```
**Solution:**
```solidity
    error BankERC20InvalidSender(address sender);
    error BankERC20InvalidReceiver(address receiver);
    /// carries balance + needed so ops can see the shortfall without a debugger
    error BankERC20InsufficientBalance(address sender, uint256 balance, uint256 needed);
```
**Validation rule:** `error\s+BankERC20InsufficientBalance\(address\s+sender,\s*uint256\s+balance,\s*uint256\s+needed\)\s*;` — checks the insufficient-balance error carries sender, balance, and needed amount.



```checker
{"id": "ch04-l2-s1", "type": "regex", "pattern": "error\\s+BankERC20InsufficientBalance\\(address\\s+sender,\\s+uint256\\s+balance,\\s+uint256\\s+needed\\);", "flags": "m", "target": "solidity", "error_hint": "Your code should match the solution for this step."}
```
### Step 2.2 — The booking engine: `_update`
**Instruction:** Write `function _update(address from, address to, uint256 value) internal`. If `from` is the zero address, add `value` to `_totalSupply` (checked); otherwise verify `_balances[from] >= value` (revert with `BankERC20InsufficientBalance` if not) and debit it inside `unchecked`. If `to` is the zero address, subtract `value` from `_totalSupply` inside `unchecked`; otherwise credit `_balances[to]` inside `unchecked`. Emit `Transfer(from, to, value)` at the end.
**Explanation:** This is the OpenZeppelin 5.x pattern (`// modeled on OZ ERC20._update`), and it is the *single* booking engine for transfers, mints, and burns: `address(0)` as `from` means issuance, `address(0)` as `to` means cancellation. One code path = one place to attach compliance hooks later (CMTAT's ValidationModule wraps exactly this choke point). The `unchecked` blocks are a deliberate optimization, each justified by an invariant: the debit can't underflow because we just checked the balance; the credit can't overflow because no balance can exceed `_totalSupply`, whose *checked* add is the single overflow gate. This is "validate at the boundary, trust internally" — the same discipline as validating an inbound message once, then trusting the parsed object.
**Starter code:**
```solidity
    function _update(address from, address to, uint256 value) internal {
        if (from == address(0)) {
            _totalSupply += value; // checked: caps total issuance
        } else {
            uint256 fromBalance = _balances[from];
            if (fromBalance < value) {
                revert ____(from, fromBalance, value);
            }
            unchecked {
                _balances[from] = ____;
            }
        }

        if (to == address(0)) {
            unchecked {
                _totalSupply -= value;
            }
        } else {
            unchecked {
                _balances[to] += ____;
            }
        }

        emit ____(from, to, value);
    }
```
**Solution:**
```solidity
    /// Single booking engine for transfer/mint/burn.
    /// from == address(0) => issuance; to == address(0) => cancellation.
    function _update(address from, address to, uint256 value) internal {
        if (from == address(0)) {
            _totalSupply += value; // checked add: the one overflow gate
        } else {
            uint256 fromBalance = _balances[from];
            if (fromBalance < value) {
                revert BankERC20InsufficientBalance(from, fromBalance, value);
            }
            unchecked {
                _balances[from] = fromBalance - value; // safe: just checked
            }
        }

        if (to == address(0)) {
            unchecked {
                _totalSupply -= value; // safe: value came out of a real balance
            }
        } else {
            unchecked {
                _balances[to] += value; // safe: bounded by _totalSupply
            }
        }

        emit Transfer(from, to, value); // THE booking entry
    }
```
**Validation rule:** `function\s+_update\(address\s+from,\s*address\s+to,\s*uint256\s+value\)\s+internal[\s\S]*emit\s+Transfer\(from,\s*to,\s*value\);` — checks _update exists with the standard signature and ends by emitting Transfer.



```checker
{"id": "ch04-l2-s2", "type": "regex", "pattern": "revert\\s+BankERC20InsufficientBalance\\(from,\\s+fromBalance,\\s+value\\);", "flags": "m", "target": "solidity", "error_hint": "Your code should match the solution for this step."}
```
### Step 2.3 — Zero-address guards: `_transfer`
**Instruction:** Write `function _transfer(address from, address to, uint256 value) internal` that reverts with `BankERC20InvalidSender(address(0))` if `from` is zero, reverts with `BankERC20InvalidReceiver(address(0))` if `to` is zero, then calls `_update(from, to, value)`.
**Explanation:** `address(0)` (`0x000...000`) is not a real account — nobody holds its private key. Tokens sent there are irrecoverably destroyed, which is why the standard reserves it as the mint/burn marker. A *holder-to-holder* transfer must never touch it: transferring **to** zero would silently destroy a client's shares (an unauthorized capital reduction!), and a zero **from** would be a spoofed issuance. Think of `address(0)` as a suspense account that only the issuance/cancellation processes may post against — `_transfer` is the client-payments channel, and it blocks that account on both legs.
**Starter code:**
```solidity
    function _transfer(address from, address to, uint256 value) internal {
        if (from == ____) revert BankERC20InvalidSender(address(0));
        if (to == ____) revert BankERC20InvalidReceiver(address(0));
        _update(____, ____, ____);
    }
```
**Solution:**
```solidity
    /// Holder-to-holder leg: zero address blocked on both sides.
    function _transfer(address from, address to, uint256 value) internal {
        if (from == address(0)) revert BankERC20InvalidSender(address(0));
        if (to == address(0)) revert BankERC20InvalidReceiver(address(0)); // would destroy shares
        _update(from, to, value);
    }
```
**Validation rule:** `if\s*\(to\s*==\s*address\(0\)\)\s*revert\s+BankERC20InvalidReceiver\(address\(0\)\);` — checks the receiver zero-address guard is present.



```checker
{"id": "ch04-l2-s3", "type": "regex", "pattern": "if\\s+\\(from\\s+==\\s+address\\(0\\)\\)\\s+revert\\s+BankERC20InvalidSender\\(address\\(0\\)\\);", "flags": "m", "target": "solidity", "error_hint": "Your code should match the solution for this step."}
```
### Step 2.4 — The public `transfer`
**Instruction:** Implement `function transfer(address to, uint256 value) public returns (bool)`: call `_transfer(msg.sender, to, value)` and `return true`.
**Explanation:** `msg.sender` is the cryptographically authenticated caller — the EVM's equivalent of the authenticated principal in your security context, except it is established by the transaction signature, not a session token, so it cannot be spoofed (Chapter 01). The sender of a booking is *always* the signer; there is no parameter for it, which is what makes `transfer` safe by construction. Returning `true` unconditionally is correct in the revert-on-failure style: if you reach the `return`, every check has passed — like a method that either throws or succeeds, with the `boolean` kept for interface compatibility.
**Starter code:**
```solidity
    function transfer(address to, uint256 value) public returns (bool) {
        _transfer(____, to, value);
        return ____;
    }
```
**Solution:**
```solidity
    function transfer(address to, uint256 value) public returns (bool) {
        _transfer(msg.sender, to, value); // sender = authenticated signer, never a parameter
        return true;                      // failures revert; reaching here means success
    }
```
**Validation rule:** `_transfer\(msg\.sender,\s*to,\s*value\);\s*return\s+true;` — checks transfer debits msg.sender and returns true.



```checker
{"id": "ch04-l2-s4", "type": "regex", "pattern": "_transfer\\(msg\\.sender,\\s+to,\\s+value\\);", "flags": "m", "target": "solidity", "error_hint": "Your code should match the solution for this step."}
```
### Step 2.5 — Read the booking entries from Java
**Instruction:** No new Solidity. Study the event-consumption pattern below — your `Erc20Operations.java` adapter implements it in full — and answer for yourself: why does the consumer persist the last processed block number?
**Explanation:** The bank's shadow ledger is rebuilt from `Transfer` events, exactly like rebuilding positions from a journal. The event stream is your `camt.054` notification feed: each log carries the transaction hash and log index — a globally unique booking reference, your idempotency key. Persisting the last processed block lets a crashed consumer *replay from block N* instead of losing entries — the same checkpoint/restart discipline as a message-queue consumer committing offsets. Mint and burn appear on this same stream as transfers from/to the zero address, so one consumer covers issuance, cancellation, and trading.
**Starter code:**
```solidity
// (no Solidity in this step — Java-side reading exercise)
```
**Solution:**
```solidity
// Key takeaways encoded in Erc20Operations.java:
// 1. topics[0] = event signature hash; topics[1]/topics[2] = indexed from/to.
// 2. value is ABI-decoded from the data field (non-indexed).
// 3. txHash + logIndex = unique booking reference (idempotency key).
// 4. Replay-from-block N makes the consumer crash-restartable.
```
**Validation rule:** `topics\[0\]|logIndex|replay` — checks the learner's notes mention topic layout, log index, or replay (free-form step).

> **Banking integration note:** The decoding core from `Erc20Operations.postTransferToLedger` (web3j 4.x):
> ```java
> Event TRANSFER = new Event("Transfer", List.of(
>         new TypeReference<Address>(true) {},    // from  -> topics[1]
>         new TypeReference<Address>(true) {},    // to    -> topics[2]
>         new TypeReference<Uint256>(false) {})); // value -> data
> EthFilter filter = new EthFilter(DefaultBlockParameter.valueOf(lastProcessedBlock),
>         DefaultBlockParameterName.LATEST, contractAddress);
> filter.addSingleTopic(EventEncoder.encode(TRANSFER));
> web3j.ethLogFlowable(filter).subscribe(log -> {
>     String from = "0x" + log.getTopics().get(1).substring(26); // last 20 bytes
>     String to   = "0x" + log.getTopics().get(2).substring(26);
>     BigInteger value = ((Uint256) FunctionReturnDecoder
>             .decode(log.getData(), TRANSFER.getNonIndexedParameters()).get(0)).getValue();
>     ledger.merge(from, value.negate(), BigInteger::add); // debit
>     ledger.merge(to, value, BigInteger::add);            // credit
> });
> ```
> An indexed `address` topic is 32 bytes with the address right-aligned — hence `substring(26)` to skip "0x" + 24 zero-pad hex chars.

---

## Lesson 4.3 — `approve` / `transferFrom`: Delegated Authority

**Learning objective:** Implement the allowance mechanism and map it to bank concepts: `approve` = registering a power of attorney with a limit, `transferFrom` = the attorney executing under it, `allowance` = the remaining mandate. Understand the approve race caveat.
**Emphasis tags:** `[BANK]` · **Track:** shared

Why does ERC-20 need a second transfer path? Because contracts cannot be "pushed" into reacting to an incoming transfer. If a client wants an exchange contract to take her tokens as part of a trade, she first grants the exchange a mandate (`approve`), then the exchange *pulls* the tokens (`transferFrom`) during settlement. This is precisely a limited power of attorney, or a standing order with a cap: the principal sets the limit, the agent draws against it, every draw reduces the remaining mandate.

### Step 3.1 — The mandate register: nested mapping
**Instruction:** Declare `mapping(address => mapping(address => uint256)) private _allowances;` and implement `function allowance(address owner, address spender) public view returns (uint256)` returning the stored value.
**Explanation:** A nested mapping is a two-column composite key — in JPA terms, `Map<Owner, Map<Spender, Limit>>`, or a table with primary key `(owner, spender)` and a `remaining_limit` column. `_allowances[alice][exchange]` reads as "how much of Alice's position may the exchange still move." Like `_balances`, unset combinations read as `0` — no mandate by default, which is the right security posture (deny-by-default, like an empty entitlements table).
**Starter code:**
```solidity
    // owner => (spender => remaining mandate, base units)
    mapping(address => mapping(____ => ____)) private _allowances;

    function allowance(address owner, address spender) public view returns (uint256) {
        return ____;
    }
```
**Solution:**
```solidity
    /// The power-of-attorney register: owner => (spender => remaining mandate).
    mapping(address => mapping(address => uint256)) private _allowances;

    function allowance(address owner, address spender) public view returns (uint256) {
        return _allowances[owner][spender]; // no entry => 0 => no mandate (deny by default)
    }
```
**Validation rule:** `mapping\(address\s*=>\s*mapping\(address\s*=>\s*uint256\)\)\s+private\s+_allowances\s*;` — checks the nested allowance mapping is declared.



```checker
{"id": "ch04-l3-s1", "type": "regex", "pattern": "mapping\\(address\\s+=>\\s+mapping\\(address\\s+=>\\s+uint256\\)\\)\\s+private\\s+_allowances;", "flags": "m", "target": "solidity", "error_hint": "Your code should match the solution for this step."}
```
### Step 3.2 — Recording the mandate: `_approve`
**Instruction:** Add errors `BankERC20InvalidApprover(address approver)` and `BankERC20InvalidSpender(address spender)`. Write `function _approve(address owner, address spender, uint256 value) internal`: revert with the respective error if `owner` or `spender` is the zero address, then set `_allowances[owner][spender] = value` and emit `Approval(owner, spender, value)`.
**Explanation:** The zero-address checks mirror `_transfer`'s: a mandate from or to a nonexistent principal is meaningless and would only pollute the audit trail. Note that `_approve` *overwrites* — it does not add to the previous mandate. That is "set the standing-order limit to X," not "raise it by X" — and it is the root of the race caveat in the next step. The `Approval` event is the audit record: your compliance archive can reconstruct the full history of who authorized whom for how much, the same way you'd audit signing-authority changes.
**Starter code:**
```solidity
    error BankERC20InvalidApprover(address approver);
    error BankERC20InvalidSpender(address spender);

    function _approve(address owner, address spender, uint256 value) internal {
        if (owner == address(0)) revert ____(address(0));
        if (spender == address(0)) revert ____(address(0));
        _allowances[owner][spender] = ____;
        emit ____(owner, spender, value);
    }
```
**Solution:**
```solidity
    error BankERC20InvalidApprover(address approver);
    error BankERC20InvalidSpender(address spender);

    function _approve(address owner, address spender, uint256 value) internal {
        if (owner == address(0)) revert BankERC20InvalidApprover(address(0));
        if (spender == address(0)) revert BankERC20InvalidSpender(address(0));
        _allowances[owner][spender] = value; // OVERWRITE semantics — not additive
        emit Approval(owner, spender, value); // audit record of the mandate change
    }
```
**Validation rule:** `_allowances\[owner\]\[spender\]\s*=\s*value;\s*emit\s+Approval\(owner,\s*spender,\s*value\);` — checks _approve stores the mandate and emits Approval.



```checker
{"id": "ch04-l3-s2", "type": "regex", "pattern": "if\\s+\\(spender\\s+==\\s+address\\(0\\)\\)\\s+revert\\s+BankERC20InvalidSpender\\(address\\(0\\)\\);", "flags": "m", "target": "solidity", "error_hint": "Your code should match the solution for this step."}
```
### Step 3.3 — The public `approve` and the race caveat
**Instruction:** Implement `function approve(address spender, uint256 value) public returns (bool)`: call `_approve(msg.sender, spender, value)` and return `true`.
**Explanation:** Now the caveat every integrator must know. Suppose Alice approved an operator for 100, then decides to *reduce* it to 50 and sends `approve(operator, 50)`. The operator, watching the mempool (the queue of pending transactions), front-runs her: spends the old 100 *before* her transaction lands, then spends the fresh 50 after — total 150, more than Alice ever intended at once. The banking analogy: amending a power of attorney by fax while the attorney is standing at the counter — the order of execution at the counter decides. Mitigations: set to 0 first and *confirm on-chain* before setting the new value, or use increase/decrease-style functions (non-standard), or EIP-2612 permits (later chapters). Your bank-side adapter should treat allowance changes as a two-step confirmed workflow, never fire-and-forget.
**Starter code:**
```solidity
    function approve(address spender, uint256 value) public returns (bool) {
        _approve(____, spender, value);
        return true;
    }
```
**Solution:**
```solidity
    function approve(address spender, uint256 value) public returns (bool) {
        _approve(msg.sender, spender, value); // only the owner can set their own mandates
        return true;
    }
    // CAVEAT: changing a non-zero allowance to another non-zero value is
    // front-runnable (old + new can both be spent). Ops procedure:
    // approve(spender, 0) -> wait for confirmation -> approve(spender, newValue).
```
**Validation rule:** `_approve\(msg\.sender,\s*spender,\s*value\);` — checks approve binds the mandate to msg.sender as owner.



```checker
{"id": "ch04-l3-s3", "type": "regex", "pattern": "_approve\\(msg\\.sender,\\s+spender,\\s+value\\);", "flags": "m", "target": "solidity", "error_hint": "Your code should match the solution for this step."}
```
### Step 3.4 — Consuming the mandate: `_spendAllowance`
**Instruction:** Add error `BankERC20InsufficientAllowance(address spender, uint256 currentAllowance, uint256 needed)`. Write `function _spendAllowance(address owner, address spender, uint256 value) internal`: read the current allowance; if it is *not* `type(uint256).max`, revert with the error when it is below `value`, otherwise decrement it inside `unchecked`.
**Explanation:** Every draw reduces the remaining mandate — standard standing-order accounting. The `type(uint256).max` special case is an industry convention: the maximum `uint256` (~1.16×10⁷⁷) is treated as "unlimited mandate" and never decremented, saving a storage write (~5,000 gas) on every `transferFrom`. Compare with an uncapped power of attorney where you don't bother tracking a running total. For a *securities* register your compliance team will likely forbid unlimited mandates operationally — but the code pattern is standard, and CMTAT inherits it via OZ. The `unchecked` decrement is safe for the same reason as in `_update`: the comparison just above guarantees no underflow.
**Starter code:**
```solidity
    error BankERC20InsufficientAllowance(address spender, uint256 currentAllowance, uint256 needed);

    function _spendAllowance(address owner, address spender, uint256 value) internal {
        uint256 currentAllowance = _allowances[owner][spender];
        if (currentAllowance != ____) {
            if (currentAllowance < value) {
                revert ____(spender, currentAllowance, value);
            }
            unchecked {
                _allowances[owner][spender] = ____;
            }
        }
    }
```
**Solution:**
```solidity
    error BankERC20InsufficientAllowance(address spender, uint256 currentAllowance, uint256 needed);

    function _spendAllowance(address owner, address spender, uint256 value) internal {
        uint256 currentAllowance = _allowances[owner][spender];
        if (currentAllowance != type(uint256).max) { // max = unlimited mandate, never decremented
            if (currentAllowance < value) {
                revert BankERC20InsufficientAllowance(spender, currentAllowance, value);
            }
            unchecked {
                _allowances[owner][spender] = currentAllowance - value; // safe: just checked
            }
        }
    }
```
**Validation rule:** `if\s*\(currentAllowance\s*!=\s*type\(uint256\)\.max\)` — checks the unlimited-allowance short-circuit is present.



```checker
{"id": "ch04-l3-s4", "type": "regex", "pattern": "revert\\s+BankERC20InsufficientAllowance\\(spender,\\s+currentAllowance,\\s+value\\);", "flags": "m", "target": "solidity", "error_hint": "Your code should match the solution for this step."}
```
### Step 3.5 — The attorney executes: `transferFrom`
**Instruction:** Implement `function transferFrom(address from, address to, uint256 value) public returns (bool)`: call `_spendAllowance(from, msg.sender, value)`, then `_transfer(from, to, value)`, then return `true`.
**Explanation:** Order matters: consume the mandate *first*, then book the movement. Both happen in one atomic transaction, so there is no window where the mandate is spent but the booking missing (or vice versa) — the EVM gives you the all-or-nothing guarantee you'd build with a DB transaction. Note who is who: `msg.sender` is the *spender* (the attorney executing), `from` is the principal whose position moves. The balance check happens inside `_update`, the mandate check in `_spendAllowance` — two independent limits, both enforced, like checking both the account balance and the attorney's authority limit before releasing a payment.
**Starter code:**
```solidity
    function transferFrom(address from, address to, uint256 value) public returns (bool) {
        _spendAllowance(from, ____, value);
        _transfer(____, to, value);
        return true;
    }
```
**Solution:**
```solidity
    function transferFrom(address from, address to, uint256 value) public returns (bool) {
        _spendAllowance(from, msg.sender, value); // msg.sender = the executing attorney
        _transfer(from, to, value);               // both legs atomic with the mandate draw
        return true;
    }
```
**Validation rule:** `_spendAllowance\(from,\s*msg\.sender,\s*value\);\s*_transfer\(from,\s*to,\s*value\);` — checks transferFrom consumes the spender's allowance before booking the transfer.

> **Banking integration note:** When the bank acts as the *spender* (e.g. settling a client's exchange trade from a custody adapter), the Java side sends `transferFrom` exactly like `transfer` — only the encoded `Function` changes:
> ```java
> Function fn = new Function("transferFrom",
>         List.of(new Address(clientAddress), new Address(counterpartyAddress),
>                 new Uint256(baseUnits)),
>         Collections.emptyList());
> EthSendTransaction resp = txManager.sendTransaction(
>         gasPrice, gasLimit, contractAddress, FunctionEncoder.encode(fn), BigInteger.ZERO);
> ```
> Before sending, the adapter should `eth_call` `allowance(client, bankAddress)` and pre-validate the mandate — turning an expensive on-chain revert into a cheap off-chain rejection, the way you pre-validate a payment against limits before submitting it to the clearing system.

> **Datatype/parser note:** The `Approval` event decodes identically to `Transfer`: two indexed `Address` topics plus a `Uint256` data field. Pitfall: indexed topics are zero-padded to 32 bytes, so comparing a topic against a Java address string requires either the `substring(26)` strip or `org.web3j.abi.TypeDecoder`-style decoding — naive `equals` on the raw topic always fails.

---

## Lesson 4.4 — `decimals` and Monetary Representation

**Learning objective:** Choose and implement the right decimal precision for a securities register; convert between on-chain base units (`uint256`) and bank-side `BigDecimal` without rounding surprises.
**Emphasis tags:** `[TYPES]` · **Track:** shared

The EVM has no floating point and no decimal type — only integers. Every "amount" on chain is an integer count of *base units*, and `decimals` declares where the decimal point sits for display: `humanValue = baseUnits / 10^decimals`. You already live by this rule: core-banking systems store CHF amounts as integer *Rappen* (or 1/100ths) precisely to avoid `double` rounding. The EVM just makes the discipline mandatory.



```checker
{"id": "ch04-l3-s5", "type": "regex", "pattern": "_spendAllowance\\(from,\\s+msg\\.sender,\\s+value\\);", "flags": "m", "target": "solidity", "error_hint": "Your code should match the solution for this step."}
```
### Step 4.1 — Store decimals as an immutable
**Instruction:** Declare `uint8 private immutable _decimals;` plus `string private _name;` and `string private _symbol;` as contract state.
**Explanation:** `immutable` means: assigned once in the constructor, then baked into the deployed bytecode — reads cost no storage access (no `SLOAD`, ~2,100 gas saved per read), like a `final` field inlined by the JIT. An instrument's precision must never change after issuance (imagine repricing every position by 10× mid-life), so `immutable` both documents and enforces that. `name`/`symbol` stay as ordinary storage strings — `immutable` only supports value types (another difference from Java's `final`, which takes references too).
**Starter code:**
```solidity
    string private _name;
    string private _symbol;
    ____ private ____ _decimals;
```
**Solution:**
```solidity
    string private _name;            // e.g. "Helvetia AG Registered Shares"
    string private _symbol;          // e.g. "HELV"
    uint8 private immutable _decimals; // fixed at issuance, read from bytecode not storage
```
**Validation rule:** `uint8\s+private\s+immutable\s+_decimals\s*;` — checks decimals is a uint8 immutable.



```checker
{"id": "ch04-l4-s1", "type": "regex", "pattern": "uint8\\s+private\\s+immutable\\s+_decimals;", "flags": "m", "target": "solidity", "error_hint": "Your code should match the solution for this step."}
```
### Step 4.2 — Wire the constructor
**Instruction:** Write the constructor taking `(string memory name_, string memory symbol_, uint8 decimals_)` and assigning all three fields.
**Explanation:** The trailing-underscore parameter names avoid shadowing the state variables — the Solidity equivalent of `this.name = name` disambiguation, since Solidity has no `this.field` assignment syntax for that purpose. The constructor runs exactly once, at deployment, in the deployment transaction itself — closer to a DB migration's `INSERT` of reference data than to a Java constructor that runs per instance: there is only ever *one* instance of a deployed contract.
**Starter code:**
```solidity
    constructor(string memory name_, string memory symbol_, uint8 decimals_) {
        _name = ____;
        _symbol = ____;
        _decimals = ____;
    }
```
**Solution:**
```solidity
    constructor(string memory name_, string memory symbol_, uint8 decimals_) {
        _name = name_;
        _symbol = symbol_;
        _decimals = decimals_; // immutable: this is the only place it can be set
    }
```
**Validation rule:** `constructor\(string\s+memory\s+name_,\s*string\s+memory\s+symbol_,\s*uint8\s+decimals_\)` — checks the constructor takes name, symbol, decimals.



```checker
{"id": "ch04-l4-s2", "type": "regex", "pattern": "_decimals\\s+=\\s+decimals_;", "flags": "m", "target": "solidity", "error_hint": "Your code should match the solution for this step."}
```
### Step 4.3 — Metadata accessors
**Instruction:** Implement `name()`, `symbol()`, and `decimals()` as `public view` functions returning the stored fields.
**Explanation:** These satisfy the interface from Lesson 4.1. They are the instrument's static data record — what your securities-master file holds per ISIN. `decimals` is *display metadata only*: nothing in `_update` looks at it; the chain's arithmetic is pure integer regardless. Wallets, your Java adapter, and reporting layers apply it. Get this division of labor wrong (e.g. scaling amounts on-chain "for convenience") and you create double-scaling bugs — the classic cents-vs-francs interface mismatch.
**Starter code:**
```solidity
    function name() public view returns (string memory) { return ____; }
    function symbol() public view returns (string memory) { return ____; }
    function decimals() public view returns (uint8) { return ____; }
```
**Solution:**
```solidity
    function name() public view returns (string memory) { return _name; }
    function symbol() public view returns (string memory) { return _symbol; }
    function decimals() public view returns (uint8) { return _decimals; } // display metadata only
```
**Validation rule:** `function\s+decimals\(\)\s+public\s+view\s+returns\s*\(uint8\)\s*\{\s*return\s+_decimals;\s*\}` — checks decimals() returns the immutable field.



```checker
{"id": "ch04-l4-s3", "type": "regex", "pattern": "function\\s+symbol\\(\\)\\s+public\\s+view\\s+returns\\s+\\(string\\s+memory\\)\\s+\\{\\s+return\\s+_symbol;\\s+\\}", "flags": "m", "target": "solidity", "error_hint": "Your code should match the solution for this step."}
```
### Step 4.4 — Choose the precision: 0 vs 18
**Instruction:** No new code — decide the deployment parameter. For a registered-share token, deploy with `decimals_ = 0`. Write the one-line deployment comment in your contract header recording the decision and why.
**Explanation:** The decision rule for tokenized securities: **shares are indivisible legal units — use `decimals = 0`** so one base unit = one share, and fractional transfers are *unrepresentable* (the type system enforces what the articles of association say; CMTAT equity deployments commonly do exactly this). **Cash legs and cash-like instruments use high precision** — 18 is the EVM convention (ether itself is 18; stablecoins often 6) — because interest and price computations need sub-unit precision before final rounding. A bond token may use 0 (one unit = one note of CHF 1,000 par) while its coupon *payments* are computed in an 18- or 6-decimal cash token. Mixing the two without explicit scaling is the on-chain version of adding Rappen to Francs.
**Starter code:**
```solidity
// Deployment: new BankERC20("Helvetia AG Registered Shares", "HELV", ____);
// Rationale: ____
```
**Solution:**
```solidity
// Deployment: new BankERC20("Helvetia AG Registered Shares", "HELV", 0);
// Rationale: registered shares are indivisible legal units; decimals=0 makes
// fractional positions unrepresentable at the type level. Cash legs (coupon,
// dividend) settle in a separate high-decimals cash token (see Track chapters).
```
**Validation rule:** `new\s+BankERC20\(.*,\s*0\)` — checks the share deployment uses decimals = 0.

> **Datatype/parser note:** The lossless round-trip in Java (`Erc20Operations`):
> ```java
> // chain -> bank: BigInteger base units + decimals -> exact BigDecimal
> BigDecimal display = new BigDecimal(baseUnits, decimals); // scale=decimals, NO rounding ever
> // bank -> chain: BigDecimal -> base units; reject sub-unit fractions
> BigInteger out = amount.setScale(decimals).unscaledValue(); // throws ArithmeticException
> //                       ^ deliberately no RoundingMode: 1.5 shares on a
> //                         decimals=0 register must FAIL, not round.
> ```
> `new BigDecimal(BigInteger, int)` sets the scale directly — no division, no precision loss. The outbound `setScale(decimals)` *without* a `RoundingMode` is the rounding policy: refuse, don't round. Where rounding is genuinely required (coupon math, Chapter 11), the policy must be explicit and documented — Swiss practice typically `RoundingMode.HALF_UP` at the final cash step only.

---

## Lesson 4.5 — Mint and Burn: Issuance and Cancellation

**Learning objective:** Implement `_mint`/`_burn` primitives and registrar-gated external `mint`/`burn` with idempotency-keyed events; understand mint/burn as primary-market capital events distinct from secondary-market transfers.
**Emphasis tags:** `[BANK]` `[TYPES]` · **Track:** shared

Transfers move existing shares between holders — secondary market. Mint and burn *change issued capital* — primary market: a capital increase credits new shares to subscribers; a buyback-and-cancellation or redemption destroys them. In bank terms: transfers are client bookings; mint/burn are corporate actions executed by the registrar/transfer agent under board authority. That difference in gravity is why they get a privilege gate.

### Step 5.1 — The registrar gate
**Instruction:** Declare `address public immutable registrar;`, error `BankERC20UnauthorizedRegistrar(address caller)`, and `modifier onlyRegistrar()` that reverts with the error unless `msg.sender == registrar`. Assign `registrar = msg.sender;` in the constructor.
**Explanation:** This is the simplest possible access control: one privileged address, fixed at deployment — a single named signatory. A modifier (Chapter 03) is an around-advice: the `_;` is the join point where the guarded function body runs, like a `@PreAuthorize("hasRole('REGISTRAR')")` interceptor. Deliberately primitive: real deployments need multiple roles, grant/revoke, and four-eyes — Chapter 05 replaces this with the full role-based pattern (CMTAT's AuthorizationModule approach). Teaching note: shipping the simple version first, then upgrading, mirrors how you'd actually review the privilege model's evolution in an audit.
**Starter code:**
```solidity
    address public immutable registrar;

    error BankERC20UnauthorizedRegistrar(address caller);

    modifier onlyRegistrar() {
        if (msg.sender != ____) {
            revert BankERC20UnauthorizedRegistrar(____);
        }
        ____;
    }

    constructor(string memory name_, string memory symbol_, uint8 decimals_) {
        _name = name_;
        _symbol = symbol_;
        _decimals = decimals_;
        registrar = ____;
    }
```
**Solution:**
```solidity
    address public immutable registrar; // single transfer-agent key; full RBAC in Ch. 05

    error BankERC20UnauthorizedRegistrar(address caller);

    modifier onlyRegistrar() {
        if (msg.sender != registrar) {
            revert BankERC20UnauthorizedRegistrar(msg.sender);
        }
        _; // join point: guarded function body executes here
    }

    constructor(string memory name_, string memory symbol_, uint8 decimals_) {
        _name = name_;
        _symbol = symbol_;
        _decimals = decimals_;
        registrar = msg.sender; // deployer = registrar until Ch. 05 introduces roles
    }
```
**Validation rule:** `modifier\s+onlyRegistrar\(\)\s*\{[\s\S]*revert\s+BankERC20UnauthorizedRegistrar\(msg\.sender\);[\s\S]*_;` — checks the modifier reverts for non-registrar callers before the body placeholder.



```checker
{"id": "ch04-l5-s1", "type": "regex", "pattern": "revert\\s+BankERC20UnauthorizedRegistrar\\(msg\\.sender\\);", "flags": "m", "target": "solidity", "error_hint": "Your code should match the solution for this step."}
```
### Step 5.2 — Issuance/cancellation events with idempotency keys
**Instruction:** Declare two events: `Issued(address indexed to, uint256 value, bytes32 indexed operationId)` and `Cancelled(address indexed from, uint256 value, bytes32 indexed operationId)`.
**Explanation:** `_update` already emits the standard zero-address `Transfer` for mints and burns — wallets and explorers rely on that. These *additional* events exist for the bank: `operationId` is a `bytes32` idempotency key assigned by the issuance system (e.g. `keccak256` of the corporate-action reference, or the reference itself right-padded to 32 ASCII bytes, per the Chapter 02 ISIN convention). The reconciliation job matches `Issued`/`Cancelled` events 1:1 against its instruction file — like matching `camt.054` entries back to your original `pain.001` by end-to-end ID. Indexing `operationId` lets Java query "did operation X land?" directly via a topic filter, which is exactly how an idempotent retry decides whether to resend.
**Starter code:**
```solidity
    event Issued(address indexed to, uint256 value, ____ indexed operationId);
    event ____(address indexed from, uint256 value, bytes32 indexed operationId);
```
**Solution:**
```solidity
    /// operationId = bank-assigned idempotency key (end-to-end reference)
    event Issued(address indexed to, uint256 value, bytes32 indexed operationId);
    event Cancelled(address indexed from, uint256 value, bytes32 indexed operationId);
```
**Validation rule:** `event\s+Issued\(address\s+indexed\s+to,\s*uint256\s+value,\s*bytes32\s+indexed\s+operationId\)` — checks Issued carries an indexed bytes32 operationId.



```checker
{"id": "ch04-l5-s2", "type": "regex", "pattern": "event\\s+Cancelled\\(address\\s+indexed\\s+from,\\s+uint256\\s+value,\\s+bytes32\\s+indexed\\s+operationId\\);", "flags": "m", "target": "solidity", "error_hint": "Your code should match the solution for this step."}
```
### Step 5.3 — The `_mint` primitive
**Instruction:** Write `function _mint(address to, uint256 value) internal`: revert with `BankERC20InvalidReceiver(address(0))` if `to` is zero, then call `_update(address(0), to, value)`.
**Explanation:** Minting *to* the zero address would create supply and destroy it in one booking — `_totalSupply` would rise while no balance does, breaking the register invariant, so it's blocked. Then `_mint` simply routes through the booking engine with `from = address(0)`: supply grows (checked add), receiver is credited, and the emitted `Transfer(address(0), to, value)` is the standard's canonical issuance signal. One engine, three operations — the audit story stays simple because every capital movement flows through one function. (`// modeled on OZ ERC20._mint`.)
**Starter code:**
```solidity
    function _mint(address to, uint256 value) internal {
        if (to == address(0)) revert ____(address(0));
        _update(____, to, value);
    }
```
**Solution:**
```solidity
    /// Issuance primitive — Transfer(address(0), to, value) is the standard signal.
    function _mint(address to, uint256 value) internal {
        if (to == address(0)) revert BankERC20InvalidReceiver(address(0));
        _update(address(0), to, value); // from = zero => supply grows in _update
    }
```
**Validation rule:** `_update\(address\(0\),\s*to,\s*value\);` — checks _mint routes through _update with the zero address as sender.



```checker
{"id": "ch04-l5-s3", "type": "regex", "pattern": "if\\s+\\(to\\s+==\\s+address\\(0\\)\\)\\s+revert\\s+BankERC20InvalidReceiver\\(address\\(0\\)\\);", "flags": "m", "target": "solidity", "error_hint": "Your code should match the solution for this step."}
```
### Step 5.4 — The `_burn` primitive
**Instruction:** Write `function _burn(address from, uint256 value) internal`: revert with `BankERC20InvalidSender(address(0))` if `from` is zero, then call `_update(from, address(0), value)`.
**Explanation:** Burning checks the holder actually has the shares — `_update`'s balance check covers it — then debits the holder and shrinks `_totalSupply`. This is share cancellation: after a buyback, after a bond redemption, after a capital reduction. Note what `_burn` does *not* do: it does not require the holder's consent — the caller decides. That is exactly why the public wrapper must be registrar-gated (next step), and why in CMTAT proper, forced operations are a separate, explicitly-evented EnforcementModule concern (Chapter 07): regulators distinguish "holder asked to redeem" from "registrar seized and cancelled," and your event trail must too.
**Starter code:**
```solidity
    function _burn(address from, uint256 value) internal {
        if (from == address(0)) revert ____(address(0));
        _update(from, ____, value);
    }
```
**Solution:**
```solidity
    /// Cancellation primitive — Transfer(from, address(0), value) is the standard signal.
    function _burn(address from, uint256 value) internal {
        if (from == address(0)) revert BankERC20InvalidSender(address(0));
        _update(from, address(0), value); // to = zero => supply shrinks in _update
    }
```
**Validation rule:** `_update\(from,\s*address\(0\),\s*value\);` — checks _burn routes through _update with the zero address as receiver.



```checker
{"id": "ch04-l5-s4", "type": "regex", "pattern": "if\\s+\\(from\\s+==\\s+address\\(0\\)\\)\\s+revert\\s+BankERC20InvalidSender\\(address\\(0\\)\\);", "flags": "m", "target": "solidity", "error_hint": "Your code should match the solution for this step."}
```
### Step 5.5 — Registrar-gated `mint` and `burn` with operation IDs
**Instruction:** Implement `function mint(address to, uint256 value, bytes32 operationId) external onlyRegistrar` calling `_mint(to, value)` then emitting `Issued(to, value, operationId)`; and the symmetric `burn(address from, uint256 value, bytes32 operationId) external onlyRegistrar` calling `_burn` then emitting `Cancelled`.
**Explanation:** The external functions are the corporate-action entry points: privilege gate first (`onlyRegistrar` runs before the body), booking second, bank-facing audit event third — all atomic. Threading `operationId` from the bank's instruction through to the event closes the reconciliation loop: instruction file → signed transaction → on-chain event → matched booking, one key end to end. Check the invariant survives: `mint` raises `_totalSupply` and one balance by the same amount; `burn` lowers both by the same amount; `transfer` touches balances only. `sum(_balances) == _totalSupply` holds after every operation — your register always reconciles to issued capital, by construction.
**Starter code:**
```solidity
    function mint(address to, uint256 value, bytes32 operationId) external ____ {
        _mint(to, value);
        emit ____(to, value, operationId);
    }

    function burn(address from, uint256 value, bytes32 operationId) external onlyRegistrar {
        ____(from, value);
        emit Cancelled(from, value, ____);
    }
```
**Solution:**
```solidity
    /// Primary-market issuance. operationId threads the bank's end-to-end
    /// reference into the on-chain audit trail (idempotent reconciliation).
    function mint(address to, uint256 value, bytes32 operationId) external onlyRegistrar {
        _mint(to, value);
        emit Issued(to, value, operationId);
    }

    /// Capital reduction / redemption cancellation.
    function burn(address from, uint256 value, bytes32 operationId) external onlyRegistrar {
        _burn(from, value);
        emit Cancelled(from, value, operationId);
    }
```
**Validation rule:** `function\s+mint\(address\s+to,\s*uint256\s+value,\s*bytes32\s+operationId\)\s+external\s+onlyRegistrar` — checks mint is external, registrar-gated, and takes an operationId.

> **Banking integration note:** Idempotent issuance from Java — before (re)sending a mint, the adapter checks whether `operationId` already landed, by topic-filtering the `Issued` event:
> ```java
> Event ISSUED = new Event("Issued", List.of(
>         new TypeReference<Address>(true) {},      // to          -> topics[1]
>         new TypeReference<Uint256>(false) {},     // value       -> data
>         new TypeReference<Bytes32>(true) {}));    // operationId -> topics[2]
> EthFilter f = new EthFilter(DefaultBlockParameterName.EARLIEST,
>         DefaultBlockParameterName.LATEST, contractAddress);
> f.addSingleTopic(EventEncoder.encode(ISSUED));
> f.addNullTopic();                                  // any `to`
> f.addSingleTopic(Numeric.toHexString(operationId)); // exact operation
> boolean alreadyExecuted = !web3j.ethGetLogs(f).send().getLogs().isEmpty();
> if (!alreadyExecuted) { /* encode + send mint(to, value, operationId) */ }
> ```
> Same pattern as checking the end-to-end ID against the archive before resubmitting a payment — the chain itself is the dedupe store.

> **Datatype/parser note:** `bytes32 operationId` ⟷ web3j `Bytes32` ⟷ Java `byte[32]`. To carry a bank reference like `"CA-2026-000123"`, right-pad the ASCII bytes with zeros to 32 (Chapter 02's ISIN convention): `byte[] id = Arrays.copyOf(ref.getBytes(StandardCharsets.US_ASCII), 32);` and decode back with `new String(bytes, US_ASCII).trim()` — or skip reversibility and use `Hash.sha3(ref.getBytes())` when the reference exceeds 32 chars. Because `operationId` is *indexed*, it appears as a raw 32-byte topic — directly comparable, no ABI decoding required.

---

## Assembled contract — `contracts/shared/BankERC20.sol`

```solidity
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
```

The full bank-side adapter is in `java-adapters/Erc20Operations.java`: contract loading via hand-encoded `Function` objects, `balanceOf` as `BigInteger` → `BigDecimal` scaling, `transfer` submission via `RawTransactionManager`, `Transfer`-event subscription posting into a shadow ledger, and a `reconcileHolder` method that reports breaks against the chain.

---

## Quiz

**Q1 (multiple choice).** In the context of building an ERC-20 token for a securities register, which of the following best describes the purpose of the `approve`/`transferFrom` functions?
a) To manage the total supply of tokens.
b) To delegate authority to transfer tokens on behalf of another account.
c) To record the transaction history of each token.
d) To mint new tokens into existence.
**Answer: b.** The `approve`/`transferFrom` functions allow one account to grant another account permission to transfer a specified amount of tokens, which is analogous to granting power of attorney in traditional banking systems.

**Q2 (multiple choice).** When implementing an ERC-20 token for a securities register, why is it important to emit the `Transfer` event during a token transfer?
a) To update the balances mapping.
b) To record the transaction history and maintain audit trails.
c) To increase the total supply of tokens.
d) To decrease the total supply of tokens.
**Answer: b.** Emitting the `Transfer` event is crucial for recording each transaction, which helps in maintaining an immutable ledger that can be audited for compliance and transparency.

**Q3 (multiple choice).** In the context of a securities register using ERC-20 tokens, what does the `decimals` function represent?
a) The number of tokens minted initially.
b) The smallest unit of the token that can be transferred.
c) The total supply of tokens in circulation.
d) The maximum amount of tokens an account can hold.
**Answer: b.** The `decimals` function specifies the smallest unit of the token, which is essential for monetary representation and ensuring precision in financial transactions.

**Q4 (short answer).** Explain how the `mint` and `burn` functions contribute to the lifecycle of a security in an ERC-20 token implementation.
**Answer:** The `mint` function allows for the creation of new tokens, representing the issuance of securities. Conversely, the `burn` function destroys tokens, symbolizing the cancellation or redemption of securities. Together, these functions manage the supply of securities, ensuring that they can be issued and retired as needed.

**Q5 (short answer).** How does the `balances` mapping in an ERC-20 token correspond to traditional banking practices?
**Answer:** The `balances` mapping acts as a position-keeping table, similar to how bank accounts track balances. It records the number of tokens held by each account, allowing for accurate accounting and transaction processing.

---

**Next:** Chapter 05 replaces the single `registrar` address with a full role-based access-control layer — DEFAULT_ADMIN_ROLE, MINTER_ROLE, BURNER_ROLE and friends — mapped onto the bank's org structure.


```checker
{"id": "ch04-l5-s5", "type": "regex", "pattern": "function\\s+mint\\(address\\s+to,\\s+uint256\\s+value,\\s+bytes32\\s+operationId\\)\\s+external\\s+onlyRegistrar\\s+\\{", "flags": "m", "target": "solidity", "error_hint": "Your code should match the solution for this step."}
```
