package games.strategy.triplea.ui.settings;

import java.util.List;

public interface SettingsTab<T>  {
  String getTabTitle();
  List<SettingInputComponent> getInputs();
  void updateSettings(List<SettingInputComponent> inputs);
}
