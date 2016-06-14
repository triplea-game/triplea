package games.strategy.triplea.settings.ai;

import games.strategy.engine.ClientContext;
import games.strategy.triplea.settings.SettingInputComponent;
import games.strategy.triplea.settings.SettingsTab;
import games.strategy.triplea.settings.InputValidator;

import javax.swing.JTextField;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class AiTab implements SettingsTab<AiSettings> {

  private final AiSettings aiSettings;

  public AiTab(AiSettings aiSettings) {
    this.aiSettings = aiSettings;
  }

  @Override
  public String getTabTitle() {
    return "AI";
  }

  @Override
  public List<SettingInputComponent<AiSettings>> getInputs() {

    BiConsumer<AiSettings,String> aiPauseDuractionUpdater = ((aiSettings, s) -> aiSettings.setAiPauseDuration(s));
    Function<AiSettings,String> aiPauseDuractionExtractor = aiSettings -> String.valueOf(aiSettings.getAiPauseDuration());

    return Arrays.asList(
        SettingInputComponent.build("(0-10000) AI Pause Duration", "Time delay between AI moves, allows for AI moves to be watched",
        new JTextField(String.valueOf(aiSettings.getAiPauseDuration()), 5 ), aiPauseDuractionUpdater, aiPauseDuractionExtractor,
            InputValidator.inRange(0, 10000)
        )
    );
  }

  @Override
  public AiSettings getSettingsObject() {
    return ClientContext.aiSettings();
  }
}
