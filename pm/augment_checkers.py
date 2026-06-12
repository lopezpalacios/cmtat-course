#!/usr/bin/env python3
"""Add a valid ```checker block to any step that has a Solution but no checker.
Deterministic (no model): derives a whitespace-tolerant regex from a distinctive
single line of the solution, so it is guaranteed to compile and match. Closes
gap #1 for the pre-schema chapters.

Usage: augment_checkers.py course/NN-foo.md [...]
"""
import json, os, re, sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from repair_checkers import derive_pattern  # reuse the same derivation

STEP = re.compile(r"^###\s+Step\s+([\d]+)\.([\d]+)\b", re.M)
SOL = re.compile(r"\*\*Solution:?\*\*\s*```[a-z]*\n(.*?)```", re.S)


def chapter_no(path):
    m = re.match(r"(\d+)", os.path.basename(path))
    return m.group(1) if m else "00"


def augment(path):
    txt = open(path).read()
    nn = chapter_no(path)
    steps = list(STEP.finditer(txt))
    added = 0
    # right-to-left so insertion offsets stay valid
    for i in range(len(steps) - 1, -1, -1):
        m = steps[i]
        start = m.end()
        end = steps[i + 1].start() if i + 1 < len(steps) else len(txt)
        body = txt[start:end]
        if "```checker" in body:
            continue
        sm = SOL.search(body)
        if not sm:
            continue
        pat = derive_pattern(sm.group(1))
        if not pat:
            continue
        lesson, step = m.group(1), m.group(2)
        chk = {
            "id": f"ch{nn}-l{lesson}-s{step}",
            "type": "regex", "pattern": pat, "flags": "m",
            "target": "java" if re.search(r"\.java\b|web3j|public\s+class", sm.group(1)) else "solidity",
            "error_hint": "Your code should match the solution for this step.",
        }
        block = "\n\n```checker\n" + json.dumps(chk) + "\n```\n"
        # insert at end of step body (trim trailing blank lines first)
        insert_at = end
        txt = txt[:insert_at] + block + txt[insert_at:]
        added += 1
    open(path, "w").write(txt)
    print(f"{os.path.basename(path)}: added {added} checkers")
    return added


def main():
    for p in sys.argv[1:]:
        augment(p)


if __name__ == "__main__":
    main()
