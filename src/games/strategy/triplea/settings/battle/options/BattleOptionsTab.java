package games.strategy.triplea.settings.battle.options;

import java.util.Arrays;
import java.util.List;

import javax.swing.JRadioButton;

import games.strategy.engine.ClientContext;
import games.strategy.triplea.settings.SettingInputComponent;
import games.strategy.triplea.settings.SettingsTab;
import games.strategy.ui.SwingComponents;

public class BattleOptionsTab implements SettingsTab<BattleOptionsSettings> {
  private final List<SettingInputComponent<BattleOptionsSettings>> inputs;

  public BattleOptionsTab(final BattleOptionsSettings battleOptionSettings) {
    final JRadioButton radioButtonYes = new JRadioButton("Yes");
    final JRadioButton radioButtonNo = new JRadioButton("No");
    SwingComponents.createButtonGroup(radioButtonYes, radioButtonNo);

    final JRadioButton confirmDefensiveRollsYes = new JRadioButton("Yes");
    final JRadioButton confirmDefensiveRollsNo = new JRadioButton("No");
    SwingComponents.createButtonGroup(confirmDefensiveRollsYes, confirmDefensiveRollsNo);

    inputs = Arrays.asList(
        SettingInputComponent.buildYesOrNoRadioButtons("Confirm Enemy Casualties",
            "When set to yes, enemy casualty selections will always require confirmation.",
            battleOptionSettings.confirmEnemyCasualties(),
            ((settings, s) -> settings.setConfirmEnemyCasualties(Boolean.valueOf(s))),
            (settings -> String.valueOf(settings.confirmEnemyCasualties()))),
        SettingInputComponent.buildYesOrNoRadioButtons("Confirm Defensive Rolls",
            "When set to yes, defender dice rolls will always require confirmation.",
            battleOptionSettings.confirmDefensiveRolls(),
            ((settings, s) -> settings.setConfirmDefensiveRolls(Boolean.valueOf(s))),
            (settings -> String.valueOf(settings.confirmDefensiveRolls()))),
        SettingInputComponent.buildYesOrNoRadioButtons("Focus on own casualties",
            "When set to yes, the default casualty selection can be accepted by pressing space bar. When set to 'no', "
                + "you will always have to click a button to confirm casualty selections.",
            battleOptionSettings.focusOnOwnCasualties(),
            ((settings, s) -> settings.setFocusOnOwnCasualties(Boolean.valueOf(s))),
            (settings -> String.valueOf(settings.focusOnOwnCasualties()))));
  }

  @Override
  public String getTabTitle() {
    return "Combat Options";
  }

  @Override
  public List<SettingInputComponent<BattleOptionsSettings>> getInputs() {
    return inputs;
  }

  @Override
  public BattleOptionsSettings getSettingsObject() {
    return ClientContext.battleOptionsSettings();
  }
}
