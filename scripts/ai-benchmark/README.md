# AI benchmark

Plays a TripleA AI over a set of scenarios ("kata"), seat by seat, and
records game state at the end of every player turn. Comparing runs is the
tabulator's job: the scorecard sets any number of run folders side by side,
each kata seat a box score, each metric an inning, each run a team. To measure
a change, keep a run of the stock AI (eg `just ai=Hard run`) and compare it
with later runs; it does not need replaying.

## Run

From this folder (overrides go before the recipe):

```
just run                          # Hard Beta on every kata
just ai=Hard kata='big_*' run     # another AI, a kata glob
just kata=_pipeline run           # the pipeline check on a tiny map
just workers=8 run                # 8 games at once (default: half the cores, capped by free memory)
just list                         # kata found and their limits
just tabulate <run dir>...        # compare runs from raw data; others are diffs from the first
```

AIs: `Hard`, `Hard Beta`, `Fast`, `Easy` (see `AiRegistry`). Kata whose folder
starts with `_` are internal and only run when the glob starts with `_` too.
Each run writes `build/ai-benchmark/<timestamp>-<ai>/` at the repo root.

## How a kata is played

For each seat (an alliance), the AI under test plays the seat and
`reference_ai` plays everyone else. Games go in pairs, one per seat, so the
budget cuts all seats evenly; a started pair is always finished. Each game runs
in its own JVM, several at once; a game whose worker fails keeps its log under
`parts/` in the run folder. Runs are compared in the same seat only; maps
aren't balanced, so swapping sides isn't comparable.

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
limits:
  rounds: 10                # round cap per game (required)
  min_games: 2              # default 2; per seat
  max_games: 20             # default 20
  budget_minutes: 20        # default 20; no new pair starts after this
  game_timeout_minutes: 30  # default 30
primary_metric: tuv         # default tuv
metrics: [tuv, income]      # default: all registered
props: {Low Luck: true}     # game options by XML name
```

`name` defaults to the folder name. Unknown keys, AIs, and metrics are errors.

## Open a kata in the client

`just open <kata>` launches the TripleA client on the kata's start file. To change a
save-start position, edit it in the client and save over the kata's `.tsvg`.

## Decisive end

A full game ends early, as a win, once one alliance has led at two round ends in a row: more
than 10% more unit value and more income than everyone outside it, all its own capitals held, and
at least one enemy capital held. The round cap still applies. The thresholds are constants in
`runner/Decisive.java`, shared by every kata; a `stop_after` kata never uses the rule. The scorecard
counts a decisive game as a win for the leader; `end` in `games.jsonl` tells them apart.

## Focused runs: one turn, with detail

To study how an AI plays one position (a bug report, a specific turn), drop in a kata
for it and run only that. `limits.stop_after` ends each game when the named *player's* first
turn ends, so `rounds: 1, stop_after: Russians` plays Russia's turn and nothing after it.
A kata with `stop_after` records detail (override with `detail: true|false`):

- `steps.jsonl`: after each non-bid step of the seat's players, every land territory's owner and
  every unit-holding territory's units by type, split into the seat's side (`u`) and the rest
  (`e`). The tabulator tallies it per run: seat-owned % and mean seat units per territory per
  step, keeping the territories where the runs differ or the outcome varies (all of them in
  `scorecard.json`, the top 15 per step in the terminal).
- `logs/<kata>/<seat>/<pair>.log`: the Pro AI's own log for each game (what its log window
  shows; `=== round N <step>` lines mark where each step starts). Grep these for why.

```yaml
description: Russia round 1 on WW2v5 (forum 81063), one turn
start: {map: game.xml}          # copy the map's game XML next to this file
seats: [Allies]
limits: {rounds: 1, stop_after: Russians, min_games: 10, max_games: 10}
```

Games run one at a time in one JVM: a kata of 10 pairs takes minutes, and the default 2g heap
(`-Pheap=`) is plenty.

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
- `games.jsonl`, one per game: `kata`, `seat`, `pair`, `game` (id), `ai`, `reference_ai`, `seed` (null for
  now), `status` (`ok`, `timeout`, `crash`), `winner` (alliances joined by
  `+`, or null), `winners` (players), `rounds`, `capped`, `stopped` (ended by `stop_after`),
  `end` (`won`, `decisive`, `stop`, `cap`, `timeout`; null on a crash), `error`, `wall_seconds`,
  `cpu_seconds`.
- `steps.jsonl` (detail kata only), one per recorded step: `kata`, `seat`, `pair`, `game`,
  `round`, `player`, `step`, and `territories`: `{<name>: {o: owner, s: owner is in the seat, u:
  {<unit type>: n} seat's units, e: {<unit type>: n} everyone else's}}`.
- `snapshots.jsonl`, one per player-turn end plus one with `last: true` for
  the final state: `kata`, `seat`, `pair`, `game`, `round`, `player`
  (whose turn ended; null on the last record), `turn_index`, `last`, and
  `metrics`: `{<id>: {seat, enemy, gap}}`.

## Scorecard

`scorecard.json`, `scorecard.html` (self-contained), and the terminal box
scores. Cells are the mean gap at each game's final snapshot with a 95%
Student-t CI; each run after the first gets a diff row, that run minus the
first, with a Welch CI. One run is written into its own folder; several into a
new `compare-<timestamp>` folder beside the first.
Only `ok` games count; crashes and timeouts show in the record column.
Trajectory charts plot the mean gap per turn index with n per point, since
games that end early drop out of later turns.

Runs written when the runner still played a control AI also hold `arm`
(`control` or `experimental`) on each record; the tabulator uses only the
`experimental` games, so those folders still compare.
