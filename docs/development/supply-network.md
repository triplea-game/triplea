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
- capturing a road node or source changes current reachability immediately
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

## Isolation and movement timing

Movement blocking is based on the isolation state recorded at the unit owner's supply phase, not on an unannounced mid-turn network recalculation.

With the default two-turn threshold:

1. when a road is cut, the territory immediately reports that current road supply is cut, but units that have not yet reached their next owner supply phase remain in `PENDING_ISOLATION` and may still move
2. at the next owner supply phase, each affected land unit records one out-of-supply owner turn; it remains on the map, receives an `O1/2` marker, and cannot move in Combat Move or redeployment
3. at the following owner supply phase, the unit reaches `O2/2` and is removed through the normal synchronized change stream
4. restored supply clears the counter before removal and returns the unit to normal movement

`SupplyAwareMoveDelegate` reads the recorded counter before accepting an authoritative move. Maps that enable the supply network but omit `SupplyDelegate` fall back to immediate current-network rejection so a misconfigured scenario cannot bypass supply restrictions. Edit mode bypasses the rule.

## Attrition

At the owner's supply phase:

1. supplied units clear any existing isolation counter
2. unsupplied land units increment their counter once
3. a unit reaching the configured limit is removed through the normal network-synchronized change stream

Changing unit ownership resets the effective counter for the new owner.

## Player-visible status

The Swing map exposes the operational state directly:

- road edges are visible as public infrastructure
- green solid roads connect currently supplied endpoints for the local perspective
- red dashed roads indicate a visible cut connection
- gray roads retain public topology when fog of war hides an endpoint's state
- `D` marks a supply source or depot
- `S` marks a supplied road territory
- `P` marks a current road cut that has not yet been recorded at the owner's supply phase
- `O1/2` on a unit stack marks one recorded owner turn without supply and shows that movement is blocked

Hidden ownership, unit counters, and connection status remain filtered by the existing visibility boundary.

## Save and network behavior

`SupplyTracker` is serialized as delegate state and records both the last processed player-round and per-unit UUID counters. Unit removals use standard `Change` objects, so the server remains authoritative and all clients receive the same board mutation.

## Strategic observation

`SupplyDelegate.getObservation()` returns schema version 2 with:

- current and last processed rounds
- player and configured removal limit
- visible land territories, source flags, resolved road neighbors, friendly control, and supply status
- every visible owned land unit's UUID, territory, unit type, supply status, accumulated isolation turns, and turns remaining before removal

Strategic observation schema version 2 also exposes public road connections and, for visible friendly land-unit groups, `supplied`, `outOfSupplyTurns`, and `turnsUntilRemoval`.

The public `SupplyNetworkResolver` is available to strategic AI, map rendering, and evaluation code.
