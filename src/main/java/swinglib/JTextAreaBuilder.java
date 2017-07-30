package swinglib;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.BorderFactory;
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
 *     .maximumSize(120, 50)
 *     .readOnly()
 *     .borderWidth(1)
 *     .build();
 * </pre></code>
 */
public final class JTextAreaBuilder {

  private String text;
  private int rows = 3;
  private int columns = 15;
  private int borderWidth;
  private boolean readOnly = false;
  private Dimension maxSize;

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
    Preconditions.checkArgument(rows > 0);
    Preconditions.checkArgument(columns > 0);
    final JTextArea textArea = new JTextArea(Strings.nullToEmpty(text), rows, columns);
    textArea.setWrapStyleWord(true);

    if (borderWidth > 0) {
      textArea.setBorder(BorderFactory.createLineBorder(Color.black, borderWidth));
    }

    if (readOnly) {
      textArea.setEditable(false);
    }

    if (maxSize != null) {
      textArea.setMaximumSize(maxSize);
    }
    return textArea;
  }

  public JTextAreaBuilder readOnly() {
    readOnly = true;
    return this;
  }

  /* TODO: test me */
  public JTextAreaBuilder borderWidth(final int width) {
    this.borderWidth = width;
    return this;
  }

  public JTextAreaBuilder maximumSize(final int width, final int height) {
    maxSize = new Dimension(width, height);
    return this;
  }

  public JTextAreaBuilder rows(final int value) {
    this.rows = value;
    return this;
  }

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
