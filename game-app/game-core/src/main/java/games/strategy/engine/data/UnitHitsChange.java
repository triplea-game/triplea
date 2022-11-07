package games.strategy.engine.data;

import java.util.Collection;
import org.triplea.java.RemoveOnNextMajorRelease;
import org.triplea.java.collections.IntegerMap;

/** A game data change that captures the damage done to a collection of units. */
@Deprecated
@RemoveOnNextMajorRelease
public class UnitHitsChange extends Change {
  private static final long serialVersionUID = 2862726651812142713L;

  private final IntegerMap<Unit> hits;
  private final IntegerMap<Unit> undoHits;
  private final Collection<Territory> territoriesToNotify;

  private UnitHitsChange(
      final IntegerMap<Unit> hits,
      final IntegerMap<Unit> undoHits,
      final Collection<Territory> territoriesToNotify) {
    this.hits = hits;
    this.undoHits = undoHits;
    this.territoriesToNotify = territoriesToNotify;
  }

  public UnitHitsChange(
      final IntegerMap<Unit> hits, final Collection<Territory> territoriesToNotify) {
    this(new IntegerMap<>(hits), undoHits(hits), territoriesToNotify);
  }

  private static IntegerMap<Unit> undoHits(final IntegerMap<Unit> hits) {
    final var undoHits = new IntegerMap<Unit>();
    for (final Unit item : hits.keySet()) {
      undoHits.put(item, item.getHits());
    }
    return undoHits;
  }

  @Override
  protected void perform(final GameState data) {
    for (final Unit item : hits.keySet()) {
      item.setHits(hits.getInt(item));
    }
    for (final Territory territory : territoriesToNotify) {
      territory.notifyChanged();
    }
  }

  @Override
  public Change invert() {
    return new UnitHitsChange(undoHits, hits, territoriesToNotify);
  }
}
