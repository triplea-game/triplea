package games.strategy.triplea.ai.fastAI;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.proAI.util.ProBattleUtils;
import games.strategy.triplea.oddsCalculator.ta.AggregateResults;
import games.strategy.triplea.oddsCalculator.ta.IOddsCalculator;
import games.strategy.triplea.oddsCalculator.ta.OddsCalculatorListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FastOddsEstimator implements IOddsCalculator {

  @SuppressWarnings("unused")
  private GameData data;
  @SuppressWarnings("unused")
  private PlayerID attacker = null;
  @SuppressWarnings("unused")
  private PlayerID defender = null;
  private Territory location = null;
  private Collection<Unit> attackingUnits = new ArrayList<>();
  private Collection<Unit> defendingUnits = new ArrayList<>();
  @SuppressWarnings("unused")
  private Collection<Unit> bombardingUnits = new ArrayList<>();
  @SuppressWarnings("unused")
  private Collection<TerritoryEffect> territoryEffects = new ArrayList<>();

  @Override
  public void setGameData(final GameData data) {
    this.data = data;
  }

  @Override
  public void setCalculateData(final PlayerID attacker, final PlayerID defender, final Territory location,
      final Collection<Unit> attackingUnits, final Collection<Unit> defendingUnits,
      final Collection<Unit> bombardingUnits, final Collection<TerritoryEffect> territoryEffects, final int runCount) {
    this.attacker = attacker;
    this.defender = defender;
    this.location = location;
    this.attackingUnits = attackingUnits;
    this.defendingUnits = defendingUnits;
    this.bombardingUnits = bombardingUnits;
    this.territoryEffects = territoryEffects;
  }

  @Override
  public AggregateResults calculate() {
    final double winPercentage =
        ProBattleUtils.estimateStrengthDifference(location, new ArrayList<>(attackingUnits), new ArrayList<>(
            defendingUnits));
    final int battleRoundsFought = 3;
    final List<Unit> remainingAttackingUnits = new ArrayList<>();
    final List<Unit> remainingDefendingUnits = new ArrayList<>();
    if (winPercentage > 50) {
      remainingAttackingUnits.addAll(attackingUnits);
    } else {
      remainingDefendingUnits.addAll(defendingUnits);
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
  public int getRunCount() {
    return 1;
  }

  @Override
  public boolean getIsReady() {
    return true;
  }

  @Override
  public void setKeepOneAttackingLandUnit(final boolean bool) {

  }

  @Override
  public void setAmphibious(final boolean bool) {

  }

  @Override
  public void setRetreatAfterRound(final int value) {

  }

  @Override
  public void setRetreatAfterXUnitsLeft(final int value) {

  }

  @Override
  public void setRetreatWhenOnlyAirLeft(final boolean value) {

  }

  @Override
  public void setRetreatWhenMetaPowerIsLower(final boolean value) {

  }

  @Override
  public void setAttackerOrderOfLosses(final String attackerOrderOfLosses) {

  }

  @Override
  public void setDefenderOrderOfLosses(final String defenderOrderOfLosses) {

  }

  @Override
  public void cancel() {

  }

  @Override
  public void shutdown() {

  }

  @Override
  public int getThreadCount() {
    return 1;
  }

  @Override
  public void addOddsCalculatorListener(final OddsCalculatorListener listener) {

  }

  @Override
  public void removeOddsCalculatorListener(final OddsCalculatorListener listener) {

  }

}
