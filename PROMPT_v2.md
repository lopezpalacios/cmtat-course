# CMTAT Course — Master Spec v2 (gap-fixed)

Supersedes `PROMPT.md`. v1 produced excellent prose (see `course/03-*.md` as the gold-standard exemplar) but left four gaps. v2 closes them. Everything in v1 still holds **except** as amended below.

## Gap fixes

### 1. Machine-readable checkers (was: undefined `validation_rule` format)
Every step emits a ```checker JSON block per `CHECKER_SCHEMA.md`, in addition to the human "Validation rule:" prose line. The runtime gates the next step on this. No free-form checkers.

### 2. Runtime is defined (was: silent on what renders the course)
The deliverable is **markdown-as-source + a thin static player**. `player/` holds a zero-dependency static site (vanilla JS, no build) that:
- renders a chapter's lessons/steps,
- shows a CodeMirror-free `<textarea>` editor seeded with `starter_code`,
- runs the step's ```checker regex against the buffer in-browser to unlock the next step,
- reveals `solution_code` on demand.
Markdown is the canonical content; the player reads a generated `player/course.json` index. The player is generated LAST, after content exists. Course is fully usable as plain markdown even without the player.

### 3. Per-track serialization (was: parallel agents corrupt incremental contracts)
Contracts that "evolve across chapters" (BondToken v1→v2→final, ShareToken, FundShareToken) MUST be generated **serially within a track**, each chapter receiving the previous chapter's assembled contract as input context. Across *different* tracks (A/B/C) and across *independent* shared chapters, parallel generation is fine. The chapter map's `Contract:` line pins the exact evolution; honor it.

### 4. STATUS protocol (was: STATUS never updated → drifted from reality)
`STATUS.md` is a checklist with one line per deliverable. The harness (`pm/`) — not free prose — appends `[x]` lines after each artifact is written AND verified (chapter file exists + non-trivial length; contract forge-builds where applicable; checkers validate). `<<COURSE COMPLETE>>` is appended only when every box is checked.

## Division of labor (this build)
- **qwen2.5-coder:14b** (local, via ollama) authors ALL chapter markdown, Solidity, and Java. It is the only author of course content.
- **PM/orchestrator** (Claude) writes the qwen prompts, runs the harness, verifies output (forge build, checker validation, style/coverage QA), feeds failures back to qwen for repair, updates STATUS, and deploys. PM writes no course content itself.

## Style contract (enforce on every generated chapter)
Match `course/03-functions-events-modifiers.md` exactly:
- Header block: `# Chapter NN — <title>`, then **Track**, **Emphasis threads**, **Chapter learning objective**, **Prerequisites**, **You will build**.
- Lessons numbered `## Lesson N — <title>` with objective + emphasis tags + track.
- Steps `### Step N.M — <title>` each with **Instruction**, **Explanation** (JVM/core-banking analogy mandatory), **Starter code**, **Solution**, **Validation rule:** prose, then a ```checker block.
- `> **Banking integration note:**` and `> **Datatype/parser note:**` callouts where relevant, with real web3j Java.
- End: assembled contract + short quiz (3–5 Q&A).
- Solidity `^0.8.20`, self-contained (CMTAT patterns re-implemented inline, labeled "modeled on CMTAT <module>").
