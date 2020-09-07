package org.triplea.game.server;

import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.gameparser.ShallowGameParser;
import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import lombok.extern.java.Log;
import org.triplea.io.FileUtils;
import org.triplea.java.UrlStreams;

/**
 * A list of all available games. We make sure we can parse them all, but we don't keep them in
 * memory.
 */
@Log
@Immutable
final class AvailableGames {
  private static final String ZIP_EXTENSION = ".zip";
  private final Map<String, URI> availableGames;

  AvailableGames() {

    availableGames = new HashMap<>();
    findAllXmlFiles()
        .forEach(
            xmlInZipUri ->
                readGameName(xmlInZipUri)
                    .ifPresent(
                        gameName -> {
                          if (!availableGames.containsKey(gameName)) {
                            availableGames.put(gameName, xmlInZipUri);
                          } else {
                            log.warning(
                                String.format(
                                    "DUPLICATE GAME ENTRY! Ignoring game name: %s, "
                                        + "at path entry: %s, existing value is: %s",
                                    gameName, xmlInZipUri, availableGames.get(gameName)));
                          }
                        }));
    log.info(
        String.format(
            "Done loading maps, " + "availableGames count: %s, contents: %s",
            availableGames.keySet().size(), availableGames.keySet()));
  }

  private static List<URI> findAllXmlFiles() {
    return FileUtils.listFiles(ClientFileSystemHelper.getUserMapsFolder()).stream()
        .filter(File::isFile)
        .filter(file -> file.getName().toLowerCase().endsWith(ZIP_EXTENSION))
        .map(MapZipReaderUtil::findGameXmlFilesInZip)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  private static Optional<String> readGameName(@Nonnull final URI uri) {
    log.info("Loading XML: " + uri);
    return UrlStreams.openStream(
        uri,
        inputStream -> {
          try {
            return ShallowGameParser.readGameName(uri.toString(), inputStream);
          } catch (final GameParseException e) {
            log.log(Level.SEVERE, "Exception while parsing: " + uri, e);
            return null;
          }
        });
  }

  boolean hasGame(final String gameName) {
    return availableGames.containsKey(gameName);
  }

  /** Returns a read-only view of available games. */
  Set<String> getGameNames() {
    return availableGames.keySet();
  }

  /**
   * Returns the path to the file associated with the specified game.
   *
   * <p>The "path" is actually a URI in string form.
   *
   * @param gameName The name of the game whose file path is to be retrieved; may be {@code null}.
   * @return The path to the game file; or {@code null} if the game is not available.
   */
  URI getGameFilePath(final String gameName) {
    return availableGames.get(gameName);
  }
}
