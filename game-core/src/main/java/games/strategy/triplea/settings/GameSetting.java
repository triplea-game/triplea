package games.strategy.triplea.settings;

import java.util.Optional;
import java.util.function.Consumer;
import javax.annotation.Nullable;

/**
 * Basic save/read API for a 'game setting'. Generally these will be values the user can modify over
 * the course of the game, either directly or indirectly. For example, default window size may saved
 * here, which could be set from a UI control or it could be based on the last window size used.
 *
 * @param <T> The type of the setting value.
 */
public interface GameSetting<T> {
  /** Returns {@code true} if the setting has a value (either user-defined or the default). */
  boolean isSet();

  /**
   * Sets the current value of the setting.
   *
   * @param value The new setting value or {@code null} to clear the setting value. Clearing the
   *     setting value will result in the default value being returned on future reads of the
   *     setting value. If no default value is defined for the setting, future reads will return an
   *     empty result.
   */
  void setValue(@Nullable T value);

  /** Returns the default value of the setting or empty if the setting has no default value. */
  Optional<T> getDefaultValue();

  /**
   * Returns the current value of the setting, the default value if the setting has no current
   * value, or empty if the setting has no current or default value.
   */
  Optional<T> getValue();

  /**
   * Returns the current value of the setting, the default value if the setting has no current
   * value, or throws {@link java.util.NoSuchElementException} if the setting has no current or
   * default value.
   */
  default T getValueOrThrow() {
    return getValue().orElseThrow();
  }

  /** Resets the setting to its default value or empty if it has no default value. */
  void resetValue();

  /**
   * Registers {@code listener} to receive a notification whenever the setting value has changed.
   */
  void addListener(Consumer<GameSetting<T>> listener);

  /**
   * Unregisters {@code listener} to no longer receive a notification whenever the setting value has
   * changed.
   */
  void removeListener(Consumer<GameSetting<T>> listener);
}
