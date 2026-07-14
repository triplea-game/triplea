# Terrain stack capacity

Small Front terrain can limit the amount of one allied force that may occupy a territory.

```xml
<attachment name="territoryEffectAttachment" attachTo="mountain">
  <option name="stackCapacity" value="6"/>
</attachment>

<attachment name="unitAttachment" attachTo="armor">
  <option name="stackCost" value="2"/>
</attachment>
```

`stackCapacity` accepts a non-negative integer or `-1` for unlimited capacity. An omitted value keeps legacy unlimited behavior. When several territory effects overlap, the shortest finite capacity wins. `stackCost` defaults to `1`, accepts zero for units that do not consume front capacity, and rejects negative values.

Capacity is counted per allied force. Enemy defenders do not consume an attacker's entry capacity. Existing over-capacity territories are not modified or purged; they may accept zero-cost units, but no additional positive-cost units until their occupied cost is low enough.

The common resolver is applied before legacy movement, attacking, and placement stacking limits. The same API accepts a pending-unit collection, so scheduled reinforcement batches are evaluated consistently. General retreat destinations are removed when the full non-air retreating force cannot fit.
