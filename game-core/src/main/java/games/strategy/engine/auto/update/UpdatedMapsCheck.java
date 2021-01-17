package games.strategy.engine.auto.update;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.framework.map.download.DownloadFileDescription;
import games.strategy.engine.framework.map.download.DownloadFileProperties;
import games.strategy.engine.framework.map.download.DownloadMapsWindow;
import games.strategy.engine.framework.map.listing.MapListingFetcher;
import games.strategy.triplea.settings.ClientSetting;
import java.io.File;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.triplea.io.FileUtils;
import org.triplea.swing.SwingComponents;

@UtilityClass
@Slf4j
class UpdatedMapsCheck {

  static final int THRESHOLD_DAYS = 7;

  static boolean isMapUpdateCheckRequired() {
    return isMapUpdateCheckRequired(
        ClientSetting.lastCheckForMapUpdates.getValue().orElse(0L),
        () -> ClientSetting.lastCheckForMapUpdates.setValueAndFlush(Instant.now().toEpochMilli()));
  }

  @VisibleForTesting
  static boolean isMapUpdateCheckRequired(
      final long lastCheckEpochMilli, final Runnable lastCheckSetter) {
    final Instant cutOff = Instant.now().minus(THRESHOLD_DAYS, ChronoUnit.DAYS);
    final Instant lastCheck = Instant.ofEpochMilli(lastCheckEpochMilli);

    lastCheckSetter.run();

    return lastCheck.isBefore(cutOff);
  }

  /** Prompts user to download map updates if maps are out of date. */
  public static void checkDownloadedMapsAreLatest() {
    if (!isMapUpdateCheckRequired()) {
      return;
    }

    final List<DownloadFileDescription> availableToDownloadMaps =
        MapListingFetcher.getMapDownloadList();

    if (availableToDownloadMaps.isEmpty()) {
      // A failure happened getting maps. User is already notified.
      return;
    }

    final Collection<String> outOfDateMapNames = computeOutOfDateMaps(availableToDownloadMaps);

    if (!outOfDateMapNames.isEmpty()) {
      promptUserToUpdateMaps(outOfDateMapNames);
    }
  }

  /**
   * Computes maps that are out of date.
   *
   * @param availableToDownloadMaps List of maps that are available for download.
   * @return Set of map names that are installed where the available version is greater than the
   *     installed version.
   */
  private static Collection<String> computeOutOfDateMaps(
      final Collection<DownloadFileDescription> availableToDownloadMaps) {

    final Collection<String> outOfDateMapNames = new ArrayList<>();

    // Loop over all available maps, check if we have that map present, its version,
    // and remember any whose version is less than what is available.
    for (final DownloadFileDescription availableMap : availableToDownloadMaps) {
      getMapVersionByName(availableMap.getMapName())
          .ifPresent(
              installedVersion -> {
                if (installedVersion < availableMap.getVersion()) {
                  outOfDateMapNames.add(availableMap.getMapName());
                }
              });
    }
    return outOfDateMapNames;
  }

  /**
   * For a given a map name (fuzzy matched), if the map is installed, will return the map version
   * installed othewrise returns an empty. Fuzzy matching means we will do name normalization and
   * replace spaces with underscores, convert to lower case, etc.
   */
  private static Optional<Integer> getMapVersionByName(final String mapName) {
    final String normalizedMapName = normalizeName(mapName);

    // see if we have a map folder that matches the target name
    // if so, check for a .properties file
    // if that exists, then return the map version value from the properties file
    return FileUtils.listFiles(ClientFileSystemHelper.getUserMapsFolder()).stream()
        .filter(file -> normalizeName(file.getName()).equals(normalizedMapName))
        .findAny()
        .map(
            file ->
                ClientFileSystemHelper.getUserMapsFolder()
                    .toPath()
                    .resolve(file.getName() + ".properties")
                    .toFile())
        .filter(File::exists)
        .flatMap(UpdatedMapsCheck::readVersionFromPropertyFile);
  }

  private static Optional<Integer> readVersionFromPropertyFile(final File propertyFile) {
    return DownloadFileProperties.loadForZipPropertyFile(propertyFile).getVersion();
  }

  /**
   * Returns a normalized version of the input. Trims off a '.properties' suffix if present,
   * converts to lower case and replaces all spaces with underscores.
   */
  private static String normalizeName(final String inputName) {
    String normalizedName = inputName;
    if (inputName.endsWith(".zip.properties")) {
      normalizedName = inputName.substring(0, inputName.indexOf(".zip.properties"));
    }
    if (normalizedName.endsWith("-master")) {
      normalizedName = inputName.substring(0, inputName.indexOf("-master"));
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
}
