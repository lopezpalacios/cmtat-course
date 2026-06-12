# Assumptions

- No CMTAT/OpenZeppelin npm deps installed; course contracts are SELF-CONTAINED Solidity ^0.8.20 re-implementing CMTAT module patterns inline (clearly labeled "modeled on CMTAT <module>"). Real CMTAT repo referenced for names/semantics, not imported.
- CMTAT module names/semantics taken from CMTA reference impl (v2.x lineage): BaseModule, ERC20BaseModule, PauseModule, EnforcementModule, ERC20SnapshotModule, ValidationModule + RuleEngine, DocumentModule, DebtModule/CreditEventsModule, AuthorizationModule (AccessControl-based). Where exact signatures are uncertain, chapters state the assumption inline.
- Compilation: `forge build` (forge 1.6.0 present, solc auto-downloaded). solc standalone not installed.
- Standalone (non-proxy) CMTAT deployment model taught; proxy/upgradeability covered conceptually in Ch.06 only.
- web3j 4.x assumed on bank side; Java snippets are illustrative, compiled against no toolchain (no JDK build configured in repo).
- Currency: CHF for all instruments; ISIN/LEI encoded as right-padded ASCII in bytes32.
- Swiss regulatory content (Ch.09) is engineering-oriented summary as of training knowledge; not legal advice.
- Track chapters 10-12, 13-15, 16-18 each evolve one contract; intermediate versions live inside chapter markdown, final assembled .sol files in contracts/<track>/.
