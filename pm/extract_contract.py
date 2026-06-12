#!/usr/bin/env python3
"""Extract the assembled contract from a chapter md (the last solidity block
that declares a contract) and write it to a .sol path. Enables per-track
serialization: chapter N's assembled contract feeds chapter N+1's prompt.

Usage: extract_contract.py course/NN-foo.md contracts/bond/BondToken.sol
"""
import os, re, sys

SOL_BLOCK = re.compile(r"```solidity\s*\n(.*?)```", re.S)


def main():
    md, out = sys.argv[1], sys.argv[2]
    blocks = [b for b in SOL_BLOCK.findall(open(md).read())
              if re.search(r"\bcontract\s+\w+", b)]
    if not blocks:
        sys.exit(f"no contract block in {md}")
    # the assembled contract is the longest contract-bearing block
    code = max(blocks, key=len).strip()
    os.makedirs(os.path.dirname(out), exist_ok=True)
    open(out, "w").write(code + "\n")
    print(f"extracted {out} ({len(code)} chars)")


if __name__ == "__main__":
    main()
