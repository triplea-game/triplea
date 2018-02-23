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

import games.strategy.engine.framework.startup.ui.editors.validators.IValidator;
import games.strategy.engine.framework.startup.ui.editors.validators.NonEmptyValidator;

/**
 * Base class for editors.
 * Editors fire property Events in response when changed, so other editors or GUI can be notified
 */
public abstract class EditorPanel extends JPanel {
  private static final long serialVersionUID = 8156959717037201321L;
  public static final String EDITOR_CHANGE = "EditorChange";
  protected final Color labelColor;

  public EditorPanel() {
    super(new GridBagLayout());
    labelColor = new JLabel().getForeground();
  }

  /**
   * registers a listener for editor changes
   *
   * @param listener
   *        the listener. be aware that the oldValue and newValue properties of the PropertyChangeEvent
   *        will both be null
   */
  @Override
  public void addPropertyChangeListener(final PropertyChangeListener listener) {
    super.addPropertyChangeListener(EDITOR_CHANGE, listener);
  }

  /**
   * Validates that a text field is not empty. if the content is not valid the associated label is marked in red
   *
   * @param field
   *        the field to validate
   * @param label
   *        the associated label (or null)
   * @return true if text field content is valid
   */
  protected boolean validateTextFieldNotEmpty(final JTextField field, final JLabel label) {
    return validateTextField(field, label, new NonEmptyValidator());
  }

  /**
   * Validates a the contents of a text field using a specified validator. if the content is not valid the associated
   * label is marked in red
   *
   * @param field
   *        the field to validate
   * @param label
   *        the associated label (or null)
   * @param validator
   *        the validator
   * @return true if text field content is valid
   */
  protected boolean validateTextField(final JTextField field, final JLabel label, final IValidator validator) {
    return validateText(field.getText(), label, validator);
  }

  /**
   * Validates a the contents of textusing a specified validator. if the content is not valid the associated label is
   * marked in red
   *
   * @param text
   *        the text to validate
   * @param label
   *        the associated label (or null)
   * @param validator
   *        the validator
   * @return true if text field content is valid
   */
  protected boolean validateText(final String text, final JLabel label, final IValidator validator) {
    boolean valid = true;
    Color color = labelColor;
    if (!validator.isValid(text)) {
      valid = false;
      color = Color.RED;
      label.setForeground(color);
    }
    if (label != null) {
      label.setForeground(color);
    }
    return valid;
  }

  /**
   * called to see if the bean that is edited is in a valid state.
   * This is typically called by editor listeners in response to a change in the editor
   *
   * @return true if valid
   */
  public abstract boolean isBeanValid();

  /**
   * Get the bean that is being edited. You should only call this when #isBeanValid return true
   *
   * @return the bean modified by the editor
   */
  public abstract IBean getBean();

  /**
   * Returns the Label width, this can be used by wrapping editors to try to align label sizes.
   *
   * @return the size of the largest label in the first column
   */
  public int getLabelWidth() {
    int width = 0;
    final GridBagLayout layout = (GridBagLayout) getLayout();
    final Component[] components = getComponents();
    for (final Component component : components) {
      // label in first column
      if ((component instanceof JLabel)
          && (layout.getConstraints(component).gridx == 0)
          && (component.getPreferredSize().width > width)) {
        width = component.getPreferredSize().width;
      }
    }
    return width;
  }

  /**
   * Sets the label with for labels in the first column of the gridBagLayout.
   * This can be used to align components in a GUI, so all editors (or nested editors) have same label width
   *
   * @param width the new width of the labels
   */
  public void setLabelWidth(final int width) {
    final GridBagLayout layout = (GridBagLayout) getLayout();
    final Component[] components = getComponents();
    for (final Component component : components) {
      // label in first column
      if ((component instanceof JLabel) && (layout.getConstraints(component).gridx == 0)) {
        final int height = component.getPreferredSize().height;
        final Dimension dimension = new Dimension(width, height);
        component.setPreferredSize(dimension);
        component.setSize(dimension);
      }
    }
  }

  /**
   * Fires the EDITOR_CHANGE property change, to notify propertyChangeListeners which have registered to be
   * notified when the editor modifies the bean.
   */
  protected void fireEditorChanged() {
    firePropertyChange(EDITOR_CHANGE, null, null);
  }

  /**
   * Document listener which calls fireEditorChanged in response to any document change.
   */
  protected class EditorChangedFiringDocumentListener implements DocumentListener {
    @Override
    public void changedUpdate(final DocumentEvent e) {
      fireEditorChanged();
    }

    @Override
    public void insertUpdate(final DocumentEvent e) {
      fireEditorChanged();
    }

    @Override
    public void removeUpdate(final DocumentEvent e) {
      fireEditorChanged();
    }
  }
}
