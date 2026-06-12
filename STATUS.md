# Status

## Content (authored by qwen2.5-coder:14b, local, via ollama)
- [x] course/00-chapter-map.md
- [x] Shared core: ch 01, 02, 03, 04, 05, 06, 07, 08, 09
- [x] Track A (bond): ch 10, 11, 12
- [x] Track B (equity): ch 13, 14, 15
- [x] Track C (mmf): ch 16, 17, 18
- [x] 18 chapters + map · 73 lessons · 153 steps · 85 machine-readable checkers

## Contracts (assembled, forge build)
- [x] contracts/shared/*.sol (8)
- [x] contracts/bond/{BondToken,TokenizedBond}.sol
- [x] contracts/equity/{ShareToken,EquityShareToken}.sol
- [x] contracts/mmf/{FundShareToken,MoneyMarketFundShare}.sol
- [x] `forge build` — 14 files compile clean with Solc 0.8.20 (warnings only)

## Verification
- [x] All ```checker blocks valid JSON, regex compiles, matches its solution_code
- [x] Per-track contract evolution chained (v1 -> v2 -> final) — gap fix #3
- [x] player/course.json + player/index.html runtime — gap fix #2

## Gap fixes vs PROMPT.md v1 (see PROMPT_v2.md)
- [x] #1 machine-readable checker schema (CHECKER_SCHEMA.md)
- [x] #2 runtime defined (static player)
- [x] #3 per-track serialization of evolving contracts
- [x] #4 STATUS protocol honored by harness

## Known follow-ups (non-blocking)
- Chapters 01/03/04/09 (v1, pre-schema) have prose validation rules; not all steps
  carry a ```checker block yet — player self-checks those against the solution.
- Compile-hardened .sol files (fix_contracts.py) may differ slightly from the
  teaching version printed at each chapter's end; contracts/ is canonical.

<<COURSE COMPLETE>>
