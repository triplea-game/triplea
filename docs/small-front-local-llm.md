# Small Front local LLM player

`play-small-front-llm.cmd` runs the Meuse scenario through TripleA's headless strategic environment.
The local Ollama model acts as a **turn-level operational planner**, while a deterministic executor
chooses individual actions from TripleA's current legal action mask.

## Decision architecture

At the first strategic decision for each `(round, player)` pair:

1. TripleA produces a fog-filtered strategic observation.
2. Ollama returns one schema-constrained operational plan.
3. The plan remains active across Combat Move, Air Assignment, battles, and Redeployment.
4. Every individual action is scored and selected deterministically from the current legal mask.
5. The executor ends the phase when no legal action has positive plan value.

The model is therefore not called separately for every move. A second planning call is permitted only
when at least two operational actions have already completed, non-ending legal actions remain, and the
current plan cannot produce a positive action. Each plan can allow at most one such replan.

Public action explanations are deterministic and are generated from the exact engine action. They do
not require another model request.

## Operational plan schema

The planner returns a versioned JSON object with:

- `planId`, `playerName`, `round`, and Korean `commanderIntent`
- prioritized objectives
- objective dependencies
- protected supply or reserve territories
- `maximumReplans`, limited to `0` or `1`

Supported objective types are:

- `CAPTURE`
- `HOLD`
- `PROTECT_SUPPLY`
- `GAIN_AIR_SUPERIORITY`
- `REDEPLOY_RESERVE`
- `SCREEN`

The controller validates schema version, objective IDs, territory names, prerequisite references, and
dependency cycles. Invalid output falls back to a deterministic fog-safe plan.

## Deterministic executor

Action scoring combines:

- direct progress on active objectives
- movement toward the primary objective
- air assignment over a planned battle
- preservation of protected supply sources and reserves
- visible destination supply
- visible enemy pressure
- uncertainty penalties
- immediate movement-reversal penalties
- penalties for actions unrelated to the active plan

All candidates still come from TripleA's legal action mask. The planner does not hard-code movement,
scramble radius, airbase capacity, stack capacity, combat values, air-control persistence, or
combined-arms bonuses. The current engine rules and delegates remain authoritative.

Fogged owners, unit stacks, supply state, and air-control state are never added to the planning input.

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
REM Default qwen3:8b, seed 1, 12 rounds
play-small-front-llm.cmd

REM Use another local model with structured-output support
play-small-front-llm.cmd qwen3:14b
```

The JSONL trace is written to:

```text
runs\local-llm\small-front-llm.jsonl
```

It includes turn plans, fallback or replan events, deterministic action scores, accepted actions, and
battle transitions.

## Shadow simulation

The repository still includes bounded shadow simulation for the exploratory and verified-action agent
modes. The operational-plan launcher does not invoke shadow simulation for every action. This avoids
the previous behavior where evaluating one candidate immediately ended the remaining Combat Move and
Air Assignment phases and consequently undervalued multi-action setup sequences.

## Battle decisions

Casualty selection and optional retreat decisions are handled automatically with the engine default.
The operational planner controls reinforcement allocation, combat movement, air assignment,
redeployment, and phase completion through its persistent plan and the deterministic executor.
