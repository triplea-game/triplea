package games.strategy.triplea.settings;

import java.util.Optional;
import java.util.function.Consumer;

import javax.annotation.Nullable;

/**
 * Basic save/read API for a 'game setting'. Generally these will be values the user can modify over the
 * course of the game, either directly or indirectly. For example, default window size may saved here,
 * which could be set from a UI control or it could be based on the last window size used.
 *
 * @param <T> The type of the setting value.
 */
public interface GameSetting<T> {
  /**
   * Returns {@code true} if the setting has a value (either user-defined or the default).
   */
  boolean isSet();

  /**
   * Sets the current value of the setting using an untyped value.
   *
   * @param newValue The new setting value or {@code null} to clear the setting value. Clearing the setting value will
   *        result in the default value being returned on future reads of the setting value. If no default value is
   *        defined for the setting, future reads will return an empty result.
   *
   * @throws ClassCastException If the type of of {@code newValue} is incompatible with the setting value type.
   */
  void saveObject(@Nullable Object newValue);

  /**
   * Sets the current value of the setting.
   *
   * @param newValue The new setting value or {@code null} to clear the setting value. Clearing the setting value will
   *        result in the default value being returned on future reads of the setting value. If no default value is
   *        defined for the setting, future reads will return an empty result.
   */
  void save(@Nullable T newValue);

  /**
   * Returns the current value of the setting or the default value if the setting has no current value.
   *
   * @throws java.util.NoSuchElementException If the setting has no current or default value.
   */
  default T value() {
    return getValue().get();
  }

  /**
   * Returns the default value of the setting or empty if the setting has no default value.
   */
  Optional<T> getDefaultValue();

  /**
   * Returns the current value of the setting, the default value if the setting has no current value, or empty if the
   * setting has no current or default value.
   */
  Optional<T> getValue();

  /**
   * Resets the setting to its default value or empty if it has no default value.
   */
  void resetValue();

  void addSaveListener(Consumer<String> listener);

  void removeSaveListener(Consumer<String> listener);
}
