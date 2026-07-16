# TripleA Battle Gym

`triplea-battle-gym` is a typed Python client and Gymnasium adapter for TripleA's
newline-delimited JSON simulation server. The server exposes two levels of play
and this package wraps both: single battles (`TripleABattleEnv`) and whole player
turns (`TripleAStrategicEnv`).

## Install

```bash
cd python/battle-gym
python -m pip install -e '.[dev]'
```

Start the Java server with quiet Gradle output:

```bash
./gradlew -q :game-headless:runBattleSimulationServer
```

The Python client can launch that command itself.

## Basic use

```python
from triplea_battle_gym import BattleResetRequest, TripleABattleEnv

env = TripleABattleEnv(
    server_command=("./gradlew", "-q", ":game-headless:runBattleSimulationServer"),
    reset_request=BattleResetRequest(
        scenario_path="/absolute/path/to/game.tsvg",
        seed=12345,
        territory="Eastern Ukraine",
    ),
)

observation, info = env.reset()
while True:
    valid_indices = observation["action_mask"].nonzero()[0]
    action = int(valid_indices[0])
    observation, reward, terminated, truncated, info = env.step(action)
    if terminated or truncated:
        break

env.close()
```

The Gymnasium action space is fixed-size `Discrete(max_actions)`. The adapter
expands TripleA's casualty-selection descriptor into concrete legal actions.
If a decision has more concrete combinations than `max_actions`, reset or step
raises `ActionSpaceOverflow` rather than silently discarding legal actions.

## Strategic environment

`TripleAStrategicEnv` plays a whole turn rather than one battle. An episode is a
single player's turn: the scenario restores that player from a save, runs its
reinforcement, combat move, battle and redeployment phases, and ends at
`COMPLETE`. There is no victory inside an episode, so the reward is the score
margin the turn swung — the player's operational score minus the best rival
score, before and after each action.

```python
from triplea_battle_gym import StrategicResetRequest, TripleAStrategicEnv

env = TripleAStrategicEnv(
    server_command=("./gradlew", "-q", ":game-headless:runBattleSimulationServer"),
    reset_request=StrategicResetRequest(
        scenario_path="/absolute/path/to/game.tsvg",
        seed=12345,
        player="Germans",
    ),
    max_territories=64,
)

observation, info = env.reset()
while True:
    valid_indices = observation["action_mask"].nonzero()[0]
    observation, reward, terminated, truncated, info = env.step(int(valid_indices[0]))
    if terminated or truncated:
        break

env.close()
```

Observations are filtered through fog of war by the server before they are sent.
Territories occupy fixed slots ordered by name, so a territory always lands in
the same slot. A fogged territory is not encoded as an empty one: its owner,
supply and unit counts encode as the sentinel `-1` rather than `0`, so the policy
cannot mistake "unseen" for "empty". Only the map graph — names, neighbours and
road counts — stays visible under fog, because the map layout is public.

Scores are deliberately absent from the observation. A score counts territory the
acting player may not be able to see, so putting it in the observation would leak
exactly what the fog hides. The reward is computed server-side from the true
state and only ever reaches the agent as a scalar.

Moves and phase ends arrive concrete, but a battle decision does not: casualty
selection arrives as a descriptor naming the candidates and the hit count. The
environment expands those through the same routine the battle environment uses,
so `env.step(i)` always indexes a mask the server will accept. Raise
`max_actions` if the expanded mask outgrows it.

## Self-play

Pass `self_play=True` and an episode becomes a whole game instead of one turn.
Every player's turn is chained over one game state — nothing is written to disk
between turns — and `info["player"]` says who is acting now, so a shared policy
sees both sides.

```python
request = StrategicResetRequest(
    scenario_path="/absolute/path/to/game.tsvg",
    seed=12345,
    self_play=True,
    max_rounds=12,   # a backstop; the map's own scoring round normally ends it first
)
```

Each step's reward is the acting player's margin swing **since that player last
acted**, not since the action started. The difference matters: measuring only
within an action would pay a player for ground it took and never charge it for
ground the opponent took back on the turn in between, which trains a policy to
attack and never cover. Anchoring on the player's previous decision folds the
opponent's whole turn into the next reward that player sees, and makes each
player's rewards sum to its margin change across the episode.

Because that reward carries per-episode state, one reward function belongs to one
environment. `reset()` clears it.

### One-command Small Front training on Windows

From the repository root, this command creates `.venv`, installs the optional
training dependencies, starts the Java simulation server and begins Maskable PPO
self-play on the Meuse map:

```bat
train-small-front.cmd
```

The defaults are 200,000 timesteps and one environment. Positional arguments are
`timesteps`, `parallel environments`, `output model`, and `checkpoint interval`:

```bat
REM Quick pipeline check
train-small-front.cmd 5000 1 runs\small-front-smoke.zip 1000

REM Four parallel self-play games
train-small-front.cmd 200000 4 runs\small-front-ppo.zip 20000
```

The final model is written to `runs\small-front-ppo.zip` by default. Checkpoints
are written under `runs\checkpoints` by Stable-Baselines3.

After installing the package manually, the equivalent console command is
`triplea-strategic-train`.

## Baselines

```bash
triplea-battle-evaluate \
  --server-command './gradlew -q :game-headless:runBattleSimulationServer' \
  --scenario /absolute/path/to/game.tsvg \
  --episodes 100 \
  --policy scripted
```

The `scripted` policy uses TripleA's default casualties and declines optional
retreats. The `random` policy samples uniformly from the current concrete legal
actions.

## Maskable PPO example

Install optional training dependencies:

```bash
python -m pip install -e '.[train]'
```

Then run:

```bash
triplea-battle-train \
  --server-command './gradlew -q :game-headless:runBattleSimulationServer' \
  --scenario /absolute/path/to/game.tsvg \
  --timesteps 100000 \
  --output battle-policy.zip
```

For whole-game strategic self-play, use:

```bash
triplea-strategic-train \
  --server-command './gradlew -q :game-headless:runBattleSimulationServer' \
  --scenario /absolute/path/to/Small_Front_Meuse.xml \
  --timesteps 200000 \
  --output strategic-policy.zip
```

## Process workers

`BattleWorkerPool` launches one independent Java NDJSON process per worker and
runs reset/step calls concurrently while preserving input order.
