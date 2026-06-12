#!/usr/bin/env bash
cd "$HOME/projects/cmtat-course" || exit 1
source ./.env
PROMPT="$(cat PROMPT.md)"
echo "START $(date)" >> logs/run.log
exec claude --dangerously-skip-permissions --model claude-fable-5 --verbose -p "$PROMPT" >> logs/run.log 2>&1
