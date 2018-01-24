package games.strategy.engine.framework.ui;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.data.EngineVersionException;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.framework.GameRunner;
import games.strategy.io.FileUtils;
import games.strategy.ui.SwingAction;

/**
 * The model for a {@link GameChooser} dialog.
 */
public final class GameChooserModel extends DefaultListModel<GameChooserEntry> {
  private static final long serialVersionUID = -2044689419834812524L;

  private enum ZipProcessingResult {
    SUCCESS, ERROR
  }

  /**
   * Initializes a new {@code GameChooserModel} using all available maps installed in the user's maps folder. This
   * method will block until all maps are parsed and should not be called from the EDT.
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

  static Set<GameChooserEntry> parseMapFiles() {
    final Set<GameChooserEntry> parsedMapSet = new HashSet<>();
    FileUtils.listFiles(ClientFileSystemHelper.getUserMapsFolder()).parallelStream()
        .forEach(map -> {
          if (map.isDirectory()) {
            parsedMapSet.addAll(populateFromDirectory(map));
          } else if (map.isFile() && map.getName().toLowerCase().endsWith(".zip")) {
            parsedMapSet.addAll(populateFromZip(map));
          }
        });
    return parsedMapSet;
  }

  private static Set<GameChooserEntry> populateFromZip(final File map) {
    boolean badMapZip = false;
    final Set<GameChooserEntry> entries = new HashSet<>();

    try (ZipFile zipFile = new ZipFile(map);
        final URLClassLoader loader = new URLClassLoader(new URL[] {map.toURI().toURL()})) {
      final Enumeration<? extends ZipEntry> zipEntryEnumeration = zipFile.entries();
      while (zipEntryEnumeration.hasMoreElements()) {
        final ZipEntry entry = zipEntryEnumeration.nextElement();
        if (entry.getName().contains("games/") && entry.getName().toLowerCase().endsWith(".xml")) {
          final ZipProcessingResult result = processZipEntry(loader, entry, entries);
          if (result == ZipProcessingResult.ERROR) {
            badMapZip = true;
            break;
          }
        }
      }
    } catch (final IOException e) {
      confirmWithUserAndThenDeleteCorruptZipFile(map, Optional.of(e.getMessage()));
    }

    if (badMapZip) {
      confirmWithUserAndThenDeleteCorruptZipFile(map, Optional.empty());
    }
    return entries;
  }

  private static ZipProcessingResult processZipEntry(final URLClassLoader loader, final ZipEntry entry,
      final Set<GameChooserEntry> entries) {
    final URL url = loader.getResource(entry.getName());
    if (url == null) {
      // not loading the URL means the XML is truncated or otherwise in bad shape
      return ZipProcessingResult.ERROR;
    }

    createGameChooserEntry(URI.create(url.toString().replace(" ", "%20")))
        .ifPresent(entries::add);
    return ZipProcessingResult.SUCCESS;
  }

  /*
   * Open up a confirmation dialog, if user says yes, delete the map specified by
   * parameter, then show confirmation of deletion.
   */
  private static void confirmWithUserAndThenDeleteCorruptZipFile(final File map, final Optional<String> errorDetails) {
    SwingAction.invokeAndWait(() -> {
      String message = "Could not parse map file correctly, would you like to remove it?\n" + map.getAbsolutePath()
          + "\n(You may see this error message again if you keep the file)";
      String title = "Corrup Map File Found";
      final int optionType = JOptionPane.YES_NO_OPTION;
      int messageType = JOptionPane.WARNING_MESSAGE;
      final int result = GameRunner.showConfirmDialog(
          message, GameRunner.Title.of(title), optionType, messageType);
      if (result == JOptionPane.YES_OPTION) {
        if (map.delete()) {
          messageType = JOptionPane.INFORMATION_MESSAGE;
          message = "File was deleted successfully.";
        } else if (map.exists()) {
          message = "Unable to delete file, please remove it in the file system and restart tripleA:\n" + map
              .getAbsolutePath();
          if (errorDetails.isPresent()) {
            message += "\nError details: " + errorDetails;
          }
        }
        title = "File Removal Result";
        JOptionPane.showMessageDialog(null, message, title, messageType);
      }
    });
  }

  /**
   * From a given URI, creates a GameChooserEntry and adds to the given entries list.
   *
   * @param uri
   *        URI of the new entry
   */
  private static Optional<GameChooserEntry> createGameChooserEntry(final URI uri) {
    try {
      return Optional.of(createEntry(uri));
    } catch (final EngineVersionException e) {
      ClientLogger.logQuietly("Engine version problem:" + uri, e);
    } catch (final Exception e) {
      ClientLogger.logQuietly("Could not parse: " + uri, e);
    }
    return Optional.empty();
  }

  /**
   * Searches for a GameChooserEntry whose gameName matches the input parameter.
   */
  public Optional<GameChooserEntry> findByName(final String name) {
    return IntStream.range(0, size())
        .mapToObj(this::get)
        .filter(e -> e.getGameData().getGameName().equals(name))
        .findAny();
  }

  private static GameChooserEntry createEntry(final URI uri)
      throws IOException, GameParseException, EngineVersionException {
    return new GameChooserEntry(uri);
  }

  private static Set<GameChooserEntry> populateFromDirectory(final File mapDir) {
    final Set<GameChooserEntry> entries = new HashSet<>();

    // use contents under a "mapDir/map" folder if present, otherwise use the "mapDir/" contents directly
    final File mapFolder = new File(mapDir, "map");

    final File parentFolder = mapFolder.exists() ? mapFolder : mapDir;
    final File games = new File(parentFolder, "games");
    for (final File game : FileUtils.listFiles(games)) {
      if (game.isFile() && game.getName().toLowerCase().endsWith("xml")) {
        createGameChooserEntry(game.toURI()).ifPresent(entries::add);
      }
    }
    return entries;
  }

  /**
   * Removes the given entry from this model.
   *
   * @param entryToBeRemoved The element to be removed.
   * @return Returns true, if the given element could successfully be removed.
   */
  public boolean removeEntry(final GameChooserEntry entryToBeRemoved) {
    return this.removeElement(entryToBeRemoved);
  }
}
