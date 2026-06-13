#!/usr/bin/env python3
"""Normalize every chapter's quiz into one canonical, gradable format via qwen:
3 multiple-choice + 2 short-answer, each with an answer key + rationale. Replaces
the existing `## Quiz` section (or inserts one before the trailing **Next:**).

PM supplies the prompt + a compact chapter skeleton; qwen authors the questions.
Serial. Usage: gen_quizzes.py course/NN-foo.md [...]
"""
import json, os, re, sys, time, urllib.request

OLLAMA = "http://localhost:11434/api/generate"
MODEL = "qwen2.5-coder:14b"

H1 = re.compile(r"^#\s+(.*)$", re.M)
OBJ = re.compile(r"\*\*Chapter learning objective:?\*\*\s*(.*)")
LESSON = re.compile(r"^##\s+Lesson\s+[\d.]+\s*[—-]\s*(.*)$", re.M)
STEP = re.compile(r"^###\s+Step\s+[\d.]+\s*[—-]\s*(.*)$", re.M)
# quiz section: from a Quiz heading to next --- / ## / **Next:** / ```checker / EOF
QUIZ = re.compile(r"\n#+\s*(?:Chapter\s+\d+\s+)?Quiz\s*\n.*?(?=\n---|\n##\s|\n\*\*Next|\n```checker|\Z)", re.S | re.I)

PROMPT = """Write the end-of-chapter quiz for this chapter of an interactive Solidity + CMTAT course for Swiss core-banking developers (Java/.NET, zero blockchain). Output EXACTLY 5 questions — Q1-Q3 multiple choice, Q4-Q5 short answer — grounded in the chapter content. Use THIS EXACT format and nothing else (no heading, no preamble, no closing text):

**Q1 (multiple choice).** <question>
a) <option> — b) <option> — c) <option> — d) <option>
**Answer: <letter>.** <one-sentence rationale>

**Q2 (multiple choice).** <question>
a) <option> — b) <option> — c) <option> — d) <option>
**Answer: <letter>.** <rationale>

**Q3 (multiple choice).** <question>
a) <option> — b) <option> — c) <option> — d) <option>
**Answer: <letter>.** <rationale>

**Q4 (short answer).** <question>
**Answer:** <model answer, 1-2 sentences>

**Q5 (short answer).** <question>
**Answer:** <model answer, 1-2 sentences>

Make questions test understanding/transfer (banking integration, datatypes, security), not trivia. Vary the correct letter across Q1-Q3.

CHAPTER: {title}
OBJECTIVE: {objective}
LESSONS & STEPS (what was taught):
{skeleton}
"""


def qwen(prompt):
    body = json.dumps({"model": MODEL, "prompt": prompt, "stream": False,
                       "options": {"temperature": 0.3, "num_ctx": 8192, "num_predict": 2200}}).encode()
    req = urllib.request.Request(OLLAMA, data=body, headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=600) as r:
        return json.load(r)["response"]


def skeleton(text):
    out = []
    for lm in LESSON.finditer(text):
        out.append("- " + lm.group(1).strip())
    return "\n".join(out[:12])


def extract_quiz(resp):
    m = re.search(r"\*\*Q1\b.*", resp, re.S)
    if not m:
        return None
    q = m.group(0).strip()
    # cut anything after Q5's answer if the model rambles
    return q


def process(path):
    text = open(path).read()
    title = H1.search(text).group(1).strip() if H1.search(text) else os.path.basename(path)
    objm = OBJ.search(text)
    objective = objm.group(1).strip() if objm else ""
    prompt = PROMPT.format(title=title, objective=objective[:400], skeleton=skeleton(text))
    quiz = extract_quiz(qwen(prompt))
    if not quiz or quiz.count("**Answer") < 4:
        print(f"{os.path.basename(path)}: qwen quiz unusable — skipped")
        return
    block = "\n## Quiz\n\n" + quiz + "\n"
    if QUIZ.search(text):
        text = QUIZ.sub(block, text, count=1)
    else:  # insert before trailing **Next:** or append
        nm = re.search(r"\n\*\*Next", text)
        text = (text[:nm.start()] + "\n" + block + text[nm.start():]) if nm else (text + "\n" + block)
    open(path, "w").write(text)
    print(f"{os.path.basename(path)}: quiz normalized ({quiz.count('**Q')} Qs)")


def main():
    for p in sys.argv[1:]:
        t0 = time.time()
        process(p)
        print(f"  ({time.time()-t0:.0f}s)", flush=True)


if __name__ == "__main__":
    main()
