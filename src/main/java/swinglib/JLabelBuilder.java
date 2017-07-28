package swinglib;

import java.awt.Dimension;

import javax.swing.JLabel;

import com.google.common.base.Preconditions;

/**
 * Example usage:
 * <code><pre>
 *   JLabel label = JLabelBuilder.builder()
 *     .text("label text")
 *     .build();
 * </pre></code>
 */
public class JLabelBuilder {

  private String text;
  private Alignment alignment;
  private Dimension maxSize;

  private JLabelBuilder() {}

  public static JLabelBuilder builder() {
    return new JLabelBuilder();
  }

  /**
   * Constructs a Swing JLabel using current builder values.
   * Values that must be set: text
   */
  public JLabel build() {
    Preconditions.checkNotNull(text);
    Preconditions.checkState(!text.trim().isEmpty());
    return new JLabel();
  }

  public JLabelBuilder leftAlign() {
    alignment = Alignment.LEFT;
    return this;
  }

  public JLabelBuilder text(final String text) {
    this.text = text;
    return this;
  }

  public JLabelBuilder maximumSize(final int width, final int height) {
    maxSize = new Dimension(width, height);
    return this;
  }

  private enum Alignment {
    LEFT
  }
}
