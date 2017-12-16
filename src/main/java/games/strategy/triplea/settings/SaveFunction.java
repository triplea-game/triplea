package games.strategy.triplea.settings;

import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JOptionPane;

/**
 * Executes a 'save' action
 * <p>
 * Input: set of settings
 * Output: SaveResult with message and dialog code, which can be used to create a confirmation or a warning dialog.
 * Side effects: value of each setting is read from UI, validated, and valid values are persisted to system settings
 * </p>
 */
interface SaveFunction {

  /**
   * Returns a result message after persisting settings.
   */
  static SaveResult saveSettings(
      final Iterable<? extends GameSettingUiBinding<JComponent>> settings,
      final Runnable settingsFlushToDisk) {
    final StringBuilder successMsg = new StringBuilder();
    final StringBuilder failMsg = new StringBuilder();

    // save all the values, save stuff that is valid and that was updated
    settings.forEach(setting -> {
      if (setting.isValid()) {
        // read and save all settings
        setting.readValues()
            .entrySet()
            .stream()
            .filter(entry -> !entry.getKey().value().equals(entry.getValue()))
            .forEach(entry -> {
              entry.getKey().save(entry.getValue());
              successMsg.append(String.format("%s was updated to: %s\n", entry.getKey(), entry.getValue()));
            });
      } else {
        final Map<GameSetting, String> values = setting.readValues();
        values.forEach((entry, value) -> failMsg.append(String.format("Could not set %s to %s, %s\n",
            setting.getTitle(), value, setting.validValueDescription())));
      }
    });

    final String success = successMsg.toString();
    if (!success.isEmpty()) {
      settingsFlushToDisk.run();
    }

    final String fail = failMsg.toString();


    if (success.isEmpty() && fail.isEmpty()) {
      return new SaveResult("No changes saved", JOptionPane.WARNING_MESSAGE);
    }
    if (!success.isEmpty() && !fail.isEmpty()) {
      return new SaveResult("Some changes were not saved!:\n\nSaved:\n"
          + success + "\n\nFailed to save:\n" + fail, JOptionPane.WARNING_MESSAGE);
    }

    if (success.isEmpty()) {
      return new SaveResult(fail, JOptionPane.WARNING_MESSAGE);
    }
    return new SaveResult(success, JOptionPane.INFORMATION_MESSAGE);
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
