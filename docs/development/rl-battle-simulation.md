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

`SavedGameBattleEnvironment` is registered through Java `ServiceLoader`. It loads a TripleA save game, selects one pending battle that implements `BattleState`, and returns a serializable observation.

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

The response uses observation schema version 2 and includes the requested seed. The seed is retained with the loaded scenario so later decision hooks can use the same episode seed.

## Environment lifecycle

`StatefulBattleEnvironment` supplies the common episode lifecycle used by concrete providers:

1. `reset` loads a fresh `BattleScenario` and returns its first observation.
2. `legalActions` returns a deterministic, sorted action mask.
3. `step` rejects actions outside that mask before changing engine state.
4. each transition records an episode ID, step ID, and action type.
5. terminated or truncated episodes expose no further legal actions.

Calling stateful methods before `reset`, or calling `step` after the episode finishes, is an error.

The saved-game provider is intentionally read-only in this milestone: `reset` and observation work, while `legalActions` is empty and `step` reports that battle decision hooks are not installed yet. Casualty and retreat actions are the next milestone.

`BattleObservationFactory` produces stable unit groups sorted by owner, type, hits, and movement used. The DTO intentionally excludes UI objects and direct engine references so it can be serialized, replayed, and converted into an RL tensor later.
