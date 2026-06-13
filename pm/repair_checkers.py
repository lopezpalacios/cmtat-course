#!/usr/bin/env python3
"""Repair failing ```checker blocks by asking qwen to correct the regex so it
matches its own solution. PM-authored prompt; qwen authors the fix. Harness
verifies the fix actually matches before splicing it back in.

Usage: repair_checkers.py course/NN-foo.md
"""
import json, os, re, sys, urllib.request

OLLAMA = "http://localhost:11434/api/generate"
MODEL = "qwen2.5-coder:14b"
SOL_RE = re.compile(r"\*\*Solution:?\*\*\s*```[a-z]*\n(.*?)```", re.S)
CHK_RE = re.compile(r"```checker\s*\n(\{.*?\})\s*```", re.S)


def qwen(prompt):
    body = json.dumps({"model": MODEL, "prompt": prompt, "stream": False,
                       "options": {"temperature": 0.1, "num_ctx": 8192}}).encode()
    req = urllib.request.Request(OLLAMA, data=body, headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=300) as r:
        return json.load(r)["response"]


def prior_solution(txt, pos):
    sols = [(m.start(), m.group(1)) for m in SOL_RE.finditer(txt) if m.start() < pos]
    return sols[-1][1] if sols else ""


def compiles_and_matches(obj, sol):
    if obj.get("type") != "regex":
        return True
    try:
        rx = re.compile(obj["pattern"], re.M)
    except re.error:
        return False
    return bool(rx.search(sol))


PROMPT = """You are fixing ONE validation checker for a Solidity course. The checker's regex `pattern` must match the SOLUTION code below, but it currently fails. Output ONLY a single-line JSON object with keys: id, type, pattern, flags, target, error_hint. Keep the SAME id and error_hint. type MUST be "regex".

CRITICAL RULES for the pattern:
- Make it SHORT. Anchor on ONE distinctive single line from the SOLUTION (e.g. `struct\\s+Snapshot`, `function\\s+payCoupon\\(`, `event\\s+CouponPaid`).
- NEVER use `.` or `.*` to span multiple lines — `.` does not match newlines, so any multi-line pattern FAILS.
- Match exactly one construct on one line. Escape backslashes for JSON.

SOLUTION:
```
{sol}
```

BROKEN CHECKER: {chk}

Corrected JSON:"""

# meaningful single line = code, not braces/comments/pragma
_TRIVIAL = re.compile(r"^\s*(//|/\*|\*|\}|\{|pragma|//\s*SPDX|$)")


def _strip_comment(line):
    """Remove inline // and /* */ comments so checkers target code, not prose."""
    line = re.sub(r"/\*.*?\*/", "", line)
    line = re.sub(r"//.*$", "", line)
    return line.strip()


def derive_pattern(sol):
    """Deterministic fallback: pick the longest distinctive CODE line (comments
    stripped) that occurs exactly once, as a whitespace-tolerant regex."""
    code = [_strip_comment(l) for l in sol.splitlines()]
    cand = [c for c in code if len(c) > 12 and not _TRIVIAL.match(c)
            and code.count(c) == 1]
    if not cand:
        return None
    line = max(cand, key=len)
    # escape each whitespace-delimited token, rejoin tolerant of whitespace
    return r"\s+".join(re.escape(tok) for tok in line.split())


def derive_pattern_diff(sol, starter):
    """Pick the longest distinctive CODE line present in the solution but NOT in
    the starter — guarantees the checker fails on the starter and passes on the
    solution (no freebie)."""
    sset = {_strip_comment(l) for l in (starter or "").splitlines()}
    code = [_strip_comment(l) for l in sol.splitlines()]
    cand = [c for c in code if len(c) > 12 and not _TRIVIAL.match(c)
            and code.count(c) == 1 and c not in sset]
    if not cand:
        return None
    line = max(cand, key=len)
    return r"\s+".join(re.escape(tok) for tok in line.split())


def extract_json(s):
    m = re.search(r"\{.*\}", s, re.S)
    return m.group(0) if m else None


def main():
    path = sys.argv[1]
    txt = open(path).read()
    checkers = list(CHK_RE.finditer(txt))
    fixed = 0
    failed = []
    # process right-to-left so splice offsets stay valid
    for m in reversed(checkers):
        raw = m.group(1)
        sol = prior_solution(txt, m.start())
        try:
            obj = json.loads(raw)
            ok = compiles_and_matches(obj, sol)
        except json.JSONDecodeError:
            obj, ok = None, False
        if ok:
            continue
        cid = (obj or {}).get("id", "?")
        newjson = None
        for attempt in range(3):
            resp = qwen(PROMPT.format(sol=sol[:2500], chk=raw))
            cand = extract_json(resp)
            if not cand:
                continue
            try:
                cobj = json.loads(cand)
            except json.JSONDecodeError:
                continue
            if compiles_and_matches(cobj, sol):
                newjson = json.dumps(cobj)
                break
        if not newjson:  # deterministic fallback — guarantees a match
            pat = derive_pattern(sol)
            if pat:
                base = obj if isinstance(obj, dict) else {}
                cobj = {"id": base.get("id", cid), "type": "regex", "pattern": pat,
                        "flags": "m", "target": base.get("target", "solidity"),
                        "error_hint": base.get("error_hint", "Match the solution code.")}
                if compiles_and_matches(cobj, sol):
                    newjson = json.dumps(cobj)
                    print(f"  (fallback) {cid}")
        if newjson:
            txt = txt.replace("```checker\n" + raw, "```checker\n" + newjson, 1)
            fixed += 1
            print(f"  fixed {cid}")
        else:
            failed.append(cid)
            print(f"  COULD NOT FIX {cid}")
    open(path, "w").write(txt)
    print(f"{path}: fixed {fixed}, still failing {len(failed)} {failed}")
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
