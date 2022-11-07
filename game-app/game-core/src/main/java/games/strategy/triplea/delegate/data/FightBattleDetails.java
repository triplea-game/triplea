package games.strategy.triplea.delegate.data;

import games.strategy.engine.data.Territory;
import games.strategy.triplea.delegate.battle.IBattle.BattleType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** Information about a pending battle that must be fought. */
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class FightBattleDetails {
  private final Territory where;
  private final boolean bombingRaid;
  private final BattleType battleType;
}
