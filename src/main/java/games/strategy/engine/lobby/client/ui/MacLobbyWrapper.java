package games.strategy.engine.lobby.client.ui;

import com.apple.eawt.Application;

/**
 * TODO This class should be merged with games.strategy.triplea.ui.MacQuitMenuWrapper.
 */
public class MacLobbyWrapper {
  // keep this in its own class, otherwise we get a no class def error when
  // we try to load the game and the stubs arent in the classpath
  // i think the java validator triggers this
  public static void registerMacShutdownHandler(final LobbyFrame frame) {
    Application.getApplication().setQuitHandler((quitEvent, quitResponse) -> {
      if (frame != null) {
        frame.shutdown();
      } else {
        System.exit(0);
      }
    });
  }
}
