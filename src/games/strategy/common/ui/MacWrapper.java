package games.strategy.common.ui;

import com.apple.eawt.Application;
import com.apple.eawt.ApplicationAdapter;
import com.apple.eawt.ApplicationEvent;

/**
 * Utility class to wrap Mac OS X-specific shutdown handler.
 * <p>
 * Based on TripleA code.
 * <p>
 * Needs AppleJavaExtensions.jar to compile on non-Mac platform.
 */
public class MacWrapper {
  private static MainGameFrame shutdownFrame;

  static {
    Application.getApplication().addApplicationListener(new ApplicationAdapter() {
      @Override
      public void handleQuit(final ApplicationEvent event) {
        if (shutdownFrame != null) {
          shutdownFrame.shutdown();
        } else {
          System.exit(0);
        }
      }
    });
  }

  // keep this in its own class, otherwise we get a no class def error when
  // we try to load the game and the stubs arent in the classpath
  // i think the java validator triggers this
  public static void registerMacShutdownHandler(final MainGameFrame frame) {
    shutdownFrame = frame;
  }

  public static void unregisterShutdownHandler() {
    shutdownFrame = null;
  }
}
