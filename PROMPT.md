You are building a complete, interactive, browser-based course that teaches Solidity and tokenized-securities engineering to CORE-BANKING DEVELOPERS at Swiss TradFi banks (strong Java/.NET engineers, ZERO blockchain background) entering the EVM space.

You are running FULLY AUTONOMOUS and UNSUPERVISED. Write everything to files under the current directory. Do NOT ask questions. Do NOT stop until the whole course is done.

EXECUTION MODE — AUTONOMOUS, RUN TO COMPLETION:
- Never pause for approval, confirmation, or feedback. Make every decision yourself. If something is ambiguous, pick the most reasonable option, write the assumption in an `ASSUMPTIONS.md` line, and keep going.
- Produce the ENTIRE course — chapter map + all three tracks, every chapter, every lesson, every step. Do not stop until the last chapter quiz is written and `<<COURSE COMPLETE>>` is appended to `STATUS.md`.
- Never leave "TODO" / "left as exercise" placeholders — write the real content everywhere.
- To go fast, USE PARALLEL SUBAGENTS (Task/Agent tool): after you write the chapter map, spawn one subagent per chapter to generate that chapter's full content to its file, batching several at a time. Shared/core chapters first, then the three tracks.

FILE LAYOUT (create as you go):
- `course/00-chapter-map.md` — full outline.
- `course/<NN>-<slug>.md` — one file per chapter, full lesson-by-lesson content.
- `contracts/<track>/<Name>.sol` — the assembled Solidity contract(s) each chapter builds.
- `java-adapters/<Name>.java` — the web3j off-chain parser/adapter snippets.
- `ASSUMPTIONS.md`, `STATUS.md` (append progress after each chapter), `README.md` (how the course is structured).
- If `forge`/`solc` is available, compile each finished contract and record pass/fail in `STATUS.md`. Fix until it compiles.

PEDAGOGY — model on CryptoZombies:
- Lessons = sequences of short steps. Each step: instruction + explanation, annotated code, and a CHECKER validation_rule (regex or required AST node — pattern-checkable, NOT full compile) that gates the next step.
- Each chapter ends with a working contract the learner assembled incrementally.
- Assume zero EVM knowledge: teach accounts, gas, types, deployment from scratch, always mapping EVM concepts to JVM/.NET equivalents a core-banking dev already knows.

DOMAIN ANCHOR — CMTAT:
- Teach against the CMTAT standard (CMTA Token, Solidity reference impl). Use REAL CMTAT module names/patterns (pause, freeze, snapshot, document/terms, validation/transfer-rules, debt, ERC-20 base). Don't invent APIs — if unsure, state the assumption inline and continue.
- THREE PARALLEL TRACKS, each builds one instrument to completion:
  (A) Tokenized bond — debt module, coupon, maturity, redemption
  (B) Equity/share token — snapshot for dividends/voting, registrar
  (C) Money-market fund share — NAV, daily settlement, redemption
  Shared core chapters teach common ground; track-specific chapters specialize.

TWO MANDATORY EMPHASIS THREADS — in EVERY chapter where relevant:
1. Banking integration: events as the integration contract; oracle/registrar bridges; off-chain settlement reconciliation; role-based access mapped to bank org structure; pause/freeze for regulatory action; idempotency + audit trails. Show the OFF-CHAIN side in JAVA using web3j — ABI decode, event-log parsing, sending transactions from a core-banking adapter.
2. Parsers & datatypes: deep explicit treatment of Solidity types for integration — uint256 + decimals for monetary amounts, fixed-point money, bytes32 for ISIN/LEI/identifiers, address validation, enums for lifecycle states, structs for instrument metadata, ABI encode/decode. For each, show how the Java/web3j parser on the bank side decodes the same data (Solidity type <-> web3j Java type mapping).

REGULATORY:
- ONE dedicated chapter consolidates Swiss regulatory context (FINMA, Swiss DLT Act, custody, KYC/AML, transfer-agent/registrar roles). Elsewhere stay engineering-first; reference that chapter rather than re-explaining law.

LANGUAGE: English. Solidity ^0.8.x.

PER-LESSON OUTPUT FORMAT:
- Title + learning objective + emphasis-thread tag(s) + track (shared/A/B/C)
- Ordered steps, each = { instruction, explanation, starter_code, solution_code, validation_rule }
- "Banking integration note" (with Java/web3j snippet) and "Datatype/parser note" callouts where relevant
- End-of-chapter: assembled contract + short quiz

SEQUENCE:
1. Write `course/00-chapter-map.md` (titles + objectives + emphasis tags + track labels + shared-vs-track split). Aim for a thorough course: ~6-8 shared chapters + 3-5 chapters per track + 1 regulatory chapter.
2. WITHOUT STOPPING, generate every chapter to its file (use parallel subagents), through all three tracks.
3. Compile contracts where possible, fix failures, update `STATUS.md`.
4. Append `<<COURSE COMPLETE>>` to `STATUS.md` when fully done.

Begin now.
