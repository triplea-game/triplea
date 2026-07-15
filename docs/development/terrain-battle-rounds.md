# Terrain-specific battle round limits

Terrain effects may override the number of rounds available to land and air battles in a territory.

## Map XML

```xml
<attachment name="territoryEffectAttachment" attachTo="mountain">
  <option name="maxGroundBattleRounds" value="2"/>
  <option name="maxAirBattleRounds" value="1"/>
</attachment>
```

Both options accept a positive integer or `-1`.

- a positive integer ends an unresolved battle as a stalemate after that round
- `-1` means unlimited rounds
- omitting the option preserves the existing global game property

## Resolution rules

`maxGroundBattleRounds` applies only to normal battles in land territories. Sea battles continue to use the global sea-battle round property.

`maxAirBattleRounds` applies to `AirBattle`, including air battles above land or water territories. It is independent from the ground limit.

A territory may have multiple effects. The resolver uses the shortest configured positive limit. When every configured value is `-1`, the result is unlimited. This makes combined terrain effects deterministic and prevents a permissive effect from silently extending a more restrictive terrain limit.

Examples:

| Effects | Resolved limit |
|---|---:|
| none | global fallback |
| `4` | 4 |
| `4`, `2` | 2 |
| `-1`, `3` | 3 |
| `-1`, `-1` | unlimited |

## Battle result

Normal land battles use the existing `BattleStatus.isLastRound()` and `CheckGeneralBattleEnd` flow. If both sides still have combatants at the configured final round, the engine records a draw with the existing `STALEMATE` battle result. Surviving units remain in the contested territory and ownership is not transferred.

Air battles use the same resolved limit through their existing maximum-round termination path. Ground and air limits can therefore be tuned independently for mountain, city, fortress, and open-terrain presets.

## Compatibility

Maps that do not define either option behave exactly as before. Existing save games retain the resolved `maxRounds` field already serialized by the corresponding battle class. The new attachment fields are nullable so older map definitions and saves have no implicit override.
