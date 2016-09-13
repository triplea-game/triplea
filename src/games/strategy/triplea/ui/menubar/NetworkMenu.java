package games.strategy.triplea.ui.menubar;

import java.util.Optional;

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
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

public class NetworkMenu {

  private final IGame game;

  public NetworkMenu(final TripleAMenuBar menuBar, final Optional<InGameLobbyWatcherWrapper> watcher,
      final TripleAFrame frame) {
    game = frame.getGame();
    final Menu menuNetwork = new Menu("_Network");
    addBootPlayer(menuNetwork);
    addBanPlayer(menuNetwork);
    addMutePlayer(menuNetwork);
    addSetGamePassword(menuNetwork, watcher);
    addShowPlayers(menuNetwork);
    menuBar.getMenus().add(menuNetwork);
  }

  private void addBootPlayer(final Menu parentMenu) {
    if (!game.getMessenger().isServer()) {
      return;
    }
    final IServerMessenger messenger = (IServerMessenger) game.getMessenger();
    parentMenu.getItems().add(new BootPlayerAction(messenger));
  }

  private void addBanPlayer(final Menu parentMenu) {
    if (!game.getMessenger().isServer()) {
      return;
    }
    final IServerMessenger messenger = (IServerMessenger) game.getMessenger();
    parentMenu.getItems().add(new BanPlayerAction(messenger));
  }

  private void addMutePlayer(final Menu parentMenu) {
    if (!game.getMessenger().isServer()) {
      return;
    }
    final IServerMessenger messenger = (IServerMessenger) game.getMessenger();
    parentMenu.getItems().add(new MutePlayerAction(messenger));
  }

  private void addSetGamePassword(final Menu parentMenu, final Optional<InGameLobbyWatcherWrapper> watcher) {
    if (!watcher.isPresent()) {
      return;
    }
    final IServerMessenger messenger = (IServerMessenger) game.getMessenger();
    parentMenu
        .getItems().add(new SetPasswordAction(watcher.get(), (ClientLoginValidator) messenger.getLoginValidator()));
  }

  private void addShowPlayers(final Menu menuGame) {
    if (!game.getData().getProperties().getEditableProperties().isEmpty()) {
      MenuItem showPlayers = new MenuItem("Show Who is Who");
      showPlayers.setOnAction(e -> PlayersPanel.showPlayers(game));
      menuGame.getItems().add(showPlayers);
    }
  }


}
