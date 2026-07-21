# Strategic-agent simulation

Small Front exposes a deterministic turn-level environment alongside the isolated-battle
environment. It can restore a save game or start a map XML, run normal TripleA delegates, and expose
reinforcement allocation, combat movement, air assignment, battles, and redeployment through one
versioned protocol.

## Environment lifecycle

The Java contract is `StrategicEnvironment`:

```text
reset(request) -> observation
legalActions() -> ordered action mask
step(action) -> observation, reward, terminated, truncated, info
```

`StrategicResetRequest` contains:

- `scenarioPath`: a map XML or `.tsvg` save
- `seed`: deterministic episode seed
- `player`: exact selected player outside self-play
- `maxActions`: maximum legal-action count
- `selfPlay`: chain all player turns over one game
- `maxRounds`: optional whole-game backstop

Action masks are never silently truncated. If a state produces more actions than `maxActions`, the
environment raises `StrategicActionSpaceOverflow`.

## Reward semantics

The standard strategic reward is the immediate change in the acting player's score margin:

```text
(player score - best rival score) after action
-
(player score - best rival score) before action
```

The true score is kept outside the fog-filtered observation. Reward code may use authoritative state
without exposing hidden ownership or objective information to the policy.

Reward is transition-local. It does not carry an old player margin across another player's turn,
because that would attach an opponent-caused score change to the next action submitted by the
current player.

For PPO training, `SingleSideStrategicEnv` exposes one learner side and executes the fixed opponent
internally. Opponent rewards are reversed and accumulated into the learner transition. This creates
a conventional single-agent trajectory instead of interleaving two players' actions and returns in
one rollout buffer. The wrapper currently supports two-sided Small Front scenarios.

## NDJSON commands

`BattleSimulationServer` accepts:

- `strategicReset`
- `strategicLegalActions`
- `strategicStep`

Example reset:

```json
{
  "command": "strategicReset",
  "data": {
    "scenarioPath": "/maps/small_front_meuse/games/small_front_meuse.xml",
    "seed": 7,
    "player": "Germans",
    "maxActions": 2048,
    "selfPlay": true,
    "maxRounds": 12
  }
}
```

The `schema` command reports battle and strategic observation schema versions and whether each
service-loaded environment is available.

## Strategic observation schema 2

Each observation contains:

- round, current player, sequence step, phase, decision domain, and episode seed
- public territory topology sorted by territory name
- visible owner, supply, supply-source, air-control, and grouped unit state
- current and future fixed reinforcement state
- visible pending battles
- an optional nested `BattleObservation`
- terminal state

Territory names, water status, and adjacency are public. Hidden territories retain only public
topology. Hidden owner, supply, air-control, and units are omitted.

## Python policy encoding

`StrategicObservationEncoder` produces three fixed-size arrays:

- `state`: global and territory features
- `action_features`: one row for each current legal action, padded to `maxActions`
- `action_mask`: legal rows in the padded action table

Action IDs are valid only for one decision. A bare action index therefore has no stable strategic
meaning. `action_features` supplies the current semantics for every index, including:

- action type
- origin and destination territory slots
- unit type and quantity
- movement remaining
- hidden-destination uncertainty
- visible destination ownership and supply state
- supply-source reserve exposure
- air-control relationship
- battle decision and casualty counts

Fogged destination details remain unknown sentinels. The encoder does not reconstruct hidden state
from action legality.

The current trainer uses `MaskablePPO` with `MultiInputPolicy`. A future action-scoring policy can
share one network over action rows rather than flattening the padded table, but semantic action
features remove the previous slot-only input bottleneck.

## Training

Install optional dependencies:

```bash
pip install -e "python/battle-gym[train]"
```

Run one learner side against the deterministic scripted opponent:

```bash
triplea-strategic-train \
  --server-command "./gradlew :game-headless:runBattleSimulationServer" \
  --scenario maps/small_front_meuse/games/small_front_meuse.xml \
  --learner-player Germans \
  --n-envs 4 \
  --timesteps 200000 \
  --output runs/strategic/germans.zip
```

Each worker receives a disjoint seed sequence. With four workers and base seed 1, the episode seeds
are:

```text
worker 0: 1, 5, 9, ...
worker 1: 2, 6, 10, ...
worker 2: 3, 7, 11, ...
worker 3: 4, 8, 12, ...
```

This prevents every reset from replaying one fixed dice line. Explicit `env.reset(seed=...)` remains
available for deterministic evaluation and regression tests.

Checkpoint frequency is specified in environment timesteps. The trainer divides callback frequency
by `n_envs`, matching Stable-Baselines3 vector-step accounting.

## Decision domains

- `STRATEGIC`: reinforcement, movement, air assignment, or phase transition
- `BATTLE`: casualty, retreat, or submerge decision
- `COMPLETE`: no further action is legal

Nested battle decisions reuse the isolated battle environment's validated action expansion.

## Fog-of-war-safe legal actions

Visible destinations are fully validated before exposure. A hidden adjacent territory may be
offered as `uncertain=true` when public geometry, movement allowance, and origin supply permit an
attempt.

On execution:

- success applies the move normally
- hidden-state failure returns `blockedByHiddenState=true`
- the private validation reason is withheld

The action mask therefore does not become an oracle for hidden ownership or stacks.

## Determinism

- territories and battles are name/ID ordered
- grouped units and selected unit IDs are stable
- action parameters are stored canonically
- action-mask overflow fails explicitly
- nested battles derive deterministic seeds from the episode seed and battle order
- training workers use reproducible, non-overlapping episode seed sequences

## Scope

The environment does not change normal UI gameplay. It remains a service-loaded headless feature.

The current baseline covers two-sided Small Front scenarios, fixed scripted opposition, and
Maskable PPO. Opponent snapshot pools, league/Elo evaluation, recurrent policies, graph neural
networks, and a shared per-action scoring head remain later training-system work.
