#!/usr/bin/env python3
"""PM-authored prompt assembler. Injects per-chapter context into the fixed
instruction template so qwen produces a chapter matching house style + checker
schema. The PM owns the template; qwen owns the output.

Usage: build_prompt.py <NN> [prev_contract_file]
Writes pm/prompts/ch<NN>.txt and prints its path.
"""
import os, re, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def read(p):
    return open(os.path.join(ROOT, p)).read()


def map_entry(nn):
    m = read("course/00-chapter-map.md")
    # grab the "### Chapter NN ..." block up to the next "### " or "## "
    pat = re.compile(rf"(### Chapter {nn} .*?)(?=\n### |\n## |\Z)", re.S)
    mm = pat.search(m)
    if not mm:
        sys.exit(f"no map entry for chapter {nn}")
    return mm.group(1).strip()


# A single complete lesson from ch03 as the format exemplar (trimmed for context).
EXEMPLAR = """\
## Lesson 1 — Functions and Visibility: The Contract's API Surface

**Learning objective:** ...
**Emphasis tags:** `[BANK]` `[TYPES]`
**Track:** shared

<one paragraph of prose with a JVM/core-banking analogy>

### Step 1.1 — Lay down the contract skeleton and public state

**Instruction:** Create the file `Foo.sol`. Declare pragma, contract, and two public state vars.

**Explanation:** <2-4 sentences, MUST include a JVM/core-banking analogy>

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract Foo {
    // declare here
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract Foo {
    address public admin;
    uint256 public bookingCount;
}
```

**Validation rule:** both state variables declared public with correct types.

```checker
{"id": "ch03-l1-s1", "type": "regex", "pattern": "address\\\\s+public\\\\s+admin\\\\s*;[\\\\s\\\\S]*uint256\\\\s+public\\\\s+bookingCount\\\\s*;", "flags": "m", "target": "solidity", "error_hint": "Declare both as `public` so the getters are generated."}
```

> **Datatype/parser note:** uint256 -> web3j Uint256 -> Java BigInteger. Never use long.
"""

TEMPLATE = """\
You are qwen, the sole author of a CryptoZombies-style interactive course teaching Solidity + the CMTAT tokenized-securities standard to Swiss core-banking developers (strong Java/.NET, ZERO blockchain background). Output is ONE markdown file for a single chapter. Output ONLY the markdown — no preamble, no ``` fence around the whole thing, no commentary.

=== MASTER SPEC (obey exactly) ===
{spec}

=== CHECKER SCHEMA (every step needs one ```checker block) ===
{schema}

=== HOUSE STYLE — match this exemplar's structure EXACTLY ===
{exemplar}

=== YOUR ASSIGNMENT: write Chapter {nn} in full ===
This is the chapter-map entry you must realize completely:

{entry}

REQUIREMENTS:
- 5 to 6 Lessons. Each Lesson MUST have AT LEAST 2 Steps (never a single-step lesson) — aim for 2 to 4. Every Step: **Instruction**, **Explanation** (with a mandatory JVM/core-banking analogy), **Starter code**, **Solution**, **Validation rule:** prose, then a ```checker JSON block with a UNIQUE id of form ch{nn}-lL-sS. This is a substantial chapter: target 20k+ characters of real content.
- Every ```checker regex MUST literally match its own Solution code (the harness verifies this — if it won't match, fix the pattern). Escape backslashes for JSON.
- CHECKER REGEX RULES (critical): keep each pattern SHORT — anchor on ONE distinctive single line or declaration from the Solution (e.g. `struct\\s+Snapshot`, `function\\s+payCoupon`, `event\\s+CouponPaid`). NEVER use `.` or `.*` to span multiple lines — `.` does not match newlines and the check will fail. Do NOT encode an entire function body or multiple lines in one pattern. One construct, one line.
- Header block first: `# Chapter {nn} — <title>` then **Track**, **Emphasis threads**, **Chapter learning objective**, **Prerequisites**, **You will build**.
- Include `> **Banking integration note:**` callouts with REAL web3j Java where the chapter map tags [BANK], and `> **Datatype/parser note:**` callouts where it tags [TYPES].
- Solidity `^0.8.20`, self-contained (re-implement CMTAT patterns inline, label them "modeled on CMTAT <module>"). Do NOT invent CMTAT APIs; if unsure, state the assumption inline and continue.
- End with the assembled contract for this chapter, then a 3-5 question quiz with answers.
{prev}
Begin the chapter now.
"""


def main():
    nn = sys.argv[1].zfill(2)
    prev = ""
    if len(sys.argv) > 2 and sys.argv[2]:
        prev_code = open(sys.argv[2]).read()
        prev = ("\n=== PREVIOUS CHAPTER'S ASSEMBLED CONTRACT (evolve THIS, keep it "
                "compiling, add only what this chapter introduces) ===\n```solidity\n"
                + prev_code + "\n```\n")
    spec = read("PROMPT_v2.md")
    schema = read("CHECKER_SCHEMA.md")
    out = TEMPLATE.format(spec=spec, schema=schema, exemplar=EXEMPLAR,
                          nn=nn, entry=map_entry(nn), prev=prev)
    os.makedirs(os.path.join(ROOT, "pm/prompts"), exist_ok=True)
    p = os.path.join(ROOT, f"pm/prompts/ch{nn}.txt")
    open(p, "w").write(out)
    print(p)


if __name__ == "__main__":
    main()
