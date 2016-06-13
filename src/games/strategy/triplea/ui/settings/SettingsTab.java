package games.strategy.triplea.ui.settings;

import java.util.List;

public interface SettingsTab<T>  {
  String getTabTitle();
  List<SettingInputComponent<T>> getInputs();

  default void updateSettings(List<SettingInputComponent<T>> inputs) {
      inputs.forEach(input -> input.updateSettings(getSettingsObject(), input.getInputElement()));
  }

  T getSettingsObject();
}
