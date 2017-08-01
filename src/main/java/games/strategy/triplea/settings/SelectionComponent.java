package games.strategy.triplea.settings;

import java.io.Serializable;
import java.util.Map;

import javax.swing.JComponent;

/**
 * A SelectionComponent represents a UI component that a user can use to update the value of a ClientSetting.
 * Instances of this class are created in: {@code SelectionComponentFactory}
 */
abstract class SelectionComponent implements Serializable {
  private static final long serialVersionUID = -2224094425526210088L;

  abstract JComponent getJComponent();

  abstract boolean isValid();

  abstract String validValueDescription();

  /**
   * Reads values stored in the UI components, returns a map of preference keys and the value represented in
   * the corresponding UI component.
   */
  abstract Map<GameSetting, String> readValues();

  /**
   * UI component should update to show an error, eg: background turn red.
   */
  abstract void indicateError();

  /**
   * UI component should revert back to a normal state, clearing any changes from {@code indicateError}.
   */
  abstract void clearError();

  abstract void resetToDefault();

  abstract void reset();
}

