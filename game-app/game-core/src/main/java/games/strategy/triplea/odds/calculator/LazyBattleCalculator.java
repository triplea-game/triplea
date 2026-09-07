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
 * Defers picking the concrete odds calculator until the first call, so holding one costs nothing.
 * The choice reads {@link
 * games.strategy.triplea.settings.ClientSetting#useBoundedContextBattleCalc}, which is not
 * initialized in every context that merely <em>constructs</em> an AI (eg: player-type unit tests);
 * resolving eagerly at construction would make that read explode. Selection therefore happens on
 * the first method call, by which point a running game has initialized the setting.
 */
public class LazyBattleCalculator implements IBattleCalculator {
  @Nullable private volatile IBattleCalculator delegate;

  private IBattleCalculator delegate() {
    IBattleCalculator resolved = delegate;
    if (resolved == null) {
      synchronized (this) {
        resolved = delegate;
        if (resolved == null) {
          resolved = BattleCalculatorFactory.newBattleCalculator();
          delegate = resolved;
        }
      }
    }
    return resolved;
  }

  @Override
  public AggregateResults calculate(
      final GamePlayer attacker,
      final GamePlayer defender,
      final Territory location,
      final Collection<Unit> attacking,
      final Collection<Unit> defending,
      final Collection<Unit> bombarding,
      final Collection<TerritoryEffect> territoryEffects,
      final boolean retreatWhenOnlyAirLeft,
      final int runCount) {
    return delegate()
        .calculate(
            attacker,
            defender,
            location,
            attacking,
            defending,
            bombarding,
            territoryEffects,
            retreatWhenOnlyAirLeft,
            runCount);
  }

  @Override
  public CompletableFuture<Boolean> setGameData(@Nullable final GameData data) {
    return delegate().setGameData(data);
  }

  @Override
  public void cancel() {
    delegate().cancel();
  }

  @Override
  public void setKeepOneAttackingLandUnit(final boolean value) {
    delegate().setKeepOneAttackingLandUnit(value);
  }

  @Override
  public void setAmphibious(final boolean value) {
    delegate().setAmphibious(value);
  }

  @Override
  public void setRetreatAfterRound(final int value) {
    delegate().setRetreatAfterRound(value);
  }

  @Override
  public void setRetreatAfterXUnitsLeft(final int value) {
    delegate().setRetreatAfterXUnitsLeft(value);
  }

  @Override
  public void setAttackerOrderOfLosses(final String value) {
    delegate().setAttackerOrderOfLosses(value);
  }

  @Override
  public void setDefenderOrderOfLosses(final String value) {
    delegate().setDefenderOrderOfLosses(value);
  }
}
