package games.strategy.engine.framework.ui.background;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.annotation.Nullable;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

import games.strategy.ui.SwingComponents;

/**
 * A dialog that can be displayed during a long-running operation that optionally provides the user with the ability to
 * cancel the operation.
 */
public final class WaitDialog extends JDialog {
  private static final long serialVersionUID = 7433959812027467868L;

  public WaitDialog(final Component parent, final String message) {
    this(parent, message, null);
  }

  public WaitDialog(final Component parent, final String message, final @Nullable Runnable cancelAction) {
    super(JOptionPane.getFrameForComponent(parent), "Please Wait", true);

    setLayout(new BorderLayout());
    add(new WaitPanel(message), BorderLayout.CENTER);

    setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    if (cancelAction != null) {
      final JButton cancelButton = new JButton("Cancel");
      cancelButton.addActionListener(e -> cancelAction.run());
      add(cancelButton, BorderLayout.SOUTH);

      SwingComponents.addEscapeKeyListener(this, cancelAction);
      SwingComponents.addWindowClosingListener(this, cancelAction);
    }

    pack();
    setLocationRelativeTo(parent);
  }
}
