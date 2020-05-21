package games.strategy.triplea.ai.tree;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.pro.logging.ProLogger;
import games.strategy.triplea.odds.calculator.AggregateResults;
import games.strategy.triplea.odds.calculator.ConcurrentBattleCalculator;
import games.strategy.triplea.odds.calculator.IBattleCalculator;
import java.text.DecimalFormat;
import java.util.Collection;

public class BattleTreeCompCalculator implements IBattleCalculator {

  private static final BattleTreeCalculator battleTreeCalculator = new BattleTreeCalculator();
  private static final ConcurrentBattleCalculator hardAiCalculator =
      new ConcurrentBattleCalculator();

  public BattleTreeCompCalculator() {}

  public void setGameData(final GameData data) {
    battleTreeCalculator.setGameData(data);
    hardAiCalculator.setGameData(data);
  }

  public void cancel() {
    hardAiCalculator.cancel();
  }

  @Override
  public AggregateResults calculate(
      final GamePlayer attacker,
      final GamePlayer defender,
      final Territory location,
      final Collection<Unit> attackingUnits,
      final Collection<Unit> defendingUnits,
      final Collection<Unit> bombardingUnits,
      final Collection<TerritoryEffect> territoryEffects,
      final boolean retreatWhenOnlyAirLeft,
      final int runCount) {
    final DecimalFormat timeFormat = new DecimalFormat("0.00s");
    final DecimalFormat percentFormat = new DecimalFormat("0.00%");

    long start = System.nanoTime();
    final AggregateResults battleTreeResults =
        battleTreeCalculator.calculate(
            attacker,
            defender,
            location,
            attackingUnits,
            defendingUnits,
            bombardingUnits,
            territoryEffects,
            retreatWhenOnlyAirLeft,
            runCount);
    final long battleTreeTime = System.nanoTime() - start;

    start = System.nanoTime();
    final AggregateResults hardAiResults =
        hardAiCalculator.calculate(
            attacker,
            defender,
            location,
            attackingUnits,
            defendingUnits,
            bombardingUnits,
            territoryEffects,
            retreatWhenOnlyAirLeft,
            runCount);
    final long hardAiTime = System.nanoTime() - start;

    // if the battleTree took longer or was more than 5% different with win percentage
    boolean logDetails = false;
    if (Math.abs(hardAiResults.getAttackerWinPercent() - battleTreeResults.getAttackerWinPercent())
        > .05) {
      logDetails = true;
      ProLogger.warn(
          "BT: Hard AI had win percentage of "
              + percentFormat.format(hardAiResults.getAttackerWinPercent())
              + " while BattleTree AI had win percentage of "
              + percentFormat.format(battleTreeResults.getAttackerWinPercent()));
    }
    if (battleTreeTime > hardAiTime) {
      logDetails = true;
      ProLogger.warn(
          "BT: Hard AI was faster with "
              + timeFormat.format((double) hardAiTime / 1_000_000.0)
              + " while BattleTree AI had "
              + timeFormat.format((double) battleTreeTime / 1_000_000.0));
    }
    if (logDetails) {
      ProLogger.warn("BT: Units: A: " + attackingUnits + " D: " + defendingUnits);
    }

    return battleTreeResults;
  }
}
