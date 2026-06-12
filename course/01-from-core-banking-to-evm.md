# Chapter 01 — From Core Banking to the EVM

**Track:** `[shared]`
**Emphasis threads:** `[BANK]` `[TYPES-light]`
**Chapter learning objective:** Build a working EVM mental model from a JVM/core-banking vantage point — accounts vs bank accounts, private keys vs HSM signing, transactions vs payment messages, gas vs CPU quota, blocks vs end-of-day batches, world state vs the master ledger, finality vs settlement finality — and deploy your first contract, `HelloLedger`.
**Prerequisites:** None. This is the entry chapter. Comfort with Java (or .NET) and core-banking concepts (ledgers, payment messages, EOD batch, HSMs) is assumed.
**Contract built:** `contracts/shared/HelloLedger.sol`

---

## How to read this chapter

You have spent years building systems where a **central operator** (the bank) keeps the **master ledger** in an RDBMS, authenticates clients through sessions and HSM-signed messages, and settles in batches. The EVM (Ethereum Virtual Machine) is the same business problem — *who owns what, and who may change it* — solved with a radically different trust model: thousands of independent machines each hold a full copy of the ledger and re-execute every transaction to verify it. Everything strange about blockchain engineering (gas, finality, immutable code) falls out of that one design decision.

Each lesson below maps one EVM concept onto something you already operate in production.

---

## Lesson 1 — Accounts, Keys, and Addresses

**Learning objective:** Understand EVM accounts (EOA vs contract account), how addresses derive from key pairs, and how `msg.sender` gives you cryptographically authenticated caller identity — mapped to bank account numbers, HSM signing, and session principals.
**Tags:** `[BANK]` `[TYPES-light]` · **Track:** `[shared]`

