#!/usr/bin/env python3
"""Index all course/NN-*.md into player/course.json: chapters -> lessons ->
steps -> {instruction, explanation, starter, solution, checker}. The static
player reads this to render the interactive course (gap fix #2).

Pure parsing glue (no model). Usage: build_player.py
"""
import glob, json, os, re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

H1 = re.compile(r"^#\s+(.*)$", re.M)
LESSON = re.compile(r"^##\s+Lesson\s+(\d+)\s*[—-]\s*(.*)$", re.M)
STEP = re.compile(r"^###\s+Step\s+([\d.]+)\s*[—-]\s*(.*)$", re.M)
INSTR = re.compile(r"\*\*Instruction:?\*\*\s*(.*?)(?=\n\*\*|\Z)", re.S)
EXPL = re.compile(r"\*\*Explanation:?\*\*\s*(.*?)(?=\n\*\*|\Z)", re.S)
STARTER = re.compile(r"\*\*Starter code:?\*\*\s*```[a-z]*\n(.*?)```", re.S)
SOL = re.compile(r"\*\*Solution:?\*\*\s*```[a-z]*\n(.*?)```", re.S)
CHK = re.compile(r"```checker\s*\n(\{.*?\})\s*```", re.S)


def split_blocks(text, regex):
    """Yield (header_groups, body) for each regex match up to the next match."""
    ms = list(regex.finditer(text))
    for i, m in enumerate(ms):
        end = ms[i + 1].start() if i + 1 < len(ms) else len(text)
        yield m.groups(), text[m.end():end]


def parse_step(body):
    def first(rx):
        m = rx.search(body)
        return m.group(1).strip() if m else ""
    chk = None
    cm = CHK.search(body)
    if cm:
        try:
            chk = json.loads(cm.group(1))
        except json.JSONDecodeError:
            chk = None
    return {
        "instruction": first(INSTR),
        "explanation": first(EXPL),
        "starter": first(STARTER),
        "solution": first(SOL),
        "checker": chk,
    }


def parse_chapter(path):
    text = open(path).read()
    h1 = H1.search(text)
    title = h1.group(1).strip() if h1 else os.path.basename(path)
    lessons = []
    for (lnum, ltitle), lbody in split_blocks(text, LESSON):
        steps = []
        for (snum, stitle), sbody in split_blocks(lbody, STEP):
            s = parse_step(sbody)
            s["id"] = snum
            s["title"] = stitle.strip()
            steps.append(s)
        lessons.append({"n": lnum, "title": ltitle.strip(), "steps": steps})
    return {"file": os.path.basename(path), "title": title, "lessons": lessons}


def main():
    files = sorted(glob.glob(os.path.join(ROOT, "course", "[0-9][0-9]-*.md")))
    chapters = [parse_chapter(f) for f in files]
    os.makedirs(os.path.join(ROOT, "player"), exist_ok=True)
    out = os.path.join(ROOT, "player", "course.json")
    json.dump({"chapters": chapters}, open(out, "w"), indent=1)
    nsteps = sum(len(l["steps"]) for c in chapters for l in c["lessons"])
    nchk = sum(1 for c in chapters for l in c["lessons"] for s in l["steps"] if s["checker"])
    print(f"course.json: {len(chapters)} chapters, "
          f"{sum(len(c['lessons']) for c in chapters)} lessons, "
          f"{nsteps} steps, {nchk} checkers")


if __name__ == "__main__":
    main()
