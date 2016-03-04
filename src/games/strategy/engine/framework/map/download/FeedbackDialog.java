package games.strategy.engine.framework.map.download;

import java.util.List;
import java.util.Optional;

import games.strategy.common.swing.SwingComponents;
import games.strategy.net.DesktopUtilityBrowserLauncher;

/**
 * Simple static utility class to show a confirmation dialog to the user, if they hit accept a new browser
 * window opens with the github issues page for the map selected.
 */
public final class FeedbackDialog {
  private FeedbackDialog() {}

  /** Determines the selected map and opens a confirmation dialog asking the user if its okay to open an external URL */
  public static void showFeedbackDialog(List<String> selectedValuesList, List<DownloadFileDescription> maps) {
    if (selectedValuesList.isEmpty()) {
      return;
    }
    Optional<DownloadFileDescription> mapSelection = findFirstSelectedMap(selectedValuesList, maps);

    if (mapSelection.isPresent()) {
      final String feedbackURL = mapSelection.get().getFeedbackUrl();
      final String msg = "Is it okay to open the feedback form in a web browser?\n" + feedbackURL;
      SwingComponents.promptUser("Okay to open external URL?", msg, () -> {
        DesktopUtilityBrowserLauncher.openURL(feedbackURL);
      });
    } else {
      SwingComponents.newMessageDialog(
          "To open the map feedback from in your web browser, please first select a map title, and then click the feedback button again.");
    }
  }

  /*
   * Returns an Optional.empty() if only 'header' elements were selected, otherwise the first
   * map that was selected is returned.
   */
  private static Optional<DownloadFileDescription> findFirstSelectedMap(List<String> selectedValuesList,
      List<DownloadFileDescription> maps) {
    for (String selection : selectedValuesList) {
      for (DownloadFileDescription map : maps) {
        if (!map.isDummyUrl() && map.getMapName().equals(selection)) {
          return Optional.of(map);
        }
      }
    }
    return Optional.empty();
  }

}
