package games.strategy.triplea.ai.proAI;

import java.util.ArrayList;
import java.util.List;

import games.strategy.engine.data.Unit;

public class ProBattleResultData {

  private double winPercentage;
  private double TUVSwing;
  private boolean hasLandUnitRemaining;
  private List<Unit> averageAttackersRemaining;
  private List<Unit> averageDefendersRemaining;
  private double battleRounds;

  public ProBattleResultData() {
    winPercentage = 0;
    TUVSwing = -1;
    hasLandUnitRemaining = false;
    averageAttackersRemaining = new ArrayList<Unit>();
    averageDefendersRemaining = new ArrayList<Unit>();
    battleRounds = 0;
  }

  public ProBattleResultData(final double winPercentage, final double TUVSwing, final boolean hasLandUnitRemaining,
      final List<Unit> averageAttackersRemaining, final List<Unit> averageDefendersRemaining, final double battleRounds) {
    this.winPercentage = winPercentage;
    this.TUVSwing = TUVSwing;
    this.hasLandUnitRemaining = hasLandUnitRemaining;
    this.averageAttackersRemaining = averageAttackersRemaining;
    this.averageDefendersRemaining = averageDefendersRemaining;
    this.battleRounds = battleRounds;
  }

  public double getWinPercentage() {
    return winPercentage;
  }

  public double getTUVSwing() {
    return TUVSwing;
  }

  public boolean isHasLandUnitRemaining() {
    return hasLandUnitRemaining;
  }

  public List<Unit> getAverageAttackersRemaining() {
    return averageAttackersRemaining;
  }

  public List<Unit> getAverageDefendersRemaining() {
    return averageDefendersRemaining;
  }

  public double getBattleRounds() {
    return battleRounds;
  }

  @Override
  public String toString() {
    return "winPercentage=" + winPercentage + ", TUVSwing=" + TUVSwing + ", hasLandUnitRemaining="
        + hasLandUnitRemaining + ", averageAttackersRemaining=" + averageAttackersRemaining
        + ", averageDefendersRemaining=" + averageDefendersRemaining + ", battleRounds=" + battleRounds;
  }
}
