package games.strategy.triplea.settings.ai;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

import javax.swing.JTextField;

import games.strategy.engine.ClientContext;
import games.strategy.triplea.settings.IntegerValueRange;
import games.strategy.triplea.settings.SettingInputComponent;
import games.strategy.triplea.settings.SettingInputComponentFactory;
import games.strategy.triplea.settings.SettingsTab;

public class AiTab implements SettingsTab<AiSettings> {

  private final List<SettingInputComponent<AiSettings>> inputs;

  public AiTab(final AiSettings aiSettings) {

    BiConsumer<AiSettings, String> aiSettingsWriter = (settings,s) -> settings.setAiPauseDuration(s);

    inputs = Arrays.asList(
        SettingInputComponentFactory.buildIntegerText(
            new IntegerValueRange(0, 3000, AiSettings.DEFAULT_AI_PAUSE_DURACTION),
            "AI Pause Duration",
            "Time delay (in milliseconds) between AI moves, allows for the AI moves to be watched",
            new JTextField(String.valueOf(aiSettings.getAiPauseDuration()), 5),
            aiSettingsWriter,
            (settings -> String.valueOf(aiSettings.getAiPauseDuration()))),
        SettingInputComponentFactory.buildYesOrNoRadioButtons("Show Battles Between AIs",
            "When set to yes, combats between AI players will be shown in a battle window.",
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
