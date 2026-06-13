#!/usr/bin/env python3
"""SSR — Semantic Similarity Rating (Maier et al., arXiv:2510.08338), applied to
learner-persona reactions. For each free-text reaction we embed it and the Likert
anchor statements of each dimension (local nomic-embed-text), softmax the cosine
similarities into a Likert distribution, and take the expected score (1-5).

This avoids asking the model for a number directly (the paper's key point) and
yields calibrated, comparable ratings. Output: a persona×chapter×dimension matrix
and per-chapter aggregates -> SSR_REPORT.md.

Usage: ssr.py
"""
import json, math, os, urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OLLAMA = "http://localhost:11434/api/embeddings"
MODEL = "nomic-embed-text"
TEMP = 0.10  # softmax temperature over cosine sims

# 5-point anchors per dimension (index0 -> score 1 ... index4 -> score 5)
DIMS = {
 "clarity": ["This was confusing and very hard to follow.",
             "This was somewhat unclear in places.",
             "This was moderately clear.",
             "This was clear and easy to follow.",
             "This was crystal clear and extremely easy to follow."],
 "ease":    ["This was far too hard for me.",
             "This was harder than I'd like.",
             "This was a moderate challenge.",
             "This was fairly easy for me.",
             "This was very easy for me."],
 "relevance":["This is irrelevant to my job.",
             "This is only slightly relevant to my work.",
             "This is somewhat relevant to my work.",
             "This is relevant to my work.",
             "This is highly relevant and directly useful for my job."],
 "engagement":["This was boring and I disengaged.",
             "This was a bit dull.",
             "This was moderately engaging.",
             "This was engaging.",
             "This was very engaging and motivating."],
 "trust":   ["I do not trust this; it seems broken or wrong.",
             "I have doubts about the correctness here.",
             "This seems mostly correct.",
             "I trust this content is correct.",
             "I fully trust this content is correct and production-ready."],
}


def embed(text):
    body = json.dumps({"model": MODEL, "prompt": text}).encode()
    req = urllib.request.Request(OLLAMA, data=body, headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=60) as r:
        return json.load(r)["embedding"]


def cos(a, b):
    d = sum(x*y for x, y in zip(a, b))
    na = math.sqrt(sum(x*x for x in a)); nb = math.sqrt(sum(y*y for y in b))
    return d/(na*nb+1e-9)


def softmax(xs, t):
    m = max(xs); es = [math.exp((x-m)/t) for x in xs]; s = sum(es)
    return [e/s for e in es]


def ssr_score(text_emb, anchor_embs):
    sims = [cos(text_emb, ae) for ae in anchor_embs]
    p = softmax(sims, TEMP)
    return sum((i+1)*pi for i, pi in enumerate(p))  # expected Likert 1..5


def main():
    anchor_embs = {d: [embed(s) for s in stmts] for d, stmts in DIMS.items()}
    reactions = json.load(open(os.path.join(ROOT, "pm/reactions.json")))
    rows = []
    for r in reactions:
        te = embed(r["text"])
        scores = {d: round(ssr_score(te, anchor_embs[d]), 2) for d in DIMS}
        rows.append({"persona": r["persona"], "chapter": r["chapter"], **scores})

    # aggregate per chapter
    chapters = []
    for ch in ["landing", "ch01", "ch04", "ch08"]:
        sub = [r for r in rows if r["chapter"] == ch]
        agg = {d: round(sum(r[d] for r in sub)/len(sub), 2) for d in DIMS}
        chapters.append({"chapter": ch, **agg})

    lines = ["# SSR Persona Ratings (1-5)",
             "",
             "Method: Semantic Similarity Rating (arXiv:2510.08338) over persona free-text",
             "reactions, local `nomic-embed-text`. Higher = better (ease: higher = easier).",
             "", "## Per-chapter mean across 5 personas", "",
             "| chapter | clarity | ease | relevance | engagement | trust |",
             "|---|---|---|---|---|---|"]
    for c in chapters:
        lines.append(f"| {c['chapter']} | {c['clarity']} | {c['ease']} | {c['relevance']} | {c['engagement']} | {c['trust']} |")
    lines += ["", "## Per-persona × chapter", "",
              "| persona | chapter | clarity | ease | relevance | engagement | trust |",
              "|---|---|---|---|---|---|---|"]
    for r in rows:
        lines.append(f"| {r['persona']} | {r['chapter']} | {r['clarity']} | {r['ease']} | {r['relevance']} | {r['engagement']} | {r['trust']} |")
    open(os.path.join(ROOT, "SSR_REPORT.md"), "w").write("\n".join(lines)+"\n")
    print("\n".join(lines[:18]))
    print("\nwrote SSR_REPORT.md")


if __name__ == "__main__":
    main()
