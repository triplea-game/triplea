package games.strategy.triplea.settings.battle.calc;

import java.util.Arrays;
import java.util.List;

import javax.swing.JTextField;

import games.strategy.engine.ClientContext;
import games.strategy.triplea.settings.IntegerValueRange;
import games.strategy.triplea.settings.SettingInputComponent;
import games.strategy.triplea.settings.SettingsTab;

public class BattleCalcTab implements SettingsTab<BattleCalcSettings> {

  private final List<SettingInputComponent<BattleCalcSettings>> inputs;

  private static String CALC_DESCRIPTION = "Default simulation count for the battle calculator";

  public BattleCalcTab(final BattleCalcSettings battleCalcSettings) {

    inputs = Arrays.asList(
        SettingInputComponent.build(
            new IntegerValueRange(1, 10000, BattleCalcSettings.DEFAULT_SIMULATION_COUNT_DICE),
            "Default Dice Run Count",
            CALC_DESCRIPTION + " (dice games)",
            new JTextField(String.valueOf(battleCalcSettings.getSimulationCountDice()), 5),
            ((settings, s) -> settings.setSimulationCountDice(s)),
            (settings -> String.valueOf(settings.getSimulationCountDice()))),

        SettingInputComponent.build(
            new IntegerValueRange(1, 10000, BattleCalcSettings.DEFAULT_SIMULATION_COUNT_LOW_LUCK),
            "Default Low Luck Run Count",
            CALC_DESCRIPTION + " (low luck games)",
            new JTextField(String.valueOf(battleCalcSettings.getSimulationCountLowLuck()), 5),
            ((settings, s) -> settings.setSimulationCountLowLuck(s)),
            (settings -> String.valueOf(settings.getSimulationCountLowLuck())))
    );
  }

  @Override
  public String getTabTitle() {
    return "Battle Calculator";
  }

  @Override
  public List<SettingInputComponent<BattleCalcSettings>> getInputs() {
    return inputs;

  }

  @Override
  public BattleCalcSettings getSettingsObject() {
    return ClientContext.battleCalcSettings();
  }
}
