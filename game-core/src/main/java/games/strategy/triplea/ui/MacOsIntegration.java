package games.strategy.triplea.ui;

import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.function.Consumer;
import lombok.extern.java.Log;

/** Utility class to add macOS integration. */
@Log
public final class MacOsIntegration {
  private MacOsIntegration() {}

  /** Adds the specified about handler to the application. */
  public static void addAboutHandler(final Runnable handler) {
    checkNotNull(handler);
    Desktop.getDesktop().setAboutHandler(aboutEvent -> handler.run());
  }

  /** Adds the specified open URI handler to the application. */
  public static void addOpenUriHandler(final Consumer<URI> handler) {
    checkNotNull(handler);
    Desktop.getDesktop().setOpenURIHandler(openURIEvent -> handler.accept(openURIEvent.getURI()));
  }

  public static void addOpenFilesHandler(final Consumer<List<File>> handler) {
    checkNotNull(handler);
    Desktop.getDesktop().setOpenFileHandler(event -> handler.accept(event.getFiles()));
  }

  /** Adds the specified quit handler to the application. */
  public static void addQuitHandler(final Runnable handler) {
    checkNotNull(handler);
    Desktop.getDesktop().setQuitHandler((quitEvent, quitResponse) -> handler.run());
  }
}
