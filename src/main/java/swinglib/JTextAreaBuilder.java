package swinglib;

import javax.swing.JTextArea;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * Example usage:
 * <code><pre>
 *   JTextAreaBuilder textArea = JTextAreaBuilder.builder()
 *     .text(setting.description)
 *     .rows(2)
 *     .columns(40)
 *     .readOnly()
 *     .build();
 * </pre></code>
 */
public final class JTextAreaBuilder {

  private String text;
  private int rows = 3;
  private int columns = 15;
  private boolean readOnly = false;

  private JTextAreaBuilder() {}

  public static JTextAreaBuilder builder() {
    return new JTextAreaBuilder();
  }

  /**
   * Constructs a Swing JTextArea using current builder values.
   * Values that must be set: text, rows, columns
   * The JTextArea will have line wrapping turned on.
   */
  public JTextArea build() {
    Preconditions.checkArgument(rows > 0);
    Preconditions.checkArgument(columns > 0);
    final JTextArea textArea = new JTextArea(Strings.nullToEmpty(text), rows, columns);
    textArea.setLineWrap(true);
    textArea.setWrapStyleWord(true);

    if (readOnly) {
      textArea.setEditable(false);
    }

    return textArea;
  }

  public JTextAreaBuilder readOnly() {
    readOnly = true;
    return this;
  }

  /**
   * Sets the number of text area rows.
   *
   * @param value The number of text area rows.
   *
   * @throws IllegalArgumentException If {@code value} is not positive.
   */
  public JTextAreaBuilder rows(final int value) {
    Preconditions.checkArgument(value > 0);
    this.rows = value;
    return this;
  }

  /**
   * Sets the number of text area columns.
   *
   * @param value The number of text area columns.
   *
   * @throws IllegalArgumentException If {@code value} is not positive.
   */
  public JTextAreaBuilder columns(final int value) {
    Preconditions.checkArgument(value > 0);
    this.columns = value;
    return this;
  }

  public JTextAreaBuilder text(final String text) {
    this.text = text;
    return this;
  }
}
