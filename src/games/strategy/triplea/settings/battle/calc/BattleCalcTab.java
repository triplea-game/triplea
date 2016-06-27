package games.strategy.triplea.settings.battle.calc;

import java.util.Arrays;
import java.util.List;

import javax.swing.JTextField;

import games.strategy.engine.ClientContext;
import games.strategy.triplea.settings.InputValidator;
import games.strategy.triplea.settings.SettingInputComponent;
import games.strategy.triplea.settings.SettingsTab;

public class BattleCalcTab implements SettingsTab<BattleCalcSettings> {

  private final List<SettingInputComponent<BattleCalcSettings>> inputs;

  private static String CALC_DESCRIPTION = "(1 - 10000) Default simulation count for the battle calculator";

  public BattleCalcTab(final BattleCalcSettings battleCalcSettings) {

    inputs = Arrays.asList(
        SettingInputComponent.build("Default Dice Run Count",
            CALC_DESCRIPTION + " (dice games)",
            new JTextField(String.valueOf(battleCalcSettings.getSimulationCountDice()), 5),
            ((settings, s) -> settings.setSimulationCountDice(s)),
            (settings -> String.valueOf(settings.getSimulationCountDice())),
            InputValidator.inRange(1, 10000)
        ),

        SettingInputComponent.build("Default Low Luck Run Count",
            CALC_DESCRIPTION + " (low luck games)",
            new JTextField(String.valueOf(battleCalcSettings.getSimulationCountLowLuck()), 5),
            ((settings, s) -> settings.setSimulationCountLowLuck(s)),
            (settings -> String.valueOf(settings.getSimulationCountLowLuck())),
            InputValidator.inRange(1, 10000)
        )
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
