# Small Front local LLM player

`play-small-front-llm.cmd` runs the Meuse scenario through TripleA's headless strategic environment
and lets a local Ollama model choose from the engine's legal action mask.

The model does **not** receive arbitrary Python or shell access. It can only call these bounded tools:

- `get_game_state`
- `list_legal_actions`
- `inspect_action`
- `simulate_action`
- `execute_action`
- `end_phase`

TripleA validates every executed action. Fogged owners, unit stacks, and supply state are not included
in the model observation.

## Requirements

- Windows
- Python 3.11 or newer
- Java configured for this repository
- Ollama installed and running

The launcher creates or reuses `.venv`, installs the editable Python package, and pulls the selected
model when it is missing.

## Run

From the repository root:

```bat
play-small-front-llm.cmd
```

The default model is `qwen3:8b`. Positional arguments are:

1. Ollama model
2. RNG seed
3. maximum rounds
4. default shadow rollouts

Examples:

```bat
REM Default qwen3:8b, seed 1, 12 rounds, one exact shadow rollout
play-small-front-llm.cmd

REM Use another tool-capable local model
play-small-front-llm.cmd qwen3:14b

REM Four battle rollouts before the episode has resolved a battle
play-small-front-llm.cmd qwen3:8b 7 12 4
```

The JSONL trace is written to:

```text
runs\local-llm\small-front-llm.jsonl
```

## Shadow simulation

A simulation starts a disposable TripleA server, reloads the same scenario and replays the real
action history using UUID-independent action descriptors. It then applies the candidate, immediately
ends remaining combat and air movement, uses TripleA's default casualties, declines optional
retreats, and stops at redeployment.

Before the first resolved battle, several seeds can evaluate the same deterministic movement state
with different future dice. After a battle has already consumed RNG, changing the initial seed would
also change the history that produced the current position. The tool therefore reduces a requested
multi-seed evaluation to one exact original-seed rollout in that case.

This first implementation is a bounded tactical calculator, not a perfect game-tree search. A future
server-side state fork could support true Monte Carlo evaluation from any mid-game position without
replaying the episode.

## Battle decisions

Casualty selection and optional retreat decisions are handled automatically with the engine default.
The local model controls reinforcement allocation, combat movement, air assignment, redeployment and
phase completion. This keeps the tool context strategic and makes shadow replay independent of random
unit UUIDs.
