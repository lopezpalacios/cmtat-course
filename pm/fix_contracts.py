#!/usr/bin/env python3
"""Compile-and-repair loop for the assembled .sol contracts. forge build,
parse per-file solc errors, ask qwen to return a corrected full contract,
write it back, rebuild. Repeat until clean or rounds exhausted.

PM orchestrates + verifies (forge); qwen authors every fix.

Usage: fix_contracts.py [max_rounds]
"""
import json, os, re, subprocess, sys, urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OLLAMA = "http://localhost:11434/api/generate"
MODEL = "qwen2.5-coder:14b"
ERR_RE = re.compile(r"-->\s+(contracts/\S+\.sol):", re.M)


def forge_build():
    r = subprocess.run(["forge", "build"], cwd=ROOT, capture_output=True, text=True)
    return r.returncode == 0, r.stdout + r.stderr


def failing_files(output):
    return sorted(set(ERR_RE.findall(output)))


def errors_for(output, path):
    # crude: keep error blocks mentioning this file
    blocks, cur, keep = [], [], False
    for line in output.splitlines():
        if line.startswith("Error") or line.startswith("Warning"):
            if cur and keep:
                blocks.append("\n".join(cur))
            cur, keep = [line], False
        else:
            cur.append(line)
            if path in line:
                keep = True
    if cur and keep:
        blocks.append("\n".join(cur))
    return "\n\n".join(b for b in blocks if "Error" in b)[:3000]


def qwen(prompt):
    body = json.dumps({"model": MODEL, "prompt": prompt, "stream": False,
                       "options": {"temperature": 0.1, "num_ctx": 16384,
                                   "num_predict": 6000}}).encode()
    req = urllib.request.Request(OLLAMA, data=body, headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=900) as r:
        return json.load(r)["response"]


PROMPT = """You are fixing a Solidity ^0.8.20 contract that fails to compile. Return ONLY the corrected, COMPLETE contract inside a single ```solidity code block — no explanation. Keep the same contract name, structure, and intent; change only what is needed to compile. Do not add external imports (keep it self-contained).

SOLC ERRORS:
{errs}

CURRENT CONTRACT ({path}):
```solidity
{code}
```

Corrected complete contract:"""


def extract_sol(resp):
    m = re.search(r"```solidity\s*\n(.*?)```", resp, re.S)
    return m.group(1).strip() if m else None


def main():
    rounds = int(sys.argv[1]) if len(sys.argv) > 1 else 4
    for rnd in range(1, rounds + 1):
        ok, out = forge_build()
        if ok:
            print(f"round {rnd}: BUILD CLEAN"); return 0
        files = failing_files(out)
        print(f"round {rnd}: failing {files}", flush=True)
        for path in files:
            full = os.path.join(ROOT, path)
            code = open(full).read()
            errs = errors_for(out, path) or out[:2000]
            resp = qwen(PROMPT.format(errs=errs, path=path, code=code))
            fixed = extract_sol(resp)
            if fixed and "contract" in fixed:
                open(full, "w").write(fixed + "\n")
                print(f"  rewrote {path} ({len(fixed)} chars)", flush=True)
            else:
                print(f"  qwen gave no usable contract for {path}", flush=True)
    ok, out = forge_build()
    print("FINAL:", "CLEAN" if ok else "STILL FAILING:\n" + "\n".join(failing_files(out)))
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
