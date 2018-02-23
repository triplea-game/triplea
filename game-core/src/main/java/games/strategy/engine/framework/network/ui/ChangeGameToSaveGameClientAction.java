package games.strategy.engine.framework.network.ui;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;

import games.strategy.engine.framework.startup.ui.GameSelectorPanel;
import games.strategy.net.IClientMessenger;

/**
 * An action for loading a save game across all network nodes from a client node.
 */
public class ChangeGameToSaveGameClientAction extends AbstractAction {
  private static final long serialVersionUID = -6986376382381381377L;
  private final IClientMessenger clientMessenger;

  public ChangeGameToSaveGameClientAction(final IClientMessenger clientMessenger) {
    super("Change To Gamesave (Load Game)");
    this.clientMessenger = clientMessenger;
  }

  @Override
  public void actionPerformed(final ActionEvent e) {
    final File file = GameSelectorPanel.selectGameFile();
    if ((file == null) || !file.exists()) {
      return;
    }
    clientMessenger.changeToGameSave(file, file.getName());
  }
}
