# CMTAT Course — Didactic & Design Improvement Plan

Prepared by a 4-expert committee review of the current player and content:
**MIT curriculum/assessment architect · Khan-style mastery-learning designer ·
Udemy top-instructor (engagement/retention) · Swiss-bank brand/UX designer.**

The *content* is strong (well-sequenced steps, real JVM/banking analogies, authored
quizzes, A/B/C tracks). The *player* discards ~half of it and ships a partly-invalid
grader. Everything below is static-site-safe (vanilla JS + `build_player.py`); no backend.

---

## Committee verdict — consensus problems (ranked)

| # | Problem | Severity | Consensus |
|---|---------|----------|-----------|
| 1 | **Checkers grade comment prose, not code.** Many regexes (from deterministic augmentation) key on the exact `// comment` of the solution. Correct code with a different comment FAILS; copy-paste passes. Invalid assessment + trains copy-through. | 🔴 Critical | MIT + Khan |
| 2 | **No step gating.** Whole chapter (~20-25 steps) dumped on one scroll; `.locked` CSS exists but is never used. Cognitive overload; CryptoZombies' one-step-at-a-time mechanic is lost. | 🔴 Critical | All 3 |
| 3 | **No progress/code persistence.** No `localStorage`; refresh wipes code + position. Fatal for interrupt-driven professional learners. | 🔴 Critical | All 3 |
| 4 | **Quizzes authored but never surfaced.** Each chapter has a real quiz (MC + short-answer + rationale) the player never renders — the only transfer-level assessment, dead on disk. | 🟠 High | All 3 |
| 5 | **Objectives/explanations buried.** Chapter/lesson learning objectives, prerequisites, `[BANK]`/`[TYPES]` tags dropped; the "Why" analogy (the load-bearing scaffold) hidden behind a collapsed `<details>`. | 🟠 High | MIT + Khan |
| 6 | **No onboarding/landing.** Cold-opens into chapter 1's step wall. The course's killer hook (your-world→EVM mapping) and the "zero blockchain needed" premise are never pitched. | 🟠 High | Udemy + MIT |
| 7 | **No track framing.** Flat 18-chapter list hides "9 core + pick ONE 3-chapter track"; perceived scope ~40% larger than reality. | 🟠 High | Udemy + MIT |
| 8 | **No hint ladder.** Only "Show solution" (all-or-nothing) — kills productive struggle. Need nudge → targeted hint → reveal. | 🟡 Medium | Khan |
| 9 | **No momentum.** No "Next →", no progress %, no mastery bars, no completion celebration, no time estimates, generic `error_hint` ("Your code should match…"). | 🟡 Medium | Udemy + Khan |
| 10 | **Visual identity wrong.** Dark GitHub-dev theme for a Swiss-bank professional audience. | 🟡 Medium | Swiss-bank designer |

---

## Decisions (what we will do)

1. **Fix the grader first** (P0). Regenerate every checker from the lenient "Validation rule" prose (which is already correct in the markdown), strip `//…` comments from both pattern and buffer before matching, and add a **CI assertion: starter must FAIL, solution must PASS** for every step. No gating is worth anything on an invalid grader.
2. **One-step-at-a-time flow with gating + persistence** (P1) — the core CryptoZombies mechanic, plus `localStorage` for code/position/verdicts. Distinguish *passed clean* (green) vs *revealed* (amber) for an honest mastery bar.
3. **Surface quizzes + objectives** (P2) — parse them in `build_player.py`; render interactive quiz (MC auto-graded, short-answer self-graded with deferred rationale); show objectives/"You will build" as a chapter header card.
4. **Onboarding + track framing + momentum** (P3) — landing screen (value prop + bank↔EVM mapping teaser + "you'll build" + prerequisites/tooling), sidebar grouped by Part/Track with a post-Ch09 "choose your path", progress %, "Next →", completion celebration, time estimates.
5. **Hint ladder + warm microcopy** (P4) — 3-rung disclosure; per-step real hints in `error_hint`.
6. **Swiss-bank reskin** (P5) — apply the design system below.

---

## Swiss-bank design system (white / red / black)