In core banking, an account is a **row in the accounts table**: the bank creates it, the bank controls it, and a customer proves identity through layers the bank operates (login, 2FA, signed messages from a partner bank's HSM). On the EVM there is no operator to open an account for you. An account **is** a key pair: generate a random 256-bit private key on your laptop, derive the public key (ECDSA on curve secp256k1), hash it (Keccak-256), keep the last 20 bytes — that is your **address**. Nobody approved it; it simply exists the moment you can sign with it.

Two account types share the same address space:

| | Externally Owned Account (EOA) | Contract Account |
|---|---|---|
| Controlled by | A private key (a person/system that signs) | Its own deployed code — nothing else |
| Bank analogy | A customer or a bank's settlement account + HSM that signs its messages | A stored procedure with its **own** account number and balance |
| Can initiate transactions | Yes | No — only acts when called |
| Has code | No | Yes |

> **Banking integration note:** In a bank, transaction authority lives in an **HSM**: payment messages are signed inside the appliance and the key never leaves it. An EVM private key plays exactly the same role — whoever holds it *is* the account — so banks custody EVM keys the same way: in HSMs or MPC custody platforms, never in application config. When you see `Credentials.create(privateKeyHex)` in web3j examples (including this course), read it as "dev-only stand-in for the HSM signer."

### Step 1.1 — Declare your first address

**Instruction:** In the scratch contract `HelloLedgerWarmup`, declare a public state variable named `treasuryAccount` of type `address`.

**Explanation:** `address` is a built-in 20-byte (160-bit) value type — think of it as the EVM's IBAN, except it is *derived from a public key*, not issued by an institution. Declaring it `public` makes the compiler auto-generate a read-only accessor, the way Lombok's `@Getter` generates one on a Java field. In Java/web3j this value arrives as a `String` like `"0x9f8e7d6c..."` (hex, 40 chars after `0x`).

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract HelloLedgerWarmup {
    // Declare a public state variable `treasuryAccount` of type address here

}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract HelloLedgerWarmup {
    // 20-byte account identifier — the EVM's IBAN, derived from a key pair.
    // `public` auto-generates a getter, like Lombok @Getter on a Java field.
    address public treasuryAccount;
}
```

**Validation rule:** `address\s+public\s+treasuryAccount\s*;` — checks a public state variable `treasuryAccount` of type `address` is declared.

> **Datatype/parser note:** Solidity `address` ↔ web3j `org.web3j.abi.datatypes.Address`, which unwraps to a Java `String` (`"0x"`-prefixed, 40 hex chars). Addresses have an EIP-55 mixed-case checksum: `org.web3j.crypto.Keys.toChecksumAddress(addr)` produces it, and your boundary validation should reject inputs whose mixed-case form fails the checksum — the EVM itself will accept any well-formed 20 bytes, so a typo silently sends value to a stranger. Treat this like IBAN check-digit validation in a payment gateway.

### Step 1.2 — Capture the authenticated caller with `msg.sender`

**Instruction:** Write a function `claimTreasury()` with `external` visibility that sets `treasuryAccount` to `msg.sender`.

**Explanation:** `msg.sender` is the address that called the current function — and it is **unforgeable**, because the EVM only executes a transaction after verifying its ECDSA signature against the sender's address. Compare with your world: a Java service trusts the `Principal` on the session because the *gateway* authenticated it; the EVM bakes authentication into the protocol itself, so every contract gets a verified caller identity for free. No session, no JWT, no mTLS — the signature *is* the authentication.

**Starter code:**
```solidity
contract HelloLedgerWarmup {
    address public treasuryAccount;

    // Write claimTreasury() here: external, sets treasuryAccount to the caller

}
```

**Solution:**
```solidity
contract HelloLedgerWarmup {
    address public treasuryAccount;

    function claimTreasury() external {
        // msg.sender = signature-verified caller; the protocol-level
        // equivalent of an authenticated Principal on a Java session.
        treasuryAccount = msg.sender;
    }
}
```

**Validation rule:** `function\s+claimTreasury\s*\(\s*\)\s+external[\s\S]*?treasuryAccount\s*=\s*msg\.sender\s*;` — checks an external `claimTreasury()` assigns `msg.sender` to `treasuryAccount`.

### Step 1.3 — Distinguish EOA from contract account

**Instruction:** Write a `view` function `isContractAccount(address account)` returning `bool` — `true` when `account.code.length > 0`.

**Explanation:** Every address has a `code` property: empty for EOAs, non-empty for deployed contracts. This matters operationally — a payment to an EOA just moves value; a payment to a contract *executes its code*. Bank analogy: transferring to a customer account vs posting to an account that has a **standing stored procedure** attached which runs on every credit. (Edge case worth knowing: during a contract's own constructor, its code is not yet stored, so this check returns `false` mid-construction — never use it as a security gate, only as a heuristic.)

**Starter code:**
```solidity
    // Returns true if `account` is a contract account (has deployed code)
    function isContractAccount(address account) external view returns (bool) {
        // return whether the account's code is non-empty

    }
```

**Solution:**
```solidity
    function isContractAccount(address account) external view returns (bool) {
        // EOA: code.length == 0. Contract: deployed bytecode lives at the address.
        return account.code.length > 0;
    }
```

**Validation rule:** `account\.code\.length\s*>\s*0` — checks the function inspects `account.code.length`.

### Step 1.4 — Read a native balance

**Instruction:** Write a `view` function `nativeBalanceOf(address account)` that returns `account.balance` as `uint256`.

**Explanation:** Every account — EOA or contract — carries a native-coin balance maintained by the protocol itself, denominated in **wei** (1 ether = 10^18 wei). This is the "minor units" pattern you already use: Swiss core systems store CHF in rappen (10^2), the EVM stores ether in wei (10^18) — always integers, never floats, so no rounding drift. `uint256` is an unsigned 256-bit integer; balances cannot go negative by construction, which deletes an entire class of ledger bugs.

**Starter code:**
```solidity
    // Return the native (wei) balance of `account`
    function nativeBalanceOf(address account) external view returns (uint256) {

    }
```

**Solution:**
```solidity
    function nativeBalanceOf(address account) external view returns (uint256) {
        // Protocol-maintained balance in wei (10^-18 ether) — the same
        // minor-units discipline as storing CHF amounts in rappen.
        return account.balance;
    }
```

**Validation rule:** `return\s+account\.balance\s*;` — checks the function returns `account.balance`.

> **Datatype/parser note:** Solidity `uint256` ↔ web3j `org.web3j.abi.datatypes.generated.Uint256`, which unwraps to `java.math.BigInteger`. Never route a 256-bit balance through `long` (max ~9.2×10^18 — barely 9 ether in wei) or `double` (silent precision loss above 2^53). `BigInteger` end-to-end, convert to `BigDecimal` only at the presentation layer with an explicit scale: `new BigDecimal(weiAmount, 18)` semantics via `Convert.fromWei(...)`.

### Step 1.5 — Validate against the zero address

**Instruction:** At the top of `claimTreasury()`, add `require(treasuryAccount == address(0), "already claimed");` so the treasury can only be claimed once.

**Explanation:** `address(0)` (`0x000...000`) is the zero value of the `address` type — the EVM equivalent of `null`, except it is a *real, valid-looking address* that nobody controls. Value sent there is destroyed. Treat zero-address checks like null checks at a service boundary: uninitialized `address` state variables default to `address(0)`, so comparing against it answers "has this ever been set?". `require(condition, "msg")` aborts and rolls back the whole transaction when the condition is false — Lesson 2 dissects exactly what that rollback means.

**Starter code:**
```solidity
    function claimTreasury() external {
        // require that treasuryAccount is still unset (the zero address)

        treasuryAccount = msg.sender;
    }
```

**Solution:**
```solidity
    function claimTreasury() external {
        // address(0) = the type's zero value; default for unset storage.
        // Guard = "INSERT only if row absent" — one-shot initialization.
        require(treasuryAccount == address(0), "already claimed");
        treasuryAccount = msg.sender;
    }
```

**Validation rule:** `require\s*\(\s*treasuryAccount\s*==\s*address\(0\)` — checks the one-shot guard compares against `address(0)`.

---

## Lesson 2 — Transactions, Gas, and Blocks

**Learning objective:** Map EVM transactions to payment messages, gas to CPU quotas, blocks to end-of-day batches; understand gas price vs gas limit and why `revert` is an atomic rollback that still costs money.
**Tags:** `[BANK]` · **Track:** `[shared]`

A transaction is a **signed instruction** from an EOA: "call function X on contract Y with arguments Z, and I'll pay for the computation." Structurally it is close to an ISO 20022 `pain.001` payment instruction — sender, target, payload, signature — but instead of landing in a bank's MQ for batch processing, it lands in a public mempool where **validators** pick it up, execute it, and bundle it with others into a **block** (every ~12 seconds on Ethereum). A block is a micro end-of-day batch: an ordered set of transactions plus the resulting ledger state, cryptographically chained to its predecessor.

**Gas** is the metering unit. Every EVM opcode has a fixed gas cost (an addition: 3 gas; writing a fresh storage slot: 20,000 gas). You attach two numbers to each transaction:

- **gas limit** — maximum gas you authorize (your CPU quota; the transaction aborts if exceeded),
- **gas price** (post-EIP-1559: `maxFeePerGas` + `maxPriorityFeePerGas`) — what you pay per unit, which determines how quickly validators include you.

Fee = gas used × price. The mainframe analogy is exact: MIPS-metered batch jobs, where an expensive query costs real money and a runaway job gets killed at its quota.

### Step 2.1 — Write a state-changing function (a transaction target)

**Instruction:** In scratch contract `HelloLedgerTxLab`, declare `uint256 public postedTotal;` and a function `postAmount(uint256 amount)` (`external`) that adds `amount` to `postedTotal`.

**Explanation:** Any function that writes storage can only be invoked through a **transaction** — signed, paid for, and globally replicated. A `view` query is free and local; a write costs gas and waits for a block, just as in your world a balance inquiry hits a read replica for free while a booking must travel through the payment pipeline. Design consequence you will carry through the whole course: keep on-chain writes minimal and let reads happen off-chain via events (Chapter 03).

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract HelloLedgerTxLab {
    // declare postedTotal, then postAmount(uint256 amount) adding to it

}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract HelloLedgerTxLab {
    uint256 public postedTotal;

    function postAmount(uint256 amount) external {
        // Storage write => only reachable via a signed, gas-paid transaction.
        postedTotal += amount;
    }
}
```

**Validation rule:** `function\s+postAmount\s*\(\s*uint256\s+amount\s*\)\s+external[\s\S]*?postedTotal\s*\+=\s*amount` — checks `postAmount` is external and accumulates into `postedTotal`.

### Step 2.2 — Revert: the all-or-nothing rollback

**Instruction:** At the top of `postAmount`, add `require(amount > 0, "amount must be positive");`.

**Explanation:** When `require` fails (or anything reverts deeper in the call), **every** state change of the transaction is rolled back — storage writes, balance moves, emitted events, all of it. This is your database transaction's `ROLLBACK`, but enforced by the VM with no way to opt out: there is no "catch and commit partially" unless you explicitly structure for it. Two sharp edges vs the JDBC mental model: (1) the failed transaction **still appears in the block** and **still pays gas** for the work done up to the revert — like being billed for a batch job that abended; (2) the revert reason travels back to the caller as ABI-encoded return data, which web3j surfaces when you query the receipt of a failed transaction.

**Starter code:**
```solidity
    function postAmount(uint256 amount) external {
        // reject zero amounts with require(...)

        postedTotal += amount;
    }
```

**Solution:**
```solidity
    function postAmount(uint256 amount) external {
        // Revert = VM-enforced full rollback (storage, events, value moves).
        // Unlike a DB rollback: tx still lands on-chain and still pays gas.
        require(amount > 0, "amount must be positive");
        postedTotal += amount;
    }
```

**Validation rule:** `require\s*\(\s*amount\s*>\s*0\s*,` — checks a `require` guards `amount > 0` with a reason.

### Step 2.3 — Custom errors: typed reason codes

**Instruction:** Declare `error HelloLedgerLabZeroAmount();` at contract level, and replace the `require` with `if (amount == 0) revert HelloLedgerLabZeroAmount();`.

**Explanation:** Revert strings cost gas to deploy and to revert with, byte by byte. A **custom error** is a declared, named failure type encoded as a 4-byte selector plus typed parameters — your `ErrorCode` enum / structured exception, not a free-text message. CMTAT and modern OZ code use custom errors throughout, so train the reflex now. Off-chain, web3j gives you the raw revert data; you match the selector exactly like mapping a SQLSTATE to a business exception.

**Starter code:**
```solidity
contract HelloLedgerTxLab {
    uint256 public postedTotal;

    // declare the custom error here

    function postAmount(uint256 amount) external {
        // replace require with: if (amount == 0) revert <YourError>();
        require(amount > 0, "amount must be positive");
        postedTotal += amount;
    }
}
```

**Solution:**
```solidity
contract HelloLedgerTxLab {
    uint256 public postedTotal;

    // Typed failure: 4-byte selector + params — an error code, not log text.
    error HelloLedgerLabZeroAmount();

    function postAmount(uint256 amount) external {
        if (amount == 0) revert HelloLedgerLabZeroAmount();
        postedTotal += amount;
    }
}
```

**Validation rule:** `error\s+HelloLedgerLabZeroAmount\s*\(\s*\)\s*;[\s\S]*?revert\s+HelloLedgerLabZeroAmount\s*\(\s*\)` — checks the custom error is declared and used in a `revert`.

### Step 2.4 — Block context: timestamp and number

**Instruction:** Declare `uint256 public lastPostedAt;` and `uint256 public lastPostedBlock;`, and set them in `postAmount` from `block.timestamp` and `block.number`.

**Explanation:** Every transaction executes inside a block carrying global context: `block.timestamp` (Unix seconds, set by the block proposer — accurate to ~seconds, **not** a precision clock; never compare with sub-minute tolerance) and `block.number` (a strictly increasing sequence — your EOD batch run number, except a "day" lasts 12 seconds). Banking mapping: `block.timestamp` ≈ booking timestamp, `block.number` ≈ batch/posting cycle id. You will later use block numbers as replay cursors for event reconciliation (Chapter 08).

**Starter code:**
```solidity
    uint256 public postedTotal;
    // declare lastPostedAt and lastPostedBlock here

    function postAmount(uint256 amount) external {
        if (amount == 0) revert HelloLedgerLabZeroAmount();
        postedTotal += amount;
        // record block.timestamp and block.number here

    }
```

**Solution:**
```solidity
    uint256 public postedTotal;
    uint256 public lastPostedAt;     // booking timestamp (Unix seconds)
    uint256 public lastPostedBlock;  // posting-cycle id (block height)

    function postAmount(uint256 amount) external {
        if (amount == 0) revert HelloLedgerLabZeroAmount();
        postedTotal += amount;
        lastPostedAt = block.timestamp;  // proposer-set; second-level precision only
        lastPostedBlock = block.number;  // strictly increasing sequence
    }
```

**Validation rule:** `lastPostedAt\s*=\s*block\.timestamp\s*;[\s\S]*?lastPostedBlock\s*=\s*block\.number\s*;` — checks both block-context values are recorded.

### Step 2.5 — Respect the gas limit: bound your loops

**Instruction:** Write `postBatch(uint256[] calldata amounts)` (`external`) that requires `amounts.length <= 100` (reason `"batch too large"`), then loops and accumulates each non-zero amount into `postedTotal`.

**Explanation:** Gas turns complexity bugs into outages. An unbounded loop over a growing array works in test, then one day the iteration cost exceeds the **block gas limit** (~30M gas) and the function becomes permanently uncallable — funds-freezing bugs in the wild have exactly this shape. Mainframe instinct applies: every batch job has a chunk size. Cap the input, push pagination to the caller. This is why CMTAT-style contracts avoid on-chain iteration over holders entirely (you'll see the snapshot design in Chapter 11/14 work around it).

**Starter code:**
```solidity
    // Batch-post amounts; cap the batch at 100 entries
    function postBatch(uint256[] calldata amounts) external {



    }
```

**Solution:**
```solidity
    function postBatch(uint256[] calldata amounts) external {
        // Bounded batch: per-tx work must fit the gas limit (CPU quota).
        require(amounts.length <= 100, "batch too large");
        for (uint256 i = 0; i < amounts.length; i++) {
            if (amounts[i] == 0) revert HelloLedgerLabZeroAmount();
            postedTotal += amounts[i];
        }
    }
```

**Validation rule:** `require\s*\(\s*amounts\.length\s*<=\s*100[\s\S]*?for\s*\(` — checks the batch size is capped before the loop.

> **Banking integration note:** Submitting a transaction from Java mirrors building and HSM-signing a payment message. With web3j 4.x against any JSON-RPC endpoint:
>
> ```java
> import org.web3j.crypto.Credentials;
> import org.web3j.crypto.RawTransaction;
> import org.web3j.crypto.TransactionEncoder;
> import org.web3j.protocol.Web3j;
> import org.web3j.protocol.core.DefaultBlockParameterName;
> import org.web3j.protocol.core.methods.response.EthSendTransaction;
> import org.web3j.protocol.http.HttpService;
> import org.web3j.utils.Numeric;
> import java.math.BigInteger;
>
> Web3j web3 = Web3j.build(new HttpService("https://rpc.your-bank-node.ch:8545"));
>
> // Dev-only key material — production: HSM/MPC custody signs, key never in JVM.
> Credentials signer = Credentials.create("0x<hex-private-key>");
>
> BigInteger nonce = web3.ethGetTransactionCount(
>         signer.getAddress(), DefaultBlockParameterName.PENDING)
>     .send().getTransactionCount();           // = per-account sequence number,
>                                              //   like a message sequence in SIC/SWIFT
>
> // ABI-encoded call payload: "postAmount(uint256)" with amount 150_000.
> // Chapter 08 dissects this encoding; here, note it's just the request body.
> org.web3j.abi.datatypes.Function fn = new org.web3j.abi.datatypes.Function(
>     "postAmount",
>     java.util.List.of(new org.web3j.abi.datatypes.generated.Uint256(
>         BigInteger.valueOf(150_000))),
>     java.util.Collections.emptyList());
> String callData = org.web3j.abi.FunctionEncoder.encode(fn);
>
> RawTransaction tx = RawTransaction.createTransaction(
>     nonce,
>     BigInteger.valueOf(2_000_000_000L),      // gas price (wei) — fee bid
>     BigInteger.valueOf(100_000L),            // gas limit — CPU quota for this call
>     "0xContractAddress...",
>     callData);
>
> byte[] signed = TransactionEncoder.signMessage(tx, signer); // the "HSM moment"
> EthSendTransaction resp =
>     web3.ethSendRawTransaction(Numeric.toHexString(signed)).send();
> String txHash = resp.getTransactionHash();   // your end-to-end reference (UETR analogue)
> ```
>
> The **nonce** is the account's transaction sequence number: gaps stall everything behind them, duplicates are rejected — the same per-sender sequencing discipline as SIC/SWIFT message sequence numbers. Chapter 08 builds full nonce management and idempotent submission.

---

## Lesson 3 — State, Storage, and Finality

**Learning objective:** Treat the EVM world state as the master ledger; distinguish storage/memory/calldata; understand finality vs settlement finality and why reorgs force confirmation-depth policies in bank adapters.
**Tags:** `[BANK]` · **Track:** `[shared]`

The **world state** is one global key-value structure: every account's balance, nonce, code, and — for contracts — a private 2^256-slot **storage** array. Your mental model: the master ledger of a core system, except (a) every full node holds the complete copy, (b) it can only be modified by replaying signed transactions through the VM, and (c) historical states are reconstructible from the chain of blocks — a perfect, built-in audit log. There is no DBA, no `UPDATE ... WHERE` issued from a console, no out-of-band correction. Corrections are **new, compensating transactions** — exactly how a proper accounting system reverses a booking rather than editing it.

**Finality** needs care. In Swiss settlement, finality is a legal moment (SIC settlement is final and irrevocable when posted). On a blockchain, a new block is at first only *probably* permanent: if validators briefly disagree, a competing chain segment can replace the last few blocks — a **reorg** — and "your" transaction may land in a different block or return to the pending pool. Ethereum's proof-of-stake then **finalizes** checkpoints (~2 epochs ≈ 12–13 minutes): a finalized block cannot be reverted without an economically suicidal attack. Bank policy translation: treat inclusion as *processing*, treat finalization (or your chosen confirmation depth) as *settlement finality*, and make the reconciliation job reorg-aware (Chapter 08). Chapter 09 covers how Swiss law maps legal finality onto this.

### Step 3.1 — Storage vs memory vs calldata

**Instruction:** In scratch contract `HelloLedgerStateLab`, declare `string private note;` (storage), then write `function stash(string calldata input) external` that assigns `note = input;`.

**Explanation:** Three data locations, three lifetimes — and you already know all of them. `storage`: persisted in world state, survives across transactions, expensive (≈20k gas per fresh 32-byte slot) — your database table. `memory`: scratch space wiped after the call — your JVM heap inside one request. `calldata`: the read-only encoded arguments of the incoming transaction — the immutable request payload, like the raw bytes of an ISO 20022 message you parse but never mutate. Reference-type parameters (`string`, `bytes`, arrays, structs) must declare their location; `calldata` is cheapest for inputs you only read.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract HelloLedgerStateLab {
    // declare `note` as a private string in storage

    // write stash(...) taking a string in calldata and persisting it

}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract HelloLedgerStateLab {
    string private note; // storage: the "database table" — persists across txs

    function stash(string calldata input) external {
        // calldata -> storage copy: parse-and-persist, request payload to DB row
        note = input;
    }
}
```

**Validation rule:** `string\s+private\s+note\s*;[\s\S]*?function\s+stash\s*\(\s*string\s+calldata\s+input\s*\)` — checks a storage string and a `calldata` parameter are used.

### Step 3.2 — view and pure: free reads

**Instruction:** Add `function readNote() external view returns (string memory)` returning `note`, and `function shout(string memory s) external pure returns (string memory)` returning `s` unchanged.

**Explanation:** `view` promises no state writes; `pure` promises no state *reads* either. Both can be served by any single node locally via `eth_call` — no transaction, no gas paid by the caller, no waiting for a block. This is your read-replica query vs booking distinction, enforced by the compiler. Note the return location is `memory`: you hand back a copy, never a live storage reference.

**Starter code:**
```solidity
    // readNote(): external view, returns the stored note

    // shout(s): external pure, returns its argument unchanged

```

**Solution:**
```solidity
    function readNote() external view returns (string memory) {
        return note; // view = read replica query: free, local, no tx
    }

    function shout(string memory s) external pure returns (string memory) {
        return s;    // pure = static utility method: touches no state at all
    }
```

**Validation rule:** `function\s+readNote\s*\(\s*\)\s+external\s+view[\s\S]*?function\s+shout\s*\([^)]*\)\s+external\s+pure` — checks both functions exist with `view` and `pure` mutability.

### Step 3.3 — mapping: the keyed ledger table

**Instruction:** Declare `mapping(address => uint256) public postingsBy;` and write `function post() external` that increments `postingsBy[msg.sender]`.

**Explanation:** A `mapping` is the EVM's keyed table: `mapping(address => uint256)` reads as `CREATE TABLE postings (account CHAR(40) PRIMARY KEY, count NUMERIC)`. Crucial differences from a `HashMap` or table: every key conceptually exists already (unset = zero value, no null), there is **no iteration** — you cannot `SELECT *` over a mapping, by design (remember the gas limit) — and lookups are O(1) hashed storage slots. From Chapter 04 on, a `mapping(address => uint256)` of balances *is* the securities register. When the bank needs the full holder list, it rebuilds it off-chain from events — the indexer pattern, Chapter 08.

**Starter code:**
```solidity
    // declare the postingsBy mapping (address => uint256), public

    // post(): external, increments the caller's entry

```

**Solution:**
```solidity
    // Keyed ledger table: account -> posting count. No iteration on-chain;
    // every address "exists" with default 0 — no null semantics.
    mapping(address => uint256) public postingsBy;

    function post() external {
        postingsBy[msg.sender] += 1;
    }
```

**Validation rule:** `mapping\s*\(\s*address\s*=>\s*uint256\s*\)\s+public\s+postingsBy\s*;[\s\S]*?postingsBy\[msg\.sender\]\s*\+=\s*1` — checks the mapping is declared and incremented per caller.

### Step 3.4 — Emit block context for the reconciler

**Instruction:** Declare `event Posted(address indexed account, uint256 count, uint256 blockNumber);` and emit it at the end of `post()` with `msg.sender`, the caller's new count, and `block.number`.

**Explanation:** Events are append-only log entries written into the transaction receipt — cheap, indexed, and *not* readable by contracts, only by off-chain consumers. They are the EVM's outbound message queue: your downstream integration feed. Carrying `block.number` in the payload helps the bank-side reconciler implement its **confirmation-depth policy**: process an event only once `currentBlock - event.blockNumber >= N` (e.g. N=2 for ops dashboards, finalized checkpoint for irreversible postings like releasing a CHF payment). If a reorg drops the block, the event vanishes with it — which is precisely why "seen" ≠ "settled."

**Starter code:**
```solidity
    // declare the Posted event (indexed account, count, blockNumber)

    function post() external {
        postingsBy[msg.sender] += 1;
        // emit Posted here

    }
```

**Solution:**
```solidity
    // Outbound feed entry for the reconciler; indexed account is filterable.
    event Posted(address indexed account, uint256 count, uint256 blockNumber);

    function post() external {
        postingsBy[msg.sender] += 1;
        // Event lives in the tx receipt; reorg drops it with the block —
        // bank adapter must wait for confirmation depth before acting.
        emit Posted(msg.sender, postingsBy[msg.sender], block.number);
    }
```

**Validation rule:** `event\s+Posted\s*\(\s*address\s+indexed\s+account[\s\S]*?emit\s+Posted\s*\(\s*msg\.sender` — checks the event is declared with an indexed account and emitted in `post()`.

> **Banking integration note:** Your first taste of web3j connectivity — connect, read chain state, check finality status:
>
> ```java
> import org.web3j.protocol.Web3j;
> import org.web3j.protocol.core.DefaultBlockParameterName;
> import org.web3j.protocol.http.HttpService;
> import org.web3j.utils.Convert;
> import java.math.BigDecimal;
> import java.math.BigInteger;
>
> // One Web3j instance per endpoint — pooled, thread-safe, like a DataSource.
> Web3j web3 = Web3j.build(new HttpService("https://rpc.your-bank-node.ch:8545"));
>
> // Liveness probe (node version) — your connection health check
> String clientVersion = web3.web3ClientVersion().send().getWeb3ClientVersion();
>
> // Read a balance — free 'view' query, equivalent of a read-replica SELECT
> BigInteger wei = web3.ethGetBalance(
>         "0x9f8e7d6c5b4a39281706f5e4d3c2b1a098765432",
>         DefaultBlockParameterName.LATEST)     // also: FINALIZED / SAFE / a block number
>     .send().getBalance();
> BigDecimal ether = Convert.fromWei(new BigDecimal(wei), Convert.Unit.ETHER);
>
> // Finality-aware cursor: highest block your reconciler may treat as settled
> BigInteger finalized = web3.ethGetBlockByNumber(
>         DefaultBlockParameterName.FINALIZED, false)
>     .send().getBlock().getNumber();
> ```
>
> Querying at `FINALIZED` instead of `LATEST` is the settlement-finality switch in code form: `LATEST` shows you the processing ledger, `FINALIZED` shows you what can never be reorged away. A bank reconciliation job reads positions at `FINALIZED` for booking, `LATEST` only for monitoring. (Assumption: the JSON-RPC node supports the post-merge `finalized`/`safe` block tags — any mainstream client since 2022 does.)

---

## Lesson 4 — First Contract: Build and Deploy `HelloLedger`

**Learning objective:** Assemble `HelloLedger` — greeting + entry counter + `EntryRecorded` event — and deploy it, understanding deployment as "bytecode + constructor in a transaction" in contrast to deploying a WAR to an app server.
**Tags:** `[BANK]` `[TYPES-light]` · **Track:** `[shared]`

Time to ship. `HelloLedger` is intentionally small but already shaped like a real integration contract: persistent state, a guarded mutation, typed errors, and events designed for an off-chain consumer with an idempotency key. Everything you build through Chapter 18 keeps this skeleton.

Deployment first, because it breaks JVM intuition: there is **no application server**. You compile Solidity to EVM bytecode, then send a transaction with an *empty `to` field* whose payload is `initcode` = (deployment bytecode + ABI-encoded constructor arguments). Validators execute the initcode; the constructor runs **exactly once, ever**; whatever the initcode returns becomes the contract's permanent runtime code, stored at a freshly derived address (from your address + nonce). Compare a WAR deploy:

| WAR on app server | Contract on EVM |
|---|---|
| Ops copies artifact to *your* server | Anyone sends a deploy tx; code replicated to *every* node |
| Container runs `init()`; restart re-runs it | Constructor runs once in deployment tx, then is discarded |
| Hot-fix: redeploy same URL | Code at an address is immutable; "upgrade" = new address or proxy pattern (Chapter 06) |
| App dies when server stops | Contract exists as long as the chain does |
| Endpoint URL assigned by ops | Address deterministically derived from deployer + nonce |

Immutability is the point: holders of a CMTAT security token rely on the fact that no developer can hot-patch the ledger logic. The release-management discipline you know from production banking gets pushed *before* deployment — audits, testnets — because there is no patch window afterward.

### Step 4.1 — Contract skeleton

**Instruction:** Create `contracts/shared/HelloLedger.sol` with the SPDX line, `pragma solidity ^0.8.20;`, and an empty contract `HelloLedger`.

**Explanation:** The `pragma` pins the compiler range — your `<maven.compiler.release>` in a POM. `^0.8.20` means ≥0.8.20 and <0.9.0. The 0.8 line matters: arithmetic overflow/underflow reverts by default (pre-0.8 silently wrapped — the EVM's Y2K-class footgun). The SPDX identifier is a machine-readable license header, required by tooling convention.

**Starter code:**
```solidity
// add SPDX line, pragma, and the empty contract declaration

```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;   // compiler range pin, like maven.compiler.release

contract HelloLedger {
}
```

**Validation rule:** `pragma\s+solidity\s+\^0\.8\.20\s*;[\s\S]*?contract\s+HelloLedger\s*\{` — checks the pragma and contract declaration.

### Step 4.2 — State: deployer, greeting, counter

**Instruction:** Inside `HelloLedger`, declare: `address public immutable deployer;`, `string private greeting;`, `uint256 public entryCount;`.

**Explanation:** `immutable` means "assigned once during construction, then read-only forever" — Java's `final` field, but the value is embedded into the runtime code, making reads as cheap as constants. The greeting is `private`, so we'll expose it through an explicit getter (`private` hides it from *other contracts' code*, **not** from observers — all chain data is public; never put secrets in storage, the way you'd never put PINs in a replicated log). `entryCount` is our monotonically increasing sequence — the entry serial number a bank reconciler can use as an idempotency key.

**Starter code:**
```solidity
contract HelloLedger {
    // declare: immutable public deployer (address),
    //          private greeting (string),
    //          public entryCount (uint256)



}
```

**Solution:**
```solidity
contract HelloLedger {
    address public immutable deployer; // set once at construction — `final` field
    string private greeting;           // hidden from contracts, NOT from observers
    uint256 public entryCount;         // monotonic sequence -> idempotency key
}
```

**Validation rule:** `address\s+public\s+immutable\s+deployer\s*;[\s\S]*?string\s+private\s+greeting\s*;[\s\S]*?uint256\s+public\s+entryCount\s*;` — checks all three state variables with correct visibility/mutability.

### Step 4.3 — Events and errors: declare the integration surface

**Instruction:** Declare two events — `EntryRecorded(uint256 indexed entryId, address indexed recordedBy, string note, uint256 timestamp)` and `GreetingChanged(string oldGreeting, string newGreeting, address indexed changedBy)` — and two custom errors: `HelloLedgerEmptyNote()` and `HelloLedgerNotDeployer(address caller)`.

**Explanation:** Declare events **before** any function emits them (like declaring a checked exception before throwing it). `indexed` parameters (max 3) become searchable log *topics* — your message-queue routing keys: the bank's listener can subscribe to "all `EntryRecorded` where `recordedBy` = our omnibus address" without scanning every log. Non-indexed parameters travel in the ABI-encoded data section, cheaper and preserving full values (an indexed `string` would be hashed — only its Keccak digest stored — so keep strings non-indexed when you need the actual text). The error names carry a `HelloLedger` prefix — course convention to keep every top-level and easily-confused identifier unique when all chapters compile together.

**Starter code:**
```solidity
    // declare EntryRecorded and GreetingChanged events



    // declare HelloLedgerEmptyNote and HelloLedgerNotDeployer errors


```

**Solution:**
```solidity
    // Integration surface: what the bank's event listener consumes.
    // (contractAddress, entryId) = the idempotency key downstream.
    event EntryRecorded(
        uint256 indexed entryId,      // topic: filterable, like an MQ routing key
        address indexed recordedBy,   // topic: filter by booking party
        string note,                  // data section: full text preserved
        uint256 timestamp
    );

    // Audit trail for parameter changes — config-change record.
    event GreetingChanged(string oldGreeting, string newGreeting, address indexed changedBy);

    error HelloLedgerEmptyNote();                  // input validation failure
    error HelloLedgerNotDeployer(address caller);  // authorization failure + evidence
```

**Validation rule:** `event\s+EntryRecorded\s*\(\s*uint256\s+indexed\s+entryId\s*,\s*address\s+indexed\s+recordedBy[\s\S]*?error\s+HelloLedgerNotDeployer\s*\(\s*address\s+caller\s*\)` — checks both indexed topics on `EntryRecorded` and the parameterized error.

### Step 4.4 — Constructor: the one-shot initializer

**Instruction:** Write a constructor taking `string memory initialGreeting` that sets `deployer = msg.sender;` and `greeting = initialGreeting;`.

**Explanation:** The constructor executes inside the deployment transaction and is then **thrown away** — it is not part of the runtime bytecode. Contrast a servlet's `init()`: that re-runs on every container restart; this runs once in the contract's entire existence. Constructor arguments are ABI-encoded and appended to the deployment bytecode — which is why a block explorer "verify contract" step asks for them. During construction `msg.sender` is the deploying account, which we capture as our owner. (CMTAT in proxy deployments replaces constructors with `initialize()` functions — Chapter 06 explains why; for a standalone contract, a constructor is correct.)

**Starter code:**
```solidity
    // constructor: take initialGreeting (string memory),
    // capture deployer, store greeting



```

**Solution:**
```solidity
    constructor(string memory initialGreeting) {
        deployer = msg.sender;       // deploying account — captured once, forever
        greeting = initialGreeting;  // constructor args ride along with the initcode
    }
```

**Validation rule:** `constructor\s*\(\s*string\s+memory\s+initialGreeting\s*\)\s*\{[\s\S]*?deployer\s*=\s*msg\.sender\s*;[\s\S]*?greeting\s*=\s*initialGreeting\s*;` — checks the constructor captures deployer and stores the greeting.

### Step 4.5 — recordEntry: the business transaction

**Instruction:** Write `function recordEntry(string calldata note) external returns (uint256 entryId)`: revert with `HelloLedgerEmptyNote` if `bytes(note).length == 0`; increment `entryCount`; set `entryId = entryCount`; emit `EntryRecorded(entryId, msg.sender, note, block.timestamp)`.

**Explanation:** The full write-path pattern you will reuse in every CMTAT module: **validate → mutate → emit**, in that order (the checks-effects pattern; the "interactions" third leg arrives in later chapters). `bytes(note).length` is how you length-check a `string` — Solidity strings are raw UTF-8 byte arrays with no `.length()` of their own, closer to `byte[]` than to `java.lang.String`. Returning `entryId` serves *contract* callers; the **event** serves the bank — a transaction sent from Java does not get return values back directly (you get a receipt), which is one more reason events are the real integration contract.

**Starter code:**
```solidity
    function recordEntry(string calldata note) external returns (uint256 entryId) {
        // 1) validate: revert HelloLedgerEmptyNote if note is empty

        // 2) mutate: increment entryCount, assign entryId

        // 3) emit: EntryRecorded with entryId, msg.sender, note, block.timestamp

    }
```

**Solution:**
```solidity
    function recordEntry(string calldata note) external returns (uint256 entryId) {
        // validate — boundary check, typed error
        if (bytes(note).length == 0) revert HelloLedgerEmptyNote();
        // mutate — ^0.8 checked arithmetic: overflow would revert
        entryCount += 1;
        entryId = entryCount;
        // emit — receipt-log entry; the bank's listener consumes this,
        // NOT the return value (txs don't return values to off-chain callers)
        emit EntryRecorded(entryId, msg.sender, note, block.timestamp);
    }
```

**Validation rule:** `if\s*\(\s*bytes\(note\)\.length\s*==\s*0\s*\)\s*revert\s+HelloLedgerEmptyNote\(\)\s*;[\s\S]*?entryCount\s*\+=\s*1[\s\S]*?emit\s+EntryRecorded\s*\(\s*entryId\s*,\s*msg\.sender\s*,\s*note\s*,\s*block\.timestamp\s*\)` — checks validate → mutate → emit order with the exact event arguments.

> **Datatype/parser note:** Decoding `EntryRecorded` in Java — the per-field type mapping:
>
> | Solidity (event field) | ABI location | web3j type | Java value |
> |---|---|---|---|
> | `uint256 indexed entryId` | topic[1] | `Uint256` | `BigInteger` |
> | `address indexed recordedBy` | topic[2] | `Address` | `String` (`0x…`, checksum via `Keys.toChecksumAddress`) |
> | `string note` | data | `Utf8String` | `String` (UTF-8 decoded) |
> | `uint256 timestamp` | data | `Uint256` | `BigInteger` → `Instant.ofEpochSecond(ts.longValueExact())` |
>
> Had `note` been declared `indexed`, topic[3] would hold only its Keccak-256 hash — web3j would give you `byte[32]`, the text unrecoverable. Rule of thumb: index what you filter on, keep what you must *read* in the data section.

### Step 4.6 — Guarded mutation and the explicit getter

**Instruction:** Add `setGreeting(string calldata newGreeting)` (`external`) that reverts with `HelloLedgerNotDeployer(msg.sender)` unless `msg.sender == deployer`, stores the old greeting in a local `string memory old`, updates `greeting`, and emits `GreetingChanged(old, newGreeting, msg.sender)`. Then add `getGreeting() external view returns (string memory)` and `isContractAccount(address account) external view returns (bool)` (from Step 1.3).

**Explanation:** The deployer-only gate is the smallest possible access control — a single hardcoded principal, like a service that only accepts requests from one technical user. Chapter 05 replaces this with role-based access control mapped to bank org structure; CMTAT's AuthorizationModule is exactly that, grown up. Note we snapshot the *old* value into `memory` **before** overwriting — the audit event must carry before/after, the same before-image/after-image discipline as a database journal. The explicit getter exists because `greeting` is private; `view` keeps it a free read.

**Starter code:**
```solidity
    function setGreeting(string calldata newGreeting) external {
        // gate: only deployer; revert HelloLedgerNotDeployer(msg.sender) otherwise

        // snapshot old value, update, emit GreetingChanged(old, new, msg.sender)



    }

    // getGreeting(): external view returning the greeting


    // isContractAccount(account): external view, true if code length > 0


```

**Solution:**
```solidity
    function setGreeting(string calldata newGreeting) external {
        // single-principal gate — Chapter 05 upgrades this to full RBAC
        if (msg.sender != deployer) revert HelloLedgerNotDeployer(msg.sender);
        string memory old = greeting;     // before-image, journal-style
        greeting = newGreeting;
        emit GreetingChanged(old, newGreeting, msg.sender); // audit: before/after/who
    }

    function getGreeting() external view returns (string memory) {
        return greeting;
    }

    function isContractAccount(address account) external view returns (bool) {
        return account.code.length > 0; // false during a contract's own constructor
    }
```

**Validation rule:** `if\s*\(\s*msg\.sender\s*!=\s*deployer\s*\)\s*revert\s+HelloLedgerNotDeployer\s*\(\s*msg\.sender\s*\)[\s\S]*?emit\s+GreetingChanged\s*\(\s*old\s*,[\s\S]*?function\s+getGreeting\s*\(\s*\)\s+external\s+view` — checks the gate, the before/after audit emit, and the view getter.

### Step 4.7 — Compile and deploy

**Instruction:** Compile with Foundry, then deploy to a local dev node:

```bash
forge build
anvil &                                # local dev chain (your embedded H2 of EVMs)
forge create contracts/shared/HelloLedger.sol:HelloLedger \
  --rpc-url http://127.0.0.1:8545 \
  --private-key 0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80 \
  --constructor-args "Grüezi, Ledger"
```

**Explanation:** `forge build` compiles Solidity to two artifacts: the **bytecode** (what gets deployed) and the **ABI** (a JSON interface description — your WSDL/OpenAPI spec, which web3j consumes to generate Java stubs in Chapter 08). `forge create` then wraps initcode + ABI-encoded constructor args into a transaction with an empty `to` field and signs it with the given key (that key is anvil's well-known dev key #0 — burned, public, never for real funds). The command prints `Deployed to: 0x…` — the contract's address, derived deterministically from `keccak256(rlp(deployerAddress, nonce))`. There is nothing else to "start": no port, no process, no container. The code now exists on (dev-)chain and any node can serve calls to it. (Assumption: Foundry ≥ 0.2.x; on some versions the flag is `--constructor-args` exactly as shown, and `forge create` may require `--broadcast`.)

**Starter code:**
```bash
# compile, start a local node, deploy HelloLedger with greeting "Grüezi, Ledger"
forge build
anvil &
forge create ____________________________________ \
  --rpc-url http://127.0.0.1:8545 \
  --private-key 0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80 \
  --constructor-args ____________________
```

**Solution:**
```bash
forge build                            # -> bytecode + ABI (your WSDL/OpenAPI)
anvil &                                # throwaway local chain, prefunded dev keys
forge create contracts/shared/HelloLedger.sol:HelloLedger \
  --rpc-url http://127.0.0.1:8545 \
  --private-key 0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80 \
  --constructor-args "Grüezi, Ledger"
# Output: "Deployed to: 0x5FbDB2315678afecb367f032d93F642f64180aa3"
# That address = your service endpoint, forever. No server to keep running.
```

**Validation rule:** `forge\s+create\s+contracts/shared/HelloLedger\.sol:HelloLedger[\s\S]*?--constructor-args` — checks the deploy command targets `HelloLedger` and passes constructor args.

> **Banking integration note:** Verifying the deployment from the bank side — the smoke test your release runbook would script:
>
> ```java
> Web3j web3 = Web3j.build(new HttpService("http://127.0.0.1:8545"));
>
> // 1) Is there code at the address? (EOA would return "0x")
> String code = web3.ethGetCode(deployedAddress, DefaultBlockParameterName.LATEST)
>     .send().getCode();
> if ("0x".equals(code)) throw new IllegalStateException("no contract at " + deployedAddress);
>
> // 2) Free view call: read the greeting (no gas, no tx)
> org.web3j.abi.datatypes.Function getGreeting = new org.web3j.abi.datatypes.Function(
>     "getGreeting",
>     java.util.Collections.emptyList(),
>     java.util.List.of(new org.web3j.abi.TypeReference<org.web3j.abi.datatypes.Utf8String>() {}));
> String callData = org.web3j.abi.FunctionEncoder.encode(getGreeting);
> String raw = web3.ethCall(
>         org.web3j.protocol.core.methods.request.Transaction
>             .createEthCallTransaction(null, deployedAddress, callData),
>         DefaultBlockParameterName.LATEST)
>     .send().getValue();
> String greeting = (String) org.web3j.abi.FunctionReturnDecoder
>     .decode(raw, getGreeting.getOutputParameters()).get(0).getValue();
> // greeting -> "Grüezi, Ledger"
> ```
>
> `eth_call` with `from = null` works because `view` functions need no signer — the read side of your adapter never touches key material. The write side (calling `recordEntry`) reuses the raw-transaction flow from Lesson 2's note; Chapter 08 industrializes both.

---

## Assembled contract — `contracts/shared/HelloLedger.sol`

```solidity
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
```

---

## Chapter quiz

**Q1 (multiple choice).** A payment instruction arrives at your bank signed by a partner bank's HSM. What is the closest EVM equivalent?
a) A `view` call via `eth_call`
b) A transaction signed by an EOA's private key
c) A contract account calling another contract
d) An event emitted in a receipt

**Answer: b.** An EOA's ECDSA signature plays the HSM-signed-message role: it proves origin authority at the protocol level, and only EOAs can *initiate* transactions. (c) is wrong because contract accounts never initiate — they only act when called within an existing transaction.

**Q2 (multiple choice).** A transaction calling `recordEntry("")` is included in block 19,000,000 and reverts with `HelloLedgerEmptyNote()`. Which statement is true?
a) The transaction disappears; the sender pays nothing
b) `entryCount` is incremented but no event is emitted
c) All state changes are rolled back, but the transaction is on-chain and gas was paid
d) The event is emitted but `entryCount` rolls back

