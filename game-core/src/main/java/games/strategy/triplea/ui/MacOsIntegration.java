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

  /** Sets the specified about handler to the application. */
  public static void setAboutHandler(final Runnable handler) {
    checkNotNull(handler);
    Desktop.getDesktop().setAboutHandler(aboutEvent -> handler.run());
  }

  /** Sets the specified open URI handler to the application. */
  public static void setOpenUriHandler(final Consumer<URI> handler) {
    checkNotNull(handler);
    Desktop.getDesktop().setOpenURIHandler(openURIEvent -> handler.accept(openURIEvent.getURI()));
  }

  /** Sets the specified open files handler to the application. */
  public static void setOpenFilesHandler(final Consumer<List<File>> handler) {
    checkNotNull(handler);
    Desktop.getDesktop().setOpenFileHandler(event -> handler.accept(event.getFiles()));
  }

  /** Sets the specified quit handler to the application. */
  public static void setQuitHandler(final Runnable handler) {
    checkNotNull(handler);
    Desktop.getDesktop().setQuitHandler((quitEvent, quitResponse) -> handler.run());
  }
}
