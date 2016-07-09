package games.strategy.triplea.settings.ai;

import java.util.Arrays;
import java.util.List;

import javax.swing.JTextField;

import games.strategy.engine.ClientContext;
import games.strategy.triplea.settings.IntegerValueRange;
import games.strategy.triplea.settings.SettingInputComponent;
import games.strategy.triplea.settings.SettingsTab;

public class AiTab implements SettingsTab<AiSettings> {

  private final List<SettingInputComponent<AiSettings>> inputs;

  public AiTab(AiSettings aiSettings) {
    inputs = Arrays.asList(
        SettingInputComponent.build(
            new IntegerValueRange(0, 10000, AiSettings.DEFAULT_AI_PAUSE_DURACTION),
            "AI Pause Duration",
            "Time delay (in milliseconds) between AI moves, allows for the AI moves to be watched",
            new JTextField(String.valueOf(aiSettings.getAiPauseDuration()), 5),
            ((settings, s) -> settings.setAiPauseDuration(s)),
            (settings -> String.valueOf(aiSettings.getAiPauseDuration()))),
        SettingInputComponent.buildYesOrNoRadioButtons("Show Battles Between AIs",
            "When set to yes, combats between two AI players will be shown in a battle window.",
            aiSettings.showBattlesBetweenAi(),
            ((settings, s) -> settings.setShowBattlesBetweenAi(Boolean.valueOf(s))),
            (settings -> String.valueOf(settings.showBattlesBetweenAi()))));
  }

  @Override
  public String getTabTitle() {
    return "AI";
  }

  @Override
  public List<SettingInputComponent<AiSettings>> getInputs() {
    return inputs;
  }

  @Override
  public AiSettings getSettingsObject() {
    return ClientContext.aiSettings();
  }
}
