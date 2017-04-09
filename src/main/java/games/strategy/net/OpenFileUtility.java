package games.strategy.net;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;

import games.strategy.debug.ClientLogger;
import games.strategy.ui.SwingComponents;

/**
 * A wrapper class for opening Files & URLs using the Desktop API.
 */
public class OpenFileUtility {

  /**
   * Opens a specific file on the user's computer using the local computer's file associations.
   *
   * @param file The file to be opened
   */
  public static void openFile(final File file) {
    if (Desktop.isDesktopSupported()) {
      try {
        Desktop.getDesktop().open(file);
      } catch (IOException e) {
        ClientLogger.logError("Could not open File " + file.getAbsolutePath(), e);
      }
    } else {
      logDesktopAPIMessage(file.getAbsolutePath());
    }
  }

  /**
   * Opens the specified web page in the user's default browser
   *
   * @param url An URL of a web page (ex: "http://www.google.com/")
   */
  public static void openURL(final String url) {
    if (Desktop.isDesktopSupported()) {
      try {
        Desktop.getDesktop().browse(URI.create(url));
      } catch (IOException e) {
        ClientLogger.logError("Could not open URL " + url, e);
      }
    } else {
      logDesktopAPIMessage(url);
    }
  }

  private static void logDesktopAPIMessage(String path) {
    SwingComponents.showDialog("Desktop API not supported",
        "We're sorry, but it seems that your installed java version doesn't support the Desktop API required to open "
            + path);
  }
}
