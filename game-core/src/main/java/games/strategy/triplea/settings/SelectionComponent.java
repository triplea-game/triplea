package games.strategy.triplea.settings;

import javax.annotation.Nullable;

/**
 * A SelectionComponent represents a UI component that a user can use to update the value of a ClientSetting.
 * Instances of this type are created in: {@code SelectionComponentFactory}
 *
 * @param <T> The Type of the underlying UI Component
 */
public interface SelectionComponent<T> {
  T getUiComponent();

  /**
   * Reads the value set in the UI and return true if it is valid, false otherwise.
   */
  boolean isValid();

  String validValueDescription();

  /**
   * Saves values stored in the UI components to the underlying game settings.
   */
  void save(SaveContext context);

  void resetToDefault();

  /**
   * Reset any settings that are bound, and reset the UI.
   */
  void reset();

  /**
   * The execution context for the {@link SelectionComponent#save(SaveContext)} method.
   */
  interface SaveContext {
    /**
     * Sets the value for the specified game setting. Selection components must call this method to change a setting
     * value and must not call {@link GameSetting#setValue(Object)} directly.
     *
     * @param gameSetting The game setting whose value is to be set.
     * @param value The new setting value or {@code null} to clear the setting value.
     */
    <T> void setValue(GameSetting<T> gameSetting, @Nullable T value);
  }
}
