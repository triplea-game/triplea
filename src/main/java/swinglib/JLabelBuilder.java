package swinglib;

import java.awt.Dimension;

import javax.swing.JComponent;
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
  private int maxTextLength;
  private String tooltip;

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

    final String truncated = maxTextLength  > 0 && text.length() > maxTextLength ?
        text.substring(0, maxTextLength) + "..." : text;

    final JLabel label = new JLabel(truncated);

    if (alignment != null) {
      switch (alignment) {
        case LEFT:
        default:
          label.setAlignmentX(JComponent.LEFT_ALIGNMENT);
          break;
      }
    }

    if(tooltip != null) {
      label.setToolTipText(tooltip);
    }
    if (maxSize != null) {
      label.setMaximumSize(maxSize);
    }

    return label;
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

  public JLabelBuilder tooltip(final String tooltip) {
    this.tooltip = tooltip;
    return this;
  }

  public JLabelBuilder textWithMaxLength(final String text, final int maxTextLength ) {
    this.maxTextLength = maxTextLength;
    this.text = text;
    return this;
  }

  private enum Alignment {
    LEFT
  }
}
