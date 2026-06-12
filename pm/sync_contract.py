#!/usr/bin/env python3
"""Sync a compile-hardened .sol into its chapter markdown: replace the longest
```solidity block whose `contract <Name>` matches the .sol's main contract, so
the chapter's final assembled code compiles like the canonical artifact.

Name-matched + longest-block only, so step snippets are never touched. Skips
(no write) if no matching block is found.

Usage: sync_contract.py course/NN-foo.md contracts/x/Name.sol
"""
import os, re, sys

BLOCK = re.compile(r"```solidity\s*\n(.*?)```", re.S)
NAME = re.compile(r"\bcontract\s+(\w+)")


def main():
    md_path, sol_path = sys.argv[1], sys.argv[2]
    sol = open(sol_path).read().strip()
    names = NAME.findall(sol)
    if not names:
        sys.exit(f"no contract name in {sol_path}")
    main_name = names[-1]  # the assembled/top contract is typically last

    md = open(md_path).read()
    best = None  # (length, match)
    for m in BLOCK.finditer(md):
        body = m.group(1)
        if re.search(rf"\bcontract\s+{main_name}\b", body):
            if best is None or len(body) > best[0]:
                best = (len(body), m)
    if not best:
        print(f"{os.path.basename(md_path)}: no block for contract {main_name} — skipped")
        return
    m = best[1]
    new_md = md[:m.start()] + "```solidity\n" + sol + "\n```" + md[m.end():]
    if new_md == md:
        print(f"{os.path.basename(md_path)}: {main_name} already in sync")
        return
    open(md_path, "w").write(new_md)
    print(f"{os.path.basename(md_path)}: synced {main_name} ({len(sol)} chars)")


if __name__ == "__main__":
    main()
