package games.strategy.triplea.settings;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import games.strategy.triplea.settings.SelectionComponent.SaveContext.ValueSensitivity;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import lombok.AllArgsConstructor;

/**
 * Executes a 'save' action.
 *
 * <p>Input: set of settings Output: SaveResult with message and dialog code, which can be used to
 * create a confirmation or a warning dialog. Side effects: value of each setting is read from UI,
 * validated, and valid values are persisted to system settings
 */
final class SaveFunction {
  private SaveFunction() {}

  /** Returns a result message after persisting settings. */
  static SaveResult saveSettings(
      final Iterable<? extends SelectionComponent<JComponent>> selectionComponents,
      final Runnable settingsFlushToDisk) {
    final StringBuilder successMsg = new StringBuilder();
    final StringBuilder failMsg = new StringBuilder();

    // save all the values, save stuff that is valid and that was updated
    selectionComponents.forEach(
        selectionComponent -> {
          final SelectionComponent.SaveContext context =
              new SelectionComponent.SaveContext() {
                @Override
                public void reportError(
                    final GameSetting<?> gameSetting,
                    final String message,
                    final @Nullable Object value,
                    final ValueSensitivity valueSensitivity) {
                  failMsg.append(
                      String.format(
                          "Could not set %s to %s (%s)\n",
                          gameSetting,
                          toDisplayString(
                              value, gameSetting.getDefaultValue().orElse(null), valueSensitivity),
                          message));
                }

                @Override
                public <T> void setValue(
                    final GameSetting<T> gameSetting,
                    final @Nullable T value,
                    final ValueSensitivity valueSensitivity) {
                  if (doesNewSettingValueDiffer(gameSetting, value)) {
                    gameSetting.setValue(value);
                    successMsg.append(
                        String.format(
                            "%s was updated to %s\n",
                            gameSetting,
                            toDisplayString(
                                value,
                                gameSetting.getDefaultValue().orElse(null),
                                valueSensitivity)));
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
      return new SaveResult(
          "Some changes were not saved!:\n\nSaved:\n" + success + "\n\nFailed to save:\n" + fail,
          JOptionPane.WARNING_MESSAGE);
    }

    if (success.isEmpty()) {
      return new SaveResult(fail, JOptionPane.WARNING_MESSAGE);
    }
    return new SaveResult(success, JOptionPane.INFORMATION_MESSAGE);
  }

  private static boolean doesNewSettingValueDiffer(
      final GameSetting<?> setting, final @Nullable Object newValue) {
    return !Objects.deepEquals(setting.getValue().orElse(null), newValue);
  }

  @VisibleForTesting
  static String toDisplayString(
      final @Nullable Object value,
      final @Nullable Object defaultValue,
      final ValueSensitivity valueSensitivity) {
    if ((value == null) || Objects.deepEquals(value, defaultValue)) {
      return (defaultValue != null)
          ? ("<default> (" + toDisplayString(defaultValue, null, valueSensitivity) + ")")
          : "<unset>";
    } else if (value instanceof String) {
      return toDisplayString((String) value, valueSensitivity);
    } else if (value instanceof char[]) {
      return toDisplayString((char[]) value, valueSensitivity);
    }

    return toDisplayString(Objects.toString(value), valueSensitivity);
  }

  private static String toDisplayString(
      final String value, final ValueSensitivity valueSensitivity) {
    return (valueSensitivity == ValueSensitivity.SENSITIVE) ? mask(value.length()) : value;
  }

  private static String toDisplayString(
      final char[] value, final ValueSensitivity valueSensitivity) {
    return (valueSensitivity == ValueSensitivity.SENSITIVE)
        ? mask(value.length)
        : new String(value);
  }

  private static String mask(final int length) {
    return Strings.repeat("*", length);
  }

  @AllArgsConstructor
  static final class SaveResult {
    final String message;
    final int dialogType;
  }
}