Mental model shift: **this is a document, not an IDE.** White paper, black ink, one
disciplined red used only as a *signal* (active nav, the hairline rule, primary action,
errors). Restraint is the brand; whitespace is the luxury. Light-mode canonical.

**Tokens**
- Surfaces: `--bg #FFFFFF`, `--bg-side #FAFAF9`, `--surface-sunk #F6F6F5`
- Ink: `--ink #0A0A0A`, `--ink-2 #52524E`, `--ink-3 #8A8A85`
- Lines: `--line #E4E4E1`, `--line-strong #CFCFCB`
- Red signal: `--red #C8102E`, `--red-hover #A50D26`, `--red-active #85091E`, `--red-wash #FBEEF0`
- Success (only second hue, muted): `--ok #1E7A46`, `--ok-wash #EBF4EF`
- Error reuses `--red` (one signal color)

**Type** — system stack (no web-font dep) + monospace for code; 16px/1.65 body; 720px
content measure; **uppercase tracked eyebrows** (`+0.12em`) for labels/sidebar/tags;
tabular-nums for counters; only weights 400/600.

**Signature elements** — full-width **2px red hairline** under a white masthead;
sidebar active state = flush-left **2px red margin marker** + `--red-wash` (not a filled
pill); step cards = 1px lines, **no shadows**; primary "Check" button = solid red; verdict
= bordered strip with a 3px coloured left "spine"; slim 2px red progress fill in masthead.

**A11y** — all pairings WCAG-AA verified; `:focus-visible` red outline; verdicts pair
colour with ✓/✗ glyph + text (never colour alone); `prefers-reduced-motion` honoured;
`role="status" aria-live="polite"` on verdicts; heading levels not skipped.

Full token block + component CSS captured from the design review and ready to drop into
`player/index.html`.

---

## Phased plan (ordered by leverage)

- **P0 — Grader integrity** (`pm/`): rebuild checkers from the prose validation rules;
  comment-insensitive matching; CI assert starter-fails/solution-passes across all steps.
- **P1 — Flow + state** (`index.html`): render one step at a time; gate next on pass;
  `localStorage` for code/verdicts/position; resume; per-chapter mastery bar (clean vs revealed).
- **P2 — Assessment + objectives** (`build_player.py` + `index.html`): parse quizzes &
  objective/prereq/tags into `course.json`; interactive quiz UI with deferred rationale;
  chapter header card; explanations open by default.
- **P3 — Onboarding + structure + momentum** (`index.html` + build): landing screen;
  sidebar grouped by Part/Track + "choose your path" after Ch09; progress %; "Next →";
  completion celebration; time estimates (≈1 min/step or authored).
- **P4 — Hints + microcopy**: 3-rung hint ladder; populate per-step `error_hint`.
- **P5 — Swiss-bank reskin**: apply the design system; restructure shell (masthead + flex
  row + 720px column); add progress + quiz components.

**Build surface:** roughly half the work is in `pm/build_player.py` (parse more: quizzes,
objectives, tags, hints; fix checker generation + CI gate) and half in `player/index.html`
(state machine, gating, persistence, quiz/landing/celebration rendering, reskin). No
framework, no backend — stays GitHub-Pages-deployable.

**Success criteria:** every step's starter fails & solution passes; one-step flow with
resume; quizzes playable; landing + track selection present; Swiss-bank look; all checkers
still valid under the player's JS RegExp (`pm/verify_player.js`).

---

## Status: SHIPPED (all phases P0-P5)

- P0 grader: 0 comment-locked, 0 solution-fails, 3 benign assembly freebies; `check_grader.py` gate.
- P1 flow: one-step gating + localStorage persistence + mastery bars + non-destructive review.
- P2 assessment: richer `course.json`; 18 quizzes normalized (90 Qs, MC auto-graded + short-answer self-grade with deferred rationale); objectives/tracks/estimates surfaced.
- P3 onboarding: landing (value prop + your-world→EVM map + you'll-build), Part/Track sidebar, post-core track selection, % progress, Next→, completion screens.
- P4 hints: 3-rung ladder + warmer microcopy.
- P5 design: Swiss-bank white/red/black system, dependency-free, WCAG-AA.

Live: https://lopezpalacios.github.io/cmtat-course/
