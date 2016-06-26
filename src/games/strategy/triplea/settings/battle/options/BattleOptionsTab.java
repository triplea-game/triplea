package games.strategy.triplea.settings.battle.options;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import javax.swing.JPanel;
import javax.swing.JRadioButton;

import games.strategy.engine.ClientContext;
import games.strategy.triplea.settings.SettingInputComponent;
import games.strategy.triplea.settings.SettingsTab;
import games.strategy.ui.SwingComponents;

public class BattleOptionsTab implements SettingsTab<BattleOptionsSettings> {
  private List<SettingInputComponent<BattleOptionsSettings>> inputs;

  private JRadioButton radioButtonYes = new JRadioButton("Yes");
  private JRadioButton radioButtonNo = new JRadioButton("No");

  public BattleOptionsTab(final BattleOptionsSettings battleOptionSettings) {
    SwingComponents.createButtonGroup(radioButtonYes, radioButtonNo);

    inputs = Arrays.asList(
        SettingInputComponent.build("Confirm Enemy Casualties",
            "When set to yes, will require confirmaton of enemy casualty selections",
            createConfirmEnemyCasualtiesButtonPanel(battleOptionSettings),
            buildConfirmEnemyCasualtiesRadioButtonReader(),
            ((settings, s) -> settings.setConfirmEnemyCasualties(Boolean.valueOf(s))),
            (settings -> String.valueOf(settings.confirmEnemyCasualties()))));
  }

  private JPanel createConfirmEnemyCasualtiesButtonPanel(final BattleOptionsSettings battleCalcSettings) {
    if (battleCalcSettings.confirmEnemyCasualties()) {
      radioButtonYes.setSelected(true);
    } else {
      radioButtonNo.setSelected(true);
    }
    JPanel panel = new JPanel();
    panel.add(radioButtonYes);
    panel.add(radioButtonNo);
    return panel;
  }

  private Supplier<String> buildConfirmEnemyCasualtiesRadioButtonReader() {
    return () -> radioButtonYes.isSelected() ? Boolean.TRUE.toString() : Boolean.FALSE.toString();
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