**Answer: c.** Revert = full atomic rollback of storage writes *and* events, but unlike a DB rollback the failed transaction remains recorded in the block and the gas consumed up to the revert is charged — like being billed for an abended batch job.

**Q3 (short answer).** Your reconciliation job sees an `EntryRecorded` event in the latest block. Why must it NOT immediately release the corresponding CHF payment leg, and what two block tags / policies address this?

**Answer:** The latest block can still be replaced by a **reorg**, which would erase the event — inclusion is "processing," not "settlement finality." The job should either wait a confirmation depth (`currentBlock - eventBlock >= N`) or read/act only at the `FINALIZED` (or `SAFE`) block tag, which marks blocks that cannot be reverted without an economically prohibitive attack. (Legal mapping: Chapter 09.)

**Q4 (multiple choice).** Which Solidity → web3j/Java mapping is WRONG?
a) `uint256` → `Uint256` → `BigInteger`
b) `address` → `Address` → `String`
c) `string` → `Utf8String` → `String`
d) `uint256` → `Uint256` → `long`

**Answer: d.** `long` is 64-bit signed (max ~9.2×10^18) — it overflows on amounts as small as 10 ether in wei. All 256-bit integers must stay `BigInteger` end-to-end.

**Q5 (short answer).** `HelloLedger.greeting` is declared `private`. A competitor bank runs a full node. Can they read the greeting? Why or why not?

