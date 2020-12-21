package games.strategy.engine.framework.map.download;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.framework.map.listing.MapListingFetcher;
import games.strategy.triplea.ResourceLoader;
import games.strategy.triplea.settings.ClientSetting;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.triplea.swing.SwingComponents;
import org.triplea.util.Version;

/** Controller for in-game map download actions. */
@Slf4j
public final class MapDownloadController {
  private MapDownloadController() {}

  /** Prompts user to download map updates if maps are out of date. */
  public static void checkDownloadedMapsAreLatest() {

    try {
      final Collection<String> outOfDateMapNames =
          getOutOfDateMapNames(MapListingFetcher.getMapDownloadList(), getDownloadedMaps());
      if (!outOfDateMapNames.isEmpty()) {
        final StringBuilder text = new StringBuilder();
        text.append(
            "<html>Some of the maps you have are out of date, and newer versions of those "
                + "maps exist.<br><br>");
        text.append("Would you like to update (re-download) the following maps now?<br><ul>");
        for (final String mapName : outOfDateMapNames) {
          text.append("<li> ").append(mapName).append("</li>");
        }
        text.append("</ul></html>");
        SwingComponents.promptUser(
            "Update Your Maps?",
            text.toString(),
            () -> DownloadMapsWindow.showDownloadMapsWindowAndDownload(outOfDateMapNames));
      }
    } catch (final Exception e) {
      log.warn("Failed to getting list of most recent maps", e);
    }
  }

  @VisibleForTesting
  static Collection<String> getOutOfDateMapNames(
      final Collection<DownloadFileDescription> downloads, final DownloadedMaps downloadedMaps) {
    return downloads.stream()
        .filter(Objects::nonNull)
        .filter(it -> isMapOutOfDate(it, downloadedMaps))
        .map(DownloadFileDescription::getMapName)
        .collect(Collectors.toList());
  }

  private static boolean isMapOutOfDate(
      final DownloadFileDescription download, final DownloadedMaps downloadedMaps) {
    final Optional<Version> latestVersion = Optional.ofNullable(download.getVersion());
    final Optional<Version> downloadedVersion =
        getDownloadedVersion(download.getMapName(), downloadedMaps);

    final AtomicBoolean mapOutOfDate = new AtomicBoolean(false);
    latestVersion.ifPresent(
        latest ->
            downloadedVersion.ifPresent(
                downloaded -> mapOutOfDate.set(latest.isGreaterThan(downloaded))));
    return mapOutOfDate.get();
  }

  private static Optional<Version> getDownloadedVersion(
      final String mapName, final DownloadedMaps downloadedMaps) {
    return downloadedMaps.getZipFileCandidates(mapName).stream()
        .map(downloadedMaps::getVersionForZipFile)
        .filter(Optional::isPresent)
        .findFirst()
        .orElseGet(Optional::empty);
  }

  @VisibleForTesting
  interface DownloadedMaps {
    Optional<Version> getVersionForZipFile(File mapZipFile);

    List<File> getZipFileCandidates(String mapName);
  }

  private static DownloadedMaps getDownloadedMaps() {
    return new DownloadedMaps() {
      @Override
      public Optional<Version> getVersionForZipFile(final File mapZipFile) {
        return DownloadFileProperties.loadForZip(mapZipFile).getVersion();
      }

      @Override
      public List<File> getZipFileCandidates(final String mapName) {
        return ResourceLoader.getMapZipFileCandidates(
            mapName, ClientFileSystemHelper.getUserMapsFolder());
      }
    };
  }

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
      final int entryCount = Optional.ofNullable(entries).map(it -> it.length).orElse(0);
      return entryCount == 0;
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
