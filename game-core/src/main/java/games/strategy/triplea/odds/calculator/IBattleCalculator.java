package games.strategy.triplea.odds.calculator;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import java.util.Collection;

/**
 * Interface to ensure different implementations of the odds calculator all have the same public
 * methods.
 */
public interface IBattleCalculator {
  void setGameData(GameData data);

  AggregateResults calculate(
      GamePlayer attacker,
      GamePlayer defender,
      Territory location,
      Collection<Unit> attacking,
      Collection<Unit> defending,
      Collection<Unit> bombarding,
      Collection<TerritoryEffect> territoryEffects,
      int runCount);

  void setKeepOneAttackingLandUnit(boolean bool);

  void setAmphibious(boolean bool);

  void setRetreatAfterRound(int value);

  void setRetreatAfterXUnitsLeft(int value);

  void setRetreatWhenOnlyAirLeft(boolean value);

  void setAttackerOrderOfLosses(String attackerOrderOfLosses);

  void setDefenderOrderOfLosses(String defenderOrderOfLosses);

  int getThreadCount();

  boolean isAlive();
}
