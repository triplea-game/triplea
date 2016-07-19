package games.strategy.engine.data.changefactory;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;

class PlayerWhoAmIChange extends Change {
  private static final long serialVersionUID = -1486914230174337300L;
  private final String m_startWhoAmI;
  private final String m_endWhoAmI;
  private final String m_player;

  PlayerWhoAmIChange(final String newWhoAmI, final PlayerID player) {
    m_startWhoAmI = player.getWhoAmI();
    m_endWhoAmI = newWhoAmI;
    m_player = player.getName();
  }

  PlayerWhoAmIChange(final String startWhoAmI, final String endWhoAmI, final String player) {
    m_startWhoAmI = startWhoAmI;
    m_endWhoAmI = endWhoAmI;
    m_player = player;
  }

  @Override
  protected void perform(final GameData data) {
    final PlayerID player = data.getPlayerList().getPlayerID(m_player);
    player.setWhoAmI(m_endWhoAmI);
  }

  @Override
  public Change invert() {
    return new PlayerWhoAmIChange(m_endWhoAmI, m_startWhoAmI, m_player);
  }

  @Override
  public String toString() {
    return m_player + " changed from " + m_startWhoAmI + " to " + m_endWhoAmI;
  }
}
