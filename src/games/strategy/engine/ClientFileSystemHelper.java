package games.strategy.engine;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.GameRunner2;
import games.strategy.util.Version;

/** Pure utility class, final and private constructor to enforce this */
public final class ClientFileSystemHelper {
  private ClientFileSystemHelper() {

  }

  /** This method is available via ClientContext */
  protected static File getRootFolder() {
    final String fileName = getGameRunnerFileLocation("GameRunner2.class");

    final String tripleaJarName = "triplea.jar!";
    if (fileName.contains(tripleaJarName)) {
      return getRootFolderRelativeToJar(fileName, tripleaJarName);
    }

//    final String tripleaJarNameWithEngineVersion = getTripleaJarWithEngineVersionStringPath();
    if (fileName.contains("triplea_") && fileName.contains(".jar!")) {
      Pattern pattern = Pattern.compile("triplea_.*\\.jar!");
      Matcher matcher = pattern.matcher(fileName);

      String tripleaJarNameWithEngineVersion =   matcher.group();
      return getRootFolderRelativeToJar(fileName, tripleaJarNameWithEngineVersion);
    }

    return getRootRelativeToClassFile(fileName);
  }


  private static String getGameRunnerFileLocation(final String runnerClassName) {
    final URL url = GameRunner2.class.getResource(runnerClassName);
    String fileName = url.getFile();

    try {
      // Deal with spaces in the file name which would be url encoded
      fileName = URLDecoder.decode(fileName, "UTF-8");
    } catch (final UnsupportedEncodingException e) {
      ClientLogger.logError("Unsupported encoding of fileName: " + fileName + ", error: " + e.getMessage());
    }
    return fileName;
  }


  private static String getTripleaJarWithEngineVersionStringPath() {
    ClientContext context = ClientContext.getInstance();
    EngineVersion engine = context.engineVersion();
    Version version = engine.getVersion();

    return "triplea_" + version.toStringFull("_") + ".jar!";
  }

  private static File getRootFolderRelativeToJar(final String fileName, final String tripleaJarName) {
    final String subString =
        fileName.substring("file:/".length() - (GameRunner.isWindows() ? 0 : 1), fileName.indexOf(tripleaJarName) - 1);
    final File f = new File(subString).getParentFile();
    if (!f.exists()) {
      throw new IllegalStateException("File not found:" + f);
    }
    return f;
  }

  private static File getRootRelativeToClassFile(final String fileName) {
    File f = new File(fileName);

    // move up 1 directory for each package
    final int moveUpCount = GameRunner2.class.getName().split("\\.").length + 1;
    for (int i = 0; i < moveUpCount; i++) {
      f = f.getParentFile();
    }
    if (!f.exists()) {
      System.err.println("Could not find root folder, does  not exist:" + f);
      return new File(System.getProperties().getProperty("user.dir"));
    }
    return f;
  }

  protected static boolean areWeOldExtraJar() {
    final URL url = GameRunner2.class.getResource("GameRunner2.class");
    String fileName = url.getFile();
    try {
      fileName = URLDecoder.decode(fileName, "UTF-8");
    } catch (final UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    final String tripleaJarNameWithEngineVersion = getTripleaJarWithEngineVersionStringPath();
    if (fileName.contains(tripleaJarNameWithEngineVersion)) {
      final String subString = fileName.substring("file:/".length() - (GameRunner.isWindows() ? 0 : 1),
          fileName.indexOf(tripleaJarNameWithEngineVersion) - 1);
      final File f = new File(subString);
      if (!f.exists()) {
        throw new IllegalStateException("File not found:" + f);
      }
      String path;
      try {
        path = f.getCanonicalPath();
      } catch (final IOException e) {
        path = f.getPath();
      }
      return path.contains("old");
    }
    return false;
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
  public static File getFile(final String game, final String[] possibleFolders) {
    for (final String possibleFolder : possibleFolders) {
      final File start = ClientContext.getRootFolder();
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
    File f = new File(getGameRunnerFileLocation("GameRunner2.class"));

    while (f != null && f.exists() && !folderContains(f, folderToFind)) {
      f = f.getParentFile();
    }
    return f;
  }


  /* Check if a folder contains another folder or file */
  private static boolean folderContains(final File folder, final String childToFind) {
    if (folder == null || folder.list() == null || folder.list().length == 0) {
      return false;
    }
    return Arrays.asList(folder.list()).contains(childToFind);
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


}
