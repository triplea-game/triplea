# Small Front variant development roadmap

This roadmap keeps engine infrastructure, variant rules, map content, and reinforcement-learning integration separate. Each numbered milestone should normally be delivered as an independently reviewable pull request after the foundation PR is merged into `variant-dev`.

## 0. Simulation foundation

Status: complete in PR #1.

- UI-independent battle observation DTOs
- deterministic unit grouping
- generic action envelope
- reset / legal-actions / step environment contract
- NDJSON headless protocol server
- protocol tests and documentation
- deterministic environment session lifecycle

Exit condition: the protocol boundary is stable and fully testable without loading a real map.

## 1. TripleA battle environment provider

Status: complete in PR #2.

- load a saved-game or scenario fixture from a path
- select one pending battle by battle ID or territory
- bind the selected `BattleState` to the environment session
- retain a configured episode seed in the observation
- return a first real observation through `reset`
- register the provider through `ServiceLoader`

Exit condition: a Python process can reset a real TripleA battle and receive its observation.

## 2. Battle decision hooks and legal-action masks

Status: next.

- casualty-selection action
- retreat and retreat-destination action
- submerge action
- target-selection action where the engine exposes targeting
- explicit decision-point type in observations
- stable legal-action ordering and validation

Exit condition: a headless agent can complete one existing TripleA battle without UI callbacks.

## 3. Deterministic replay, reward, and batch execution

- transition log with seed, observation, action, reward, and result
- replay command
- configurable reward function
- deterministic regression fixtures
- multiple worker processes and batch episode command
- throughput and memory benchmarks

Exit condition: the same seed and actions reproduce the same battle result, and thousands of battles can be run automatically.

## 4. Python Gymnasium client

- typed Python protocol client
- Gymnasium environment wrapper
- legal-action mask support
- vectorized worker launcher
- baseline random and scripted policies
- training and evaluation scripts

Exit condition: an unmodified baseline agent can train and evaluate against existing TripleA battle rules.

## 5. Terrain-specific battle rounds

- `maxGroundBattleRounds` on territory effects
- optional `maxAirBattleRounds`
- `BattleRoundResolver`
- fallback to existing global land/sea round settings
- stalemate regression tests

Exit condition: mountain, city, and open terrain can end combat after different numbers of rounds.

## 6. Territory stack capacity

- `stackCapacity` on territory effects
- `stackCost` on unit attachments
- common `StackCapacityResolver`
- validation for combat movement, redeployment, placement, reinforcement, and retreat
- defined handling of pre-existing over-capacity stacks

Exit condition: every unit-entry path enforces the same terrain capacity rule.

## 7. Combat and redeployment movement

- separate combat and after-combat movement values
- `MovementAllowanceResolver`
- validator, pathfinder, UI, AI, transport, and aircraft integration
- non-air units used in combat cannot redeploy afterward

Exit condition: reserve movement is faster than combat movement without rule discrepancies between engine components.

## 8. Fixed reinforcement scenario system

- remove economic purchase flow from the scenario
- reinforcement schedules by player and turn
- capacity-aware reinforcement placement
- reinforcement queue observation for strategic agents
- scenario victory conditions

Exit condition: the map plays as a narrow-front reinforcement game without IC income.

## 9. Separate air and ground combat

- distinct air-battle and ground-battle scheduling
- dependency ordering: air battle before ground battle
- aircraft cannot capture ground territory
- aircraft withdrawal and destruction rules
- separate air and ground round limits

Exit condition: air units resolve combat independently and never change ground ownership directly.

## 10. Air control

- `AirControlTracker` separate from `Territory.owner`
- serializable and network-safe air-control changes
- persistent or turn-limited control option
- ground combat modifier for allied air control
- observation and history support

Exit condition: each territory can have independent ground ownership and air control.

## 11. Supply network

- supply-source territory properties
- road/supply connection graph
- deterministic reachability calculation
- immediate movement prohibition when cut off
- owner-turn out-of-supply counter
- removal after two owner turns
- observation and AI hooks

Exit condition: cutting a road isolates units consistently across save/load and network play.

## 12. Fog of war

- player-specific visibility service
- one-territory default vision radius
- filtered observations for RL agents
- single-player map masking
- audit history, odds calculators, and logs for information leaks
- later server-authoritative multiplayer filtering

Exit condition: an agent or player receives only information permitted by the visibility rules.

## 13. Strategic-agent environment

- local-front observation graph
- legal move candidate generator
- reinforcement allocation actions
- air-assignment actions
- supply-aware and visibility-aware observation
- hierarchical policy split between strategic and battle decisions

Exit condition: an agent can play complete turns rather than isolated battles.

## 14. Scenario content and balance

- 10–20 territory prototype map
- NATO unit assets
- infantry, artillery, armor, reconnaissance, fortress, and aircraft definitions
- mountain, city, and open terrain presets
- automated matchup and stalemate statistics
- self-play balance evaluation

Exit condition: the variant is playable end to end and its core parameters have automated balance reports.

## 15. Packaging and maintenance

- release build and installation instructions
- save-game compatibility policy
- upstream synchronization procedure
- isolate generally useful engine changes into upstreamable PRs
- version the map rules, observation schema, and protocol

Exit condition: players and contributors can install, reproduce, and maintain the variant without depending on a development workspace.
