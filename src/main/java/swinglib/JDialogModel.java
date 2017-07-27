package swinglib;

import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.KeyStroke;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import games.strategy.ui.SwingAction;

public final class JDialogModel {
  private final JFrame parentFrame;
  private final String title;
  private final JComponent contents;


  private JDialogModel(final JFrame parentFrame, final String title, final JComponent contents) {
    this.parentFrame = parentFrame;
    this.title = Preconditions.checkNotNull(title);
    this.contents = Preconditions.checkNotNull(contents);
  }

  public static SwingJDialogModelBuilder builder() {
    return new SwingJDialogModelBuilder();
  }

  private JDialog swingComponent() {
    final JDialog dialog = new JDialog(parentFrame, title, true);
    dialog.getContentPane().add(contents);
    final Action closeAction = SwingAction.of("", e -> dialog.setVisible(false));
    final KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
    final String key = "dialog.close";
    dialog.getRootPane().getActionMap().put(key, closeAction);
    dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(stroke, key);
    return dialog;
  }


  public static class SwingJDialogModelBuilder {
    private JFrame parentFrame;
    private String title;
    private JComponent contents;

    public SwingJDialogModelBuilder withParentFrame(final JFrame parentFrame) {
      this.parentFrame = Preconditions.checkNotNull(parentFrame);
      return this;
    }

    public SwingJDialogModelBuilder withTitle(final String title) {
      Preconditions.checkArgument(!Strings.nullToEmpty(title).trim().isEmpty());
      this.title = title;
      return this;
    }

    public SwingJDialogModelBuilder withContents(final JComponent contents) {
      this.contents = Preconditions.checkNotNull(contents);
      return this;
    }

    public JDialog swingComponent() {
      return new JDialogModel(
          parentFrame,
          title,
          contents)
              .swingComponent();
    }
  }
}
