package org.triplea.swing;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.util.Optional;
import java.util.function.Consumer;
import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;

/**
 * Builder class for building swing text fields. Example usage:
 *
 * <pre>
 * <code>
 *   JTextField field = JTextFieldBuilder.builder()
 *      .columns(15)
 *      .text("initial text")
 *      .actionListener(textValue -> enterPressedWithTextInput(textValue)
 *      .keyTypedListener(textValue -> processKeyTypedWithText(textValue)
 *      .compositeBuilder()
 *      .withButton("buttonText", textValue -> buttonPressedAction(textValue)
 *      .build();
 * </code>
 * </pre>
 */
public class JTextFieldBuilder {

  private String text;
  private String toolTip;
  private Integer columns;
  private Integer maxLength;

  private Consumer<JTextComponent> actionListener;
  private Consumer<String> textListener;
  private boolean enabled = true;
  private boolean readOnly;

  /** Builds the swing component. */
  public JTextField build() {
    final JTextField textField = new JTextField(Strings.nullToEmpty(this.text));

    Optional.ofNullable(columns).ifPresent(textField::setColumns);

    Optional.ofNullable(actionListener)
        .ifPresent(action -> textField.addActionListener(e -> action.accept(textField)));

    Optional.ofNullable(textListener)
        .ifPresent(
            listener ->
                new DocumentListenerBuilder(() -> textListener.accept(textField.getText()))
                    .attachTo(textField));

    Optional.ofNullable(maxLength).map(JTextFieldLimit::new).ifPresent(textField::setDocument);

    Optional.ofNullable(text).ifPresent(textField::setText);

    Optional.ofNullable(toolTip).ifPresent(textField::setToolTipText);

    textField.setEnabled(enabled);
    textField.setEditable(!readOnly);
    return textField;
  }

  /** Sets the initial value displayed on the text field. */
  public JTextFieldBuilder text(final String value) {
    this.text = Optional.ofNullable(value).orElse("");
    return this;
  }

  /** Convenience method for setting the text value to a numeric value. */
  public JTextFieldBuilder text(final int value) {
    this.text = String.valueOf(value);
    return this;
  }

  /**
   * Defines the width of the text field. Value is passed directly to swing. TODO: list some
   * typical/reasonable value examples
   */
  public JTextFieldBuilder columns(final int columns) {
    Preconditions.checkArgument(columns > 0);
    this.columns = columns;
    return this;
  }

  /**
   * Sets a max of how many characters can be entered into the text field.
   *
   * @param maxChars Character limit of the text field.
   */
  public JTextFieldBuilder maxLength(final int maxChars) {
    maxLength = maxChars;
    return this;
  }

  /*
   * {@code JTextFieldLimit} is from:
   * https://stackoverflow.com/questions/3519151/how-to-limit-the-number-of-characters-in-jtextfield
   */
  private static class JTextFieldLimit extends PlainDocument {
    private static final long serialVersionUID = -6269113182585526764L;
    private final int limit;

    JTextFieldLimit(final int limit) {
      this.limit = limit;
    }

    @Override
    public void insertString(final int offset, final String str, final AttributeSet attr)
        throws BadLocationException {
      if (str == null) {
        return;
      }

      if ((getLength() + str.length()) <= limit) {
        super.insertString(offset, str, attr);
      }
    }
  }

  public static JTextFieldBuilder builder() {
    return new JTextFieldBuilder();
  }

  /**
   * Adds an action listener that is fired when the user presses enter after entering text into the
   * text field.
   *
   * @param actionListener Action to fire on 'enter', consumed value will be a reference to text
   *     field firing the action.
   */
  public JTextFieldBuilder actionListener(final Consumer<JTextComponent> actionListener) {
    Preconditions.checkNotNull(actionListener);
    this.actionListener = actionListener;
    return this;
  }

  /** Adds a listener that is fired when text data is changed. */
  public JTextFieldBuilder textListener(final Consumer<String> textListener) {
    Preconditions.checkNotNull(textListener);
    this.textListener = textListener;
    return this;
  }

  /**
   * A ready only text field can't be changed, but the user can still click on it and select the
   * text.
   */
  public JTextFieldBuilder readOnly() {
    this.readOnly = true;
    return this;
  }

  public JTextFieldBuilder enabled(final boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /** Disables a text field, can no longer be clicked on. */
  public JTextFieldBuilder disabled() {
    this.enabled = false;
    return this;
  }

  public JTextFieldBuilder toolTip(final String toolTip) {
    Preconditions.checkArgument(toolTip != null && !toolTip.isEmpty());
    this.toolTip = toolTip;
    return this;
  }
}
