package games.strategy.engine.framework.map.download;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.framework.map.listing.MapListingFetcher;
import games.strategy.triplea.settings.ClientSetting;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.triplea.swing.SwingComponents;
import org.triplea.util.Version;

/** Controller for in-game map download actions. */
@Slf4j
@UtilityClass
public final class MapDownloadController {

  /**
   * Indicates the user should be prompted to download the tutorial map.
   *
   * @return {@code true} if the user should be prompted to download the tutorial map; otherwise
   *     {@code false}.
   */
  public static boolean shouldPromptToDownloadTutorialMap() {
    return shouldPromptToDownloadTutorialMap(getTutorialMapPreferences(), getUserMaps());
  }

  @VisibleForTesting
  static boolean shouldPromptToDownloadTutorialMap(
      final TutorialMapPreferences tutorialMapPreferences, final UserMaps userMaps) {
    return tutorialMapPreferences.canPromptToDownload() && userMaps.isEmpty();
  }

  @VisibleForTesting
  interface TutorialMapPreferences {
    boolean canPromptToDownload();

    void preventPromptToDownload();
  }

  private static TutorialMapPreferences getTutorialMapPreferences() {
    return new TutorialMapPreferences() {
      @Override
      public void preventPromptToDownload() {
        ClientSetting.promptToDownloadTutorialMap.setValue(false);
        ClientSetting.flush();
      }

      @Override
      public boolean canPromptToDownload() {
        return ClientSetting.promptToDownloadTutorialMap.getValueOrThrow();
      }
    };
  }

  @VisibleForTesting
  interface UserMaps {
    boolean isEmpty();
  }

  private static UserMaps getUserMaps() {
    return () -> {
      final String[] entries = ClientFileSystemHelper.getUserMapsFolder().list();
      return entries == null || entries.length == 0;
    };
  }

  /** Prevents the user from being prompted to download the tutorial map. */
  public static void preventPromptToDownloadTutorialMap() {
    preventPromptToDownloadTutorialMap(getTutorialMapPreferences());
  }

  @VisibleForTesting
  static void preventPromptToDownloadTutorialMap(
      final TutorialMapPreferences tutorialMapPreferences) {
    tutorialMapPreferences.preventPromptToDownload();
  }
}
