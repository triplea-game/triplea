package games.strategy.engine.data;

import java.util.Collections;
import java.util.Set;
import org.triplea.java.RemoveOnNextMajorRelease;
import org.triplea.java.collections.IntegerMap;

/**
 * A game data change that captures the damage caused to a collection of units by a bombing attack.
 */
@Deprecated
@RemoveOnNextMajorRelease
public class BombingUnitDamageChange extends Change {
  private static final long serialVersionUID = -6425858423179501847L;
  private final IntegerMap<Unit> hits;
  private final IntegerMap<Unit> undoHits;

  private BombingUnitDamageChange(final IntegerMap<Unit> hits, final IntegerMap<Unit> undoHits) {
    this.hits = hits;
    this.undoHits = undoHits;
  }

  public BombingUnitDamageChange(final IntegerMap<Unit> hits) {
    this.hits = new IntegerMap<>(hits);
    undoHits = new IntegerMap<>();
    for (final Unit unit : this.hits.keySet()) {
      undoHits.put(unit, unit.getUnitDamage());
    }
  }

  @Override
  protected void perform(final GameState data) {
    for (final Unit item : hits.keySet()) {
      item.setUnitDamage(hits.getInt(item));
    }
    final Set<Unit> units = hits.keySet();
    for (final Territory element : data.getMap().getTerritories()) {
      if (!Collections.disjoint(element.getUnits(), units)) {
        element.notifyChanged();
      }
    }
  }

  @Override
  public Change invert() {
    return new BombingUnitDamageChange(undoHits, hits);
  }

  @Override
  public String toString() {
    return "Bombing unit damage change. Hits: " + hits + " undoHits: " + undoHits;
  }
}
