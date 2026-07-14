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

Status: complete in PR #3.

- casualty-selection action
- retreat and retreat-destination action
- submerge action
- explicit decision-point type in observations
- stable legal-action ordering and scenario-specific validation
- resumable `ExecutionStack` integration through engine player callbacks
- real saved battle completion without UI callbacks

Specialized target-selection callbacks that do not use casualty or retreat player methods are deferred to a later combat-extension milestone.

Exit condition: a headless agent can complete one existing TripleA battle without UI callbacks.

## 3. Deterministic replay, reward, and batch execution

Status: complete in PR #4.

- immutable transition log with seed, observations, legal masks, action, reward, and result
- versioned serializable episode logs
- replay command with first-mismatch reporting
- configurable material and terminal reward function
- deterministic replay and mismatch regression fixtures
- ordered parallel-worker batch replay command
- throughput and JVM-memory metrics in batch results

Process-level vectorization is deferred to the Python client, which can launch multiple NDJSON server processes while each process uses the built-in batch workers.

Exit condition: the same seed and actions reproduce the same battle result, and large replay batches can be run automatically.

## 4. Python Gymnasium client

Status: complete in PR #5.

- typed Python models and NDJSON process client
- fixed-size Gymnasium observation and discrete action spaces
- deterministic legal-action masks
- complete casualty-combination expansion with overflow detection
- process-level vector worker launcher
- baseline random and scripted policies
- evaluation command and optional Maskable PPO training command
- Python 3.11 and 3.12 tests, lint, and type checks

Exit condition: an unmodified baseline agent can train and evaluate against existing TripleA battle rules.

## 5. Terrain-specific battle rounds

Status: complete in PR #6.

- `maxGroundBattleRounds` and `maxAirBattleRounds` territory-effect properties
- positive finite limits and `-1` unlimited validation
- common `BattleRoundResolver`
- fallback to existing global land, sea, and air round settings
- shortest-finite-limit resolution for overlapping terrain effects
- normal land-battle and air-battle integration
- attachment, fallback, overlap, constructor, and stalemate regression coverage
- map XML and compatibility documentation

Exit condition: mountain, city, fortress, and open terrain can end combat after different numbers of rounds.

## 6. Territory stack capacity

Status: complete in PR #8.

- `stackCapacity` on territory effects
- `stackCost` on unit attachments
- common `StackCapacityResolver`
- per-allied-force capacity accounting
- movement, attack, placement, pending reinforcement, and general retreat integration
- stable candidate ordering and shortest-finite-capacity resolution
- pre-existing over-capacity stacks remain intact but reject positive-cost entries
- zero-cost unit support and XML configuration documentation

Exit condition: every current unit-entry path enforces the same terrain capacity rule, and future reinforcement batches can use the same pending-unit API.

## 7. Combat and redeployment movement

Status: complete in PR #9.

- optional `combatMovement` and `redeploymentMovement` unit-attachment properties
- legacy `movement` fallback for both phases
- common `MovementAllowanceResolver` driven by the current game step
- technology and local bonus movement applied after phase allowance selection
- `Unit.getMaxMovementAllowed()` and `Unit.getMovementLeft()` integration shared by validators, pathfinding, UI, AI, transports, and aircraft checks
- air units retain combat movement expenditure and may use remaining redeployment movement
- moved non-air units are exhausted against the redeployment allowance at Combat Move end
- focused fallback, property, validation, and resolver tests plus XML documentation

Exit condition: reserve movement is faster than combat movement without rule discrepancies between engine components.

## 8. Fixed reinforcement scenario system

Status: complete in PR #10.

- player attachment with repeated round, territory, unit-type, and quantity schedule entries
- explicit reinforcement delegate for placement before supply checking and Combat Move
- purchase-free and placement-free scenario sequence support
- shared `StackCapacityResolver` integration with deterministic partial placement
- serializable queue for blocked and non-friendly destinations
- owner-round idempotence across repeated step entry and save/load
- versioned queued and future reinforcement observation for strategic agents
- existing EndRound, rules attachment, victory-city, and trigger victory integration
- XML configuration, compatibility documentation, and focused regression tests

Exit condition: the map plays as a narrow-front reinforcement game without IC income.

## 9. Separate air and ground combat

Status: complete in PR #12.

- explicit ground, air, and raid combat domains
- deterministic dependency ordering: raid, then air, then ground
- opt-in compatibility property for existing maps
- aircraft removed from normal combat through the existing air-battle state path
- surviving aircraft remain on the board without receiving a second firing pass
- aircraft withdrawal and destruction reuse existing battle behavior
- aircraft cannot capture ground territory
- separate air and ground round limits remain authoritative

Exit condition: air units resolve combat independently and never change ground ownership directly.

## 10. Air control

Status: complete in PR #13.

- serializable `AirControlTracker` separate from `Territory.owner`
- invertible and network-safe air-control changes
- persistent or current-round control option
- control resolved from surviving, non-withdrawn aircraft before dependent ground combat
- contested airspace clears control while raid battles remain excluded
- configurable allied land attack-strength bonus
- actual dice and casualty valuation use the same modified combat value
- battle observation schema 4 and Python fixed-size encoding support
- human-readable history, expiration, serialization, and compatibility tests
- XML configuration and rule documentation

Exit condition: each territory can have independent ground ownership and air control.

## 11. Supply network

Status: complete in PR #14.

- opt-in supply-source and road-link territory attachments
- undirected road graph with deterministic territory-name ordering
- allied land-control reachability from friendly supply sources
- enemy and neutral road-node interdiction
- supply-aware movement delegate with immediate server-side rejection
- serializable owner-turn isolation counters with same-round idempotence
- configurable removal threshold with a two-turn default
- normal network-synchronized unit removal and resupply recovery
- versioned topology and unit-counter observation for strategic agents
- public resolver hooks for future AI move generation
- XML sequence, compatibility, save/load, and rule documentation
- attachment, reachability, movement, attrition, delegate-state, and observation tests

Exit condition: cutting a road isolates units consistently across save/load and network play.

## 12. Fog of war

Status: next.

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
