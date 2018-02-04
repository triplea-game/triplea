package games.strategy.triplea.ai.fastAI;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.proAI.util.ProBattleUtils;
import games.strategy.triplea.ai.proAI.util.ProPurchaseUtils;
import games.strategy.triplea.oddsCalculator.ta.AggregateResults;
import games.strategy.triplea.oddsCalculator.ta.IOddsCalculator;
import games.strategy.triplea.oddsCalculator.ta.OddsCalculatorListener;

public class FastOddsEstimator implements IOddsCalculator {

  private Territory location = null;
  private Collection<Unit> attackingUnits = new ArrayList<>();
  private Collection<Unit> defendingUnits = new ArrayList<>();

  @Override
  public void setGameData(final GameData data) {}

  @Override
  public void setCalculateData(final PlayerID attacker, final PlayerID defender, final Territory location,
      final Collection<Unit> attackingUnits, final Collection<Unit> defendingUnits,
      final Collection<Unit> bombardingUnits, final Collection<TerritoryEffect> territoryEffects, final int runCount) {
    this.location = location;
    this.attackingUnits = attackingUnits;
    this.defendingUnits = defendingUnits;
  }

  @Override
  public AggregateResults calculate() {
    final double winPercentage = ProBattleUtils.estimateStrengthDifference(location, new ArrayList<>(attackingUnits),
        new ArrayList<>(defendingUnits));
    final int battleRoundsFought = 3;
    List<Unit> remainingAttackingUnits = new ArrayList<>();
    List<Unit> remainingDefendingUnits = new ArrayList<>();
    if (winPercentage > 50) {
      remainingAttackingUnits.addAll(attackingUnits);
      Collections.sort(remainingAttackingUnits, ProPurchaseUtils.getCostComparator().reversed());
      final int numRemainingUnits = (int) Math.ceil(attackingUnits.size() * (Math.min(100, winPercentage) - 50) / 50);
      remainingAttackingUnits = remainingAttackingUnits.subList(0, numRemainingUnits);
    } else {
      remainingDefendingUnits.addAll(defendingUnits);
      Collections.sort(remainingDefendingUnits, ProPurchaseUtils.getCostComparator().reversed());
      final int numRemainingUnits = (int) Math.ceil(defendingUnits.size() * (50 - Math.max(0, winPercentage)) / 50);
      remainingDefendingUnits = remainingDefendingUnits.subList(0, numRemainingUnits);
    }
    return new AggregateEstimate(battleRoundsFought, winPercentage / 100, remainingAttackingUnits,
        remainingDefendingUnits);
  }

  @Override
  public AggregateResults setCalculateDataAndCalculate(final PlayerID attacker, final PlayerID defender,
      final Territory location, final Collection<Unit> attacking, final Collection<Unit> defending,
      final Collection<Unit> bombarding, final Collection<TerritoryEffect> territoryEffects, final int runCount) {
    setCalculateData(attacker, defender, location, attacking, defending, bombarding, territoryEffects, runCount);
    return calculate();
  }

  @Override
  public void setKeepOneAttackingLandUnit(final boolean bool) {}

  @Override
  public void setAmphibious(final boolean bool) {}

  @Override
  public void setRetreatAfterRound(final int value) {}

  @Override
  public void setRetreatAfterXUnitsLeft(final int value) {}

  @Override
  public void setRetreatWhenOnlyAirLeft(final boolean value) {}

  @Override
  public void setAttackerOrderOfLosses(final String attackerOrderOfLosses) {}

  @Override
  public void setDefenderOrderOfLosses(final String defenderOrderOfLosses) {}

  @Override
  public void cancel() {}

  @Override
  public void shutdown() {}

  @Override
  public void addOddsCalculatorListener(final OddsCalculatorListener listener) {}
}
