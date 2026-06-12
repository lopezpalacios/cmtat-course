# Validation-Rule (Checker) Schema — v1

Gap fix #1: every lesson step's `validation_rule` must be **machine-readable** so the runtime can gate progression. Markdown lessons embed a fenced ```checker block (JSON) immediately after the prose validation rule.

## Canonical shape

```checker
{
  "id": "ch03-l1-s1",
  "type": "regex",
  "pattern": "address\\s+public\\s+admin\\s*;",
  "flags": "m",
  "target": "solidity",
  "error_hint": "Declare `address public admin;` — must be public so the getter is generated."
}
```

Fields:
- `id` (string, required) — `ch<NN>-l<L>-s<S>`, globally unique. Used by the runtime to key learner progress.
- `type` (enum, required) — `regex` | `ast` | `compile`.
  - `regex`: `pattern` is a JS-flavored RegExp tested against the learner's editor buffer.
  - `ast`: `pattern` is a required solc AST node selector (node type + optional name), e.g. `FunctionDefinition[name=transfer]`. Use only when regex is too brittle.
  - `compile`: full `forge build` of the assembled file must pass (reserved for end-of-chapter assembly step only).
- `pattern` (string, required for regex/ast) — escape backslashes for JSON.
- `flags` (string, optional) — RegExp flags, default `"m"`.
- `target` (enum, optional) — `solidity` (default) | `java`.
- `error_hint` (string, required) — shown to learner on failure. Actionable, not "wrong".

## Rules for the author (qwen)

1. Every step has exactly one ```checker block.
2. `regex` patterns must match the **solution_code**, not just be plausible. The harness verifies this (see `pm/validate_checkers.py`).
3. Patterns check the *meaningful* construct the step teaches, not whitespace trivia.
4. Keep prose "Validation rule:" line for human readers; the ```checker block is the source of truth.
