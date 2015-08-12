package games.strategy.triplea.ai.proAI;

import java.util.ArrayList;
import java.util.List;

import games.strategy.engine.data.Unit;

public class ProBattleResultData {
  private double winPercentage;
  private double TUVSwing;
  private boolean hasLandUnitRemaining;
  private List<Unit> averageUnitsRemaining;
  private double battleRounds;

  public ProBattleResultData() {
    winPercentage = 0;
    TUVSwing = -1;
    hasLandUnitRemaining = false;
    averageUnitsRemaining = new ArrayList<Unit>();
    battleRounds = 0;
  }

  public ProBattleResultData(final double winPercentage, final double TUVSwing, final boolean hasLandUnitRemaining,
      final List<Unit> averageUnitsRemaining, final double battleRounds) {
    this.winPercentage = winPercentage;
    this.TUVSwing = TUVSwing;
    this.hasLandUnitRemaining = hasLandUnitRemaining;
    this.averageUnitsRemaining = averageUnitsRemaining;
    this.battleRounds = battleRounds;
  }

  public double getWinPercentage() {
    return winPercentage;
  }

  public void setWinPercentage(final double winPercentage) {
    this.winPercentage = winPercentage;
  }

  public double getTUVSwing() {
    return TUVSwing;
  }

  public void setTUVSwing(final double tUVSwing) {
    TUVSwing = tUVSwing;
  }

  public boolean isHasLandUnitRemaining() {
    return hasLandUnitRemaining;
  }

  public void setHasLandUnitRemaining(final boolean hasLandUnitRemaining) {
    this.hasLandUnitRemaining = hasLandUnitRemaining;
  }

  public void setAverageUnitsRemaining(final List<Unit> averageUnitsRemaining) {
    this.averageUnitsRemaining = averageUnitsRemaining;
  }

  public List<Unit> getAverageUnitsRemaining() {
    return averageUnitsRemaining;
  }

  public void setBattleRounds(final double battleRounds) {
    this.battleRounds = battleRounds;
  }

  public double getBattleRounds() {
    return battleRounds;
  }
}
