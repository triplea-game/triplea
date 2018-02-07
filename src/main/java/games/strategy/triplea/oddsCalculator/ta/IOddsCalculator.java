package games.strategy.triplea.oddsCalculator.ta;

import java.util.Collection;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;

/**
 * Interface to ensure different implementations of the odds calculator all have the same public methods.
 */
public interface IOddsCalculator {
  void setGameData(GameData data);

  AggregateResults setCalculateDataAndCalculate(PlayerID attacker, PlayerID defender, Territory location,
      Collection<Unit> attacking, Collection<Unit> defending, Collection<Unit> bombarding,
      Collection<TerritoryEffect> territoryEffects, int runCount);

  void setKeepOneAttackingLandUnit(boolean bool);

  void setAmphibious(boolean bool);

  void setRetreatAfterRound(int value);

  void setRetreatAfterXUnitsLeft(int value);

  void setRetreatWhenOnlyAirLeft(boolean value);

  void setAttackerOrderOfLosses(String attackerOrderOfLosses);

  void setDefenderOrderOfLosses(String defenderOrderOfLosses);

  void cancel();

  void shutdown();
}
