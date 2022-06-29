package games.strategy.engine.auto.update;

import games.strategy.engine.framework.map.download.DownloadMapsWindow;
import games.strategy.engine.framework.map.download.MapDownloadController;
import org.triplea.swing.SwingComponents;

final class TutorialMapCheck {
  private TutorialMapCheck() {}

  static void checkForTutorialMap() {
    final boolean promptToDownloadTutorialMap =
        MapDownloadController.shouldPromptToDownloadTutorialMap();
    MapDownloadController.preventPromptToDownloadTutorialMap();
    if (!promptToDownloadTutorialMap) {
      return;
    }

    final String message =
        "<html>Would you like to download the tutorial map?<br><br>"
            + "(You can always download it later using the Download Maps<br>"
            + "command if you don't want to do it now.)</html>";
    SwingComponents.promptUser(
        "Welcome to TripleA",
        message,
        () -> DownloadMapsWindow.showDownloadMapsWindowAndDownload("Tutorial"));
  }
}
