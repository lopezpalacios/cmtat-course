# CMTAT Tokenized-Securities Course for Core-Banking Developers

Interactive, CryptoZombies-style course teaching Solidity + CMTAT tokenized-securities engineering to Java/.NET core-banking developers at Swiss banks. Zero blockchain background assumed.

## Structure

- `course/00-chapter-map.md` — full outline: 9 shared chapters (incl. 1 Swiss regulatory chapter) + 3 chapters per instrument track.
- `course/<NN>-<slug>.md` — one file per chapter. Each chapter = lessons = ordered steps; each step has `instruction`, `explanation`, `starter_code`, `solution_code`, `validation_rule` (regex/AST pattern that gates progression). Chapters end with the assembled contract + quiz.
- `contracts/shared|bond|equity|mmf/*.sol` — the contracts each chapter assembles. Self-contained Solidity ^0.8.20 (CMTAT/OZ patterns re-implemented inline), compiled with `forge build`.
- `java-adapters/*.java` — bank-side web3j adapters: event-log parsing, ABI decoding, tx submission, reconciliation, idempotency.

## Tracks

| Track | Instrument | Chapters | Final contract |
|---|---|---|---|
| Shared | Core EVM + Solidity + CMTAT + integration | 01–09 | `ComplianceToken.sol` et al. |
| A | Tokenized bond (debt module, coupons, redemption) | 10–12 | `TokenizedBond.sol` |
| B | Equity / share token (snapshots, registrar, corporate actions) | 13–15 | `EquityShareToken.sol` |
| C | Money-market fund share (NAV, daily settlement) | 16–18 | `MoneyMarketFundShare.sol` |

## Emphasis threads (woven through every chapter)

1. **Banking integration** — events as the integration contract, web3j adapters, reconciliation, role-mapping to bank org structure, pause/freeze for regulatory action, idempotency + audit trails.
2. **Parsers & datatypes** — uint256/decimals money, bytes32 ISIN/LEI, enums, structs, ABI codec, full Solidity↔web3j Java type mapping.

Regulatory grounding (FINMA, Swiss DLT Act, custody, KYC/AML) consolidated in Chapter 09.

## Compile

```sh
forge build   # foundry.toml points src at contracts/  — 14 contracts, Solc 0.8.20, clean
```

## Run the interactive player

Each step carries a machine-readable `checker` (a regex over the learner's code) that
gates progress, CryptoZombies-style. The zero-dependency static player runs these
in-browser:

```sh
python3 pm/build_player.py          # (re)build player/course.json from the markdown
python3 -m http.server -d player    # then open http://localhost:8000
```

The markdown under `course/` is the canonical source; the player is generated from it,
so the course is fully usable as plain markdown too.

## How this was built

Content (every chapter, contract, and Java adapter) was authored by a **local LLM**
(`qwen2.5-coder:14b` via Ollama). The `pm/` directory is the orchestration harness that
prompted the model per chapter, chained each track's evolving contract, then **verified**
the output — validating every checker regex against its own solution and compile-hardening
contracts with `forge build` in a fix-and-retry loop. See `PROMPT_v2.md` and
`CHECKER_SCHEMA.md` for the spec the model was held to.

See `STATUS.md` for per-chapter progress and compile results, `ASSUMPTIONS.md` for decisions made autonomously.
