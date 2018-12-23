package games.strategy.engine.framework.startup.ui.editors;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.beans.PropertyChangeListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.google.common.base.Preconditions;

import games.strategy.engine.framework.startup.ui.editors.validators.IValidator;
import games.strategy.engine.framework.startup.ui.editors.validators.NonEmptyValidator;

/**
 * Base class for editors.
 * Editors fire property Events in response when changed, so other editors or GUI can be notified
 */
public abstract class EditorPanel extends JPanel {
  private static final long serialVersionUID = 8156959717037201321L;
  private static final String EDITOR_CHANGE = "EditorChange";
  protected final Color labelColor;

  public EditorPanel() {
    super(new GridBagLayout());
    labelColor = new JLabel().getForeground();
  }

  /**
   * Registers a listener for editor changes.
   *
   * @param listener the listener. be aware that the oldValue and newValue properties of the PropertyChangeEvent
   *        will both be null
   */
  @Override
  public void addPropertyChangeListener(final PropertyChangeListener listener) {
    super.addPropertyChangeListener(EDITOR_CHANGE, listener);
  }

  /**
   * Validates that a text field is not empty. if the content is not valid the associated label is marked in red
   *
   * @param field the field to validate
   * @param label the associated label
   * @return true if text field content is valid
   */
  protected boolean validateTextFieldNotEmpty(final JTextField field, final JLabel label) {
    return validateTextField(field, label, new NonEmptyValidator());
  }

  /**
   * Validates a the contents of a text field using a specified validator. if the content is not valid the associated
   * label is marked in red
   *
   * @param field the field to validate
   * @param label the associated label
   * @param validator the validator
   * @return true if text field content is valid
   */
  protected boolean validateTextField(final JTextField field, final JLabel label, final IValidator validator) {
    return validateText(field.getText(), label, validator);
  }

  /**
   * Validates a the contents of text using a specified validator. if the content is not valid the associated label is
   * marked in red
   *
   * @param text the text to validate
   * @param label the associated label
   * @param validator the validator
   * @return true if text field content is valid
   */
  private boolean validateText(final String text, final JLabel label, final IValidator validator) {
    Preconditions.checkNotNull(label);
    Preconditions.checkNotNull(validator);
    final boolean valid = validator.isValid(text);
    label.setForeground(valid ? labelColor : Color.RED);
    return valid;
  }
}
