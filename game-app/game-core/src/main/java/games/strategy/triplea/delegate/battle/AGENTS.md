# Battle System

The battle subsystem implements all combat resolution in TripleA. It uses a step-based execution model where each battle is broken into ordered phases (steps) executed on a stack.

## Architecture Overview

- **IBattle** — Interface for all battle types. Key methods: `fight()`, `isOver()`, `getWhoWon()`, `addAttackChange()`.
- **AbstractBattle** — Base class holding common state: `battleSite`, `attacker`/`defender`, `round`, `isOver`, `whoWon`, `attackingUnits`/`defendingUnits`, lost TUV tracking.
- **DependentBattle** — Extends AbstractBattle; tracks `attackingFromMap` (unit origins) and amphibious attack sources. Base for MustFightBattle and NonFightingBattle.
- **BattleTracker** — Central registry of all pending battles, their dependencies (one battle blocks another), conquered/blitzed territories, and battle records. The dependency graph ensures battles execute in correct order (e.g., naval battle before amphibious landing).
- **BattleDelegate** — The game phase delegate that orchestrates the battle phase. Manages scramble, rockets, kamikaze, bombardment sources, and iterates through pending battles.

## Battle Types

| Class | Purpose |
|---|---|
| **MustFightBattle** | Standard combat — the main implementation. Implements `BattleState` and `BattleActions` interfaces to expose state/actions to steps. Uses an `ExecutionStack` to run battle steps. |
| **NonFightingBattle** | No combat occurs — used for moves into empty territory that depend on another battle resolving first (e.g., naval invasion into undefended territory waiting on the sea battle). If attackers remain after dependencies resolve, attacker wins. |
| **FinishedBattle** | Pre-scripted outcome set at construction. Used for blitzed/conquered territories. |
| **AirBattle** | Air-to-air interception combat before a main battle or bombing raid. |
| **StrategicBombingRaidBattle** | Strategic bombing raids with specialized AA defense and damage tracking per unit. |

## Battle Step State Machine (`steps/`)

`BattleStep` is the interface for atomic battle operations. Each step has an `Order` enum value that determines execution sequence. `BattleSteps` is the factory that builds and sorts all steps.

### Execution Order

Steps execute in this order (each step may be skipped if not applicable):

1. AA Offensive/Defensive Fire → AA Casualty Removal
2. Naval Bombardment → Bombardment Casualty Removal
3. Remove Non-Combatants, Land Paratroopers, Mark No Movement Left
4. Sub Offensive/Defensive Retreat (before battle)
5. Remove Unprotected Units (transports without escorts)
6. Submerge Subs vs Only Air
7. First Strike Offensive/Defensive → First Strike Casualty Removal
8. Air vs Non-Subs (offensive/defensive)
9. General Offensive/Defensive → General Casualty Removal
10. Suicide Unit Removal
11. Remove Unprotected Units (general)
12. Battle End Check
13. Offensive General Retreat
14. Stalemate Check
15. Sub Defensive Retreat (after battle)

After step 15, if the battle is not over, the round increments and steps 4–15 repeat.

### Step Subdirectories

**`steps/fire/`** (~25 files) — Firing phases:
- `FiringGroup` — A group of units that fires together at specific targets. Tracks `suicideOnHit`.
- `FireRoundStepsFactory` — Builds 3-step sequences per firing group: `RollDiceStep` → `SelectCasualties` → `MarkCasualties`.
- `OffensiveGeneral` / `DefensiveGeneral` — General-phase fire for attackers/defenders.
- `OffensiveAaFire` / `DefensiveAaFire` — Anti-aircraft fire steps.
- `OffensiveFirstStrike` / `DefensiveFirstStrike` — First-strike unit fire.
- `NavalBombardment` — Shore bombardment fire.
- `AirAttackVsNonSubsStep` / `AirDefendVsNonSubsStep` — Air units firing at non-submarine targets.
- `MainDiceRoller` — Dice rolling implementation.
- `SelectMainBattleCasualties` — Main battle casualty selection logic.

**`steps/change/`** (~12 files) — State changes and casualty removal:
- `CheckGeneralBattleEnd` / `CheckStalemateBattleEnd` — Win/loss/stalemate condition evaluation.
- `ClearAaCasualties` / `ClearBombardmentCasualties` / `ClearGeneralCasualties` / `ClearFirstStrikeCasualties` — Remove casualties from the battle.
- `RemoveNonCombatants` — Remove non-combat units from battle.
- `RemoveUnprotectedUnits` / `RemoveUnprotectedUnitsGeneral` — Remove unescorted transports.
- `LandParatroopers`, `MarkNoMovementLeft` — Special state changes.
- `suicide/RemoveFirstStrikeSuicide`, `RemoveGeneralSuicide` — Remove suicide attack units.

**`steps/retreat/`** (~9 files) — Retreat logic:
- `OffensiveSubsRetreat` / `DefensiveSubsRetreat` — Submarine retreat/submerge.
- `OffensiveGeneralRetreat` — General offensive retreat option.
- `RetreaterGeneral`, `RetreaterAirAmphibious`, `RetreaterPartialAmphibious` — Retreat strategy implementations.
- `EvaderRetreat` — Evader retreat logic.
- `SubmergeSubsVsOnlyAirStep` — Submarines auto-submerge when facing only air.
- `RetreatChecks` — Validates whether retreat is possible.

## State Interfaces

**BattleState** — Read/write access to battle state, used by steps:
- `filterUnits(UnitBattleFilter, Side...)` — Get units by status (ALIVE, CASUALTY, REMOVED_CASUALTY) and side (OFFENSE, DEFENSE).
- `markCasualties()`, `retreatUnits()`, `removeNonCombatants()` — Mutate unit state.
- `BattleStatus` inner class — Tracks round number, max rounds, isOver, isAmphibious, isHeadless.

**BattleActions** — Side-effect operations requiring `IDelegateBridge`:
- `clearWaitingToDieAndDamagedChangesInto()` — Process pending casualties.
- `endBattle()`, `removeUnits()` — End battle or remove units.
- `queryRetreatTerritory()`, `querySubmergeTerritory()` — Player interaction for retreat/submerge decisions.

## Casualty System (`casualty/`)

- **CasualtySelector** — Main entry point. `selectCasualties()` coordinates with the player (via remote) to choose which units die. Handles transport-dependent unit logic.
- **CasualtyOrderOfLosses** — Intelligent default casualty ordering that interleaves units to preserve support effects (e.g., alternates Artillery/Infantry to maximize support value). Uses caching for performance.
- **AaCasualtySelector** — Specialized casualty selection for AA fire.
- **CasualtyUtil** — Utility: `getTotalHitpointsLeft()`, `getDependents()` (maps units to transported units).
- **CasualtySortingUtil** — Sorting/categorizing casualties.
- **LowLuckTargetGroups** — Handles the low-luck dice variant's casualty targeting.

## Other Key Files

- **BattleResults** — Immutable snapshot of battle outcome: remaining units, who won, rounds fought.
- **ScrambleLogic** — Logic for scrambling interceptors to defend against attacks.
- **UnitBattleComparator** — Orders units for casualty selection priority.
- **BattleStepStrings** — UI display name constants for battle steps.
- **Fire.java / FireAa.java** — Deprecated legacy fire handling; replaced by the `steps/fire/` system.

## Compatibility Warnings

- MustFightBattle is serialized in save games — do not rename/remove its private fields.
- Battle step `Order` enum values determine execution sequence — reordering changes game behavior.
