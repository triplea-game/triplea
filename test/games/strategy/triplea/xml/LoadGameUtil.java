package games.strategy.triplea.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.gameparser.GameParser;

public class LoadGameUtil {

  public static GameData loadGame(final String game) {
    return loadGame(game, new String[] {"maps"});
  }

  public static GameData loadTestGame(final String game) {
    return loadGame(game, new String[] {"test_data"});
  }

  private static GameData loadGame(final String game, final String[] possibleFolders) {

    try (final InputStream is = openInputStream(game, possibleFolders)) {
      if (is == null) {
        throw new IllegalStateException(game + " does not exist");
      }
      return (new GameParser()).parse(is, new AtomicReference<>(), false);
    } catch (final Exception e) {
      throw new IllegalStateException(e);
    }
  }

  /*
   * First try to load the game as a file on the classpath, if not found there
   * then try to load it from either the "maps" or "test_data" folders.
   */
  private static InputStream openInputStream(final String game, String[] possibleFolders) {
    InputStream is = LoadGameUtil.class.getResourceAsStream(game);
    if (is == null) {
      final File f = getFile(game, possibleFolders);
      if (f.exists()) {
        try {
          is = new FileInputStream(f);
        } catch (final FileNotFoundException e) {
          // ignore, we'll throw an exception anyways when the client sees we returned null
        }
      }
    }
    return is;
  }


  /**
   * Search for a file that may be contained in one of multiple folders.
   *
   * The file to search for is given by first parameter, second is the list of folders.
   * We will search all possible paths of the first folder before moving on to the next,
   * so ordering of the possible folders is more important than the ordering of search paths.
   *
   * The search paths vary by if this class is being run from a class file instance,
   * or a copy compiled into a jar.
   *
   * @param game The name of the file to find
   * @param possibleFolders An array containing a sequence of possible folders that may contain
   *        the search file.
   * @return Throws illegal state if not found. Otherwise returns a file reference whose name
   *         matches the first parameter and parent folder matches an element of "possibleFolders"
   */
  private static File getFile(final String game, final String[] possibleFolders) {
    for (final String possibleFolder : possibleFolders) {
      final File start = ClientFileSystemHelper.getRootFolder();
      if (folderContainsFolderAndFile(start, possibleFolder, game)) {
        return new File(new File(start, possibleFolder), game);
      }

      final File secondStart = getParentFolder(possibleFolder);
      if (folderContainsFolderAndFile(secondStart, possibleFolder, game)) {
        return new File(new File(secondStart, possibleFolder), game);
      }

    }
    throw new IllegalStateException(
        "Could not find any of these folders: " + Arrays.asList(possibleFolders) + ", containing game file: " + game);
  }


  /* From the Game Runner root location, walk up directories until we find a given folder */
  private static File getParentFolder(final String folderToFind) {
    File f = new File(ClientFileSystemHelper.getGameRunnerFileLocation("GameRunner2.class"));

    while (f != null && f.exists() && !folderContains(f, folderToFind)) {
      f = f.getParentFile();
    }
    return f;
  }

  /* Check if a given folder contains another folder that in turn contains a given file */
  private static boolean folderContainsFolderAndFile(final File f, final String childFolder, final String child) {
    if (folderContains(f, childFolder)) {
      final File possibleParent = new File(f, childFolder);
      if (folderContains(possibleParent, child)) {
        return true;
      }
    }
    return false;
  }

  /* Check if a folder contains another folder or file */
  private static boolean folderContains(final File folder, final String childToFind) {
    if (folder == null || folder.list() == null || folder.list().length == 0) {
      return false;
    }
    return Arrays.asList(folder.list()).contains(childToFind);
  }

}
