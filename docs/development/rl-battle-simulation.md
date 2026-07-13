# Battle simulation protocol

The simulation API exposes TripleA battles without depending on Swing UI objects.

The full implementation order is tracked in [`small-front-roadmap.md`](small-front-roadmap.md).

## Run

```bash
./gradlew :game-headless:runBattleSimulationServer
```

The process reads one JSON object per stdin line and writes one response object per stdout line.

```json
{"command":"ping","data":{}}
```

`SavedGameBattleEnvironment` is registered through Java `ServiceLoader`. It loads a TripleA save game, selects one pending battle that implements `BattleState`, and advances it until the battle ends or a supported player decision is required.

## Reset a saved battle

```json
{
  "command": "reset",
  "data": {
    "scenarioPath": "/absolute/path/to/game.tsvg",
    "seed": 12345,
    "territory": "Eastern Ukraine"
  }
}
```

`battleId` and `territory` are optional selectors. When neither is supplied, the provider chooses the first observable pending battle using a stable ordering by territory, battle type, and battle ID. When both are supplied, both must match.

The response uses observation schema version 3. It includes the requested seed and a `decision` object. `decision.type` is one of `NONE`, `SELECT_CASUALTIES`, `RETREAT`, or `SUBMERGE`.

## Casualty selection

For a casualty decision, `legalActions` returns one `select_casualties` mask descriptor containing:

- `candidateUnitIds`
- `requiredHits`
- `allowMultipleHitsPerUnit`
- the engine's default killed and damaged unit IDs

Submit the selected unit IDs through `step`:

```json
{
  "command": "step",
  "data": {
    "type": "select_casualties",
    "parameters": {
      "killedUnitIds": "0f3b4e0b-6de4-4cdf-a03e-973e954a5a2e",
      "damagedUnitIds": ""
    }
  }
}
```

`damagedUnitIds` may contain the same UUID more than once when multiple non-lethal hits are permitted. The submitted killed and damaged entries must assign exactly `requiredHits` hits.

## Retreat and submerge

A retreat decision returns a `continue` action and one `retreat` action for each legal destination. A submerge decision uses `submerge` instead of `retreat`.

```json
{
  "command": "step",
  "data": {
    "type": "retreat",
    "parameters": {
      "territory": "Rear Area"
    }
  }
}
```

The engine resumes from the interrupted execution-stack step after each accepted action and advances automatically to the next decision or terminal battle result.

## Environment lifecycle

`StatefulBattleEnvironment` supplies the common episode lifecycle used by concrete providers:

1. `reset` loads a fresh `BattleScenario` and advances it to the first decision.
2. `legalActions` returns a deterministic, sorted action mask.
3. `step` validates the submitted action before changing engine state.
4. each transition records an episode ID, step ID, action type, resolved decision, and next decision.
5. terminated or truncated episodes expose no further legal actions.

Calling stateful methods before `reset`, or calling `step` after the episode finishes, is an error.

Standard `MustFightBattle` casualty, general retreat, and submerge callbacks are supported. Specialized target-selection callbacks remain outside the current provider scope.

`BattleObservationFactory` produces stable unit groups sorted by owner, type, hits, and movement used. Decision candidates additionally expose UUIDs because exact engine units must be selected. The DTO excludes UI objects and direct engine references so it can be serialized, replayed, and converted into an RL tensor later.
