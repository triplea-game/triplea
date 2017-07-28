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

public final class JDialogBuilder {

  private JFrame parentFrame;
  private String title;
  private JComponent contents;


  private JDialogBuilder() {}

  public static JDialogBuilder builder() {
    return new JDialogBuilder();
  }

  public JDialog build() {
    Preconditions.checkNotNull(title);
    Preconditions.checkNotNull(contents);

    final JDialog dialog = new JDialog(parentFrame, title, true);
    dialog.getContentPane().add(contents);
    final Action closeAction = SwingAction.of("", e -> dialog.setVisible(false));
    final KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
    final String key = "dialog.close";
    dialog.getRootPane().getActionMap().put(key, closeAction);
    dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(stroke, key);
    return dialog;
  }

  public JDialogBuilder parentFrame(final JFrame parentFrame) {
    this.parentFrame = Preconditions.checkNotNull(parentFrame);
    return this;
  }

  public JDialogBuilder title(final String title) {
    Preconditions.checkArgument(!Strings.nullToEmpty(title).trim().isEmpty());
    this.title = title;
    return this;
  }

  public JDialogBuilder contents(final JComponent contents) {
    this.contents = Preconditions.checkNotNull(contents);
    return this;
  }
}
