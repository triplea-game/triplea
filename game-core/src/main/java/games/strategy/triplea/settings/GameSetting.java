package games.strategy.triplea.settings;

import java.util.function.Consumer;

import javax.annotation.Nullable;

/**
 * Basic save/read API for a 'game setting'. Generally these will be values the user can modify over the
 * course of the game, either directly or indirectly. For example, default window size may saved here,
 * which could be set from a UI control or it could be based on the last window size used.
 *
 * <p>
 * All settings are typed but have an underlying string representation. Thus, implementations must provide methods for
 * converting a setting value between its typed form and its string form. While clients may use the
 * {@link #stringValue()} and {@link #saveString(String)} methods to load and save a setting value, respectively, one
 * should prefer the typed {@link #value()} and {@link #save(Object)} methods whenever possible to ensure type safety.
 * </p>
 *
 * @param <T> The type of the setting value.
 */
public interface GameSetting<T> {
  /**
   * Return true if the setting has been specified by the user or updated from default.
   */
  boolean isSet();

  /**
   * Queues a new string value for the setting, may not persist right away.
   *
   * @param newValue The new setting value or {@code null} to clear the setting value. Clearing the setting value will
   *        result in the default value being returned on future reads of the setting value.
   */
  void saveString(@Nullable String newValue);

  /**
   * Queues a new typed value for the setting, may not persist right away.
   *
   * @param newValue The new setting value or {@code null} to clear the setting value. Clearing the setting value will
   *        result in the default value being returned on future reads of the setting value.
   */
  void save(@Nullable T newValue);

  /**
   * Returns the current persisted string value of the setting.
   */
  String stringValue();

  /**
   * Returns the current persisted typed value of the setting.
   */
  T value();

  void resetAndFlush();

  void addSaveListener(Consumer<String> listener);

  void removeSaveListener(Consumer<String> listener);
}
