package games.strategy.engine.framework.ui.background;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.Optional;

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
    this(parent, message, Optional.empty());
  }

  public WaitDialog(final Component parent, final String message, final Runnable cancelAction) {
    this(parent, message, Optional.of(cancelAction));
  }

  private WaitDialog(final Component parent, final String message, final Optional<Runnable> cancelAction) {
    super(JOptionPane.getFrameForComponent(parent), "Please Wait", true);

    setLayout(new BorderLayout());
    add(new WaitPanel(message), BorderLayout.CENTER);

    setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    cancelAction.ifPresent(it -> {
      final JButton cancelButton = new JButton("Cancel");
      cancelButton.addActionListener(e -> it.run());
      add(cancelButton, BorderLayout.SOUTH);

      SwingComponents.addEscapeKeyListener(this, it);
      SwingComponents.addWindowClosingListener(this, it);
    });

    pack();
    setLocationRelativeTo(parent);
  }
}
