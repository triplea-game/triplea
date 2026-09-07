package games.strategy.triplea.odds.calculator;

import games.strategy.triplea.settings.ClientSetting;

/**
 * Selects the odds-calculator implementation from {@link
 * ClientSetting#useBoundedContextBattleCalc}: the engine-free {@link
 * BoundedContextBattleCalculator} when the flag is on, otherwise the {@link
 * ConcurrentBattleCalculator} clone-per-worker path.
 */
public final class BattleCalculatorFactory {
  private BattleCalculatorFactory() {}

  public static IBattleCalculator newBattleCalculator() {
    return ClientSetting.useBoundedContextBattleCalc.getSetting()
        ? new BoundedContextBattleCalculator()
        : new ConcurrentBattleCalculator();
  }
}
