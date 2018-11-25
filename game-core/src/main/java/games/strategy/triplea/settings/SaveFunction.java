package games.strategy.triplea.settings;

import java.util.Objects;

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
      final SelectionComponent.SaveContext context = new SelectionComponent.SaveContext() {
        private final boolean selectionComponentValid = selectionComponent.isValid();

        @Override
        public <T> void setValue(final GameSetting<T> gameSetting, final T value) {
          if (selectionComponentValid) {
            if (doesNewSettingDiffer(gameSetting, value)) {
              gameSetting.setValue(value);
              successMsg.append(String.format("%s was updated to: %s\n", gameSetting, gameSetting.getDisplayValue()));
            }
          } else {
            failMsg.append(String.format("Could not set %s to %s, %s\n",
                gameSetting, value, selectionComponent.validValueDescription()));
          }
        }
      };
      selectionComponent.save(context);
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
    return !Objects.deepEquals(setting.getValue().orElse(null), newValue);
  }

  @AllArgsConstructor
  static final class SaveResult {
    final String message;
    final int dialogType;
  }
}
