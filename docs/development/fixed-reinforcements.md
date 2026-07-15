# Fixed reinforcement scenario system

Small Front scenarios can omit purchase and placement steps and deliver map-authored units through an explicit reinforcement phase before movement and combat.

## Player schedule

```xml
<attachment
    name="fixedReinforcementAttachment"
    attachTo="Allies"
    javaClass="games.strategy.triplea.attachments.FixedReinforcementAttachment"
    type="player">
  <option name="reinforcement" value="1:Western Front:infantry:3"/>
  <option name="reinforcement" value="2:Western Front:artillery:1"/>
</attachment>
```

The value format is `round:territory:unitType:quantity`. Round and quantity are positive integers. Territory and unit-type names are validated while the map is parsed. Multiple options are processed in declaration order, which is also the priority order when terrain capacity is limited.

## Sequence phase

```xml
<delegate
    name="fixedReinforcement"
    javaClass="games.strategy.triplea.delegate.reinforcement.FixedReinforcementDelegate"
    display="Reinforcements"/>

<step name="AlliesReinforcement" delegate="fixedReinforcement" player="Allies"/>
<step name="AlliesCombatMove" delegate="move" player="Allies"/>
```

Place the reinforcement step before supply checking and Combat Move. The scenario can remove economic `PurchaseDelegate` and `PlaceDelegate` steps entirely.

## Placement and queue policy

At the reinforcement step, the engine combines previously queued orders with all schedule entries that became due since that player's last processed round. Queued orders are attempted first.

A destination must be owned by the player or an ally. `StackCapacityResolver` applies the same `stackCapacity` and `stackCost` rules used by movement, placement, reinforcement batches, and retreat. Partial placement is allowed. Units that do not fit remain in a serializable queue and are retried during the player's next reinforcement step.

The tracker records the last processed player-round, so saving, loading, or re-entering the same step cannot duplicate deliveries. The original scheduled round remains attached to queued orders.

## Agent observation

`FixedReinforcementObservationFactory` exposes:

- schema version
- current round
- last processed round
- queued orders
- future schedule entries

This DTO is intended for the later strategic-agent environment.

## Victory conditions

The reinforcement system does not introduce a second game-termination mechanism. Scenarios should continue to use TripleA's existing EndRound, rules attachment, victory-city, or trigger victory paths.
