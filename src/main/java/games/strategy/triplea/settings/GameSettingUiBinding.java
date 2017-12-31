package games.strategy.triplea.settings;

import java.util.Map;

/**
 * Interface for UI components used to update ClientSetting values. Since the components can get their
 * initial value from the ClientSetting they represent, the only 'write' operation is 'resetToDefault'.
 * The rest of the operations here are essentially 'read' operations.
 *
 * @param <T> The Type of the underlying UI Component
 */
public interface GameSettingUiBinding<T> {
  /**
   * Builds or rebuilds the underlying UI component from current settings.
   */
  T buildSelectionComponent();

  /**
   * Reads the value set in the UI and return true if it is valid, false otherwise.
   */
  boolean isValid();

  /**
   * Reads values from UI components, returns them in a map of Setting -> value.
   */
  Map<GameSetting, String> readValues();

  /**
   * @return Helpful description message of what values are valid.
   */
  String validValueDescription();

  /**
   * Reset any settings that are bound, and reset the UI.
   */
  void reset();

  /**
   * The title describing the setting that can be updated in 2 or 3 words. The space for this value is very
   * limited.
   *
   * @return The value displayed to user giving a setting component a 'title', to let the user know which value
   *         is updated by which control.
   */
  String getTitle();

  void resetToDefault();
}
