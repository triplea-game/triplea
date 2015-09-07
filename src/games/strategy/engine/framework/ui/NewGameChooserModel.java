package games.strategy.engine.framework.ui;

import java.awt.Component;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.data.EngineVersionException;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.framework.GameRunner2;
import games.strategy.engine.framework.startup.ui.MainFrame;
import games.strategy.util.ClassLoaderUtil;

public class NewGameChooserModel extends DefaultListModel {
  private static final long serialVersionUID = -2044689419834812524L;

  public NewGameChooserModel() {
    populate();
  }

  @Override
  public NewGameChooserEntry get(final int i) {
    return (NewGameChooserEntry) super.get(i);
  }

  public static Collection<String> getDefaultMapNames() {
    final Collection<String> rVal = new ArrayList<String>();
    for (final File f : getDefaultMapsDir().listFiles()) {
      if (f.getName().toLowerCase().endsWith(".zip")) {
        rVal.add(f.getName().substring(0, f.getName().length() - ".zip".length()));
      } else {
        rVal.add(f.getName());
      }
    }
    return rVal;
  }

  private List<File> allMapFiles() {
    final List<File> rVal = new ArrayList<File>();
    // prioritize user maps folder over root folder
    rVal.addAll(safeListFiles(GameRunner2.getUserMapsFolder()));
    rVal.addAll(safeListFiles(getDefaultMapsDir()));
    return rVal;
  }

  public static File getDefaultMapsDir() {
    return new File(GameRunner2.getRootFolder(), "maps");
  }

  private List<File> safeListFiles(final File f) {
    final File[] files = f.listFiles();
    if (files == null) {
      return Collections.emptyList();
    }
    return Arrays.asList(files);
  }

  private void populate() {
    final List<NewGameChooserEntry> entries = new ArrayList<NewGameChooserEntry>();
    for (final File map : allMapFiles()) {
      if (map.isDirectory()) {
        populateFromDirectory(map, entries);
      } else if (map.isFile() && map.getName().toLowerCase().endsWith(".zip")) {
        populateFromZip(map, entries);
      }
    }
    // remove any null entries
    do {
    } while (entries.remove(null));
    Collections.sort(entries, new Comparator<NewGameChooserEntry>() {
      @Override
      public int compare(final NewGameChooserEntry o1, final NewGameChooserEntry o2) {
        return o1.getGameData().getGameName().toLowerCase().compareTo(o2.getGameData().getGameName().toLowerCase());
      }
    });
    for (final NewGameChooserEntry entry : entries) {
      addElement(entry);
    }
  }

  private void populateFromZip(final File map, final List<NewGameChooserEntry> entries) {
    boolean badMapZip = false;
    try (final FileInputStream fis = new FileInputStream(map);
        final ZipInputStream zis = new ZipInputStream(fis);) {

      ZipEntry entry = zis.getNextEntry();

      while (entry != null) {
        if (entry.getName().startsWith("games/") && entry.getName().toLowerCase().endsWith(".xml")) {
          final URLClassLoader loader = new URLClassLoader(new URL[] {map.toURI().toURL()});
          final URL url = loader.getResource(entry.getName());
          // we have to close the loader to allow files to be deleted on windows
          ClassLoaderUtil.closeLoader(loader);
          if (url == null) {
               // not loading the URL means the XML is truncated or otherwise in bad shape
                badMapZip = true;
                break;
          }
          try {
            addNewGameChooserEntry(entries, new URI(url.toString().replace(" ", "%20")));
          } catch (final URISyntaxException e) {
            // only happens when URI couldn't be build and therefore no entry was added. That's fine
          }
        }
        zis.closeEntry();
        entry = zis.getNextEntry();
      }
    } catch (final IOException ioe) {
      ClientLogger.logQuietly(ioe);
    }

    if (badMapZip) {
      confirmWithUserAndThenDeleteCorruptZipFile(map);
    }
  }

  /*
   * Open up a confirmation dialog, if user says yes, delete the map specified by
   * parameter, then show confirmation of deletion.
   */
  private static void confirmWithUserAndThenDeleteCorruptZipFile(final File map) {
    try {
      SwingUtilities.invokeAndWait(new Runnable() {
        @Override
        public void run() {
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
              message = "Unable to delete file, please remove it by hand:\n" + map.getAbsolutePath();
            }
            title = "File Removal Result";
            JOptionPane.showMessageDialog(parentComponent, message, title, messageType);
          }
        }
      });
    } catch (final InvocationTargetException e) {
      ClientLogger.logError(e);
    } catch (final InterruptedException e) {
      ClientLogger.logQuietly(e);
    }
  }

  /**
   * @param entries
   *        list of entries where to add the new entry
   * @param uri
   *        URI of the new entry
   */
  private void addNewGameChooserEntry(final List<NewGameChooserEntry> entries, final URI uri) {
    try {
      final NewGameChooserEntry newEntry = createEntry(uri);
      if (!entries.contains(newEntry)) {
        entries.add(newEntry);
      }
    } catch (final EngineVersionException e) {
      System.out.println(e.getMessage());
    } catch (final SAXParseException e) {
      System.err
          .println("Could not parse:" + uri + " error at line:" + e.getLineNumber() + " column:" + e.getColumnNumber());
      e.printStackTrace();
    } catch (final Exception e) {
      System.err.println("Could not parse:" + uri);
      e.printStackTrace();
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

  private NewGameChooserEntry createEntry(final URI uri)
      throws IOException, GameParseException, SAXException, EngineVersionException {
    return new NewGameChooserEntry(uri);
  }

  private void populateFromDirectory(final File mapDir, final List<NewGameChooserEntry> entries) {
    final File games = new File(mapDir, "games");
    if (!games.exists()) {
      // no games in this map dir
      return;
    }
    for (final File game : games.listFiles()) {
      if (game.isFile() && game.getName().toLowerCase().endsWith("xml")) {
        addNewGameChooserEntry(entries, game.toURI());
      }
    }
  }

  public boolean removeEntry(final NewGameChooserEntry entryToBeRemoved) {
    return this.removeElement(entryToBeRemoved);
  }
}
