package games.strategy.engine.framework.networkMaintenance;

import java.awt.event.ActionEvent;
import java.io.File;
import javax.swing.AbstractAction;

import games.strategy.engine.framework.startup.ui.GameSelectorPanel;
import games.strategy.net.IClientMessenger;

public class ChangeGameToSaveGameClientAction extends AbstractAction {
  private static final long serialVersionUID = -6986376382381381377L;
  private final IClientMessenger m_clientMessenger;

  public ChangeGameToSaveGameClientAction(final IClientMessenger clientMessenger) {
    super("Change To Gamesave (Load Game)");
    m_clientMessenger = clientMessenger;
  }

  @Override
  public void actionPerformed(final ActionEvent e) {
    final File file = GameSelectorPanel.selectGameFile();
    if (file == null || !file.exists()) {
      return;
    }
    m_clientMessenger.changeToGameSave(file, file.getName());
  }
}
