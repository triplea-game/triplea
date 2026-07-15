# Road-based supply network

Milestone 11 adds an opt-in supply network for narrow-front scenarios. Supply is resolved through map-authored road links and remains separate from movement adjacency, territory production, and ground ownership rules.

## Activation

```xml
<property name="Supply Network Enabled" value="true" editable="false"/>
<property name="Out Of Supply Removal Turns" value="2" editable="false"/>
```

The feature is disabled by default. Existing maps and normal `MoveDelegate` behavior remain unchanged.

`Out Of Supply Removal Turns` defaults to `2` and is clamped to at least one owner turn.

## Territory configuration

Attach supply metadata to each source or road node:

```xml
<attachment
    name="supplyAttachment"
    attachTo="Allied Depot"
    javaClass="games.strategy.triplea.attachments.SupplyTerritoryAttachment"
    type="territory">
  <option name="supplySource" value="true"/>
  <option name="roadConnection" value="Crossroads"/>
</attachment>

<attachment
    name="supplyAttachment"
    attachTo="Crossroads"
    javaClass="games.strategy.triplea.attachments.SupplyTerritoryAttachment"
    type="territory">
  <option name="roadConnection" value="Forward Line:Mountain Pass"/>
</attachment>
```

A `roadConnection` value may contain multiple territory names separated by colons, and the option may be repeated. Road links are interpreted as undirected: declaring an edge on either endpoint is sufficient. Self-links, unknown territories, and water endpoints are rejected.

A map can represent factories or logistics hubs by marking their territories with `supplySource=true`.

## Reachability

For each player, supply begins at every friendly land territory marked as a supply source. Breadth-first traversal follows road links through land territories owned by that player or an ally.

- the source territory itself is supplied
- a road does not need to match normal movement adjacency
- enemy, neutral, or missing relationship control blocks traversal
- capturing a road node or source changes reachability immediately
- road and source iteration is sorted by territory name for deterministic save, replay, and AI behavior

Only land units require road supply. Aircraft, naval units, and transported land units in water territories are not blocked by this system.

## Sequence delegates

Declare an explicit supply phase and use the supply-aware movement delegate:

```xml
<delegate
    name="supply"
    javaClass="games.strategy.triplea.delegate.supply.SupplyDelegate"
    display="Supply"/>

<delegate
    name="move"
    javaClass="games.strategy.triplea.delegate.supply.SupplyAwareMoveDelegate"
    display="Move"/>

<step name="AlliesReinforcement" delegate="fixedReinforcement" player="Allies"/>
<step name="AlliesSupply" delegate="supply" player="Allies"/>
<step name="AlliesCombatMove" delegate="move" player="Allies"/>
```

The supply step belongs after fixed reinforcements and before Combat Move. Re-entering or loading the same owner-round does not increment counters twice.

`SupplyAwareMoveDelegate` recalculates the current network immediately before the authoritative move. A land unit in an unsupplied territory cannot move in Combat Move or redeployment. Edit mode bypasses this rule.

## Attrition

At the owner's supply phase:

1. supplied units clear any existing isolation counter
2. unsupplied land units increment their counter once
3. a unit reaching the configured limit is removed through the normal network-synchronized change stream

With the default value of two, a newly isolated unit remains on the map during its first unsupplied owner turn and is removed during its second. Changing unit ownership resets the effective counter for the new owner.

## Save and network behavior

`SupplyTracker` is serialized as delegate state and records both the last processed player-round and per-unit UUID counters. Unit removals use standard `Change` objects, so the server remains authoritative and all clients receive the same board mutation.

## Strategic observation

`SupplyDelegate.getObservation()` returns schema version 1 with:

- current and last processed rounds
- player and configured removal limit
- all land territories, source flags, resolved road neighbors, friendly control, and supply status
- every owned land unit's UUID, territory, unit type, supply status, accumulated isolation turns, and turns remaining before removal

The public `SupplyNetworkResolver` is also available to future AI move generation and evaluation code.
