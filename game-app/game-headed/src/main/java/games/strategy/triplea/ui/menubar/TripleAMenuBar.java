package games.strategy.triplea.ui.menubar;

import games.strategy.engine.framework.startup.ui.InGameLobbyWatcherWrapper;
import games.strategy.engine.lobby.client.ui.action.EditGameCommentAction;
import games.strategy.engine.lobby.client.ui.action.RemoveGameFromLobbyAction;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.triplea.ui.menubar.help.HelpMenu;
import java.util.Optional;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import lombok.experimental.UtilityClass;
import org.triplea.swing.JMenuBuilder;
import org.triplea.swing.JMenuItemBuilder;
import org.triplea.swing.key.binding.KeyCode;

/** The game client menu bar. */
@UtilityClass
public final class TripleAMenuBar {

  public static JMenuBar get(final TripleAFrame frame) {
    final JMenuBar menuBar = new JMenuBar();
    menuBar.add(FileMenu.get(frame));
    menuBar.add(ViewMenu.get(frame));
    menuBar.add(GameMenu.get(frame));
    menuBar.add(ExportMenu.get(frame));

    final Optional<InGameLobbyWatcherWrapper> watcher = frame.getInGameLobbyWatcher();
    watcher
        .filter(InGameLobbyWatcherWrapper::isActive)
        .ifPresent(watcherWrapper -> menuBar.add(getLobbyMenu(frame, watcherWrapper)));
    if (frame.getGame().getMessengers().isConnected()) {
      menuBar.add(NetworkMenu.get(watcher, frame));
    }

    menuBar.add(DebugMenu.get(frame));
    menuBar.add(HelpMenu.buildMenu(frame, frame.getUiContext(), frame.getGame().getData()));

    return menuBar;
  }

  private static JMenu getLobbyMenu(
      final TripleAFrame frame, final InGameLobbyWatcherWrapper watcher) {
    return new JMenuBuilder("Lobby", KeyCode.L)
        .addMenuItem(new JMenuItemBuilder(new EditGameCommentAction(watcher, frame), KeyCode.E))
        .addMenuItem(new JMenuItemBuilder(new RemoveGameFromLobbyAction(watcher), KeyCode.R))
        .build();
  }
}
