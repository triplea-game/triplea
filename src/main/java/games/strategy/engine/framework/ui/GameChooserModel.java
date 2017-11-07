package games.strategy.engine.framework.ui;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.google.common.annotations.VisibleForTesting;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.data.EngineVersionException;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.ui.background.BackgroundTaskRunner;
import games.strategy.ui.SwingAction;

public class GameChooserModel extends DefaultListModel<GameChooserEntry> {
  private static final long serialVersionUID = -2044689419834812524L;

  private enum ZipProcessingResult {
    SUCCESS, ERROR
  }

  @VisibleForTesting
  GameChooserModel(final Set<GameChooserEntry> gameChooserEntries) {
    gameChooserEntries.stream().sorted().forEach(this::addElement);
  }

  /**
   * Creates a new Instance of a GameChooserModel.
   * Must be called on the EDT, will not return until all
   * map files have been scanned, but is not going to
   * effectively block the EDT.
   * 
   * @return The newly created instance.
   * 
   * @throws InterruptedException If this Thread is being interrupted
   *         while waiting for the SwingWorker to complete.
   * @throws IllegalStateException If this method is called on any other
   *         Thread than the Swing Event Dispatch Thread.
   */
  public static GameChooserModel newInstance() throws InterruptedException {
    return new GameChooserModel(BackgroundTaskRunner.runInBackgroundAndReturn(
        "Loading all available games...",
        GameChooserModel::parseMapFiles));
  }

  @Override
  public GameChooserEntry get(final int i) {
    return super.get(i);
  }

  private static List<File> allMapFiles() {
    return new ArrayList<>(safeListFiles(ClientFileSystemHelper.getUserMapsFolder()));
  }

  private static List<File> safeListFiles(final File f) {
    final File[] files = f.listFiles();
    if (files == null) {
      return Collections.emptyList();
    }
    return Arrays.asList(files);
  }

  private static Set<GameChooserEntry> parseMapFiles() {
    final List<File> files = allMapFiles();
    final Set<GameChooserEntry> parsedMapSet = new HashSet<>(files.size());
    final ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 2);
    final List<Future<List<GameChooserEntry>>> futures = new ArrayList<>(files.size());
    for (final File map : files) {
      if (map.isDirectory()) {
        futures.add(service.submit(() -> populateFromDirectory(map)));
      } else if (map.isFile() && map.getName().toLowerCase().endsWith(".zip")) {
        futures.add(service.submit(() -> populateFromZip(map)));
      }
    }
    service.shutdown();
    for (final Future<List<GameChooserEntry>> future : futures) {
      try {
        parsedMapSet.addAll(future.get());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (ExecutionException e) {
        ClientLogger.logError(e);
      }
    }
    return parsedMapSet;
  }

  private static List<GameChooserEntry> populateFromZip(final File map) {
    boolean badMapZip = false;
    final List<GameChooserEntry> entries = new ArrayList<>();

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
      final List<GameChooserEntry> entries) {
    final URL url = loader.getResource(entry.getName());
    if (url == null) {
      // not loading the URL means the XML is truncated or otherwise in bad shape
      return ZipProcessingResult.ERROR;
    }
    try {
      addNewGameChooserEntry(entries, new URI(url.toString().replace(" ", "%20")));
    } catch (final URISyntaxException e) {
      // only happens when URI couldn't be build and therefore no entry was added. That's fine ..
    }
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
        final boolean deleted = map.delete();
        if (deleted) {
          messageType = JOptionPane.INFORMATION_MESSAGE;
          message = "File was deleted successfully.";
        } else if (!deleted && map.exists()) {
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
   * @param entries
   *        list of entries where to add the new entry.
   * @param uri
   *        URI of the new entry
   */
  private static void addNewGameChooserEntry(final List<GameChooserEntry> entries, final URI uri) {
    try {
      final GameChooserEntry newEntry = createEntry(uri);
      if (newEntry != null && !entries.contains(newEntry)) {
        entries.add(newEntry);
      }
    } catch (final EngineVersionException e) {
      System.out.println(e.getMessage());
    } catch (final SAXParseException e) {
      final String msg =
          "Could not parse:" + uri + " error at line:" + e.getLineNumber() + " column:" + e.getColumnNumber();
      System.err.println(msg);
      ClientLogger.logQuietly(e);
    } catch (final Exception e) {
      System.err.println("Could not parse:" + uri);
      ClientLogger.logQuietly(e);
    }
  }

  public GameChooserEntry findByName(final String name) {
    for (int i = 0; i < size(); i++) {
      if (get(i).getGameData().getGameName().equals(name)) {
        return get(i);
      }
    }
    return null;
  }

  private static GameChooserEntry createEntry(final URI uri)
      throws IOException, GameParseException, SAXException, EngineVersionException {
    return new GameChooserEntry(uri);
  }

  private static List<GameChooserEntry> populateFromDirectory(final File mapDir) {
    final List<GameChooserEntry> entries = new ArrayList<>();

    // use contents under a "mapDir/map" folder if present, otherwise use the "mapDir/" contents directly
    final File mapFolder = new File(mapDir, "map");

    final File parentFolder = mapFolder.exists() ? mapFolder : mapDir;
    final File games = new File(parentFolder, "games");

    if (!games.exists()) {
      // no games in this map dir
      return entries;
    }
    for (final File game : games.listFiles()) {
      if (game.isFile() && game.getName().toLowerCase().endsWith("xml")) {
        addNewGameChooserEntry(entries, game.toURI());
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
