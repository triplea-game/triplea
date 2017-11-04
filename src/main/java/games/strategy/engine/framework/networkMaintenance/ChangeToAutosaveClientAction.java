package games.strategy.engine.framework.networkMaintenance;

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import games.strategy.engine.framework.ui.SaveGameFileChooser;
import games.strategy.net.IClientMessenger;

public class ChangeToAutosaveClientAction extends AbstractAction {
  private static final long serialVersionUID = 1972868158345085949L;
  private final Component parent;
  private final IClientMessenger clientMessenger;
  private final SaveGameFileChooser.AUTOSAVE_TYPE typeOfAutosave;

  public ChangeToAutosaveClientAction(final Component parent, final IClientMessenger clientMessenger,
      final SaveGameFileChooser.AUTOSAVE_TYPE typeOfAutosave) {
    super("Change To " + typeOfAutosave.toString().toLowerCase());
    this.parent = JOptionPane.getFrameForComponent(parent);
    this.clientMessenger = clientMessenger;
    this.typeOfAutosave = typeOfAutosave;
  }

  @Override
  public void actionPerformed(final ActionEvent e) {
    final int selectedOption = JOptionPane.showConfirmDialog(parent,
        new JLabel("Change Game To: " + typeOfAutosave.toString().toLowerCase()),
        "Change Game To: " + typeOfAutosave.toString().toLowerCase(), JOptionPane.OK_CANCEL_OPTION,
        JOptionPane.QUESTION_MESSAGE);
    if (selectedOption != JOptionPane.OK_OPTION) {
      return;
    }
    clientMessenger.changeToLatestAutosave(typeOfAutosave);
  }
}
