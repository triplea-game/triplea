# Battle simulation protocol

The foundation milestone adds an API boundary without changing battle rules.

The full implementation order is tracked in [`small-front-roadmap.md`](small-front-roadmap.md).

## Run

```bash
./gradlew :game-headless:runBattleSimulationServer
```

The process reads one JSON object per stdin line and writes one response object per stdout line.

```json
{"command":"ping","data":{}}
```

A real simulation provider implements `BattleEnvironment` and registers it through Java `ServiceLoader`. Until a provider is installed, `ping` and `schema` work while state-changing commands return a structured error.

## Environment lifecycle

`StatefulBattleEnvironment` supplies the common episode lifecycle used by concrete providers:

1. `reset` loads a fresh `BattleScenario` and returns its first observation.
2. `legalActions` returns a deterministic, sorted action mask.
3. `step` rejects actions outside that mask before changing engine state.
4. each transition records an episode ID, step ID, and action type.
5. terminated or truncated episodes expose no further legal actions.

Calling stateful methods before `reset`, or calling `step` after the episode finishes, is an error.

`BattleObservationFactory` produces stable unit groups sorted by owner, type, hits, and movement used. The DTO intentionally excludes UI objects and direct engine references so it can be serialized, replayed, and converted into an RL tensor later.
