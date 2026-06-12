#!/usr/bin/env python3
"""Generate the 9 referenced-but-missing web3j adapter files so the chapter map's
`Java:` references all resolve. PM supplies the prompt + an existing adapter as a
style exemplar + the contract each one integrates with; qwen authors the file.

Serial (single ollama model). Usage: gen_adapters.py
"""
import os, re, subprocess, sys, time, urllib.request, json

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OLLAMA = "http://localhost:11434/api/generate"
MODEL = "qwen2.5-coder:14b"

# (ClassName, contract_path, purpose)
SPECS = [
    ("BondMetadataReader", "contracts/bond/BondToken.sol",
     "reads and decodes the bond DebtInfo struct (interestRate bps, parValue, maturityDate, ISIN bytes32) on the bank side"),
    ("CouponPaymentJob", "contracts/bond/BondToken.sol",
     "bank-side coupon-payment batch job: reads snapshot balances, computes coupons, and submits coupon payments idempotently"),
    ("RedemptionSettlement", "contracts/bond/TokenizedBond.sol",
     "drives bond redemption: burn-against-payment at maturity with the off-chain CHF cash leg and settlement reconciliation"),
    ("ShareRegisterSync", "contracts/equity/ShareToken.sol",
     "mirrors on-chain share-register changes (Transfer/registrar events) into the bank's share-register system of record"),
    ("DividendDistributionJob", "contracts/equity/ShareToken.sol",
     "computes dividends from a snapshot with 35% Swiss withholding tax and exports voting power to the general-assembly system"),
    ("CorporateActionProcessor", "contracts/equity/EquityShareToken.sol",
     "processes corporate actions: stock split, rights-issue mint, buyback burn, and squeeze-out forced transfer"),
    ("NavPublisher", "contracts/mmf/FundShareToken.sol",
     "publishes the daily struck NAV (6-decimal fixed point) on-chain from the fund-accounting system, with four-eyes control"),
    ("DailySettlementEngine", "contracts/mmf/FundShareToken.sol",
     "runs the T+0/T+1 fund cycle: collects subscription/redemption orders, batch-settles at struck NAV, reconciles end of day"),
    ("RedemptionPayoutJob", "contracts/mmf/MoneyMarketFundShare.sol",
     "executes MMF redemption payouts with the off-chain CHF leg, honoring liquidity gates and redemption suspension"),
]

EXEMPLAR = open(os.path.join(ROOT, "java-adapters/EventLogParser.java")).read()

PROMPT = """You are writing ONE complete bank-side web3j Java adapter for a CMTAT tokenized-securities course aimed at Swiss core-banking developers. Output ONLY a single ```java code block containing the COMPLETE file — no prose outside it.

Match the EXEMPLAR's structure and quality EXACTLY:
- package ch.bank.cmtat.adapters;
- real web3j imports (org.web3j.*), java.math.BigInteger, etc.
- a thorough class-level Javadoc explaining the BANKING purpose and the Solidity<->web3j type mapping used;
- real, compilable-looking web3j code (Function/Event/EthFilter/RawTransactionManager as appropriate);
- a JVM/core-banking framing in comments.

ADAPTER TO WRITE: {name}
PURPOSE: {purpose}

CONTRACT IT INTEGRATES WITH ({contract_path}):
```solidity
{contract}
```

EXEMPLAR (match this style and depth):
```java
{exemplar}
```

Write the complete {name}.java now (one ```java block only):"""


def qwen(prompt):
    body = json.dumps({"model": MODEL, "prompt": prompt, "stream": False,
                       "options": {"temperature": 0.2, "num_ctx": 16384,
                                   "num_predict": 6000}}).encode()
    req = urllib.request.Request(OLLAMA, data=body, headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=900) as r:
        return json.load(r)["response"]


def extract_java(resp):
    m = re.search(r"```java\s*\n(.*?)```", resp, re.S)
    return m.group(1).strip() if m else None


def main():
    for name, cpath, purpose in SPECS:
        contract = open(os.path.join(ROOT, cpath)).read()
        prompt = PROMPT.format(name=name, purpose=purpose, contract_path=cpath,
                               contract=contract[:4000], exemplar=EXEMPLAR)
        t0 = time.time()
        code = extract_java(qwen(prompt))
        out = os.path.join(ROOT, f"java-adapters/{name}.java")
        if code and "class " in code and "import " in code:
            if not code.lstrip().startswith("package"):
                code = "package ch.bank.cmtat.adapters;\n\n" + code
            open(out, "w").write(code + "\n")
            print(f"OK {name}.java {len(code)} chars {time.time()-t0:.0f}s", flush=True)
        else:
            print(f"FAIL {name}: no usable java ({time.time()-t0:.0f}s)", flush=True)
    print("=== ADAPTERS DONE ===", flush=True)


if __name__ == "__main__":
    main()
