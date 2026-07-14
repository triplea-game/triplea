# Separate air and ground combat

Milestone 9 makes combat domains explicit and separates aircraft from normal land and sea combat.

## Activation

Separated combat is opt-in through the game property:

```xml
<property name="Separate Air And Ground Combat" value="true" editable="false"/>
```

Maps that want contested air battles should also enable TripleA's existing air-battle-before-normal-battle option. Keeping the new property disabled preserves the existing TripleA combat roster behavior.

## Domain model

Each `IBattle.BattleType` has one domain:

- `GROUND`: normal land or sea combat
- `AIR`: fighter and interceptor combat that determines the local air result
- `RAID`: strategic bombing and its escort/interception phase

`AirGroundBattlePolicy` is the common source for activation, unit partitioning, and resolution priority.

## Required resolution order

For battles in the same territory:

1. raid-related combat
2. air-domain combat
3. ground-domain combat

The tracker represents this through battle dependencies rather than collection iteration order.

## Unit participation

When separated combat is enabled:

- aircraft are assigned to the air domain
- non-air units are assigned to the ground domain
- immediately before a normal battle, aircraft on both sides are marked as handled by the air domain
- the existing non-combatant removal step then removes those aircraft from the normal battle roster
- the legacy behavior is unchanged when the property is disabled

The implementation reuses `Unit.PropertyName.WAS_IN_AIR_BATTLE`, so existing battle filtering, UI notifications, save serialization, and dependent-battle handling remain authoritative. Surviving aircraft remain in the territory after the air-domain result but cannot fire again in the ground battle. Existing air-battle retreat and withdrawal handling remains responsible for aircraft that leave the territory.

## Ownership boundary

The ground-domain roster change does not introduce air control. Territory ownership remains a ground concept:

- an air-only attacking force has no eligible ground combatant and cannot capture territory
- defending aircraft are removed from the ground roster, so they do not block an eligible ground force from resolving ownership
- the existing normal-battle victory path changes ownership only when a surviving non-air attacker remains

Air control is intentionally deferred to milestone 10 and will be exposed without overloading `Territory.owner`.

## Round limits

The existing `BattleRoundResolver` remains authoritative:

- air-domain battles use `maxAirBattleRounds`
- ground-domain battles use `maxGroundBattleRounds`
- global properties remain compatibility fallbacks

## Completed delivery

Milestone 9 now includes:

1. explicit combat domains and deterministic raid → air → ground ordering
2. opt-in compatibility policy for existing maps
3. aircraft removal from both normal-battle rosters through the existing `WAS_IN_AIR_BATTLE` path
4. survivor, withdrawal, and ownership behavior delegated to established TripleA engine paths
5. policy, battle-step, full game-core, game-headless, and formatting regression checks
