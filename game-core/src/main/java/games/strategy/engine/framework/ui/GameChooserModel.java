package games.strategy.engine.framework.ui;

import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.data.EngineVersionException;
import games.strategy.engine.framework.GameRunner;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.annotation.Nullable;
import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;
import lombok.extern.java.Log;
import org.triplea.io.FileUtils;
import org.triplea.java.Interruptibles;
import org.triplea.swing.SwingAction;

/** The model for a {@link GameChooser} dialog. */
@Log
public final class GameChooserModel extends DefaultListModel<GameChooserEntry> {
  private static final long serialVersionUID = -2044689419834812524L;

  private static class CorruptXmlFileException extends RuntimeException {
    CorruptXmlFileException(final String fileName) {
      super("File '" + fileName + "' was declared in zip but can't be opened.");
    }
  }

  /**
   * Initializes a new {@code GameChooserModel} using all available maps installed in the user's
   * maps folder. This method will block until all maps are parsed and should not be called from the
   * EDT.
   */
  public GameChooserModel() {
    this(parseMapFiles());
  }

  GameChooserModel(final Set<GameChooserEntry> gameChooserEntries) {
    gameChooserEntries.stream().sorted().forEach(this::addElement);
  }

  @Override
  public GameChooserEntry get(final int i) {
    return super.get(i);
  }

  public static Set<GameChooserEntry> parseMapFiles() {
    return FileUtils.listFiles(ClientFileSystemHelper.getUserMapsFolder())
        .parallelStream()
        .flatMap(
            map -> {
              if (map.isDirectory()) {
                return getDirectoryUris(map);
              } else if (map.isFile() && map.getName().toLowerCase().endsWith(".zip")) {
                return getZipUris(map);
              }
              return Stream.empty();
            })
        .map(GameChooserModel::newGameChooserEntry)
        .flatMap(Optional::stream)
        .collect(Collectors.toUnmodifiableSet());
  }

  private static Stream<URI> getZipUris(final File map) {
    try (ZipFile zipFile = new ZipFile(map);
        URLClassLoader loader = new URLClassLoader(new URL[] {map.toURI().toURL()})) {
      return zipFile.stream()
          .map(ZipEntry::getName)
          .filter(name -> name.contains("games/"))
          .filter(name -> name.toLowerCase().endsWith(".xml"))
          .map(name -> getUri(loader, name))
          // Get names before the ZipFile resource is closed
          .collect(Collectors.toUnmodifiableSet())
          .parallelStream();
    } catch (final IOException | CorruptXmlFileException e) {
      confirmWithUserAndThenDeleteCorruptZipFile(map, e.getMessage());
      return Stream.empty();
    }
  }

  private static URI getUri(final URLClassLoader loader, final String name) {
    final URL url = loader.getResource(name);
    if (url == null) {
      throw new CorruptXmlFileException(name);
    }

    return URI.create(url.toString().replace(" ", "%20"));
  }

  /**
   * Open up a confirmation dialog, if user says yes, delete the map specified by parameter, then
   * show confirmation of deletion.
   */
  private static void confirmWithUserAndThenDeleteCorruptZipFile(
      final File map, final @Nullable String errorDetails) {
    Interruptibles.await(
        () ->
            SwingAction.invokeAndWait(
                () -> {
                  String message =
                      "Could not parse map file correctly, would you like to remove it?\n"
                          + map.getAbsolutePath()
                          + "\n(You may see this error message again if you keep the file)";
                  String title = "Corrup Map File Found";
                  final int optionType = JOptionPane.YES_NO_OPTION;
                  int messageType = JOptionPane.WARNING_MESSAGE;
                  final int result =
                      GameRunner.showConfirmDialog(
                          message, GameRunner.Title.of(title), optionType, messageType);
                  if (result == JOptionPane.YES_OPTION) {
                    if (map.delete()) {
                      messageType = JOptionPane.INFORMATION_MESSAGE;
                      message = "File was deleted successfully.";
                    } else if (map.exists()) {
                      message =
                          "Unable to delete file, please remove it in the file system and "
                              + "restart tripleA:\n"
                              + map.getAbsolutePath();
                      if (errorDetails != null) {
                        message += "\nError details: " + errorDetails;
                      }
                    }
                    title = "File Removal Result";
                    JOptionPane.showMessageDialog(null, message, title, messageType);
                  }
                }));
  }

  /**
   * From a given URI, creates a GameChooserEntry and adds to the given entries list.
   *
   * @param uri URI of the new entry
   */
  private static Optional<GameChooserEntry> newGameChooserEntry(final URI uri) {
    try {
      return Optional.of(GameChooserEntry.newInstance(uri));
    } catch (final EngineVersionException e) {
      log.log(Level.SEVERE, "Engine version problem:" + uri, e);
    } catch (final Exception e) {
      log.log(Level.SEVERE, "Could not parse: " + uri, e);
    }
    return Optional.empty();
  }

  /** Searches for a GameChooserEntry whose gameName matches the input parameter. */
  public Optional<GameChooserEntry> findByName(final String name) {
    return IntStream.range(0, size())
        .mapToObj(this::get)
        .filter(e -> e.getGameData().getGameName().equals(name))
        .findAny();
  }

  private static Stream<URI> getDirectoryUris(final File mapDir) {
    // use contents under a "mapDir/map" folder if present, otherwise use the "mapDir/" contents
    // directly
    final File mapFolder = new File(mapDir, "map");

    final File parentFolder = mapFolder.exists() ? mapFolder : mapDir;
    final File games = new File(parentFolder, "games");
    return FileUtils.listFiles(games).stream()
        .parallel()
        .filter(File::isFile)
        .filter(game -> game.getName().toLowerCase().endsWith("xml"))
        .map(File::toURI);
  }

  /**
   * Removes the given entry from this model.
   *
   * @param entryToBeRemoved The element to be removed.
   */
  public void removeEntry(final GameChooserEntry entryToBeRemoved) {
    this.removeElement(entryToBeRemoved);
  }
}
