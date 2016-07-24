package games.strategy.engine.data;

import java.util.Collection;
import java.util.Set;

import games.strategy.util.IntegerMap;
import games.strategy.util.Util;

public class UnitHitsChange extends Change {
  private static final long serialVersionUID = 2862726651812142713L;
  private final IntegerMap<Unit> m_hits;
  private final IntegerMap<Unit> m_undoHits;

  private UnitHitsChange(final IntegerMap<Unit> hits, final IntegerMap<Unit> undoHits) {
    m_hits = hits;
    m_undoHits = undoHits;
  }

  public Collection<Unit> getUnits() {
    return m_hits.keySet();
  }

  public UnitHitsChange(final IntegerMap<Unit> hits) {
    m_hits = hits.copy();
    m_undoHits = new IntegerMap<>();
    for (final Unit item : m_hits.keySet()) {
      m_undoHits.put(item, item.getHits());
    }
  }

  @Override
  protected void perform(final GameData data) {
    for (final Unit item : m_hits.keySet()) {
      item.setHits(m_hits.getInt(item));
    }
    final Set<Unit> units = m_hits.keySet();
    for (final Territory element : data.getMap().getTerritories()) {
      if (Util.someIntersect(element.getUnits().getUnits(), units)) {
        element.notifyChanged();
      }
    }
  }

  @Override
  public Change invert() {
    return new UnitHitsChange(m_undoHits, m_hits);
  }
}
