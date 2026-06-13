#!/usr/bin/env python3
"""Index course/NN-*.md into player/course.json for the interactive player.

Emits per chapter: meta (track, emphasis threads, objective, prerequisites,
what you build), Part/Track grouping, time estimate, lessons -> steps
(instruction/explanation/starter/solution/checker), and the parsed end-of-chapter
quiz. The chapter map (00) is excluded — the player has its own onboarding.

Pure parsing (no model). Usage: build_player.py
"""
import glob, json, os, re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

H1 = re.compile(r"^#\s+(.*)$", re.M)
LESSON = re.compile(r"^##\s+Lesson\s+([\d.]+)\s*[—-]\s*(.*)$", re.M)
STEP = re.compile(r"^###\s+Step\s+([\d.]+)\s*[—-]\s*(.*)$", re.M)
INSTR = re.compile(r"\*\*Instruction:?\*\*\s*(.*?)(?=\n\*\*|\Z)", re.S)
EXPL = re.compile(r"\*\*Explanation:?\*\*\s*(.*?)(?=\n\*\*|\Z)", re.S)
STARTER = re.compile(r"\*\*Starter code:?\*\*\s*```[a-z]*\n(.*?)```", re.S)
SOL = re.compile(r"\*\*Solution:?\*\*\s*```[a-z]*\n(.*?)```", re.S)
CHK = re.compile(r"```checker\s*\n(\{.*?\})\s*```", re.S)

OBJECTIVE = re.compile(r"\*\*Chapter learning objective:?\*\*\s*(.*)")
PREREQ = re.compile(r"\*\*Prerequisites:?\*\*\s*(.*)")
TRACK = re.compile(r"\*\*Track:?\*\*\s*(.*?)(?:·|\n|$)")
THREADS = re.compile(r"\*\*Emphasis threads:?\*\*\s*(.*)")
BUILDS = re.compile(r"\*\*(?:Contract built|You will build|Contract):?\*\*\s*(.*)")
LOBJ = re.compile(r"\*\*Learning objective:?\*\*\s*(.*)")

QUIZ_SEC = re.compile(r"^#+\s*Quiz\s*$", re.M)
QITEM = re.compile(r"\*\*Q(\d+)\s*\(([^)]+)\)\.?\*\*\s*(.*?)(?=\n\*\*Q\d+\s*\(|\Z)", re.S)
OPT = re.compile(r"(?:^|[—-]\s*)([a-d])\)\s*(.*?)(?=\s+[—-]\s+[a-d]\)|$)", re.S)
ANSWER = re.compile(r"\*\*Answer:?\s*([a-d])?\.?\*\*\s*(.*)", re.S)

# Part / track grouping by chapter number (stable per the chapter map).
PARTS = [
    ("Shared Core", "shared", range(1, 10)),
    ("Track A — Tokenized Bond", "A", range(10, 13)),
    ("Track B — Equity Share", "B", range(13, 16)),
    ("Track C — Money-Market Fund", "C", range(16, 19)),
]


def split_blocks(text, regex):
    ms = list(regex.finditer(text))
    for i, m in enumerate(ms):
        end = ms[i + 1].start() if i + 1 < len(ms) else len(text)
        yield m.groups(), text[m.end():end]


def clean(s):
    return re.sub(r"`", "", (s or "")).strip()


def first(rx, text):
    m = rx.search(text)
    return m.group(1).strip() if m else ""


def parse_step(body):
    chk = None
    cm = CHK.search(body)
    if cm:
        try:
            chk = json.loads(cm.group(1))
        except json.JSONDecodeError:
            chk = None
    sm, stm = SOL.search(body), STARTER.search(body)
    return {
        "instruction": first(INSTR, body),
        "explanation": first(EXPL, body),
        "starter": stm.group(1) if stm else "",
        "solution": sm.group(1) if sm else "",
        "checker": chk,
    }


def parse_quiz(text):
    qm = QUIZ_SEC.search(text)
    if not qm:
        return []
    block = text[qm.end():]
    block = re.split(r"\n```checker", block)[0]  # drop trailing assembled checker
    out = []
    for n, qtype, body in (m.groups() for m in QITEM.finditer(block)):
        body = body.strip()
        am = ANSWER.search(body)
        question = body[:am.start()].strip() if am else body
        # options are the line(s) before the answer
        opts = []
        optline = question
        om = re.search(r"\n([a-d]\).*)", question, re.S)
        if om:
            question = question[:om.start()].strip()
            optline = om.group(1)
            for k, t in OPT.findall(optline.replace("\n", " ")):
                opts.append({"key": k, "text": clean(t)})
        ans_key = am.group(1) if am else None
        ans_text = clean(am.group(2)) if am else ""
        out.append({
            "n": int(n), "type": qtype.strip().lower(),
            "question": clean(question), "options": opts,
            "answer": ans_key, "rationale": ans_text,
        })
    return out


def chapter_no(path):
    m = re.match(r"(\d+)", os.path.basename(path))
    return int(m.group(1)) if m else 0


def parse_chapter(path):
    text = open(path).read()
    head = text[:text.find("\n## ")] if "\n## " in text else text[:1500]
    h1 = H1.search(text)
    title = h1.group(1).strip() if h1 else os.path.basename(path)
    lessons = []
    nsteps = 0
    for (lnum, ltitle), lbody in split_blocks(text, LESSON):
        steps = []
        for (snum, stitle), sbody in split_blocks(lbody, STEP):
            s = parse_step(sbody)
            s["id"] = snum
            s["title"] = stitle.strip()
            steps.append(s)
            nsteps += 1
        lessons.append({"n": lnum, "title": ltitle.strip(),
                        "objective": first(LOBJ, lbody), "steps": steps})
    quiz = parse_quiz(text)
    est = round(nsteps * 1.5 + (3 if quiz else 0))
    return {
        "file": os.path.basename(path), "nn": chapter_no(path), "title": title,
        "track": clean(first(TRACK, head)), "threads": clean(first(THREADS, head)),
        "objective": first(OBJECTIVE, head), "prerequisites": first(PREREQ, head),
        "builds": clean(first(BUILDS, head)), "est_min": est,
        "lessons": lessons, "quiz": quiz,
    }


def main():
    files = sorted(glob.glob(os.path.join(ROOT, "course", "[0-9][0-9]-*.md")))
    files = [f for f in files if chapter_no(f) >= 1]  # exclude 00 chapter map
    chapters = [parse_chapter(f) for f in files]
    by_nn = {c["nn"]: i for i, c in enumerate(chapters)}
    parts = []
    for title, track, rng in PARTS:
        idxs = [by_nn[n] for n in rng if n in by_nn]
        if idxs:
            parts.append({"title": title, "track": track, "chapters": idxs})
    os.makedirs(os.path.join(ROOT, "player"), exist_ok=True)
    json.dump({"parts": parts, "chapters": chapters},
              open(os.path.join(ROOT, "player", "course.json"), "w"), indent=1)
    nq = sum(len(c["quiz"]) for c in chapters)
    nstep = sum(len(l["steps"]) for c in chapters for l in c["lessons"])
    nchk = sum(1 for c in chapters for l in c["lessons"] for s in l["steps"] if s["checker"])
    print(f"course.json: {len(chapters)} chapters, {len(parts)} parts, "
          f"{nstep} steps, {nchk} checkers, {nq} quiz questions")


if __name__ == "__main__":
    main()
