# AI benchmark

Benchmarks a TripleA AI against a control AI over a set of scenarios ("kata"),
seat by seat, and reports how game state compares at the end of every player
turn. The result is a scorecard: each kata seat is a box score, each metric an
inning, control and experimental the two teams.

## Run

From this folder (overrides go before the recipe):

```
just run                          # Hard Beta on every kata
just ai=Hard kata='big_*' run     # another AI, a kata glob
just kata=_pipeline run           # the pipeline check on a tiny map
just list                         # kata found and their limits
just tabulate <run dir>           # rebuild a scorecard from raw data alone
```

AIs: `Hard`, `Hard Beta`, `Fast`, `Easy` (see `AiRegistry`). Kata whose folder
starts with `_` are internal and only run when the glob starts with `_` too.
Each run writes `build/ai-benchmark/<timestamp>-<ai>/` at the repo root.

## How a kata is played

For each seat (an alliance), two arms: the kata's `control_ai` plays the seat
in the control arm and the AI under test plays it in the experimental arm;
`reference_ai` plays everyone else in both. Games go in pairs, every seat and
both arms per pair, so the budget cuts all of them evenly. Arms are compared
in the same seat only; maps aren't balanced, so swapping sides isn't
comparable.

At each player-turn end the runner records every metric for the seat, for the
enemy (every player outside the seat's alliance), and the gap between them.
The gap is the signal: +10 PUs for the seat and -10 for the enemy is +20.

## Add a kata

A folder under `kata/` holding a `kata.yaml` and its start file:

```yaml
description: Pipeline check on a tiny map
start:
  map: game.xml             # or  save: position.tsvg
seats: [Axis, Allies]       # alliances; each is benchmarked separately
reference_ai: Hard          # default Hard
control_ai: Hard            # default Hard
limits:
  rounds: 10                # round cap per game (required)
  min_games: 2              # default 2; per seat per arm
  max_games: 20             # default 20
  budget_minutes: 20        # default 20; no new pair starts after this
  game_timeout_minutes: 30  # default 30
primary_metric: tuv         # default tuv
metrics: [tuv, income]      # default: all registered
props: {Low Luck: true}     # game options by XML name
```

`name` defaults to the folder name. Unknown keys, AIs, and metrics are errors.

## Add a metric

Implement `metrics.Metric` (a value summed over a group of players) and add it
to the list in `metrics.Metrics`. The runner computes the gap.

Current metrics: `tuv`, `income` (production), `pus` (on hand), `units`, `vc`
(victory cities). `tuv` and `units` count every unit, including ones a map's
display settings hide, since a headless run has no map resources.

## Raw data (schema 1)

The runner never analyses; the tabulator reads only these files.

- `run.json`: `schema`, `ai`, `git_sha`, `git_dirty`, `workers`, `java`,
  `cpus`, `started_at`, `ended_at`, and `kata`: each descriptor as resolved.
- `games.jsonl`, one per game: `kata`, `seat`, `arm` (`control` or
  `experimental`), `pair`, `game` (id), `ai`, `reference_ai`, `seed` (null for
  now), `status` (`ok`, `timeout`, `crash`), `winner` (alliances joined by
  `+`, or null), `winners` (players), `rounds`, `capped`, `error`,
  `wall_seconds`, `cpu_seconds`.
- `snapshots.jsonl`, one per player-turn end plus one with `last: true` for
  the final state: `kata`, `seat`, `arm`, `pair`, `game`, `round`, `player`
  (whose turn ended; null on the last record), `turn_index`, `last`, and
  `metrics`: `{<id>: {seat, enemy, gap}}`.

## Scorecard

`scorecard.json`, `scorecard.html` (self-contained), and the terminal box
scores. Cells are the mean gap at each game's final snapshot with a 95%
Student-t CI; the diff row is experimental minus control with a Welch CI.
Only `ok` games count; crashes and timeouts show in the record column.
Trajectory charts plot the mean gap per turn index with n per point, since
games that end early drop out of later turns.
