package games.strategy.engine.data.changefactory;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;

/**
 * Changes ownership of a territory.
 */
class OwnerChange extends Change {
  private static final long serialVersionUID = -5938125380623744929L;
  /**
   * Either new or old owner can be null.
   */
  private final String m_old;
  private final String m_new;
  private final String m_territory;

  /**
   * newOwner can be null.
   */
  OwnerChange(final Territory territory, final PlayerID newOwner) {
    m_territory = territory.getName();
    m_new = getName(newOwner);
    m_old = getName(territory.getOwner());
  }

  private OwnerChange(final String name, final String newOwner, final String oldOwner) {
    m_territory = name;
    m_new = newOwner;
    m_old = oldOwner;
  }

  private static String getName(final PlayerID player) {
    if (player == null) {
      return null;
    }
    return player.getName();
  }

  private static PlayerID getPlayerID(final String name, final GameData data) {
    if (name == null) {
      return null;
    }
    return data.getPlayerList().getPlayerID(name);
  }

  @Override
  public Change invert() {
    return new OwnerChange(m_territory, m_old, m_new);
  }

  @Override
  protected void perform(final GameData data) {
    // both names could be null
    data.getMap().getTerritory(m_territory).setOwner(getPlayerID(m_new, data));
  }

  @Override
  public String toString() {
    return m_new + " takes " + m_territory + " from " + m_old;
  }
}
