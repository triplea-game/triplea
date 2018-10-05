// CHECKSTYLE-OFF: PackageName
// rename upon next incompatible release

package games.strategy.triplea.delegate.dataObjects;

import games.strategy.engine.data.Territory;
import games.strategy.triplea.delegate.IBattle.BattleType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class FightBattleDetails {
  private final Territory where;
  private final boolean bombingRaid;
  private final BattleType battleType;
}
