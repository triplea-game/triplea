package swinglib;

import javax.swing.JTextArea;

import com.google.common.base.Preconditions;

/**
 * Example usage:
 * <code><pre>
 *   JTextAreaBuilder textArea = JTextAreaBuilder.builder()
 *     .text(setting.description)
 *     .rows(2)
 *     .columns(40)
 *     .maximumSize(120, 50)
 *     .readOnly()
 *     .borderWidth(1)
 *     .build();
 * </pre></code>
 */
public final class JTextAreaBuilder {


  private String text;
  private int rows;
  private int columns;

  private JTextAreaBuilder() {

  }

  public static JTextAreaBuilder builder() {
    return new JTextAreaBuilder();
  }

  /**
   * Constructs a Swing JTextArea using current builder values.
   * Values that must be set: text, rows, columns
   * By default the JTextArea will have line wrapping turned on.
   */
  public JTextArea build() {
    Preconditions.checkNotNull(text);
    Preconditions.checkState(rows > 0);
    Preconditions.checkState(columns > 0);

    final JTextArea textArea = new JTextArea();
    textArea.setWrapStyleWord(true);
    textArea.setLineWrap(true);
    return textArea;
  }



  public JTextAreaBuilder readOnly() {
    return this;
  }

  public JTextAreaBuilder borderWidth(final int width) {

    return this;
  }

  public JTextAreaBuilder maximumSize(final int width, final int height) {

    return this;
  }

  public JTextAreaBuilder rows(final int value) {

    return this;
  }

  public JTextAreaBuilder columns(final int value) {
    return this;
  }

  public JTextAreaBuilder text(final String text) {
    this.text = text;
    return this;
  }
}
