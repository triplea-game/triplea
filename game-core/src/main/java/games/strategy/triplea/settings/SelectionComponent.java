package games.strategy.triplea.settings;

import java.util.Map;

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
   * Reads values stored in the UI components, returns a map of preference keys and the value represented in
   * the corresponding UI component. A {@code null} value in the map indicates the value should be reset to its default
   * value.
   */
  Map<GameSetting<?>, /* @Nullable */ Object> readValues();

  void resetToDefault();

  /**
   * Reset any settings that are bound, and reset the UI.
   */
  void reset();
}
