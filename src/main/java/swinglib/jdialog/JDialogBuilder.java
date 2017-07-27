package swinglib.jdialog;

import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;

import games.strategy.ui.SwingAction;

public class JDialogBuilder {

  public static JDialog newJDialogModal(JDialogModel jDialogModel) {
    final JDialog dialog = new JDialog(jDialogModel.parentFrame, jDialogModel.title, true);
    dialog.getContentPane().add(jDialogModel.contents);
    final Action closeAction = SwingAction.of("", e -> dialog.setVisible(false));
    final KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
    final String key = "dialog.close";
    dialog.getRootPane().getActionMap().put(key, closeAction);
    dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(stroke, key);
    return dialog;
  }


}
