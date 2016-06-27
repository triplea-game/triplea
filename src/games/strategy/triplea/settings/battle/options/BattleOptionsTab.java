package games.strategy.triplea.settings.battle.options;

import java.util.Arrays;
import java.util.List;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JRadioButton;

import games.strategy.engine.ClientContext;
import games.strategy.triplea.settings.SettingInputComponent;
import games.strategy.triplea.settings.SettingsTab;
import games.strategy.triplea.ui.BattleDisplay;
import games.strategy.ui.SwingAction;
import games.strategy.ui.SwingComponents;

public class BattleOptionsTab implements SettingsTab<BattleOptionsSettings> {
  private List<SettingInputComponent<BattleOptionsSettings>> inputs;



  public BattleOptionsTab(final BattleOptionsSettings battleOptionSettings) {
    JRadioButton radioButtonYes = new JRadioButton("Yes");
    JRadioButton radioButtonNo = new JRadioButton("No");
    SwingComponents.createButtonGroup(radioButtonYes, radioButtonNo);

    JRadioButton confirmDefensiveRollsYes = new JRadioButton("Yes");
    JRadioButton confirmDefensiveRollsNo = new JRadioButton("No");
    SwingComponents.createButtonGroup(confirmDefensiveRollsYes, confirmDefensiveRollsNo);


    inputs = Arrays.asList(
        SettingInputComponent.buildYesOrNoRadioButtons("Confirm Enemy Casualties",
            "When set to yes, battles will require confirmaton of enemy casualty selections",
            battleOptionSettings.confirmEnemyCasualties(),
            ((settings, s) -> settings.setConfirmEnemyCasualties(Boolean.valueOf(s))),
            (settings -> String.valueOf(settings.confirmEnemyCasualties()))),
        SettingInputComponent.buildYesOrNoRadioButtons("Confirm Defensive Rolls",
            "When set to yes, results of defensive rolls will need confirmation",
            battleOptionSettings.confirmDefensiveRolls(),
            ((settings, s) -> settings.setConfirmDefensiveRolls(Boolean.valueOf(s))),
            (settings -> String.valueOf(settings.confirmDefensiveRolls()))),
      SettingInputComponent.buildYesOrNoRadioButtons("Focus on own casualties", // TODO: rename to confirm enemy casualties
        "Reduces battle confirmations",
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
