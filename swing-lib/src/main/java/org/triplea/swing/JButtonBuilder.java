package org.triplea.swing;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Strings;
import java.awt.Component;
import java.awt.Font;
import java.util.Optional;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.UIManager;

/**
 * Example usage:. <code><pre>
 *   JButton button = new JButtonBuilder()
 *     .text("button text")
 *     .actionListener(() -> handleClickAction())
 *     .build();
 * </pre></code>
 */
public class JButtonBuilder {

  private String title;
  private String toolTip;
  private Consumer<Component> clickAction;
  private boolean selected;
  private boolean enabled = true;
  private int biggerFont;

  public JButtonBuilder() {}

  public JButtonBuilder(final String title) {
    this.title = title;
  }

  /**
   * Constructs a Swing JButton using current builder values. Values that must be set: title,
   * actionlistener
   */
  public JButton build() {
    checkNotNull(title);

    final JButton button = new JButton(title);
    Optional.ofNullable(clickAction)
        .ifPresent(listener -> button.addActionListener(e -> clickAction.accept(button)));
    Optional.ofNullable(toolTip).ifPresent(button::setToolTipText);

    button.setSelected(selected);
    button.setEnabled(enabled);

    if (biggerFont > 0) {
      button.setFont(
          new Font(
              button.getFont().getName(),
              button.getFont().getStyle(),
              button.getFont().getSize() + biggerFont));
    }

    return button;
  }

  /** required - The text that will be on the button. */
  public JButtonBuilder title(final String title) {
    checkArgument(!Strings.nullToEmpty(title).trim().isEmpty());
    this.title = title;
    return this;
  }

  /** Sets the button title to the system's OK button text. */
  public JButtonBuilder okTitle() {
    return title(UIManager.getString("OptionPane.okButtonText"));
  }

  /** Sets the button title to the system's Cancel button text. */
  public JButtonBuilder cancelTitle() {
    return title(UIManager.getString("OptionPane.cancelButtonText"));
  }

  /**
   * Toggles the button as 'selected', which gives keyboard focus to the button. By default button
   * is not selected.
   */
  public JButtonBuilder selected(final boolean value) {
    selected = value;
    return this;
  }

  /** Increases button text size by a default amount. */
  public JButtonBuilder biggerFont() {
    return biggerFont(4);
  }

  /**
   * Increases button text size.
   *
   * @param plusAmount Text increase amount, typically somewhere around +2 to +4
   */
  public JButtonBuilder biggerFont(final int plusAmount) {
    checkArgument(plusAmount > 0);
    biggerFont = plusAmount;
    return this;
  }

  /** Sets the text shown when hovering on the button. */
  public JButtonBuilder toolTip(final String toolTip) {
    checkArgument(!Strings.nullToEmpty(toolTip).trim().isEmpty());
    this.toolTip = toolTip;
    return this;
  }

  /** An alias for toolTip */
  public JButtonBuilder toolTipText(final String toolTip) {
    return toolTip(toolTip);
  }

  /** Sets the event that occurs when the button is clicked. SIDE EFFECT: Enables the button */
  public JButtonBuilder actionListener(final Runnable actionListener) {
    checkNotNull(actionListener);
    return actionListener(c -> actionListener.run());
  }

  /**
   * Sets up an action listener that is passed an instance of the button it is attached to. For
   * example: many components want a 'parent' so that the new component can be located above the
   * 'parent' component on screen. We could easily have a button that opens a window that should be
   * located above the button. Since we do not have a button reference yet while constructing the
   * button, this method can be used in that situation.
   *
   * @param clickAction The action listener to invoke when the button is clicked, the parameter to
   *     the listener is the button that was clicked (so it can be enabled/disabled, or text
   *     updated).
   */
  public JButtonBuilder actionListener(final Consumer<Component> clickAction) {
    this.clickAction = checkNotNull(clickAction);
    return this;
  }

  /** Sets whether the button can be clicked or not. By default buttons are enabled. */
  public JButtonBuilder enabled(final boolean enabled) {
    this.enabled = enabled;
    return this;
  }
}
