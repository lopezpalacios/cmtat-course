#!/usr/bin/env python3
"""P0 grader integrity gate. For every step's checker:
  - regex compiles,
  - the SOLUTION passes it,
  - the STARTER fails it (when a distinct starter exists) — so the checker tests
    work the learner must do, not a freebie,
  - the pattern embeds no // or /* */ comment.
Exit non-zero on any violation.

Usage: check_grader.py course/NN-foo.md [...]
"""
import json, os, re, sys

STEP = re.compile(r"^###\s+Step\b", re.M)
STARTER = re.compile(r"\*\*Starter code:?\*\*\s*```[a-z]*\n(.*?)```", re.S)
SOL = re.compile(r"\*\*Solution:?\*\*\s*```[a-z]*\n(.*?)```", re.S)
CHK = re.compile(r"```checker\s*\n(\{.*?\})\s*```", re.S)
COMMENTY = re.compile(r"//|/\*|\*/")


def steps(txt):
    ms = list(STEP.finditer(txt))
    for i, m in enumerate(ms):
        end = ms[i + 1].start() if i + 1 < len(ms) else len(txt)
        yield txt[m.start():end]


def first(rx, body):
    m = rx.search(body)
    return m.group(1) if m else None


def check(path):
    txt = open(path).read()
    errs = []
    for body in steps(txt):
        cm = CHK.search(body)
        if not cm:
            continue
        try:
            obj = json.loads(cm.group(1))
        except json.JSONDecodeError as e:
            errs.append(f"  bad JSON: {e}"); continue
        cid = obj.get("id", "?")
        pat = obj.get("pattern", "")
        if obj.get("type") != "regex":
            continue
        try:
            rx = re.compile(pat, re.M)
        except re.error as e:
            errs.append(f"  {cid}: regex error {e}"); continue
        if COMMENTY.search(re.sub(r"\\.", "", pat)):
            errs.append(f"  {cid}: pattern embeds a comment")
        sol, st = first(SOL, body), first(STARTER, body)
        if sol is not None and not rx.search(sol):
            errs.append(f"  {cid}: SOLUTION does not pass its checker")
        if st is not None and sol is not None and st.strip() != sol.strip() and rx.search(st):
            errs.append(f"  {cid}: STARTER already passes (freebie checker)")
    return errs


def main():
    bad = 0
    for p in sys.argv[1:]:
        errs = check(p)
        if errs:
            bad += 1
            print(f"FAIL {os.path.basename(p)}")
            print("\n".join(errs))
        else:
            print(f"OK   {os.path.basename(p)}")
    print("\nGRADER OK" if not bad else f"\n{bad} file(s) with grader violations")
    sys.exit(1 if bad else 0)


if __name__ == "__main__":
    main()
