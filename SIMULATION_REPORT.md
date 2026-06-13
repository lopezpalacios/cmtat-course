# Persona Simulation & Adaptation Report

Five demographic-grounded personas (see `PERSONAS.md`) took the course (ch01, ch04,
ch08 + landing) in character. Reactions were SSR-scored (arXiv:2510.08338) via local
`nomic-embed-text` → `SSR_REPORT.md`. Target demographic: **delivery/services Java
developers, India + Eastern/Central Europe**.

## What the simulation found (convergent across all 5)

**🔴 Chapter 08 (web3j adapter) is broken — unanimous, lowest SSR on every axis.**
Every persona independently flagged the same defects in the Java:
- `new EthFilter(..., "CmtatToken")` passes a contract *name* where a 0x **address** is required — won't run; the checker even locks the broken string.
- Imports a non-existent `FunctionDecoder` (web3j is `FunctionReturnDecoder`); other classes used but unimported.
- "Nonce conflict", "gas strategy", "idempotency" steps are **byte-identical copies** of the basic submit — titles promise work the code never does.
- Idempotency = in-memory `HashSet` (lost on restart) — **contradicts ch04's** on-chain `operationId` dedupe.
- Reorg step checks a receipt once; audit trail stores only a hash (data loss).
This is the chapter most relevant to a delivery Java dev — and the trust-killer.

**🟠 Demographic fit — "Swiss in-house" framing misfits delivery/services devs.**
- Course assumes you *own/operate* the core (HSMs, EOD batch, SIC) — these devs *integrate to spec* for clients. Asked for a "if you build to spec rather than own it" reframe.
- All examples are CHF/Rappen/SIC. Asked for a second running example: **India** (UPI/IMPS/NPCI, INR paise) and **CEE/SEPA**.
- Core-banking concepts are in the *prerequisites*; juniors (Priya) don't have them → asked to **teach the term in 2 lines before using it as an analogy**, plus a **finance glossary** (registrar, custody, coupon, NAV, par, EOD batch, HSM, omnibus, ISO 20022…).

**🟠 Java-specific asks.**
- Map CMTAT modules → Spring concepts (`@PreAuthorize`/RBAC, interceptor chain, `@KafkaListener` offsets).
- Carry the **HSM/MPC signer** into code (every ch08 step hardcodes `Credentials.create("your-private-key")`, contradicting ch01's own warning).
- Provide a **runnable Maven project** (pom + anvil/testcontainers) so `mvn test` goes green; generate typed wrappers via `web3j` plugin instead of hand-encoding.

**🟡 Low-level CS gaps (Priya, Miloš).** `unchecked`/overflow/bytes/addresses need a gentle primer + a "break it" step (trigger an overflow, watch it revert). Lead with the instruction; collapse deep banking prose.

## SSR (1-5, higher better; ease higher = easier)
ch04 best (clarity 3.34, relevance 3.24); ch08 worst (trust 2.95, engagement 2.85). Full matrix in `SSR_REPORT.md`.

## Adaptation plan (this iteration)
1. **Rewrite ch08** to compile and do what each step claims (real EthFilter address, EIP-1559 gas, PENDING-nonce + replacement-by-fee, durable+on-chain idempotency, confirmation-depth reorg, hash-chain audit, HSM-backed signer). [qwen + verify]
2. **Delivery-dev adaptation layer** (no rewrite of 18 chapters): a **glossary** + a **"For Java delivery engineers" companion** (Spring↔CMTAT map, build-to-spec reframe, India/CEE examples), surfaced in the player; landing reframed to address delivery devs in India & CEE.
3. **Teacher section**: password-protected, **encrypted** per-chapter lecture notes (client-side decrypt; ciphertext-only in the public repo) tuned to this demographic's misconceptions.
