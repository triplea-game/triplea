package swinglib;

import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.Font;
import java.util.Optional;

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
  private boolean enabled = true;
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
    checkNotNull(title);

    final JButton button = new JButton(title);
    if (toolTip != null) {
      button.setToolTipText(toolTip);
    }
    Optional.ofNullable(actionListener)
        .ifPresent(listener -> button.addActionListener(e -> listener.run()));
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
   * Sets the button title to the system's Cancel button text.
   */
  public JButtonBuilder cancelTitle() {
    return title(UIManager.getString("OptionPane.cancelButtonText"));
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
    return this;
  }

  /**
   * Whether the button can be clicked on or not.
   */
  public JButtonBuilder disabled() {
    enabled = false;
    return this;
  }
}
