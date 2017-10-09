package swinglib;

import java.awt.Dimension;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.border.Border;

import com.google.common.base.Preconditions;

/**
 * Builds a swing JLabel.
 * <br />
 * Example usage:
 * <code><pre>
 *   JLabel label = JLabelBuilder.builder()
 *     .text("label text")
 *     .leftAlign()
 *     .build();
 * </pre></code>
 */
public class JLabelBuilder {

  private String text;
  private Alignment alignment;
  private Dimension maxSize;
  private int maxTextLength;
  private String tooltip;
  private Border border;

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

    final String truncated = maxTextLength > 0 && text.length() > maxTextLength ? text.substring(0, 25) + "..." : text;

    final JLabel label = new JLabel(truncated);

    if (alignment != null) {
      switch (alignment) {
        case LEFT:
        default:
          label.setAlignmentX(JComponent.LEFT_ALIGNMENT);
          break;
      }
    }

    if (tooltip != null) {
      label.setToolTipText(tooltip);
    }
    if (maxSize != null) {
      label.setMaximumSize(maxSize);
    }

    if (border != null) {
      label.setBorder(border);
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

  public JLabelBuilder border(final Border border) {
    this.border = border;
    return this;
  }

  public JLabelBuilder tooltip(final String tooltip) {
    this.tooltip = tooltip;
    return this;
  }


  /**
   * Builds a label with a max length enforced for the printed text. Text is truncated if exceeds the max
   * length and the full text is placed into a hover-over tooltip.
   */
  public JLabelBuilder textWithMaxLength(final String text, final int maxTextLength) {
    this.maxTextLength = maxTextLength;
    this.text = text;
    return this;
  }

  private enum Alignment {
    LEFT
  }
}
