package games.strategy.triplea.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParser;
import games.strategy.engine.framework.GameRunner;

public class LoadGameUtil {

  public enum TestMapXml {
    BIG_WORLD_1942("big_world_1942_test.xml"), IRON_BLITZ("iron_blitz_test.xml"), LHTR(
        "lhtr_test.xml"), PACIFIC_INCOMPLETE("pacific_incomplete_test.xml"), PACT_OF_STEEL_2(
            "pact_of_steel_2_test.xml"), REVISED("revised_test.xml"), VICTORY_TEST("victory_test.xml"), WW2PAC40(
                "ww2pac40_test.xml"), WW2V3_1941("ww2v3_1941_test.xml"), WW2V3_1942("ww2v3_1942_test.xml"), Global1940("ww2_g40_balanced.xml");

    private final String value;

    TestMapXml(final String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return value;
    }
  }

  public static GameData loadTestGame(final TestMapXml game) {

    try (final InputStream is = openInputStream(game.toString(), new String[] {"test_data"})) {
      if (is == null) {
        throw new IllegalStateException(game + " does not exist");
      }
      return (new GameParser(game.toString())).parse(is, new AtomicReference<>(), false);
    } catch (final Exception e) {
      throw new IllegalStateException(e);
    }
  }

  /*
   * First try to load the game as a file on the classpath, if not found there
   * then try to load it from either the "maps" or "test_data" folders.
   */
  private static InputStream openInputStream(final String game, final String[] possibleFolders) {
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
    File f = new File(ClientFileSystemHelper.getGameRunnerFileLocation(GameRunner.class.getSimpleName() + ".class"));

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
