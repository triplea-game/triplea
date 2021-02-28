package games.strategy.engine.framework.ui.background;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.Optional;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import org.triplea.swing.SwingComponents;
import org.triplea.swing.key.binding.KeyCode;
import org.triplea.swing.key.binding.SwingKeyBinding;

/**
 * A dialog that can be displayed during a long-running operation that optionally provides the user
 * with the ability to cancel the operation.
 */
public final class WaitDialog extends JDialog {
  private static final long serialVersionUID = 7433959812027467868L;

  public WaitDialog(final Component parent, final String message) {
    this(parent, message, null);
  }

  public WaitDialog(final Component parent, final String message, final Runnable cancelAction) {
    super(JOptionPane.getFrameForComponent(parent), "Please Wait", true);

    setLayout(new BorderLayout());
    add(new WaitPanel(message), BorderLayout.CENTER);

    setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    Optional.ofNullable(cancelAction)
        .ifPresent(
            action -> {
              final JButton cancelButton = new JButton("Cancel");
              cancelButton.addActionListener(e -> action.run());
              add(cancelButton, BorderLayout.SOUTH);

              SwingKeyBinding.addKeyBinding(this, KeyCode.ESCAPE, action);
              SwingComponents.addWindowClosingListener(this, action);
            });

    pack();
    setLocationRelativeTo(parent);
  }
}
