package games.strategy.triplea.settings;

import java.util.Map;

import javax.swing.JComponent;

/**
 * A SelectionComponent represents a UI component that a user can use to update the value of a ClientSetting.
 * Instances of this type are created in: {@code SelectionComponentFactory}
 */
interface SelectionComponent {
  JComponent getJComponent();

  boolean isValid();

  String validValueDescription();

  /**
   * Reads values stored in the UI components, returns a map of preference keys and the value represented in
   * the corresponding UI component.
   */
  Map<GameSetting, String> readValues();

  /**
   * UI component should update to show an error, eg: background turn red.
   */
  void indicateError();

  /**
   * UI component should revert back to a normal state, clearing any changes from {@code indicateError}.
   */
  void clearError();

  void resetToDefault();

  void reset();
}
