package swinglib;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * Builder for Swing text areas. Example usage:
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
  private String componentName;
  private int rows = 3;
  private int columns = 15;
  private boolean readOnly = false;
  private int border = 3;
  private boolean selectAllOnFocus = false;

  private JTextAreaBuilder() {}

  public static JTextAreaBuilder builder() {
    return new JTextAreaBuilder();
  }

  /**
   * Attaches a listener that is fired whenever text is typed in the textArea.
   * Note, the listener will be notified if text is either typed or pasted.
   *
   * @param textArea The component to which we attach a listener.
   * @param listener The listener that should be invoked on key events.
   */
  public static void addTextListener(final JTextArea textArea, final Runnable listener) {
    textArea.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void insertUpdate(final DocumentEvent e) {
        listener.run();
      }

      @Override
      public void removeUpdate(final DocumentEvent e) {
        listener.run();
      }

      @Override
      public void changedUpdate(final DocumentEvent e) {
        listener.run();
      }
    });
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
    textArea.setName(componentName);

    if (readOnly) {
      textArea.setEditable(false);
    }
    textArea.setBorder(new EmptyBorder(border, border, border, border));

    if (selectAllOnFocus) {
      textArea.addFocusListener(new FocusAdapter() {
        @Override
        public void focusGained(final FocusEvent e) {
          selectAll(textArea);
        }
      });

      textArea.addMouseListener(new MouseAdapter() {
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

  public JTextAreaBuilder componentName(final String componentName) {
    this.componentName = componentName;
    return this;
  }

  public JTextAreaBuilder border(final int borderWidth) {
    Preconditions.checkArgument(borderWidth >= 0);
    this.border = borderWidth;
    return this;
  }

  public JTextAreaBuilder selectAllTextOnFocus() {
    selectAllOnFocus = true;
    return this;
  }
}
