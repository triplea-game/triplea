package org.triplea.awt;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import lombok.extern.java.Log;
import org.triplea.swing.SwingComponents;

/** A wrapper class for opening Files & URLs using the Desktop API. */
@Log
public final class OpenFileUtility {
  private OpenFileUtility() {}

  /**
   * Opens a specific file on the user's computer using the local computer's file associations.
   *
   * @param file The file to be opened.
   * @param action What to do if the Desktop API is not supported.
   */
  public static void openFile(final File file, final Runnable action) {
    if (Desktop.isDesktopSupported()) {
      try {
        Desktop.getDesktop().open(file);
      } catch (final IOException e) {
        log.log(Level.SEVERE, "Could not open File " + file.getAbsolutePath(), e);
      }
    } else {
      action.run();
    }
  }

  /**
   * Opens the specified web page in the user's default browser.
   *
   * @param url A URL of a web page (ex: "http://www.google.com/").
   * @param action What to do if the Desktop API is not supported.
   */
  public static void openUrl(final String url, final Runnable action) {
    if (Desktop.isDesktopSupported()) {
      try {
        Desktop.getDesktop().browse(URI.create(url));
      } catch (final IOException e) {
        log.log(Level.SEVERE, "Could not open URL " + url, e);
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
  public static void openUrl(final String url) {
    openUrl(url, () -> logDesktopApiMessage(url));
  }

  private static void logDesktopApiMessage(final String path) {
    SwingComponents.showDialog(
        "Desktop API not supported",
        "We're sorry, but it seems that your installed java version doesn't support the "
            + "Desktop API required to open "
            + path);
  }
}
