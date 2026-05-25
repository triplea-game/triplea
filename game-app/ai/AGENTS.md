# ai

AI player implementations for TripleA. Provides computer opponents that can play the game autonomously.

## AI Implementations

Note: The default strategic AIs (ProAi, FastAi, WeakAi) live in `game-core`'s `games.strategy.triplea.ai` package, not in this module. This module provides additional AI options available only in the headed client.

### FlowFieldAi (`org.triplea.ai.flowfield.FlowFieldAi`)
An alternative strategic AI using an influence diffusion algorithm:
- Assigns strategic values to key territories (capitals, factories, resource producers)
- Diffuses values outward via breadth-first search with configurable decay rates
- Units navigate toward highest-value territories
- Computes separate influence maps for land, sea, and air units
- Uses Lanchester's Laws for combat evaluation

### DoesNothingAi (`org.triplea.ai.does.nothing.DoesNothingAi`)
A minimal/passive AI that takes no strategic actions. Purchases and places units via `WeakAi` helper, does not move, accepts all political proposals, and destroys remaining resources at turn end.

## Package Structure

- **`org.triplea.ai.flowfield`** — Flow field AI entry point
  - **`influence/`** — Territory value influence maps
    - **`offense/`** — Offensive strategy calculations
    - **`defense/`** — Defensive strategy calculations
  - **`odds/`** — Battle outcome calculation engine
  - **`neighbors/`** — Territory connectivity and neighbor mapping
- **`org.triplea.ai.does.nothing`** — Passive AI implementation

## Key Concepts

- **InfluenceMap**: Maps strategic value across territories using value diffusion. Values decrease exponentially with distance from key territories.
- **InfluenceTerritory**: Tracks accumulated influence value, battle details, and distance from source for each territory.
- **Battle odds**: Evaluates combat outcomes to decide whether to attack or avoid territories.

## Dependencies

- `game-app:game-core` (extends `AbstractAi`)
- `lib:java-extras`
- Test fixtures from `game-app:game-core`
