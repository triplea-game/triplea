package swinglib;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.swing.JButton;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * Example usage:
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

  private JButtonBuilder() {}

  public static JButtonBuilder builder() {
    return new JButtonBuilder();
  }

  /** required - The text that will be on the button. */
  public JButtonBuilder title(final String title) {
    Preconditions.checkArgument(!Strings.nullToEmpty(title).trim().isEmpty());
    this.title = title;
    return this;
  }

  /** optional, but potentially required in the future. This is the hover text when hovering on the button. */
  public JButtonBuilder toolTip(final String toolTip) {
    Preconditions.checkArgument(!Strings.nullToEmpty(toolTip).trim().isEmpty());
    this.toolTip = toolTip;
    return this;
  }

  /** request, the event that occurs when the button is clicked */
  public JButtonBuilder actionListener(final Runnable actionListener) {
    this.actionListener = checkNotNull(actionListener);
    return this;
  }

  /**
   * Constructs a Swing JButton using current builder values.
   * Values that must be set: title, actionlistener
   */
  public JButton build() {
    Preconditions.checkNotNull(title);
    Preconditions.checkNotNull(actionListener);

    final JButton button = new JButton(title);
    if (toolTip != null) {
      button.setToolTipText(toolTip);
    }
    button.addActionListener(e -> actionListener.run());
    return button;
  }

}
