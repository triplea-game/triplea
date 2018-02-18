package games.strategy.ui;

import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

/**
 * A modal dialog used to display indeterminate progress during an operation.
 */
public final class ProgressDialog extends JDialog {
  private static final long serialVersionUID = -590470596784214914L;

  /**
   * Initializes a new instance of the {@code ProgressDialog} class.
   *
   * @param owner The {@code Frame} from which the dialog is displayed or {@code null} to use a shared, hidden frame as
   *        the owner of the dialog.
   * @param message The progress message; must not be {@code null}.
   */
  public ProgressDialog(final Frame owner, final String message) {
    super(owner, true);

    checkNotNull(message);

    setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    setUndecorated(true);

    setLayout(new BorderLayout());
    add(createContent(message), BorderLayout.CENTER);

    pack();

    setSize(200, 80);
    setLocationRelativeTo(owner);
  }

  private static Component createContent(final String message) {
    final JPanel panel = new JPanel();
    panel.setBorder(new LineBorder(Color.BLACK));
    panel.setLayout(new BorderLayout());

    final JLabel label = new JLabel(message);
    label.setBorder(new EmptyBorder(10, 10, 10, 10));
    panel.add(BorderLayout.NORTH, label);

    final JProgressBar progressBar = new JProgressBar();
    progressBar.setBorder(new EmptyBorder(10, 10, 10, 10));
    progressBar.setIndeterminate(true);
    panel.add(progressBar, BorderLayout.CENTER);

    return panel;
  }
}
