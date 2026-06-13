# Learner Personas — CMTAT Course

Method note: personas and their evaluation follow **SSR (Semantic Similarity Rating)**
from Maier et al., *LLMs Reproduce Human Purchase Intent via Semantic Similarity
Elicitation of Likert Ratings* (arXiv:2510.08338). Rather than asking a simulated
learner to self-report a numeric score (which LLMs do unreliably), we elicit a
**free-text reaction** in character, then map it to a Likert distribution by
embedding similarity to anchor statements (`pm/ssr.py`, local `nomic-embed-text`).

Target demographic (per instruction): **delivery / services Java developers**,
majority **India** and **Eastern/Central Europe**, building for bank/fintech
clients, strong Spring background, ~zero blockchain. Course was originally framed
for *Swiss in-house core-banking* devs — these personas test the gap.

## P1 — Arjun (Bengaluru, India)
4 yrs. Spring Boot microservices at a large SI, delivers banking back-ends for EU
clients. Strong Java/Maven/REST; finance domain learned on the job. Time-poor
(studies after standups), ambitious, wants a credential + real skills. English:
fluent technical, prefers concrete examples over prose. Zero blockchain.

## P2 — Priya (Pune, India)
2 yrs. Moved QA → Java dev. Spring/JPA basics, shaky on concurrency and low-level
types. Domain-new to securities. Needs heavier scaffolding, defined terms, small
wins. Worries the material is "too senior". English good, finance idioms unfamiliar.

## P3 — Daniel (Kraków, Poland)
7 yrs senior. Java/Spring at a software house delivering for Swiss/German banks.
Skeptical, wants depth, real integration/reconciliation patterns, and to know
*why* not just *how*. Will bounce off hand-waving. Cares about production concerns
(reorgs, idempotency, nonce mgmt).

## P4 — Ana (Cluj, Romania)
5 yrs fullstack (Java + React), delivery/consulting. Pragmatic: "will this help me
bill and ship for a client?" Moderate finance knowledge. Wants the off-chain Java
adapter side and clear contract-to-service mapping more than Solidity minutiae.

## P5 — Miloš (Belgrade, Serbia)
3 yrs backend Java, contracts for fintech clients. Self-taught, gaps in CS theory
(EVM, cryptography), learns strictly by doing. Skims prose, lives in the editor.
Needs the interactive checks and concept visuals to carry the load.

## Dimensions rated (via SSR)
clarity · difficulty (lower=harder) · relevance-to-my-job · engagement · pace.
