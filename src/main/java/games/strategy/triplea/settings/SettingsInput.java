package games.strategy.triplea.settings;

import javax.swing.JComponent;


/**
 * Represents a single user input field, specifically the UI element a user uses to enter in a data value.
 * This may reprsent a JTextField, a set of radio buttons, etc..
 * One key is that each SettingsInput must return a simple text representation of the user input. This is so that the
 * value can be stored directly in system preferences, which requires String object types. The settings framework is
 * responsible for interpretting the text value, and mapping that to a higher level of abstraction.
 */
public interface SettingsInput {

  /**
   * @return A Swing component that contains user input elements.
   */
  JComponent getSwingComponent();

  /**
   * @return The current text value of the user input component. Should be the raw value of what is selected/chosen
   *         by the user.
   */
  String getText();

  void setText(String valueToSet);
}
