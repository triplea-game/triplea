package swinglib;

import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.Font;

import javax.swing.JButton;
import javax.swing.UIManager;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * Example usage:.
 * <code><pre>
 *   JButton button = JButtonBuilder.builder()
 *     .text("button text")
 *     .actionListener(() -> handleClickAction())
 *     .build();
 * </pre></code>
 */
public class JButtonBuilder {

  private String title;
  private String toolTip;
  private Runnable actionListener;
  private boolean enabled = false;
  private boolean selected = false;
  private int biggerFont = 0;

  private JButtonBuilder() {}

  public static JButtonBuilder builder() {
    return new JButtonBuilder();
  }

  /**
   * Constructs a Swing JButton using current builder values.
   * Values that must be set: title, actionlistener
   */
  public JButton build() {
    Preconditions.checkNotNull(title);
    Preconditions.checkState(!enabled || (actionListener != null),
        "Was enabled? " + enabled + ", action listener == null? " + (actionListener == null));

    final JButton button = new JButton(title);
    if (toolTip != null) {
      button.setToolTipText(toolTip);
    }
    button.addActionListener(e -> actionListener.run());
    button.setEnabled(enabled);

    if (biggerFont > 0) {
      button.setFont(
          new Font(
              button.getFont().getName(),
              button.getFont().getStyle(),
              button.getFont().getSize() + biggerFont));
    }

    button.setSelected(selected);
    return button;
  }


  /** required - The text that will be on the button. */
  public JButtonBuilder title(final String title) {
    Preconditions.checkArgument(!Strings.nullToEmpty(title).trim().isEmpty());
    this.title = title;
    return this;
  }

  /**
   * Sets the button title to the system's OK button text.
   */
  public JButtonBuilder okTitle() {
    return title(UIManager.getString("OptionPane.okButtonText"));
  }

  /**
   * Toggles the button as 'selected', which gives keyboard focus to the button. By default button is not selected.
   */
  public JButtonBuilder selected(final boolean value) {
    selected = value;
    return this;
  }

  /**
   * Increases button text size by a default amount.
   */
  public JButtonBuilder biggerFont() {
    return biggerFont(4);
  }

  /**
   * Increases button text size.
   *
   * @param plusAmount Text increase amount, typically somewhere around +2 to +4
   */
  public JButtonBuilder biggerFont(final int plusAmount) {
    Preconditions.checkArgument(plusAmount > 0);
    biggerFont = plusAmount;
    return this;
  }


  /**
   * Sets the text shown when hovering on the button.
   */
  public JButtonBuilder toolTip(final String toolTip) {
    Preconditions.checkArgument(!Strings.nullToEmpty(toolTip).trim().isEmpty());
    this.toolTip = toolTip;
    return this;
  }

  /**
   * Sets the event that occurs when the button is clicked.
   * SIDE EFFECT: Enables the button
   */
  public JButtonBuilder actionListener(final Runnable actionListener) {
    this.actionListener = checkNotNull(actionListener);
    enabled = true;
    return this;
  }

  /**
   * Whether the button can be clicked on or not.
   */
  public JButtonBuilder enabled(final boolean enabled) {
    this.enabled = enabled;
    return this;
  }
}
