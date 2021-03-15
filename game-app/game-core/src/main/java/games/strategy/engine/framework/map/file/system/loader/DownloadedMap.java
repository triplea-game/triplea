package games.strategy.engine.framework.map.file.system.loader;

import games.strategy.triplea.ui.mapdata.MapData;
import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.triplea.io.FileUtils;
import org.triplea.map.description.file.MapDescriptionYaml;
import org.triplea.map.game.notes.GameNotes;
import org.triplea.util.LocalizeHtml;

/** Object representing a map that is downloaded and installed. */
@AllArgsConstructor
public class DownloadedMap {
  private final MapDescriptionYaml mapDescriptionYaml;

  public String getMapName() {
    return mapDescriptionYaml.getMapName();
  }

  /**
   * Returns the map version value from the map.yml file, returns zero if there is no map.yml file.
   */
  int getMapVersion() {
    return mapDescriptionYaml.getMapVersion();
  }

  /**
   * Returns location where map content is located. Returns empty if the content root cannot be
   * found (the content root will not be found if either the polygons.txt file or a map.yml are
   * missing).
   */
  Optional<Path> findContentRoot() {
    // relative to the 'map.yml' file location, search current and child directories for
    // a polygons file, the location of the polygons file is the map content root.
    final Path mapYamlParentFolder = Path.of(mapDescriptionYaml.getYamlFileLocation()).getParent();
    return FileUtils.find(mapYamlParentFolder, 3, MapData.POLYGON_FILE)
        .map(File::toPath)
        .map(Path::getParent);
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
              GameNotes.loadGameNotes(xmlPath.get(), gameName), mapContentRoot.get()));
    }
  }
}
