#!/usr/bin/env python3
"""P0 grader fix: re-derive any checker whose regex embeds a // or /* */ comment
(or no longer matches its solution) to a comment-free, code-targeting pattern.
Deterministic; preserves id/error_hint/target. Leaves clean qwen checkers alone.

Usage: fix_comment_checkers.py course/NN-foo.md [...]
"""
import json, os, re, sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from repair_checkers import derive_pattern, prior_solution, CHK_RE, compiles_and_matches

COMMENTY = re.compile(r"//|/\\\*|\\\*/")


def fix(path):
    txt = open(path).read()
    changed = 0
    for m in reversed(list(CHK_RE.finditer(txt))):
        raw = m.group(1)
        try:
            obj = json.loads(raw)
        except json.JSONDecodeError:
            continue
        pat = obj.get("pattern", "")
        sol = prior_solution(txt, m.start())
        needs = COMMENTY.search(pat) or not compiles_and_matches(obj, sol)
        if not needs:
            continue
        newpat = derive_pattern(sol)
        if not newpat:
            print(f"  {obj.get('id')}: no code line to derive — left as-is")
            continue
        obj["pattern"] = newpat
        obj["type"] = "regex"
        if compiles_and_matches(obj, sol):
            txt = txt.replace("```checker\n" + raw, "```checker\n" + json.dumps(obj), 1)
            changed += 1
    open(path, "w").write(txt)
    print(f"{os.path.basename(path)}: re-derived {changed} checkers")
    return changed


def main():
    for p in sys.argv[1:]:
        fix(p)


if __name__ == "__main__":
    main()
