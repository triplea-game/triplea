package games.strategy.triplea.ui.menubar;

import java.awt.event.KeyEvent;
import java.util.Optional;

import javax.swing.JMenu;

import org.triplea.swing.SwingAction;

import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.network.ui.BanPlayerAction;
import games.strategy.engine.framework.network.ui.BootPlayerAction;
import games.strategy.engine.framework.network.ui.MutePlayerAction;
import games.strategy.engine.framework.network.ui.SetPasswordAction;
import games.strategy.engine.framework.startup.login.ClientLoginValidator;
import games.strategy.engine.framework.startup.ui.InGameLobbyWatcherWrapper;
import games.strategy.net.IServerMessenger;
import games.strategy.triplea.ui.PlayersPanel;
import games.strategy.triplea.ui.TripleAFrame;

final class NetworkMenu extends JMenu {
  private static final long serialVersionUID = 2947249649948115833L;

  private final IGame game;
  private final TripleAFrame frame;

  NetworkMenu(final Optional<InGameLobbyWatcherWrapper> watcher, final TripleAFrame frame) {
    super("Network");

    this.frame = frame;
    game = frame.getGame();

    setMnemonic(KeyEvent.VK_N);

    addBootPlayer();
    addBanPlayer();
    addMutePlayer();
    watcher.ifPresent(this::addSetGamePassword);
    addShowPlayers();
  }

  private void addBootPlayer() {
    if (isServer()) {
      add(new BootPlayerAction(this, getServerMessenger()));
    }
  }

  private boolean isServer() {
    return game.getMessenger().isServer();
  }

  private IServerMessenger getServerMessenger() {
    return (IServerMessenger) game.getMessenger();
  }

  private void addBanPlayer() {
    if (isServer()) {
      add(new BanPlayerAction(this, getServerMessenger()));
    }
  }

  private void addMutePlayer() {
    if (isServer()) {
      add(new MutePlayerAction(this, getServerMessenger()));
    }
  }

  private void addSetGamePassword(final InGameLobbyWatcherWrapper watcher) {
    add(new SetPasswordAction(this, watcher, (ClientLoginValidator) getServerMessenger().getLoginValidator()));
  }

  private void addShowPlayers() {
    if (!game.getData().getProperties().getEditableProperties().isEmpty()) {
      add(SwingAction.of("Show Who is Who", e -> PlayersPanel.showPlayers(game, frame)));
    }
  }
}
