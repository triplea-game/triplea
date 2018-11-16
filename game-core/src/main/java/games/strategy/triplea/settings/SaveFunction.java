package games.strategy.triplea.settings;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;
import javax.swing.JComponent;
import javax.swing.JOptionPane;

import lombok.AllArgsConstructor;

/**
 * Executes a 'save' action.
 * <p>
 * Input: set of settings
 * Output: SaveResult with message and dialog code, which can be used to create a confirmation or a warning dialog.
 * Side effects: value of each setting is read from UI, validated, and valid values are persisted to system settings
 * </p>
 */
final class SaveFunction {
  private SaveFunction() {}

  /**
   * Returns a result message after persisting settings.
   */
  static SaveResult saveSettings(
      final Iterable<? extends SelectionComponent<JComponent>> selectionComponents,
      final Runnable settingsFlushToDisk) {
    final StringBuilder successMsg = new StringBuilder();
    final StringBuilder failMsg = new StringBuilder();

    // save all the values, save stuff that is valid and that was updated
    selectionComponents.forEach(selectionComponent -> {
      if (selectionComponent.isValid()) {
        // read and save all settings
        selectionComponent.readValues()
            .entrySet()
            .stream()
            .filter(entry -> doesNewSettingDiffer(entry.getKey(), entry.getValue()))
            .forEach(entry -> {
              final GameSetting<?> setting = entry.getKey();
              setting.setObjectValue(entry.getValue());
              successMsg.append(String
                  .format("%s was updated to: %s\n", setting, setting.getDisplayValue()));
            });
      } else {
        selectionComponent.readValues().forEach((key, value) -> failMsg.append(
            String.format("Could not set %s to %s, %s\n", key, value, selectionComponent.validValueDescription())));
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

  private static boolean doesNewSettingDiffer(final GameSetting<?> setting, final Object newValue) {
    final @Nullable Object oldValue = setting.getValue().orElse(null);
    if (oldValue instanceof char[] && newValue instanceof char[]) {
      return !Arrays.equals((char[]) oldValue, (char[]) newValue);
    }
    return !Objects.equals(oldValue, newValue);
  }

  @AllArgsConstructor
  static final class SaveResult {
    final String message;
    final int dialogType;
  }
}
