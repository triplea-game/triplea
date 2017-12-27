package games.strategy.triplea.delegate.dataObjects;

import games.strategy.engine.data.Territory;
import games.strategy.triplea.delegate.IBattle.BattleType;

public class FightBattleDetails {
  private final boolean bombingRaid;
  private final BattleType type;
  private final Territory where;

  public FightBattleDetails(final Territory where, final boolean bombingRaid, final BattleType type) {
    this.bombingRaid = bombingRaid;
    this.where = where;
    this.type = type;
  }

  public boolean isBombingRaid() {
    return bombingRaid;
  }

  public Territory getWhere() {
    return where;
  }

  public BattleType getBattleType() {
    return type;
  }
}
