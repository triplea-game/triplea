package games.strategy.triplea.ui.menubar;

import java.awt.event.KeyEvent;
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;

import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.networkMaintenance.BanPlayerAction;
import games.strategy.engine.framework.networkMaintenance.BootPlayerAction;
import games.strategy.engine.framework.networkMaintenance.MutePlayerAction;
import games.strategy.engine.framework.networkMaintenance.SetPasswordAction;
import games.strategy.engine.framework.startup.login.ClientLoginValidator;
import games.strategy.engine.framework.startup.ui.InGameLobbyWatcherWrapper;
import games.strategy.net.IServerMessenger;
import games.strategy.triplea.ui.PlayersPanel;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.ui.SwingAction;

class NetworkMenu {

  private final IGame game;
  private final TripleAFrame frame;

  NetworkMenu(final TripleAMenuBar menuBar, final Optional<InGameLobbyWatcherWrapper> watcher,
      final TripleAFrame frame) {
    this.frame = frame;
    game = frame.getGame();
    final JMenu menuNetwork = new JMenu("Network");
    menuNetwork.setMnemonic(KeyEvent.VK_N);
    addBootPlayer(menuNetwork);
    addBanPlayer(menuNetwork);
    addMutePlayer(menuNetwork);
    addSetGamePassword(menuNetwork, watcher);
    addShowPlayers(menuNetwork);
    menuBar.add(menuNetwork);
  }

  private void addBootPlayer(final JMenu parentMenu) {
    if (!game.getMessenger().isServer()) {
      return;
    }
    final IServerMessenger messenger = (IServerMessenger) game.getMessenger();
    final Action boot = new BootPlayerAction(parentMenu, messenger);
    parentMenu.add(boot);
  }

  private void addBanPlayer(final JMenu parentMenu) {
    if (!game.getMessenger().isServer()) {
      return;
    }
    final IServerMessenger messenger = (IServerMessenger) game.getMessenger();
    final Action ban = new BanPlayerAction(parentMenu, messenger);
    parentMenu.add(ban);
  }

  private void addMutePlayer(final JMenu parentMenu) {
    if (!game.getMessenger().isServer()) {
      return;
    }
    final IServerMessenger messenger = (IServerMessenger) game.getMessenger();
    final Action mute = new MutePlayerAction(parentMenu, messenger);
    parentMenu.add(mute);
  }

  private void addSetGamePassword(final JMenu parentMenu, final Optional<InGameLobbyWatcherWrapper> watcher) {
    if (!watcher.isPresent()) {
      return;
    }
    final IServerMessenger messenger = (IServerMessenger) game.getMessenger();
    parentMenu
        .add(new SetPasswordAction(parentMenu, watcher.get(), (ClientLoginValidator) messenger.getLoginValidator()));
  }

  private void addShowPlayers(final JMenu menuGame) {
    if (!game.getData().getProperties().getEditableProperties().isEmpty()) {
      final AbstractAction optionsAction =
          SwingAction.of("Show Who is Who", e -> PlayersPanel.showPlayers(game, frame));
      menuGame.add(optionsAction);
    }
  }


}
