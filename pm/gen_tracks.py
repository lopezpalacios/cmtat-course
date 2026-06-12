#!/usr/bin/env python3
"""Serialized track-chapter generator. Each chapter evolves its track's contract;
the assembled contract is extracted and fed as `prev` into the next chapter
(gap fix #3). Tracks run sequentially (single ollama model).

Steps are hard-coded from course/00-chapter-map.md.
"""
import os, subprocess, sys, time

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PY = sys.executable

# (nn, slug, prev_contract_path_or_empty, extract_to_path)
STEPS = [
    ("10", "bond-instrument-debt-module", "", "contracts/bond/BondToken.sol"),
    ("11", "coupons-record-dates-snapshots", "contracts/bond/BondToken.sol", "contracts/bond/BondToken.sol"),
    ("12", "maturity-redemption-full-bond", "contracts/bond/BondToken.sol", "contracts/bond/TokenizedBond.sol"),
    ("13", "share-register-onchain", "", "contracts/equity/ShareToken.sol"),
    ("14", "snapshots-dividends-voting", "contracts/equity/ShareToken.sol", "contracts/equity/ShareToken.sol"),
    ("15", "corporate-actions-full-equity", "contracts/equity/ShareToken.sol", "contracts/equity/EquityShareToken.sol"),
    ("16", "fund-shares-nav-onchain", "", "contracts/mmf/FundShareToken.sol"),
    ("17", "daily-settlement-cycle", "contracts/mmf/FundShareToken.sol", "contracts/mmf/FundShareToken.sol"),
    ("18", "redemptions-gates-full-mmf", "contracts/mmf/FundShareToken.sol", "contracts/mmf/MoneyMarketFundShare.sol"),
]


def run(cmd):
    return subprocess.run(cmd, cwd=ROOT, capture_output=True, text=True)


def main():
    for nn, slug, prev, extract_to in STEPS:
        out_md = f"course/{nn}-{slug}.md"
        print(f"\n=== Chapter {nn} ({slug}) {time.strftime('%H:%M:%S')} prev={prev or 'none'} ===", flush=True)
        bp = run([PY, "pm/build_prompt.py", nn, prev])
        if bp.returncode:
            print("build_prompt FAILED:", bp.stderr, flush=True); continue
        q = run([PY, "pm/qwen.py", bp.stdout.strip(), os.path.join(ROOT, out_md)])
        print(q.stdout.strip() or q.stderr.strip(), flush=True)
        full = os.path.join(ROOT, out_md)
        if not os.path.exists(full) or os.path.getsize(full) < 1500:
            print(f"  WARN: {out_md} missing/short — skipping extract", flush=True); continue
        ex = run([PY, "pm/extract_contract.py", out_md, extract_to])
        print(" ", ex.stdout.strip() or ex.stderr.strip(), flush=True)
        rep = run([PY, "pm/repair_checkers.py", out_md])
        print(" ", rep.stdout.strip().splitlines()[-1] if rep.stdout.strip() else "", flush=True)
    print("\n=== TRACKS DONE ===", flush=True)


if __name__ == "__main__":
    main()
