#!/usr/bin/env python3
"""Verify every ```checker block in a chapter file:
 1. is valid JSON with required fields,
 2. its regex compiles,
 3. for type=regex, the pattern actually matches the *preceding* solution_code block.

Usage: validate_checkers.py course/NN-foo.md [...]
Exit non-zero if any chapter has a failing checker.
"""
import json, re, sys

SOL_RE = re.compile(r"\*\*Solution:?\*\*\s*```[a-z]*\n(.*?)```", re.S)
CHK_RE = re.compile(r"```checker\s*\n(\{.*?\})\s*```", re.S)
REQ = {"id", "type", "pattern", "error_hint"}


def check_file(path):
    txt = open(path).read()
    # pair each checker with the nearest preceding solution block
    sols = [(m.start(), m.group(1)) for m in SOL_RE.finditer(txt)]
    errs = []
    ids = set()
    n = 0
    for m in CHK_RE.finditer(txt):
        n += 1
        try:
            obj = json.loads(m.group(1))
        except json.JSONDecodeError as e:
            errs.append(f"  checker #{n}: invalid JSON: {e}")
            continue
        missing = REQ - obj.keys()
        if missing:
            errs.append(f"  checker {obj.get('id','?')}: missing {missing}")
        if obj.get("id") in ids:
            errs.append(f"  duplicate id {obj['id']}")
        ids.add(obj.get("id"))
        if obj.get("type") == "regex":
            try:
                flags = re.M if "m" in obj.get("flags", "m") else 0
                rx = re.compile(obj["pattern"], flags)
            except re.error as e:
                errs.append(f"  checker {obj.get('id')}: bad regex: {e}")
                continue
            prior = [s for pos, s in sols if pos < m.start()]
            if prior and not rx.search(prior[-1]):
                errs.append(f"  checker {obj.get('id')}: pattern does NOT match its solution_code")
    return n, errs


def main():
    bad = 0
    for path in sys.argv[1:]:
        n, errs = check_file(path)
        if errs:
            bad += 1
            print(f"FAIL {path} ({n} checkers)")
            print("\n".join(errs))
        else:
            print(f"OK   {path} ({n} checkers)")
    sys.exit(1 if bad else 0)


if __name__ == "__main__":
    main()
