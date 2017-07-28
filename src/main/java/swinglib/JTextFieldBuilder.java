package swinglib;

import javax.swing.JTextField;

/**
 * Example usage:
 * <code><pre>
 *   JTextFieldBuilder textField = JTextFieldBuilder.builder()
 *     .text("initial text")
 *     .size(15)
 *     .build();
 * </pre></code>
 */
public final class JTextFieldBuilder {
  private final String text = "";

  private JTextFieldBuilder() {

  }

  public static JTextFieldBuilder builder() {
    return new JTextFieldBuilder();
  }

  /**
   * Constructs a Swing JTextField using current builder values.
   * Values that must be set: (none required)
   * Default values will be an empty JTextField with length suitable for one or two words.
   */
  private JTextField build() {
    return new JTextField();
  }
}
