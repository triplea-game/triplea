package games.strategy.engine.data;

import java.util.Collections;
import java.util.Set;

import games.strategy.util.IntegerMap;

/**
 * A game data change that captures the damage done to a collection of units.
 */
public class UnitHitsChange extends Change {
  private static final long serialVersionUID = 2862726651812142713L;

  private final IntegerMap<Unit> hits;
  private final IntegerMap<Unit> undoHits;

  private UnitHitsChange(final IntegerMap<Unit> hits, final IntegerMap<Unit> undoHits) {
    this.hits = hits;
    this.undoHits = undoHits;
  }

  public UnitHitsChange(final IntegerMap<Unit> hits) {
    this.hits = new IntegerMap<>(hits);
    undoHits = new IntegerMap<>();
    for (final Unit item : this.hits.keySet()) {
      undoHits.put(item, item.getHits());
    }
  }

  @Override
  protected void perform(final GameData data) {
    for (final Unit item : hits.keySet()) {
      item.setHits(hits.getInt(item));
    }
    final Set<Unit> units = hits.keySet();
    for (final Territory element : data.getMap().getTerritories()) {
      if (!Collections.disjoint(element.getUnits().getUnits(), units)) {
        element.notifyChanged();
      }
    }
  }

  @Override
  public Change invert() {
    return new UnitHitsChange(undoHits, hits);
  }
}
