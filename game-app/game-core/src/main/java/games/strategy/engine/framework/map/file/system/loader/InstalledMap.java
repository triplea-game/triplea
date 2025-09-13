package games.strategy.engine.framework.map.file.system.loader;

import games.strategy.triplea.Constants;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.triplea.http.client.maps.listing.MapDownloadItem;
import org.triplea.io.FileUtils;
import org.triplea.map.description.file.MapDescriptionYaml;
import org.triplea.map.description.file.SkinDescriptionYaml;
import org.triplea.map.game.notes.GameNotes;
import org.triplea.util.LocalizeHtml;

/** Object representing a map that is downloaded and installed. */
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
@ToString
public class InstalledMap {
  @Nonnull private final MapDescriptionYaml mapDescriptionYaml;
  @Nullable private Instant lastModifiedDate;
  @Nullable private Path contentRoot;

  public String getMapName() {
    return mapDescriptionYaml.getMapName();
  }

  /**
   * Returns location where map content is located. Returns empty if the content root cannot be
   * found (the content root will not be found if either the polygons.txt file or a map.yml are
   * missing).
   */
  Optional<Path> findContentRoot() {
    // contentRoot is cached to avoid searching on the file system
    if (contentRoot == null) {
      // relative to the 'map.yml' file location, search current and child directories for
      // a polygons file, the location of the polygons file is the map content root.
      final Path mapYamlParentFolder = mapDescriptionYaml.getYamlFileLocation().getParent();
      contentRoot =
          FileUtils.findClosestToRoot(mapYamlParentFolder, 3, Constants.FILE_NAME_POLYGONS)
              .map(Path::getParent)
              .orElse(null);
    }
    return Optional.ofNullable(contentRoot);
  }

  /**
   * Returns all game names belonging to this map. Game names are found in the map.yml file and each
   * one should have a corresponding game XML file.
   */
  public Collection<String> getGameNames() {
    return mapDescriptionYaml.getMapGameList().stream()
        .map(MapDescriptionYaml.MapGame::getGameName)
        .sorted()
        .collect(Collectors.toList());
  }

  /** Given a game name, returns the path to the XML file for that game. */
  public Optional<Path> getGameXmlFilePath(final String gameName) {
    return mapDescriptionYaml.getGameXmlPathByGameName(gameName);
  }

  /**
   * Returns contents of a game notes file if present. Returns empty if the content root or game
   * notes file cannot be found.
   */
  public Optional<String> readGameNotes(final String gameName) {
    final Optional<Path> xmlPath = getGameXmlFilePath(gameName);
    final Optional<Path> mapContentRoot = findContentRoot();

    if (xmlPath.isEmpty() || mapContentRoot.isEmpty()) {
      return Optional.empty();
    } else {
      return Optional.of(
          LocalizeHtml.localizeImgLinksInHtml(
              GameNotes.loadGameNotes(xmlPath.get()), mapContentRoot.get()));
    }
  }

  /**
   * Checks if this installed map is out of date compared to a given map-download. This is done by
   * comparing the last modified date on the map installation folder compared to the last commit
   * date of the map-download.
   */
  boolean isOutOfDate(final MapDownloadItem download) {
    // we cache lastModifiedDate for two reasons:
    // 1. *primarily* test can inject a value
    // 2. avoid file system access
    if (lastModifiedDate == null) {
      lastModifiedDate = findContentRoot().flatMap(FileUtils::getLastModified).orElse(null);
      if (lastModifiedDate == null) {
        return false;
      }
    }

    final Instant lastCommitDate = Instant.ofEpochMilli(download.getLastCommitDateEpochMilli());
    return lastModifiedDate.isBefore(lastCommitDate);
  }

  public Optional<Path> findMapSkin(String skinName) {
    Path mapPath = mapDescriptionYaml.getYamlFileLocation().getParent();
    Collection<Path> skinYamlFiles = FileUtils.find(mapPath, 7, "skin.yml");

    return skinYamlFiles.stream()
        .map(SkinDescriptionYaml::readSkinDescriptionYamlFile)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .filter(skinDescriptionYaml -> skinDescriptionYaml.getSkinName().equalsIgnoreCase(skinName))
        .findAny()
        .map(SkinDescriptionYaml::getFilePath)
        .map(Path::getParent);
  }

  public Collection<String> getSkinNames() {
    Path mapPath = mapDescriptionYaml.getYamlFileLocation().getParent();
    Collection<Path> skinYamlFiles = FileUtils.find(mapPath, 4, "skin.yml");

    return skinYamlFiles.stream()
        .map(SkinDescriptionYaml::readSkinDescriptionYamlFile)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(SkinDescriptionYaml::getSkinName)
        .sorted()
        .collect(Collectors.toList());
  }
}
