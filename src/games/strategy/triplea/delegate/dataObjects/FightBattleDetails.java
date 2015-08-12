package games.strategy.triplea.delegate.dataObjects;

import games.strategy.engine.data.Territory;
import games.strategy.triplea.delegate.IBattle.BattleType;

public class FightBattleDetails {
  private final boolean m_bombingRaid;
  private final BattleType m_type;
  private final Territory m_where;

  public FightBattleDetails(final Territory where, final boolean bombingRaid, final BattleType type) {
    m_bombingRaid = bombingRaid;
    m_where = where;
    m_type = type;
  }

  public boolean isBombingRaid() {
    return m_bombingRaid;
  }

  public Territory getWhere() {
    return m_where;
  }

  public BattleType getBattleType() {
    return m_type;
  }
}