**Answer:** Yes. `private` only restricts access *from other contracts' code* — it is a compile-time visibility rule, not encryption. Every node holds the full world state, so any storage slot is readable by anyone (e.g. `eth_getStorageAt`). Never store confidential data on-chain; this is closer to a column hidden from one application but present in a replicated database every participant hosts.

**Q6 (multiple choice).** Deploying a contract differs from deploying a WAR in that:
a) The constructor re-runs every time a node restarts
b) The code can be hot-patched at the same address by the deployer
c) The constructor runs exactly once, and the runtime code at the address is immutable
d) An application server process must keep running for the contract to respond

**Answer: c.** The constructor executes only inside the deployment transaction and is discarded; the returned runtime bytecode is permanent at its address. (Upgradeability exists only via the proxy pattern — Chapter 06.) No process or server is needed afterward; every node can serve the contract.

**Q7 (short answer).** In `EntryRecorded`, `entryId` and `recordedBy` are `indexed` but `note` is not. Give one reason for each choice.

**Answer:** `indexed` fields become log **topics**, so the bank's listener can filter ("all entries by account X" / "entry #42") server-side without scanning every log — like MQ routing keys. `note` stays non-indexed because indexing a `string` stores only its Keccak-256 hash, making the actual text unrecoverable; data-section fields keep their full ABI-encoded value.

**Q8 (multiple choice).** Why does `postBatch` cap its input at 100 entries?
a) Solidity arrays cannot exceed 100 elements
b) More than 100 entries would exceed the mapping's capacity
c) Unbounded per-transaction work can exceed the block gas limit, making the call permanently fail
d) Events can only be emitted 100 times per transaction

**Answer: c.** Gas is the per-transaction CPU quota and blocks have a hard gas limit; unbounded loops grow until the function can no longer execute at all — the on-chain version of a batch job that outgrows its window. Chunk the work and push pagination to the caller.

---

**Next:** Chapter 02 — Solidity Datatypes for Banking Integrators: the full `[TYPES-heavy]` treatment of `uint256` money math, `bytes32` ISIN/LEI, enums, structs, and the complete Solidity↔web3j mapping table.
