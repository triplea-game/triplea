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

  /**
   * Chance in percent that the attacker wins with a land unit left to take the territory. Only Hard
   * Beta simulates it, for land; elsewhere it is the win chance when {@link #hasLandUnitRemaining},
   * which only says whether the average survivors include a land unit.
   */
  private final double capturePercentage;

  public ProBattleResult() {
    this(0, -1, false, List.of(), List.of(), 0);
  }

  /** A result whose capture chance is its win chance when a land unit survives on average. */
  public ProBattleResult(
      final double winPercentage,
      final double tuvSwing,
      final boolean hasLandUnitRemaining,
      final Collection<Unit> averageAttackersRemaining,
      final Collection<Unit> averageDefendersRemaining,
      final double battleRounds) {
    this(
        winPercentage,
        tuvSwing,
        hasLandUnitRemaining,
        averageAttackersRemaining,
        averageDefendersRemaining,
        battleRounds,
        hasLandUnitRemaining ? winPercentage : 0);
  }
}
