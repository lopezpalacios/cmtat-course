#!/usr/bin/env python3
"""Batch chapter generator. For each "NN:slug" arg: build prompt, run qwen,
write course/NN-slug.md, validate checkers. Serial (single ollama model).

Optional per-item prev-contract: "NN:slug:contracts/bond/BondToken.sol".

Usage: gen.py NN:slug[:prevcontract] ...
"""
import os, subprocess, sys, time

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PY = sys.executable


def run(cmd):
    return subprocess.run(cmd, cwd=ROOT, capture_output=True, text=True)


def main():
    for item in sys.argv[1:]:
        parts = item.split(":")
        nn, slug = parts[0], parts[1]
        prev = parts[2] if len(parts) > 2 else ""
        out_md = os.path.join(ROOT, f"course/{nn}-{slug}.md")
        print(f"\n=== Chapter {nn} ({slug}) {time.strftime('%H:%M:%S')} ===", flush=True)
        bp = run([PY, "pm/build_prompt.py", nn, prev])
        if bp.returncode:
            print("build_prompt FAILED:", bp.stderr, flush=True); continue
        prompt_path = bp.stdout.strip()
        q = run([PY, "pm/qwen.py", prompt_path, out_md])
        print(q.stdout.strip() or q.stderr.strip(), flush=True)
        if not os.path.exists(out_md) or os.path.getsize(out_md) < 1500:
            print(f"  WARN: {out_md} missing/short", flush=True); continue
        v = run([PY, "pm/validate_checkers.py", out_md])
        print(v.stdout.strip(), flush=True)
        if v.stderr.strip():
            print(v.stderr.strip(), flush=True)
    print("\n=== BATCH DONE ===", flush=True)


if __name__ == "__main__":
    main()
