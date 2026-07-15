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
- if aircraft from both sides remain, control is cleared as contested
- if no eligible aircraft remain, existing persistent control is unchanged
- raid battles do not establish territorial air control

The control state is stored by `AirControlTracker`, not by `Territory.owner`. An aircraft can therefore control the airspace without capturing or changing ground ownership.

## Ground combat modifier

The controller grants the configured attack-strength bonus to allied land units attacking in that territory.

- the bonus changes attack strength, not attack rolls
- only land units receive the bonus
- aircraft and sea units never receive it
- defense strength is unchanged
- casualty valuation uses the same modified combat value as the actual dice roll
- inactive or expired control produces no bonus

## Persistence and synchronization

Air-control mutations use serializable `Change` objects. The tracker is stored as serializable game state and is therefore included in save games and reproduced on network clients through the existing change stream.

`AirControlChange` is invertible, so undo and replay paths can restore the previous controller.

## Observation and history

Battle observation schema version 4 adds:

- `airControlPlayer`: controlling player name, or an empty string when uncontrolled or contested
- `offenseGroundAttackBonus`: the effective land attack-strength bonus for the current offensive player

The Python Gymnasium client parses and encodes both values. Human-readable history records when a player gains control or when control becomes contested.
