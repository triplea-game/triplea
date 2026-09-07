package games.strategy.triplea.odds.calculator;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

/**
 * The odds-calculator surface both the concurrent old-path calculator and the bounded-context
 * calculator satisfy, so a caller can hold either behind this type and let a factory pick the
 * implementation from {@link
 * games.strategy.triplea.settings.ClientSetting#useBoundedContextBattleCalc}.
 */
public interface IBattleCalculator {
  AggregateResults calculate(
      GamePlayer attacker,
      GamePlayer defender,
      Territory location,
      Collection<Unit> attacking,
      Collection<Unit> defending,
      Collection<Unit> bombarding,
      Collection<TerritoryEffect> territoryEffects,
      boolean retreatWhenOnlyAirLeft,
      int runCount);

  /**
   * Binds the calculator to the game to simulate; the returned future completes once it is ready to
   * {@link #calculate}. A {@code null} data clears the binding.
   */
  CompletableFuture<Boolean> setGameData(@Nullable GameData data);

  void cancel();

  void setKeepOneAttackingLandUnit(boolean value);

  void setAmphibious(boolean value);

  void setRetreatAfterRound(int value);

  void setRetreatAfterXUnitsLeft(int value);

  void setAttackerOrderOfLosses(String value);

  void setDefenderOrderOfLosses(String value);
}
