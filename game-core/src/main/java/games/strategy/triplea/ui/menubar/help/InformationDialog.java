package games.strategy.triplea.ui.menubar.help;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import lombok.experimental.UtilityClass;
import org.triplea.swing.SwingAction;

@UtilityClass
class InformationDialog {
  static JDialog createDialog(final JComponent component, final String title) {
    final JDialog dialog = new JDialog((JFrame) null, title);
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
    dialog.addWindowListener(
        new WindowAdapter() {
          @Override
          public void windowOpened(final WindowEvent e) {
            button.requestFocus();
          }
        });
    return dialog;
  }
}
