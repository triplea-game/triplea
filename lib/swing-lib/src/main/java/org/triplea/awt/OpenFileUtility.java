package org.triplea.awt;

import java.awt.Component;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.triplea.swing.SwingComponents;

/** A wrapper class for opening Files & URLs using the Desktop API. */
@Slf4j
public final class OpenFileUtility {
  private OpenFileUtility() {}

  /**
   * Opens the specified web page in the user's default browser.
   *
   * @param url A URL of a web page (ex: "http://www.google.com/").
   * @param action What to do if the Desktop API is not supported.
   */
  public static void openUrl(final String url, final Runnable action) {
    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
      try {
        Desktop.getDesktop().browse(URI.create(url));
      } catch (final IOException e) {
        log.error("Could not open URL " + url, e);
      }
    } else {
      action.run();
    }
  }

  /**
   * Opens the specified web page in the user's default browser.
   *
   * @param url A URL of a web page (ex: "http://www.google.com/")
   */
  public static void openUrl(final Component parentComponent, final String url) {
    openUrl(url, () -> logDesktopApiMessage(parentComponent, url));
  }

  private static void logDesktopApiMessage(final Component parentComponent, final String path) {
    SwingComponents.showDialog(
        parentComponent,
        "Desktop API not supported",
        "We're sorry, but it seems that your installed java version doesn't support the "
            + "Desktop API required to open "
            + path);
  }
}
