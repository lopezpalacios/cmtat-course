# CMTAT Engineering Course — Chapter Map

**Audience:** Core-banking developers (Java/.NET) at Swiss TradFi banks. Zero blockchain background.
**Goal:** Ship production-grade tokenized securities on EVM using the CMTAT standard, with full off-chain bank-side integration in Java/web3j.
**Format:** CryptoZombies-style. Every lesson = ordered steps `{instruction, explanation, starter_code, solution_code, validation_rule}`. Each chapter ends with an assembled working contract + quiz.
**Emphasis threads:** `[BANK]` = banking integration (events, web3j adapters, reconciliation, roles, pause/freeze, idempotency, audit). `[TYPES]` = parsers & datatypes (uint256/decimals money, bytes32 ISIN/LEI, enums, structs, ABI codec, Solidity↔web3j type mapping).

---

## Part I — Shared Core (all learners)

### Chapter 01 — From Core Banking to the EVM `[shared] [BANK] [TYPES-light]`
Objective: Build the EVM mental model from a JVM/core-banking engineer's vantage point. Accounts vs bank accounts, private keys vs HSM signing, transactions vs payment messages, gas vs CPU quota, blocks vs end-of-day batch, finality vs settlement finality. Deploy a first contract.
Contract: `contracts/shared/HelloLedger.sol`

### Chapter 02 — Solidity Datatypes for Banking Integrators `[shared] [TYPES-heavy]`
Objective: Master every Solidity type that crosses the bank boundary: `uint256` + decimals for monetary amounts, fixed-point money math, `bytes32` for ISIN/LEI/identifiers, `address` validation & checksums, `enum` lifecycle states, `struct` instrument metadata, `mapping` as the position-keeping table. Full Solidity↔web3j Java type-mapping table.
Contract: `contracts/shared/InstrumentTypes.sol` · Java: `java-adapters/TypeMappingDemo.java`

### Chapter 03 — Functions, Modifiers, and Events as the Integration Contract `[shared] [BANK-heavy]`
Objective: Visibility, `view`/`pure`, custom errors, modifiers as policy gates, and events as THE interface between chain and core banking. Indexed topics, log decoding, event-driven reconciliation.
Contract: `contracts/shared/EventDrivenLedger.sol` · Java: `java-adapters/EventLogParser.java`

### Chapter 04 — Building the ERC-20 Share Ledger `[shared] [TYPES] [BANK]`
Objective: Build a minimal ERC-20 from scratch — balances mapping = securities register; `decimals` and monetary representation; transfer/approve semantics; Transfer events as booking entries.
Contract: `contracts/shared/BankERC20.sol` · Java: `java-adapters/Erc20Operations.java`

### Chapter 05 — Access Control: Mapping Bank Org Structure On-Chain `[shared] [BANK]`
Objective: Role-based access control built from scratch (OZ AccessControl pattern): DEFAULT_ADMIN_ROLE, MINTER_ROLE, BURNER_ROLE, PAUSER_ROLE, ENFORCER_ROLE → mapped to issuer, registrar/transfer agent, compliance officer, operations. Four-eyes patterns, role admin hierarchies, audit of role grants via events.
Contract: `contracts/shared/RoleControlled.sol` · Java: `java-adapters/RoleAuditTrail.java`

### Chapter 06 — CMTAT Architecture: A Module Tour `[shared] [BANK] [TYPES]`
Objective: The CMTAT standard (CMTA, Switzerland): why it exists, module decomposition — ERC20BaseModule, BaseModule (tokenId/terms/information), PauseModule, EnforcementModule (freeze), ERC20SnapshotModule, ValidationModule + RuleEngine, DocumentModule, DebtModule, AuthorizationModule. Standalone vs proxy deployment. Build the skeleton base.
Contract: `contracts/shared/CMTATBase.sol`

### Chapter 07 — Compliance Modules: Pause, Freeze, Transfer Rules `[shared] [BANK-heavy]`
Objective: Implement CMTAT compliance surface: PauseModule (market-wide halt), EnforcementModule (address freeze for sanctions/court order), ValidationModule delegating to a RuleEngine (whitelist rule). Regulatory-action runbooks: who calls what, which events the bank's monitoring must consume.
Contract: `contracts/shared/ComplianceToken.sol` + `contracts/shared/WhitelistRuleEngine.sol` · Java: `java-adapters/ComplianceMonitor.java`

### Chapter 08 — The Bank-Side Adapter: web3j End-to-End `[shared] [BANK-heavy] [TYPES-heavy]`
Objective: Build the complete Java adapter layer: ABI encode/decode by hand and via web3j, event-log subscription + replay from block N, transaction submission with nonce management, gas strategy, idempotency keys, off-chain settlement reconciliation loop, append-only audit trail. Error handling: reorgs, dropped txs.
Java: `java-adapters/CmtatBankAdapter.java`, `java-adapters/ReconciliationJob.java`, `java-adapters/IdempotentTxSender.java`

### Chapter 09 — Swiss Regulatory Context for Token Engineers `[shared] [REG]`
Objective: The single regulatory chapter. FINMA guidance, Swiss DLT Act (ledger-based securities, OR Art. 973d ff.), DLT trading facility license, custody (segregation, bankruptcy remoteness), KYC/AML (AMLA, Travel Rule), transfer agent & registrar roles, CMTA's role. Engineering checklists per regulation. All other chapters reference this one.
No contract — reference chapter with engineering checklists.

