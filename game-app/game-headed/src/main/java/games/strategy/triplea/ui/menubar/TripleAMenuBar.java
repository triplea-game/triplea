package games.strategy.triplea.ui.menubar;

import games.strategy.engine.framework.startup.ui.InGameLobbyWatcherWrapper;
import games.strategy.engine.lobby.client.ui.action.EditGameCommentAction;
import games.strategy.engine.lobby.client.ui.action.RemoveGameFromLobbyAction;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.triplea.ui.menubar.help.HelpMenu;
import java.awt.event.KeyEvent;
import java.util.Optional;
import javax.swing.JMenu;
import javax.swing.JMenuBar;

/** The game client menu bar. */
public final class TripleAMenuBar extends JMenuBar {
  private static final long serialVersionUID = -1447295944297939539L;

  private final TripleAFrame frame;

  public TripleAMenuBar(final TripleAFrame frame) {
    this.frame = frame;

    add(new FileMenu(frame));
    add(new ViewMenu(frame));
    add(new GameMenu(frame));
    add(new ExportMenu(frame));

    final Optional<InGameLobbyWatcherWrapper> watcher = frame.getInGameLobbyWatcher();
    watcher.filter(InGameLobbyWatcherWrapper::isActive).ifPresent(this::createLobbyMenu);
    if (frame.getGame().getMessengers().isConnected()) {
      add(new NetworkMenu(watcher, frame));
    }

    add(new DebugMenu(frame));
    add(HelpMenu.buildMenu(frame, frame.getUiContext(), frame.getGame().getData()));
  }

  private void createLobbyMenu(final InGameLobbyWatcherWrapper watcher) {
    final JMenu lobby = new JMenu("Lobby");
    lobby.setMnemonic(KeyEvent.VK_L);
    add(lobby);
    lobby.add(new EditGameCommentAction(watcher, frame));
    lobby.add(new RemoveGameFromLobbyAction(watcher));
  }
}
