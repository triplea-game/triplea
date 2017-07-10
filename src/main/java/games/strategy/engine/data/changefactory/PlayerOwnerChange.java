package games.strategy.engine.data.changefactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.net.GUID;

/**
 * Changes ownership of a unit.
 */
class PlayerOwnerChange extends Change {
  /**
   * Maps unit id -> owner as String.
   */
  private final Map<GUID, String> m_old;
  private final Map<GUID, String> m_new;
  private final String m_location;
  private static final long serialVersionUID = -9154938431233632882L;

  PlayerOwnerChange(final Collection<Unit> units, final PlayerID newOwner, final Territory location) {
    m_old = new HashMap<>();
    m_new = new HashMap<>();
    m_location = location.getName();
    for (final Unit unit : units) {
      m_old.put(unit.getID(), unit.getOwner().getName());
      m_new.put(unit.getID(), newOwner.getName());
    }
  }

  PlayerOwnerChange(final Map<GUID, String> newOwner, final Map<GUID, String> oldOwner, final String location) {
    m_old = oldOwner;
    m_new = newOwner;
    m_location = location;
  }

  @Override
  public Change invert() {
    return new PlayerOwnerChange(m_old, m_new, m_location);
  }

  @Override
  protected void perform(final GameData data) {
    for (final GUID id : m_new.keySet()) {
      final Unit unit = data.getUnits().get(id);
      if (!m_old.get(id).equals(unit.getOwner().getName())) {
        throw new IllegalStateException("Wrong owner, expecting" + m_old.get(id) + " but got " + unit.getOwner());
      }
      final String owner = m_new.get(id);
      final PlayerID player = data.getPlayerList().getPlayerID(owner);
      unit.setOwner(player);
    }
    data.getMap().getTerritory(m_location).notifyChanged();
  }

  @Override
  public String toString() {
    return "Some units change owners in territory " + m_location;
  }
}
