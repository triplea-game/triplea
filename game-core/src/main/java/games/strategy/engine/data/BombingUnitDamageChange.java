package games.strategy.engine.data;

import java.util.Set;

import games.strategy.triplea.TripleAUnit;
import games.strategy.util.CollectionUtils;
import games.strategy.util.IntegerMap;

/**
 * A game data change that captures the damage caused to a collection of units by a bombing attack.
 */
public class BombingUnitDamageChange extends Change {
  private static final long serialVersionUID = -6425858423179501847L;
  private final IntegerMap<Unit> hits;
  private final IntegerMap<Unit> undoHits;

  private BombingUnitDamageChange(final IntegerMap<Unit> hits, final IntegerMap<Unit> undoHits) {
    this.hits = hits;
    this.undoHits = undoHits;
  }

  public BombingUnitDamageChange(final IntegerMap<Unit> hits) {
    for (final Unit u : hits.keySet()) {
      if (!(u instanceof TripleAUnit)) {
        throw new IllegalArgumentException("BombingUnitDamage can only apply to a TripleAUnit object");
      }
    }
    this.hits = hits.copy();
    undoHits = new IntegerMap<>();
    for (final Unit unit : this.hits.keySet()) {
      undoHits.put(unit, ((TripleAUnit) unit).getUnitDamage());
    }
  }

  @Override
  protected void perform(final GameData data) {
    for (final Unit item : hits.keySet()) {
      ((TripleAUnit) item).setUnitDamage(hits.getInt(item));
    }
    final Set<Unit> units = hits.keySet();
    for (final Territory element : data.getMap().getTerritories()) {
      if (CollectionUtils.someIntersect(element.getUnits().getUnits(), units)) {
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
    return "Bombing unit damage change. Hits:" + hits + " undoHits:" + undoHits;
  }
}
