package swinglib;

import java.util.function.Consumer;

import javax.swing.JComponent;
import javax.swing.JTextField;

/**
 * Composition of a text field and an 'attached' button that acts on the field value.
 * For example, useful for input fields that have 'submit' button next to it.
 * <br />
 * Example usage:
 *
 * <pre>
 * <code>
 *   JComponent textFieldButtonComposite = JTextFieldBuilder.builder()
 *      .text("initial field value")
 *      .attachButton("submit",
 *         fieldText -> executeButtonAction(fieldText)
 *      .build();
 * </code>
 * </pre>
 */
public final class JTextFieldButtonAttachmentBuilder {

  private final JTextFieldBuilder textFieldBuilder;
  /**
   * When not null will cause builder to create a flow layout field + button, where the text value of the field
   * will be set to the button when it is clicked.
   */
  private String actionButtonText;
  private Consumer<String> actionButtonAction;



  JTextFieldButtonAttachmentBuilder(final JTextFieldBuilder textFieldBuilder) {
    this.textFieldBuilder = textFieldBuilder;

  }

  /**
   * Builds a JComponent containing text field and button in a flow layout.
   */
  public JComponent build() {
    final JTextField textField = textFieldBuilder.build();

    return JPanelBuilder.builder()
        .flowLayout()
        .add(textField)
        .add(JButtonBuilder.builder()
            .title(actionButtonText)
            .actionListener(() -> actionButtonAction.accept(textField.getText()))
            .build())
        .build();
  }


  /**
   * Adds a JButtonn component with title and action that will be fired
   * when the button is clicked. Input of the consumer function is the value
   * of the associated JTextField.
   */
  public JTextFieldButtonAttachmentBuilder withButton(final String buttonText, final Consumer<String> buttonAction) {
    this.actionButtonText = buttonText;
    this.actionButtonAction = buttonAction;
    return this;
  }

}