---

## Part II — Track A: Tokenized Bond `[track-A]`

### Chapter 10 — Bond Instrument Modeling with the Debt Module `[A] [TYPES-heavy]`
Objective: Model a CHF fixed-rate bond: CMTAT DebtModule pattern (debt info struct: interestRate, parValue, maturityDate, couponFrequency, ISIN, rating fields), bytes32 identifiers, basis-point rates, day-count conventions in integer math.
Contract: `contracts/bond/BondToken.sol` (v1: debt metadata) · Java: `java-adapters/BondMetadataReader.java`

### Chapter 11 — Coupons: Record Dates and Snapshots `[A] [BANK] [TYPES]`
Objective: Snapshot mechanism for coupon record dates; coupon computation in fixed-point (rate × parValue × holdings / denominators, rounding policy); CouponDeclared/CouponPaid events; bank-side coupon-payment batch job consuming snapshot balances.
Contract: `contracts/bond/BondToken.sol` (v2: + snapshots/coupons) · Java: `java-adapters/CouponPaymentJob.java`

### Chapter 12 — Maturity, Redemption, and the Full Bond `[A] [BANK]`
Objective: Lifecycle enum (Issued → Active → Matured → Redeemed → Defaulted), maturity enforcement, redemption burn-against-payment flow with off-chain CHF leg, credit events. Assemble final TokenizedBond contract with all CMTAT modules + run full issuance→coupon→redemption scenario.
Contract: `contracts/bond/TokenizedBond.sol` (final) · Java: `java-adapters/RedemptionSettlement.java`

---

## Part III — Track B: Equity / Share Token `[track-B]`

### Chapter 13 — The Share Register On-Chain `[B] [BANK] [TYPES]`
Objective: Registered shares (Namenaktien) as CMTAT tokens: share metadata struct (ISIN, nominal value, share class), registrar role = transfer agent, shareholder identity binding (on-chain address ↔ off-chain KYC record), DocumentModule for articles of association / terms.
Contract: `contracts/equity/ShareToken.sol` (v1) · Java: `java-adapters/ShareRegisterSync.java`

### Chapter 14 — Snapshots for Dividends and Voting `[B] [BANK-heavy]`
Objective: ERC20SnapshotModule pattern in depth: scheduled snapshots, record-date semantics for dividends and general-assembly voting; dividend computation + withholding tax (35% Swiss) in integer math; voting-power export to off-chain GA system.
Contract: `contracts/equity/ShareToken.sol` (v2: + snapshots) · Java: `java-adapters/DividendDistributionJob.java`

### Chapter 15 — Corporate Actions and the Full Equity Token `[B] [BANK]`
Objective: Corporate actions as engineering problems: stock split (rebase vs reissue), capital increase (rights issue mint flow), share buyback/cancellation (burn), squeeze-out via forced transfer (EnforcementModule). Assemble final EquityShareToken with full module set + corporate-action runbooks.
Contract: `contracts/equity/EquityShareToken.sol` (final) · Java: `java-adapters/CorporateActionProcessor.java`

---

## Part IV — Track C: Money-Market Fund Share `[track-C]`

### Chapter 16 — Fund Shares and NAV On-Chain `[C] [TYPES-heavy] [BANK]`
Objective: MMF share token: NAV as oracle-fed fixed-point value (6-decimal price), NAV publisher role = fund accountant, staleness guards, NAVUpdated events; subscription at NAV (CHF in → shares out).
Contract: `contracts/mmf/FundShareToken.sol` (v1: + NAV oracle) · Java: `java-adapters/NavPublisher.java`

### Chapter 17 — The Daily Settlement Cycle `[C] [BANK-heavy]`
Objective: T+0/T+1 fund ops on-chain: subscription/redemption order queue (structs + enum order states), daily cut-off, batch settlement at struck NAV, partial fills/gates, idempotent order processing, end-of-day reconciliation between chain and fund accounting.
Contract: `contracts/mmf/FundShareToken.sol` (v2: + order queue) · Java: `java-adapters/DailySettlementEngine.java`

### Chapter 18 — Redemptions, Gates, and the Full MMF Share `[C] [BANK] [TYPES]`
Objective: Redemption payout flow with off-chain CHF leg, liquidity gates & redemption suspension (pause + custom gates), fee accrual in integer math. Assemble final MoneyMarketFundShare with full CMTAT module set + complete daily-cycle scenario.
Contract: `contracts/mmf/MoneyMarketFundShare.sol` (final) · Java: `java-adapters/RedemptionPayoutJob.java`

---

## Conventions (all chapters)

- Solidity `^0.8.20`, self-contained contracts (no external imports — OZ/CMTAT patterns re-implemented inline and labeled as such).
- Every monetary amount: `uint256` smallest-unit integers + explicit `decimals`; rounding policy stated.
- Identifiers: ISIN/LEI as `bytes32` (right-padded ASCII).
- Every state change emits an event; events carry idempotency-friendly identifiers.
- Validation rules are regex/AST pattern checks on the learner's step code, not full compiles.
- Java side: web3j 4.x, examples runnable against any JSON-RPC endpoint.
