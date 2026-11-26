package games.strategy.triplea.ai.pro.data;

import games.strategy.engine.data.Unit;
import java.util.Collection;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/** The result of an AI battle analysis. */
@Getter
@ToString
@AllArgsConstructor
public class ProBattleResult {

  private final double winPercentage;
  private final double tuvSwing;
  private final boolean hasLandUnitRemaining;
  private final Collection<Unit> averageAttackersRemaining;
  private final Collection<Unit> averageDefendersRemaining;
  private final double battleRounds;

  public ProBattleResult() {
    this(0, -1, false, List.of(), List.of(), 0);
  }
}
