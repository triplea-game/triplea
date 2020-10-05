package games.strategy.engine.data;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.triplea.java.collections.IntegerMap;

/** A game data change that captures the damage done to a collection of units. */
public class UnitHitsChange extends Change {
  private static final long serialVersionUID = 2862726651812142713L;

  private final IntegerMap<Unit> hits;
  private final IntegerMap<Unit> undoHits;
  private final Collection<Territory> territoriesToNotify;

  private final IntegerMap<String> primitiveHits;
  private final IntegerMap<String> primitiveUndoHits;
  private final Collection<String> primitiveTerritoriesToNotify;

  private UnitHitsChange(
      final IntegerMap<String> primitiveHits,
      final IntegerMap<String> primitiveUndoHits,
      final Collection<String> primitiveTerritoriesToNotify,
      final IntegerMap<Unit> hits,
      final IntegerMap<Unit> undoHits,
      final Collection<Territory> territoriesToNotify) {
    if (hits != null) {
      this.primitiveHits = new IntegerMap<>();
      for (final Map.Entry<Unit, Integer> hit : hits.entrySet()) {
        this.primitiveHits.add(hit.getKey().getId().toString(), hit.getValue());
      }
      this.primitiveUndoHits = new IntegerMap<>();
      for (final Map.Entry<Unit, Integer> hit : undoHits.entrySet()) {
        this.primitiveUndoHits.add(hit.getKey().getId().toString(), hit.getValue());
      }
      this.primitiveTerritoriesToNotify =
          territoriesToNotify.stream().map(Territory::getName).collect(Collectors.toList());
    } else {
      this.primitiveHits = primitiveHits;
      this.primitiveUndoHits = primitiveUndoHits;
      this.primitiveTerritoriesToNotify = primitiveTerritoriesToNotify;
    }

    this.hits = null;
    this.undoHits = null;
    this.territoriesToNotify = null;
  }

  public UnitHitsChange(
      final IntegerMap<Unit> hits, final Collection<Territory> territoriesToNotify) {
    this(null, null, null, new IntegerMap<>(hits), undoHits(hits), territoriesToNotify);
  }

  private static IntegerMap<Unit> undoHits(final IntegerMap<Unit> hits) {
    final var undoHits = new IntegerMap<Unit>();
    for (final Unit item : hits.keySet()) {
      undoHits.put(item, item.getHits());
    }
    return undoHits;
  }

  @Override
  protected void perform(final GameData data) {
    if (primitiveHits == null) {
      for (final Unit item : hits.keySet()) {
        item.setHits(hits.getInt(item));
      }
      for (final Territory territory : territoriesToNotify) {
        territory.notifyChanged();
      }
    } else {
      for (final Map.Entry<String, Integer> hit : primitiveHits.entrySet()) {
        final Unit unit = data.getUnits().get(UUID.fromString(hit.getKey()));
        unit.setHits(hit.getValue());
      }
      for (final String territoryName : primitiveTerritoriesToNotify) {
        data.getMap().getTerritory(territoryName).notifyChanged();
      }
    }
  }

  @Override
  public Change invert() {
    return new UnitHitsChange(
        primitiveUndoHits,
        primitiveHits,
        primitiveTerritoriesToNotify,
        undoHits,
        hits,
        territoriesToNotify);
  }
}
