package games.strategy.triplea.settings;

/**
 * Interface for UI components used to update ClientSetting values. Since the components can get
 * their initial value from the ClientSetting they represent, the only 'write' operation is
 * 'resetToDefault'. The rest of the operations here are essentially 'read' operations.
 *
 * @param <T> The Type of the underlying UI Component
 */
public interface GameSettingUiBinding<T> {
  /** Returns a new selection component for the setting. */
  SelectionComponent<T> newSelectionComponent();

  /**
   * The title describing the setting that can be updated in 2 or 3 words. The space for this value
   * is very limited.
   *
   * @return The value displayed to user giving a setting component a 'title', to let the user know
   *     which value is updated by which control.
   */
  String getTitle();

  /** Returns the setting type used to group related settings in the UI. */
  SettingType getType();
}
