#!/usr/bin/env node
// Cross-validate every checker under the SAME JS RegExp logic the static player
// uses (player/index.html runChecker), against each step's solution from
// course.json. Guards against Python-re vs JS-RegExp dialect drift.
// Usage: node pm/verify_player.js   (exit 1 if any checker fails/errors)
const fs = require("fs");
const path = require("path");

const course = JSON.parse(
  fs.readFileSync(path.join(__dirname, "..", "player", "course.json"), "utf8")
);

function run(chk, buf) {
  if (!chk || chk.type !== "regex") return true;
  try {
    return new RegExp(chk.pattern, (chk.flags || "m").includes("m") ? "m" : "").test(buf);
  } catch (e) {
    return "ERR:" + e.message;
  }
}

let total = 0, pass = 0;
const fail = [], err = [];
for (const ch of course.chapters)
  for (const l of ch.lessons)
    for (const s of l.steps) {
      if (!s.checker) continue;
      total++;
      const r = run(s.checker, s.solution || "");
      if (r === true) pass++;
      else if (typeof r === "string") err.push(s.checker.id + " " + r);
      else fail.push(s.checker.id);
    }

console.log(`JS-side: ${total} checkers, ${pass} pass`);
if (fail.length) console.log("FAIL (no match under JS):", fail.join(", "));
if (err.length) console.log("REGEX ERRORS:", err.join(" | "));
if (!fail.length && !err.length) console.log("ALL CHECKERS PASS UNDER JS REGEXP");
process.exit(fail.length || err.length ? 1 : 0);
