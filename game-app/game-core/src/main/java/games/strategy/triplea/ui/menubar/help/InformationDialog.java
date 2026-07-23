package games.strategy.triplea.ui.menubar.help;

import java.awt.BorderLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import lombok.experimental.UtilityClass;
import org.triplea.swing.SwingAction;
import org.triplea.swing.WindowAdapterFactory;

@UtilityClass
class InformationDialog {
  static JDialog createDialog(final JComponent component, final String title) {
    final JDialog dialog = new JDialog((JFrame) null, title);
    dialog.setAlwaysOnTop(true);
    dialog.add(component, BorderLayout.CENTER);
    final JPanel buttons = new JPanel();
    final JButton button =
        new JButton(
            SwingAction.of(
                "OK",
                event -> {
                  dialog.setVisible(false);
                  dialog.removeAll();
                  dialog.dispose();
                }));
    buttons.add(button);
    dialog.getRootPane().setDefaultButton(button);
    dialog.add(buttons, BorderLayout.SOUTH);
    dialog.pack();
    dialog.addWindowListener(WindowAdapterFactory.openedAndClosing(button::requestFocus, null));
    return dialog;
  }
}
