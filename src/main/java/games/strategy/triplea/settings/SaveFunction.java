package games.strategy.triplea.settings;

import java.util.Map;

import javax.swing.JOptionPane;

public class SaveFunction {

  /**
   * Returns a result message after persisting settings.
   */
  static SaveResult saveSettings(final Iterable<ClientSettingUiBinding> settings) {

    final StringBuilder successMsg = new StringBuilder();
    final StringBuilder failMsg = new StringBuilder();

    // save all the values, save stuff that is valid and that was updated
    settings.forEach(setting -> {
      if (setting.isValid()) {
        // read and save all settings
        setting.readValues().forEach((settingKey, settingValue) -> {
          if (!settingKey.value().equals(settingValue)) {
            settingKey.save(settingValue);
            successMsg.append(String.format("%s was updated to: %s\n", setting.title, settingValue));
          }
        });
        ClientSetting.flush();
      } else if (!setting.isValid()) {
        final Map<ClientSetting, String> values = setting.readValues();
        values.forEach((entry, value) -> {
          failMsg.append(String.format("Could not set %s to %s, %s\n",
              setting.title, value, setting.validValueDescription()));
        });
      }
    });

    final String success = successMsg.toString();
    final String fail = failMsg.toString();
    if (success.isEmpty() && fail.isEmpty()) {
      return new SaveResult("No changes to save", JOptionPane.WARNING_MESSAGE);
    } else if (fail.isEmpty()) {
      return new SaveResult(success, JOptionPane.INFORMATION_MESSAGE);
    } else if (success.isEmpty()) {
      return new SaveResult(fail, JOptionPane.WARNING_MESSAGE);
    } else {
      return new SaveResult(success + "\nSome changes were not saved:\n" + fail, JOptionPane.WARNING_MESSAGE);
    }
  }

  static class SaveResult {
    final String message;
    final int dialogType;

    private SaveResult(final String message, final int dialogtype) {
      this.message = message;
      this.dialogType = dialogtype;
    }
  }
}
