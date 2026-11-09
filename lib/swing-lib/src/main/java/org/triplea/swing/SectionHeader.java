package org.triplea.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import lombok.experimental.UtilityClass;

/**
 * Titles a section of a screen: a bold title over a rule, with optional actions at the right end of
 * the title row. Sections of one screen share this look so they read as parts of one whole.
 */
@UtilityClass
public class SectionHeader {
  private static final float TITLE_FONT_INCREASE = 2;

  /** Creates a header titled {@code title}, with {@code actions} right-aligned on its row. */
  public static JComponent create(final String title, final Component... actions) {
    final JLabel label = new JLabel(title);
    label.setFont(
        label.getFont().deriveFont(Font.BOLD, label.getFont().getSize2D() + TITLE_FONT_INCREASE));
    final JPanel header = new JPanel(new BorderLayout());
    header.setOpaque(false);
    header.setBorder(
        BorderFactory.createCompoundBorder(
            ruleBelow(), BorderFactory.createEmptyBorder(8, 0, 6, 0)));
    header.add(label, BorderLayout.WEST);
    if (actions.length > 0) {
      final JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
      actionRow.setOpaque(false);
      for (final Component action : actions) {
        actionRow.add(action);
      }
      header.add(actionRow, BorderLayout.EAST);
    }
    return header;
  }

  /**
   * The color of the rules that split a screen into sections. The look and feel's separator color
   * is barely visible on dark themes, so it is moved a quarter of the way toward the text color.
   */
  public static Color ruleColor() {
    final Color separator = UIManager.getColor("Separator.foreground");
    final Color text = UIManager.getColor("Label.foreground");
    if (separator == null || text == null) {
      return Color.GRAY;
    }
    return blend(separator, text, 0.25);
  }

  /** A one pixel rule along the bottom edge. */
  public static Border ruleBelow() {
    return BorderFactory.createMatteBorder(0, 0, 1, 0, ruleColor());
  }

  /** A one pixel rule along the top edge. */
  public static Border ruleAbove() {
    return BorderFactory.createMatteBorder(1, 0, 0, 0, ruleColor());
  }

  /** A one pixel rule along the right edge. */
  public static Border ruleRight() {
    return BorderFactory.createMatteBorder(0, 0, 0, 1, ruleColor());
  }

  private static Color blend(final Color from, final Color to, final double share) {
    return new Color(
        (int) Math.round(from.getRed() + (to.getRed() - from.getRed()) * share),
        (int) Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * share),
        (int) Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * share));
  }
}
