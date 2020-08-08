package games.strategy.triplea.ui;

import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.util.function.Consumer;

/** Utility class to add macOS integration. */
public final class MacOsIntegration {
  private MacOsIntegration() {}

  /** Sets the specified about handler to the application. */
  public static void setAboutHandler(final Runnable handler) {
    checkNotNull(handler);
    Desktop.getDesktop().setAboutHandler(aboutEvent -> handler.run());
  }

  /** Resets the about handler to the default one for the application. */
  public static void clearAboutHandler() {
    Desktop.getDesktop().setAboutHandler(null);
  }

  /** Sets the specified open URI handler to the application. */
  public static void setOpenUriHandler(final Consumer<URI> handler) {
    checkNotNull(handler);
    Desktop.getDesktop().setOpenURIHandler(openURIEvent -> handler.accept(openURIEvent.getURI()));
  }

  /**
   * Sets the specified open file handler to the application. Note that while the API technically
   * allows for multiple files to be opened at once this doesn't make sense for currently existing
   * use-cases. Therefore this feature can't be accessed via this wrapper.
   */
  public static void setOpenFileHandler(final Consumer<File> handler) {
    checkNotNull(handler);
    Desktop.getDesktop()
        .setOpenFileHandler(event -> event.getFiles().stream().findAny().ifPresent(handler));
  }

  /** Sets the specified quit handler to the application. */
  public static void setQuitHandler(final QuitHandler handler) {
    checkNotNull(handler);
    Desktop.getDesktop()
        .setQuitHandler(
            (quitEvent, quitResponse) -> {
              if (handler.shutdown()) {
                quitResponse.performQuit();
              } else {
                quitResponse.cancelQuit();
              }
            });
  }
}
