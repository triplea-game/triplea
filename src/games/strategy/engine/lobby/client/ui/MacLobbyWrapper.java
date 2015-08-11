package games.strategy.engine.lobby.client.ui;

import com.apple.eawt.Application;
import com.apple.eawt.ApplicationAdapter;
import com.apple.eawt.ApplicationEvent;

/**
 * TODO This class should be merged with games.strategy.common.ui.MacWrapper.
 */
public class MacLobbyWrapper {
  // keep this in its own class, otherwise we get a no class def error when
  // we try to load the game and the stubs arent in the classpath
  // i think the java validator triggers this
  public static void registerMacShutdownHandler(final LobbyFrame frame) {
    final Application application = Application.getApplication();// new Application();
    application.addApplicationListener(new ApplicationAdapter() {
      @Override
      public void handleQuit(final ApplicationEvent event) {
        if (frame != null) {
          frame.shutdown();
        } else {
          System.exit(0);
        }
      }
    });
  }
}
