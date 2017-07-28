package games.strategy.triplea.settings;

import java.util.List;

public class ResetFunction {
  public void resetSettings(final List<ClientSettingUiBinding> settings) {
    settings.forEach(ClientSettingUiBinding::resetToDefault);
    ClientSetting.flush();
  }
}
