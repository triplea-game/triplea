# Strategic-agent simulation

Small Front exposes a deterministic turn-level environment alongside the existing isolated-battle environment. It restores a TripleA save game, selects one player, runs automatic reinforcement and supply steps, and then exposes decisions through combat movement, air assignment, battles, and redeployment.

## Environment lifecycle

The Java contract is `StrategicEnvironment`:

```text
reset(request) -> observation
legalActions() -> ordered action mask
step(action) -> observation, reward, terminated, truncated, info
```

`StrategicResetRequest` contains:

- `scenarioPath`: path to a `.tsvg` save game
- `seed`: deterministic episode seed
- `player`: exact player name
- `maxActions`: maximum legal-action count, default 512

Action masks are never silently truncated. If the current state produces more actions than `maxActions`, reset or action generation fails with `StrategicActionSpaceOverflow` so the caller can increase the bound without losing legal choices.

The environment keeps an episode ID and step ID in transition metadata. Strategic reward is currently neutral (`0`). Nested battles retain the existing battle environment's decision and reward semantics; turn-level reward shaping remains a later training-policy concern.

## NDJSON commands

The existing `BattleSimulationServer` service also accepts:

- `strategicReset`
- `strategicLegalActions`
- `strategicStep`

Example reset:

```json
{"command":"strategicReset","data":{"scenarioPath":"/maps/small-front/turn.tsvg","seed":7,"player":"Blue","maxActions":512}}
```

Example legal-action request:

```json
{"command":"strategicLegalActions","data":{}}
```

Example action:

```json
{"command":"strategicStep","data":{"type":"end_phase","parameters":{"phase":"COMBAT_MOVE"}}}
```

`schema` reports both battle and strategic observation schema versions and whether each service-loaded environment is available.

## Strategic observation schema 1

Each observation contains:

- round, selected player, current sequence step, and deterministic seed
- strategic phase and decision domain
- a territory graph sorted by territory name
- visible owner, supply, supply-source, air-control, and grouped unit state
- current and future fixed reinforcement state for the selected player
- visible pending battles
- an optional nested `BattleObservation`
- terminal state

Territory names, water status, and adjacency are public topology. Hidden territories retain only that public topology. Their owner, supply state, supply-source status, air-control player, and units are removed.

Visible unit stacks are grouped by owner, unit type, domain flags, and minimum movement remaining. This keeps observations deterministic while preserving the information needed for strategic move selection.

## Decision domains

`StrategicDecisionDomain` allows hierarchical policies to route decisions:

- `STRATEGIC`: reinforcement, movement, air assignment, or phase transition
- `BATTLE`: casualty, retreat, or submerge decision from the existing battle controller
- `COMPLETE`: no further action is legal

A nested battle action uses type `battle_decision` and carries the battle ID, territory, and underlying battle action type. The legal mask supplies the battle decision template. The submitted action may add the executable casualty or retreat selection, which is validated by the existing battle controller.

## Turn phases

### Reinforcement allocation

The environment first runs the configured `FixedReinforcementDelegate`, then records units delivered during that step. Delivered groups may be allocated to visible, friendly, supplied land territories that satisfy stack capacity. Keeping a group at its original destination is also an explicit allocation.

Blocked reinforcement orders remain in the existing serializable reinforcement queue. Ending the allocation phase leaves any unallocated delivered units in their current territory.

### Combat movement

Non-air movement candidates are generated in deterministic territory, unit-type, movement, and unit-ID order. Visible routes are checked through the normal `MoveValidator` and executed by the active `MoveDelegate`.

Supply restrictions use `SupplyNetworkResolver.canMove`, so an isolated land unit disappears from the legal movement set immediately.

### Air assignment

Air units use the same combat-move delegate after ground combat movement. This gives a separate policy head an explicit air-assignment phase without creating a second movement implementation.

### Battle

Ending air assignment closes the combat-move delegate and selects pending battles in stable territory, battle-type, and battle-ID order. Each battle is handed to `LoadedBattleScenario`, so casualty, retreat, submerge, deterministic dice, and existing battle observations are shared with isolated battle training.

When all pending battles finish, the environment enters redeployment.

### Redeployment

The configured non-combat move delegate executes normal redeployment rules. Ending this phase terminates the strategic episode.

## Fog-of-war-safe legal actions

Visible destinations are fully validated before being exposed. A hidden adjacent territory is different: validating against its hidden ownership or units would leak information through the action mask.

The environment therefore exposes a movement attempt with `uncertain=true` when only public geometry, movement allowance, and the origin's supply state permit the attempt. On execution:

- success applies the move normally
- failure returns `blockedByHiddenState=true`
- the internal validation reason is not returned

This prevents the action mask or error text from becoming an oracle for hidden enemy state.

## Determinism

- territories and battles are name/ID ordered
- unit groups and selected unit IDs are stable
- action parameters are stored in sorted maps
- legal actions use canonical type-and-parameter ordering
- action-mask overflow fails explicitly
- nested battles derive deterministic seeds from the turn seed and battle order

## Compatibility and scope

The strategic environment does not change normal UI gameplay. It is activated only through the service-loaded headless environment.

The milestone controls one selected player's reinforcement, supply check, combat movement, air assignment, pending battles, and redeployment block. Purchase/economy policy, full multi-round game orchestration, scenario-specific victory reward shaping, and server-authoritative filtering for untrusted multiplayer clients remain outside this boundary.
