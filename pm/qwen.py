#!/usr/bin/env python3
"""PM orchestration glue: send a prompt file to local qwen, write the response.

Not course content — this is the harness the PM (Claude) drives. The course
itself is authored entirely by the qwen model.

Usage: qwen.py <prompt_file> <out_file> [model]
"""
import json, sys, time, urllib.request

OLLAMA = "http://localhost:11434/api/generate"
DEFAULT_MODEL = "qwen2.5-coder:14b"


def generate(prompt, model):
    body = json.dumps({
        "model": model,
        "prompt": prompt,
        "stream": False,
        "options": {"temperature": 0.2, "num_ctx": 16384, "num_predict": 10240},
    }).encode()
    req = urllib.request.Request(OLLAMA, data=body, headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=1800) as r:
        return json.load(r)


def main():
    prompt_file, out_file = sys.argv[1], sys.argv[2]
    model = sys.argv[3] if len(sys.argv) > 3 else DEFAULT_MODEL
    prompt = open(prompt_file).read()
    t0 = time.time()
    res = generate(prompt, model)
    text = res.get("response", "")
    open(out_file, "w").write(text)
    dt = time.time() - t0
    toks = res.get("eval_count", 0)
    print(f"OK {out_file} :: {len(text)} chars, {toks} tok, {dt:.0f}s, "
          f"{toks/dt:.1f} tps, model={model}")


if __name__ == "__main__":
    main()
