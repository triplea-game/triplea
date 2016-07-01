package games.strategy.engine.framework.ui;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import games.strategy.ui.SwingAction;
import games.strategy.debug.ClientLogger;
import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.data.EngineVersionException;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.framework.startup.ui.MainFrame;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;
import java.awt.Component;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class NewGameChooserModel extends DefaultListModel<NewGameChooserEntry> {
  private static final long serialVersionUID = -2044689419834812524L;

  private enum ZipProcessingResult {
    SUCCESS, ERROR
  }


  public NewGameChooserModel() {
    final Set<NewGameChooserEntry> parsedMapSet = parseMapFiles();

    final List<NewGameChooserEntry> entries = new ArrayList<>(parsedMapSet);
    Collections.sort(entries, NewGameChooserEntry.getComparator());

    for (final NewGameChooserEntry entry : entries) {
      addElement(entry);
    }
  }

  @Override
  public NewGameChooserEntry get(final int i) {
    return super.get(i);
  }

  public static File getDefaultMapsDir() {
    return new File(ClientFileSystemHelper.getRootFolder(), "maps");
  }

  private static List<File> allMapFiles() {
    final List<File> rVal = new ArrayList<>();
    // prioritize user maps folder over root folder
    rVal.addAll(safeListFiles(ClientFileSystemHelper.getUserMapsFolder()));
    rVal.addAll(safeListFiles(getDefaultMapsDir()));
    return rVal;
  }

  private static List<File> safeListFiles(final File f) {
    final File[] files = f.listFiles();
    if (files == null) {
      return Collections.emptyList();
    }
    return Arrays.asList(files);
  }


  private Set<NewGameChooserEntry> parseMapFiles() {
    final Set<NewGameChooserEntry> parsedMapSet = Sets.newHashSet();
    for (final File map : allMapFiles()) {
      if (map.isDirectory()) {
        parsedMapSet.addAll(populateFromDirectory(map));
      } else if (map.isFile() && map.getName().toLowerCase().endsWith(".zip")) {
        parsedMapSet.addAll(populateFromZip(map));
      }
    }
    return parsedMapSet;
  }


  private static List<NewGameChooserEntry> populateFromZip(final File map) {
    boolean badMapZip = false;
    final List<NewGameChooserEntry> entries = new ArrayList<>();

    try (ZipFile zipFile = new ZipFile(map);
        final URLClassLoader loader = new URLClassLoader(new URL[] {map.toURI().toURL()})) {
      Enumeration<? extends ZipEntry> zipEntryEnumeration = zipFile.entries();
      while (zipEntryEnumeration.hasMoreElements()) {
        ZipEntry entry = zipEntryEnumeration.nextElement();
        if (entry.getName().startsWith("games/") && entry.getName().toLowerCase().endsWith(".xml")) {
          ZipProcessingResult result = processZipEntry(loader, entry, entries);
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
      final List<NewGameChooserEntry> entries) {
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
  private static void confirmWithUserAndThenDeleteCorruptZipFile(final File map, Optional<String> errorDetails) {
    Runnable deleteMapRunnable = () -> {
      final Component parentComponent = MainFrame.getInstance();
      String message = "Could not parse map file correctly, would you like to remove it?\n" + map.getAbsolutePath()
          + "\n(You may see this error message again if you keep the file)";
      String title = "Corrup Map File Found";
      final int optionType = JOptionPane.YES_NO_OPTION;
      int messageType = JOptionPane.WARNING_MESSAGE;
      final int result = JOptionPane.showConfirmDialog(parentComponent, message, title, optionType, messageType);
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
        JOptionPane.showMessageDialog(parentComponent, message, title, messageType);
      }
    };

    SwingAction.invokeAndWait(deleteMapRunnable);
  }

  /**
   * @param entries
   *        list of entries where to add the new entry
   * @param uri
   *        URI of the new entry
   */
  private static void addNewGameChooserEntry(final List<NewGameChooserEntry> entries, final URI uri) {
    try {
      final NewGameChooserEntry newEntry = createEntry(uri);
      if (newEntry != null && !entries.contains(newEntry)) {
        entries.add(newEntry);
      }
    } catch (final EngineVersionException e) {
      System.out.println(e.getMessage());
    } catch (final SAXParseException e) {
      String msg = "Could not parse:" + uri + " error at line:" + e.getLineNumber() + " column:" + e.getColumnNumber();
      System.err.println(msg);
      ClientLogger.logQuietly(e);
    } catch (final Exception e) {
      System.err.println("Could not parse:" + uri);
      ClientLogger.logQuietly(e);
    }
  }

  public NewGameChooserEntry findByName(final String name) {
    for (int i = 0; i < size(); i++) {
      if (get(i).getGameData().getGameName().equals(name)) {
        return get(i);
      }
    }
    return null;
  }

  private static NewGameChooserEntry createEntry(final URI uri)
      throws IOException, GameParseException, SAXException, EngineVersionException {
    return new NewGameChooserEntry(uri);
  }

  private static List<NewGameChooserEntry> populateFromDirectory(final File mapDir) {
    final List<NewGameChooserEntry> entries = new ArrayList<>();

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

  public boolean removeEntry(final NewGameChooserEntry entryToBeRemoved) {
    return this.removeElement(entryToBeRemoved);
  }
}
