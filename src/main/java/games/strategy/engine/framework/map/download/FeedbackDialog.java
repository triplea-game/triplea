package games.strategy.engine.framework.map.download;

import java.util.List;
import java.util.Optional;

import games.strategy.ui.SwingComponents;

/**
 * Simple static utility class to show a confirmation dialog to the user, if they hit accept a new browser
 * window opens with the github issues page for the map selected.
 */
public final class FeedbackDialog {
  private FeedbackDialog() {}

  /** Determines the selected map and opens a confirmation dialog asking the user if its okay to open an external URL */
  public static void showFeedbackDialog(final List<String> selectedValuesList,
      final List<DownloadFileDescription> maps) {
    if (selectedValuesList.isEmpty()) {
      return;
    }
    final Optional<DownloadFileDescription> mapSelection = findFirstSelectedMap(selectedValuesList, maps);

    if (mapSelection.isPresent()) {
      final String feedbackURL = mapSelection.get().getFeedbackUrl();
      SwingComponents.newOpenUrlConfirmationDialog(feedbackURL);
    } else {
      SwingComponents.newMessageDialog(
          "To open the map feedback from in your web browser, please first select a map title, and then click the feedback button again.");
    }
  }

  /*
   * Returns an Optional.empty() if only 'header' elements were selected, otherwise the first
   * map that was selected is returned.
   */
  private static Optional<DownloadFileDescription> findFirstSelectedMap(final List<String> selectedValuesList,
      final List<DownloadFileDescription> maps) {
    for (final String selection : selectedValuesList) {
      for (final DownloadFileDescription map : maps) {
        if (map.getMapName().equals(selection)) {
          return Optional.of(map);
        }
      }
    }
    return Optional.empty();
  }

}
