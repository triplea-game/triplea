package games.strategy.triplea.settings.battle.calc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import games.strategy.triplea.settings.SettingInputComponent;
import games.strategy.triplea.settings.SettingsTab;
import games.strategy.triplea.settings.validators.InputValidator;

import javax.swing.JTextField;

public class BattleCalcTab implements SettingsTab<BattleCalcSettings> {
  private final BattleCalcSettings battleCalcSettings;

  public BattleCalcTab(final BattleCalcSettings battleCalcSettings) {
    this.battleCalcSettings = battleCalcSettings;
  }

  @Override
  public String getTabTitle() {
    return "Battle Calculator";
  }



  @Override
  public List<SettingInputComponent<BattleCalcSettings>> getInputs() {

    BiConsumer<BattleCalcSettings, String> diceRunCountUpdater = ((settings, s) -> settings.setSimulationCountDice(s));
    Function<BattleCalcSettings, String> diceRunCountExtractor = settings -> String.valueOf(settings.getSimulationCountDice());

    BiConsumer<BattleCalcSettings, String> lowLuckRunCountUpdater = ((settings, s) -> settings.setSimulationCountLowLuck(s));
    Function<BattleCalcSettings, String> lowLuckRunCountExtractor = settings -> String.valueOf(settings.getSimulationCountLowLuck());

    return Arrays.asList(
        SettingInputComponent.build("Default Dice Run Count", "The default number of times the battle calculator will run in a dice game",
            new JTextField(String.valueOf(battleCalcSettings.getSimulationCountDice()), 5),
            diceRunCountUpdater, diceRunCountExtractor,
            InputValidator.inRange(1, 10000)
        ),

        SettingInputComponent.build("Default Low Luck Run Count", "The default number of times the battle calculator will run in a low luck game",
            new JTextField(String.valueOf(battleCalcSettings.getSimulationCountLowLuck()), 5),
            lowLuckRunCountUpdater, lowLuckRunCountExtractor,
            InputValidator.inRange(1, 10000)
        )
    );
  }

  @Override
  public BattleCalcSettings getSettingsObject() {
    return battleCalcSettings;

  }
}
