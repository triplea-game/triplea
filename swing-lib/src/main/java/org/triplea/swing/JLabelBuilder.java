package org.triplea.swing;

import java.awt.Dimension;
import java.util.Optional;

import javax.annotation.Nullable;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

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
  private @Nullable Icon icon;
  private @Nullable Integer iconTextGap;
  private Alignment alignment;
  private Dimension maxSize;
  private String toolTip;
  private Border border;
  private Integer borderSize;

  private JLabelBuilder() {}

  public static JLabelBuilder builder() {
    return new JLabelBuilder();
  }

  /**
   * Constructs a Swing JLabel using current builder values.
   * Values that must be set: text or icon
   */
  public JLabel build() {
    Preconditions.checkState(text != null || icon != null);

    final JLabel label = new JLabel(text);

    Optional.ofNullable(icon)
        .ifPresent(label::setIcon);

    Optional.ofNullable(iconTextGap)
        .ifPresent(label::setIconTextGap);

    Optional.ofNullable(alignment)
        .ifPresent(align -> {
          if (align == Alignment.LEFT) {
            label.setAlignmentX(JComponent.LEFT_ALIGNMENT);
          }
        });

    Optional.ofNullable(toolTip)
        .ifPresent(label::setToolTipText);

    Optional.ofNullable(maxSize)
        .ifPresent(label::setMaximumSize);

    Optional.ofNullable(border)
        .ifPresent(label::setBorder);

    Optional.ofNullable(borderSize)
        .ifPresent(size -> label.setBorder(new EmptyBorder(size, size, size, size)));

    return label;
  }

  public JLabelBuilder errorIcon() {
    icon = UIManager.getIcon("OptionPane.errorIcon");
    return this;
  }

  public JLabelBuilder iconTextGap(final int iconTextGap) {
    this.iconTextGap = iconTextGap;
    return this;
  }

  public JLabelBuilder leftAlign() {
    alignment = Alignment.LEFT;
    return this;
  }

  public JLabelBuilder text(final String text) {
    this.text = text;
    return this;
  }

  public JLabelBuilder html(final String text) {
    return text("<html>" + text + "</html>");
  }

  public JLabelBuilder maximumSize(final int width, final int height) {
    maxSize = new Dimension(width, height);
    return this;
  }

  public JLabelBuilder border(final int borderSize) {
    this.borderSize = borderSize;
    return this;
  }


  public JLabelBuilder border(final Border border) {
    this.border = border;
    return this;
  }

  public JLabelBuilder toolTip(final String toolTip) {
    this.toolTip = toolTip;
    return this;
  }

  private enum Alignment {
    LEFT
  }
}
