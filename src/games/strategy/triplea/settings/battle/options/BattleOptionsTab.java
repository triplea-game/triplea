package games.strategy.triplea.settings.battle.options;

import games.strategy.triplea.settings.InputValidator;
import games.strategy.triplea.settings.SettingInputComponent;
import games.strategy.triplea.settings.SettingsInput;
import games.strategy.triplea.settings.SettingsTab;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class BattleOptionsTab implements SettingsTab<BattleOptionsSettings> {
  private final BattleOptionsSettings battleCalcSettings;

  public BattleOptionsTab(final BattleOptionsSettings battleCalcSettings) {
    this.battleCalcSettings = battleCalcSettings;
  }

  @Override
  public String getTabTitle() {
    return "Battle Calculator";
  }

  @Override
  public List<SettingInputComponent<BattleOptionsSettings>> getInputs() {
    BiConsumer<BattleOptionsSettings, String> diceRunCountUpdater = ((settings, s) -> settings.setConfirmEnemyCasualties(Boolean.valueOf(s)));
    Function<BattleOptionsSettings, String> diceRunCountExtractor = settings -> String.valueOf(settings.confirmEnemyCasualties());


    return Arrays.asList(
        SettingInputComponent.build("Confirm Enemy Casualties", "When set to yes, will require confirmaton of enemy casualty selections",
            buildPanel(), buildReader(),
            diceRunCountUpdater, diceRunCountExtractor,
            InputValidator.inRange(1, 10000)
        )
    );
  }
  JRadioButton radioButtonYes = new JRadioButton("Yes");
  JRadioButton radioButtonNo = new JRadioButton("No");


  private JPanel buildPanel() {
    JPanel panel = new JPanel();

    battleCalcSettings.confirmEnemyCasualties();
    ButtonGroup group = new ButtonGroup();

    group.add(radioButtonNo);
    group.add(radioButtonYes);
    if(battleCalcSettings.confirmEnemyCasualties()) {
      radioButtonYes.setSelected(true);
    } else {
      radioButtonNo.setSelected(true);
    }
    panel.add(radioButtonYes);
    panel.add(radioButtonNo);

    return panel;
  }


  private Supplier<String> buildReader() {
    if(radioButtonYes.isSelected()) {
      return () -> String.valueOf(true);
    } else {
      return () -> String.valueOf(false);
    }
  }


  @Override
  public BattleOptionsSettings getSettingsObject() {
    return battleCalcSettings;
  }

}
