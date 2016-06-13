package games.strategy.triplea.settings.battle.calc;

import games.strategy.triplea.settings.HasDefaults;
import games.strategy.triplea.settings.PreferenceKey;
import games.strategy.triplea.settings.SystemPreferences;

public class BattleCalcSettings implements HasDefaults {
  private static final int DEFAULT_SIMULATION_COUNT_DICE = 2000;
  private static final int DEFAULT_SIMULATION_COUNT_LOW_LUCK = 500;

  @Override
  public void setToDefault() {
    setSimulationCountDice(String.valueOf(DEFAULT_SIMULATION_COUNT_DICE));
  }

  public int getSimulationCountDice() {
    return SystemPreferences.get(PreferenceKey.BATTLE_CALC_SIMULATION_COUNT_DICE, DEFAULT_SIMULATION_COUNT_DICE);
  }

  public void setSimulationCountDice(String value) {
    SystemPreferences.put(PreferenceKey.BATTLE_CALC_SIMULATION_COUNT_DICE, value);
  }

  public int getSimulationCountLowLuck() {
    return SystemPreferences.get(PreferenceKey.BATTLE_CALC_SIMULATION_COUNT_LOW_LUCK, DEFAULT_SIMULATION_COUNT_LOW_LUCK);
  }

  public void setSimulationCountLowLuck(String value) {
    SystemPreferences.put(PreferenceKey.BATTLE_CALC_SIMULATION_COUNT_LOW_LUCK, value);
  }
}
