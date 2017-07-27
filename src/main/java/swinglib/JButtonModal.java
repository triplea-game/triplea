package swinglib;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.swing.JButton;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public class JButtonModal {

  private final String title;
  private final String toolTip;
  private final Runnable actionListener;

  public JButtonModal(final String title, final String toolTip, final Runnable actionListener) {
    this.title = Preconditions.checkNotNull(title);
    this.toolTip = toolTip;
    this.actionListener = Preconditions.checkNotNull(actionListener);
  }

  public static JButtonModalBuilder builder() {
    return new JButtonModalBuilder();
  }

  private JButton swingComponent() {
    final JButton button = new JButton(title);
    if (toolTip != null) {
      button.setToolTipText(toolTip);
    }
    button.addActionListener(e -> actionListener.run());
    return button;
  }


  public static class JButtonModalBuilder {
    private String title;
    private String toolTip;
    private Runnable actionListener;

    /** required - The text that will be on the button. */
    public JButtonModalBuilder withTitle(final String title) {
      Preconditions.checkArgument(!Strings.nullToEmpty(title).trim().isEmpty());
      this.title = title;
      return this;
    }

    /** optional, but potentially required in the future. This is the hover text when hovering on the button. */
    public JButtonModalBuilder withToolTip(final String toolTip) {
      Preconditions.checkArgument(!Strings.nullToEmpty(toolTip).trim().isEmpty());
      this.toolTip = toolTip;
      return this;
    }

    /** request, the event that occurs when the button is clicked */
    public JButtonModalBuilder withActionListener(final Runnable actionListener) {
      this.actionListener = checkNotNull(actionListener);
      return this;
    }

    public JButton swingComponent() {
      return new JButtonModal(
          title,
          toolTip,
          actionListener)
              .swingComponent();
    }
  }

}
