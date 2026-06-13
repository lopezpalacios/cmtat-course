#!/usr/bin/env python3
"""P0: fix 'freebie' checkers (pattern already matches the starter) by re-deriving
from a code line unique to the solution. Step-aware (needs starter + solution).

Usage: fix_freebie_checkers.py course/NN-foo.md [...]
"""
import json, os, re, sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from repair_checkers import derive_pattern_diff

STEP = re.compile(r"^###\s+Step\b", re.M)
STARTER = re.compile(r"\*\*Starter code:?\*\*\s*```[a-z]*\n(.*?)```", re.S)
SOL = re.compile(r"\*\*Solution:?\*\*\s*```[a-z]*\n(.*?)```", re.S)
CHK = re.compile(r"```checker\s*\n(\{.*?\})\s*```", re.S)


def fix(path):
    txt = open(path).read()
    ms = list(STEP.finditer(txt))
    changed = 0
    for i in range(len(ms) - 1, -1, -1):
        s = ms[i].start()
        e = ms[i + 1].start() if i + 1 < len(ms) else len(txt)
        body = txt[s:e]
        cm, sm, stm = CHK.search(body), SOL.search(body), STARTER.search(body)
        if not (cm and sm and stm):
            continue
        try:
            obj = json.loads(cm.group(1))
        except json.JSONDecodeError:
            continue
        if obj.get("type") != "regex":
            continue
        sol, starter = sm.group(1), stm.group(1)
        if sol.strip() == starter.strip():
            continue
        try:
            rx = re.compile(obj["pattern"], re.M)
        except re.error:
            continue
        if not rx.search(starter):  # already not a freebie
            continue
        newpat = derive_pattern_diff(sol, starter)
        if not newpat:
            continue
        try:
            nrx = re.compile(newpat, re.M)
        except re.error:
            continue
        if nrx.search(sol) and not nrx.search(starter):
            raw = cm.group(1)
            obj["pattern"] = newpat
            txt = txt[:s] + body.replace("```checker\n" + raw,
                                         "```checker\n" + json.dumps(obj), 1) + txt[e:]
            changed += 1
    open(path, "w").write(txt)
    print(f"{os.path.basename(path)}: de-freebied {changed} checkers")
    return changed


def main():
    for p in sys.argv[1:]:
        fix(p)


if __name__ == "__main__":
    main()
