# Fog of war

Small Front can enable player-specific visibility for strategic observations and the local map UI without changing the authoritative game state.

## Configuration

```xml
<property name="Fog Of War Enabled" value="true" editable="false"/>
<property name="Fog Of War Vision Radius" value="1" editable="false"/>
```

Fog of war is disabled by default. The vision radius is a non-negative number of normal map connections and defaults to one territory.

## Visibility sources

A player's current vision starts from:

- every territory controlled by that player or an ally
- every territory containing a unit owned by that player or an ally

The configured radius is applied from each source over the normal map connection graph. Territory and neighbor processing is sorted by territory name so identical game state produces identical observations and UI masks.

Visibility is current-state only. The engine does not retain last-known enemy positions in this milestone.

## Strategic observation

`VisibilityObservationFactory` produces schema version 1. Territory names, water status, and map connections remain public topology. For a hidden territory:

- `visible` is `false`
- `owner` is `null`
- `units` is empty

Visible unit stacks are grouped deterministically by owner and unit type. Supply observation schema version 2 uses the same visibility service and omits hidden territory supply status, sources, and counters.

## Single-player map masking

The Swing map combines the vision of all human-controlled local players. AI turns therefore do not switch the map to the AI player's perspective. When masking is active, hidden territories retain their public shape and name while the following layers are suppressed:

- ownership color
- unit stacks and small-map unit marks
- battles
- territory effects and dynamic sea-zone markers
- capital and victory-city markers
- mouse-hover territory and unit details

Territory clicks remain available so a player can issue movement orders into unrevealed adjacent territory. Such selection is passed through the central `VisibilityAudit` hook; hidden unit selection remains unavailable because hidden stacks have no map drawables.

## Information-disclosure boundary

`VisibilityService` and `VisibilityAudit` are the common boundary for strategic observations and UI surfaces. New history views, odds calculators, logs, or agent features that expose territory state should call this boundary rather than reading hidden territory units directly.

This milestone provides local single-player masking and filtered agent observations. The client still contains authoritative game data. Server-side filtering for untrusted multiplayer clients, filtered save files, and last-known-state intelligence remain later work.

## Compatibility

Maps that omit `Fog Of War Enabled` retain the existing complete-information UI and observations. Empty local-human viewer sets, such as observer or dedicated-server contexts, are not masked in this milestone.
