package games.strategy.triplea.ui.settings.scrolling;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

import games.strategy.triplea.ui.settings.SettingInputComponent;
import games.strategy.triplea.ui.settings.SettingsTab;

public class ScrollSettingsTab implements SettingsTab<ScrollSettings> {

  private final ScrollSettings settings;

  public ScrollSettingsTab(ScrollSettings settings) {
    this.settings = settings;

    arrowScrollSpeed = new JTextField(String.valueOf(settings.getArrowKeyScrollSpeed()), 5 );
    arrowScrollSpeedMultiplier = new JTextField(String.valueOf(settings.getFasterArrowKeyScrollMultipler()), 5);
  }

  @Override
  public String getTabTitle() {
    return "Scrolling";
  }

  private final JTextField arrowScrollSpeed;
  private final JTextField arrowScrollSpeedMultiplier;


  @Override
  public List<SettingInputComponent> getInputs() {
    BiConsumer<ScrollSettings, String> arrowKeyUpdater = ((scrollSettings, s) -> scrollSettings.setArrowKeyScrollSpeed(s));
    BiConsumer<ScrollSettings, String> arrowKeyMultiplierUpdater = ((scrollSettings, s) -> scrollSettings.setFasterArrowKeyScrollMultipler(s));
    return Arrays.asList(
        buildSettingsComponents("Arrow scroll speed", "Arrow key scrolling speed", arrowScrollSpeed, arrowKeyUpdater),
        buildSettingsComponents("Arrow scroll speed multiplier", "Arrow key scroll speed increase when control is held down", arrowScrollSpeedMultiplier, arrowKeyMultiplierUpdater)
    );
  }

  @Override
  public void updateSettings(List<SettingInputComponent> inputs) {
    inputs.forEach(input -> input.updateSettings(settings, input.getInputElement()));
  }


  private static SettingInputComponent<ScrollSettings> buildSettingsComponents(final String label,
      final String description, JTextComponent inputComponent, BiConsumer<ScrollSettings, String> updater) {
    return new SettingInputComponent<ScrollSettings>() {
      @Override
      public String getLabel() {
        return label;
      }

      @Override
      public String getDescription() {
        return description;
      }

      @Override
      public JTextComponent getInputElement() {
        return inputComponent;
      }


      @Override
      public void updateSettings(ScrollSettings toUpdate, JTextComponent inputComponent) {
        updater.accept(toUpdate, inputComponent.getText());
      }

    };
  }
}
