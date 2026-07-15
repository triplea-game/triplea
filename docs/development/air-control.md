# Air control

Milestone 10 adds territory air control as game state that is independent from ground ownership.

## Activation

```xml
<property name="Separate Air And Ground Combat" value="true" editable="false"/>
<property name="Air Control Enabled" value="true" editable="false"/>
<property name="Air Control Persistent" value="false" editable="false"/>
<property name="Air Control Ground Attack Bonus" value="1" editable="false"/>
```

`Separate Air And Ground Combat` must also be enabled because air control is resolved from the aircraft that survive the separate air domain.

The defaults preserve existing maps:

- air control is disabled
- non-persistent control lasts for the current game round
- the ground attack bonus is `1`

When `Air Control Persistent` is `true`, control remains until another side gains control or the airspace becomes contested.

## Resolution

Air control is resolved immediately before the dependent normal battle begins, after the preceding air battle has removed casualties and processed withdrawals.

- only surviving, non-withdrawn aircraft are eligible
- if only attacking aircraft remain, the attacker gains control
- if only defending aircraft remain, the defender gains control
- if aircraft from both sides remain, the territory records explicit `CONTESTED` airspace
- if no eligible aircraft remain, existing persistent control is unchanged
- raid battles do not establish territorial air control

The control state is stored by `AirControlTracker`, not by `Territory.owner`. An aircraft can therefore control the airspace without capturing or changing ground ownership. `UNCONTROLLED`, `CONTESTED`, and `CONTROLLED` are distinct serialized states; contested airspace is no longer represented by the same empty value as an untouched territory.

## Ground combat modifier

The controller grants the configured attack-strength bonus to allied land units attacking in that territory.

- the bonus changes attack strength, not attack rolls
- only land units receive the bonus
- aircraft and sea units never receive it
- defense strength is unchanged
- casualty valuation uses the same modified combat value as the actual dice roll
- contested, inactive, or expired control produces no bonus

## Player-visible status

Visible territories receive compact Swing map badges:

- `A` in the controller's player color means controlled airspace
- `AX` means contested airspace
- no badge means uncontrolled or expired airspace

The operational status text also distinguishes persistent control from control that lasts only for the current round and reports the effective friendly ground-attack bonus. Fog of war suppresses the badge and controller details for hidden territories.

## Persistence and synchronization

Air-control mutations use serializable `Change` objects. The tracker is stored as serializable game state and is therefore included in save games and reproduced on network clients through the existing change stream.

`AirControlChange` is invertible, so undo and replay paths can restore controlled, contested, or uncontrolled state.

## Observation and history

Battle observation schema version 4 includes:

- `airControlPlayer`: controlling player name, or an empty string when not controlled
- `offenseGroundAttackBonus`: the effective land attack-strength bonus for the current offensive player

Strategic observation schema version 2 adds, for visible territories:

- `airControlStatus`: `UNCONTROLLED`, `CONTESTED`, or `CONTROLLED`
- `airControlPlayer`: controller name only when the status is `CONTROLLED`
- `airControlPersistent`: whether the active status survives later rounds

The Python Gymnasium battle client continues to parse and encode the battle controller and bonus values. Human-readable history records when a player gains control or when control becomes contested.
