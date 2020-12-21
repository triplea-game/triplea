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

  /** Prompts user to download map updates if maps are out of date. */
  public static void checkDownloadedMapsAreLatest() {
    // {map name -> version}
    final Map<String, Version> availableToDownloadMapVersions =
        downloadAvailableMapsListAndComputeAvailableVersions();

    if (availableToDownloadMapVersions.isEmpty()) {
      // A failure happened getting maps. User is already notified.
      return;
    }

    // {property file name -> version}
    final Map<String, Version> installedMapVersions = readMapPropertyFilesForInstalledMapVersions();

    final Collection<String> outOfDateMapNames =
        computeOutOfDateMaps(installedMapVersions, availableToDownloadMapVersions);

    if (!outOfDateMapNames.isEmpty()) {
      promptUserToUpdateMaps(outOfDateMapNames);
    }
  }

  private static Map<String, Version> readMapPropertyFilesForInstalledMapVersions() {
    return
    // get all .property files in the downloads folder
    Arrays.stream(ClientFileSystemHelper.getUserMapsFolder().listFiles())
        .filter(file -> file.getName().endsWith(".properties"))
        // Read each property file to find map version
        // Create map of {property file name -> optional<version>}
        .collect(
            Collectors.toMap(File::getName, MapDownloadController::readVersionFromPropertyFile))
        // loop back over the map
        .entrySet()
        .stream()
        // Keep only entries that have a version (optional is present)
        .filter(entry -> entry.getValue().isPresent())
        // Now that all optionals are guaranteed to hold a value, unwrap them &
        // normalize the map names.
        // Convert from:
        //     {property file name -> Optional<Version>}
        //    to:
        //     {property file name -> Version}
        .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().get()));
  }

  private static Optional<Version> readVersionFromPropertyFile(final File propertyFile) {
    return DownloadFileProperties.loadForZipPropertyFile(propertyFile).getVersion();
  }

  private static Map<String, Version> downloadAvailableMapsListAndComputeAvailableVersions() {
    try {
      return MapListingFetcher.getMapDownloadList().stream()
          .collect(
              Collectors.toMap(
                  downloadFileDescription -> normalizeName(downloadFileDescription.getMapName()),
                  DownloadFileDescription::getVersion));
    } catch (final Exception e) {
      log.warn("Failed to getting list of most recent maps", e);
      return Map.of();
    }
  }

  private static Collection<String> computeOutOfDateMaps(
      final Map<String, Version> installedMapVersions,
      final Map<String, Version> availableToDownloadMapVersions) {

    final Collection<String> outOfDateMapNames = new ArrayList<>();

    // Loop over all available maps, check if we have that map present by comparing
    // normalized names, if so, check versions and remember any that are out of date.
    for (final Map.Entry<String, Version> availableMap :
        availableToDownloadMapVersions.entrySet()) {
      final String availableMapName = normalizeName(availableMap.getKey());

      for (final Map.Entry<String, Version> installedMap : installedMapVersions.entrySet()) {
        final String installedMapName = normalizeName(installedMap.getKey());
        if (installedMapName.equals(availableMapName)) {

          if (availableMap.getValue().isGreaterThan(installedMap.getValue())) {
            outOfDateMapNames.add(availableMap.getKey());
          }
          break;
        }
      }
    }
    return outOfDateMapNames;
  }

  /**
   * Returns a normalized version of the input. Trims off a '.properties' suffix if present,
   * converts to lower case and replaces all spaces with underscores.
   */
  private static String normalizeName(final String inputName) {
    String normalizedName = inputName;
    if (inputName.endsWith(".properties")) {
      normalizedName = inputName.substring(0, inputName.indexOf(".properties"));
    }

    normalizedName = normalizedName.replaceAll(" ", "_");
    normalizedName = normalizedName.toLowerCase();

    return normalizedName;
  }

  private static void promptUserToUpdateMaps(final Collection<String> outOfDateMapNames) {
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
