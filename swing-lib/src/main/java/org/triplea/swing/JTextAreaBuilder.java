package org.triplea.swing;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

/**
 * Builder for Swing text areas.
 *
 * <ul>
 *   <li>Line wrapping is turned on by default.
 * </ul>
 *
 * Example usage: <code><pre>
 *   JTextAreaBuilder textArea = JTextAreaBuilder.builder()
 *     .text(setting.description)
 *     .rows(2)
 *     .columns(40)
 *     .readOnly()
 *     .build();
 * </pre></code>
 */
public final class JTextAreaBuilder {

  @Nullable private String text;
  @Nullable private String componentName;
  private int rows = 3;
  private int columns = 15;
  private boolean readOnly;
  private boolean selectAllOnFocus;
  @Nullable private String toolTip;

  public JTextAreaBuilder() {}

  public static JTextAreaBuilder builder() {
    return new JTextAreaBuilder();
  }

  /**
   * Constructs a Swing JTextArea using current builder values. Values that must be set: text, rows,
   * columns The JTextArea will have line wrapping turned on.
   */
  public JTextArea build() {
    Preconditions.checkArgument(rows > 0);
    Preconditions.checkArgument(columns > 0);
    final JTextArea textArea = new JTextArea(Strings.nullToEmpty(text), rows, columns);
    textArea.setLineWrap(true);
    textArea.setWrapStyleWord(true);
    textArea.setName(componentName);
    Optional.ofNullable(toolTip).ifPresent(textArea::setToolTipText);

    if (readOnly) {
      textArea.setEditable(false);
    }
    final int border = 3;
    textArea.setBorder(new EmptyBorder(border, border, border, border));

    if (selectAllOnFocus) {
      textArea.addFocusListener(
          new FocusAdapter() {
            @Override
            public void focusGained(final FocusEvent e) {
              selectAll(textArea);
            }
          });

      textArea.addMouseListener(
          new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
              selectAll(textArea);
            }
          });
    }
    return textArea;
  }

  private static void selectAll(final JTextArea textArea) {
    textArea.setSelectionEnd(0);
    textArea.setSelectionEnd(textArea.getText().length());
  }

  public JTextAreaBuilder readOnly() {
    readOnly = true;
    return this;
  }

  /**
   * Sets the number of text area rows.
   *
   * @param value The number of text area rows.
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

  public JTextAreaBuilder componentName(final String componentName) {
    this.componentName = componentName;
    return this;
  }

  public JTextAreaBuilder selectAllTextOnFocus() {
    selectAllOnFocus = true;
    return this;
  }

  /**
   * Specifies the tooltip that is shown when user hovers over the text area. By default there is no
   * tooltip text.
   */
  public JTextAreaBuilder toolTip(final String toolTip) {
    this.toolTip = toolTip;
    return this;
  }
}
