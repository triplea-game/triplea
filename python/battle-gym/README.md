# TripleA Battle Gym

`triplea-battle-gym` is a typed Python client and Gymnasium adapter for TripleA's
newline-delimited JSON battle simulation server.

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

## Process workers

`BattleWorkerPool` launches one independent Java NDJSON process per worker and
runs reset/step calls concurrently while preserving input order.
