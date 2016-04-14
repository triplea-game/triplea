package games.strategy.engine.lobby.client.ui;

import com.apple.eawt.AppEvent.QuitEvent;
import com.apple.eawt.Application;
import com.apple.eawt.QuitHandler;
import com.apple.eawt.QuitResponse;

/**
 * TODO This class should be merged with games.strategy.common.ui.MacWrapper.
 */
public class MacLobbyWrapper {
  // keep this in its own class, otherwise we get a no class def error when
  // we try to load the game and the stubs arent in the classpath
  // i think the java validator triggers this
  public static void registerMacShutdownHandler(final LobbyFrame frame) {
    // new Application();
    Application.getApplication().setQuitHandler(new QuitHandler(){
      @Override
      public void handleQuitRequestWith(QuitEvent arg0, QuitResponse arg1) {
        if (frame != null) {
          frame.shutdown();
        } else {
          System.exit(0);
        }
      }
    });
  }
}
