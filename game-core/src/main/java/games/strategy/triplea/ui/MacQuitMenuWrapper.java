package games.strategy.triplea.ui;

import com.apple.eawt.AppEvent.QuitEvent;
import com.apple.eawt.Application;
import com.apple.eawt.QuitHandler;
import com.apple.eawt.QuitResponse;

import games.strategy.util.ExitStatus;

/**
 * Utility class to wrap Mac OS X-specific shutdown handler.
 *
 * <p>Based on TripleA code.
 *
 * <p>Needs AppleJavaExtensions.jar to compile on non-Mac platform.
 */
public final class MacQuitMenuWrapper {
  private static TripleAFrame shutdownFrame;

  static {
    Application.getApplication()
        .setQuitHandler(
            new QuitHandler() {
              @Override
              public void handleQuitRequestWith(final QuitEvent arg0, final QuitResponse arg1) {
                if (shutdownFrame != null) {
                  shutdownFrame.shutdown();
                } else {
                  ExitStatus.SUCCESS.exit();
                }
              }
            });
  }

  private MacQuitMenuWrapper() {}

  // keep this in its own class, otherwise we get a no class def error when
  // we try to load the game and the stubs arent in the classpath
  // i think the java validator triggers this
  public static void registerMacShutdownHandler(final TripleAFrame frame) {
    shutdownFrame = frame;
  }

  public static void unregisterShutdownHandler() {
    shutdownFrame = null;
  }
}
