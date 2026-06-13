#!/usr/bin/env python3
"""Generate per-chapter TEACHER lecture notes via qwen, tuned to the course's real
demographic (delivery/services Java devs, India + Central/Eastern Europe). Output
plaintext to teacher/notes.src.md (gitignored). It is then encrypted with
pm/encrypt_notes.js into player/teacher/notes.enc (ciphertext-only in the repo).

PM supplies the prompt; qwen authors. Usage: gen_teacher_notes.py
"""
import glob, json, os, re, time, urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OLLAMA = "http://localhost:11434/api/generate"
MODEL = "qwen2.5-coder:14b"
H1 = re.compile(r"^#\s+(.*)$", re.M)
OBJ = re.compile(r"\*\*Chapter learning objective:?\*\*\s*(.*)")
LESSON = re.compile(r"^##\s+Lesson\s+[\d.]+\s*[—-]\s*(.*)$", re.M)

PROMPT = """You are writing PRIVATE LECTURE NOTES for the instructor of a CMTAT/Solidity course. The students are DELIVERY / SERVICES JAVA developers (strong Spring; mostly India and Central/Eastern Europe) building for bank/fintech CLIENTS — they integrate to spec, they do not run a bank core. Many are junior and new to the finance domain; some are self-taught with CS-theory gaps. Write notes the teacher can teach from. Be concrete and specific to THIS chapter. Use this structure exactly, in Markdown:

## {title}
**Teaching goal (1-2 sentences).**
**Lead with this analogy:** <the single Spring/Java analogy to open the chapter with>
**Common misconceptions for this audience:** <3-4 bullets — what delivery Java devs / juniors get wrong here>
**Where they get stuck:** <2-3 bullets — specific friction points + how to unblock>
**Localise:** <1-2 bullets mapping the Swiss example to India (UPI/IMPS/NPCI, INR) or EU/CEE (SEPA/TARGET2)>
**Check for understanding:** <2 quick questions to ask the room, with the answer>
**Timing:** <rough minutes + what to demo live>

Output ONLY the notes for this chapter, no preamble.

CHAPTER: {title}
OBJECTIVE: {objective}
LESSONS: {lessons}
"""


def qwen(prompt):
    body = json.dumps({"model": MODEL, "prompt": prompt, "stream": False,
                       "options": {"temperature": 0.4, "num_ctx": 8192, "num_predict": 1400}}).encode()
    req = urllib.request.Request(OLLAMA, data=body, headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=600) as r:
        return json.load(r)["response"]


def main():
    files = sorted(glob.glob(os.path.join(ROOT, "course", "[0-9][0-9]-*.md")))
    files = [f for f in files if not os.path.basename(f).startswith("00")]
    out = ["# CMTAT Course — Teacher Lecture Notes (PRIVATE)",
           "_Audience: delivery/services Java developers, India + Central/Eastern Europe._", ""]
    for f in files:
        t = open(f).read()
        title = H1.search(t).group(1).strip() if H1.search(t) else os.path.basename(f)
        objm = OBJ.search(t); objective = objm.group(1).strip() if objm else ""
        lessons = " · ".join(m.group(1).strip() for m in LESSON.finditer(t))[:600]
        t0 = time.time()
        notes = qwen(PROMPT.format(title=title, objective=objective[:400], lessons=lessons)).strip()
        out.append(notes); out.append("\n---\n")
        print(f"{os.path.basename(f)}: {len(notes)} chars ({time.time()-t0:.0f}s)", flush=True)
    os.makedirs(os.path.join(ROOT, "teacher"), exist_ok=True)
    open(os.path.join(ROOT, "teacher/notes.src.md"), "w").write("\n".join(out))
    print("wrote teacher/notes.src.md")


if __name__ == "__main__":
    main()
