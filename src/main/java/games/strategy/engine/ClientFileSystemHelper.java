package games.strategy.engine;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.config.GameEnginePropertyFileReader;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.system.SystemProperties;
import games.strategy.util.Version;

/**
 * Pure utility class, final and private constructor to enforce this
 * WARNING: do not call ClientContext in this class. ClientContext call this class in turn
 * during construction, depending upon ordering this can cause an infinite call loop.
 */
public final class ClientFileSystemHelper {

  private ClientFileSystemHelper() {}

  public static File getRootFolder() {
    final String fileName = getGameRunnerFileLocation(GameRunner.class.getSimpleName() + ".class");

    final String tripleaJarName = "triplea.jar!";
    if (fileName.contains(tripleaJarName)) {
      return getRootFolderRelativeToJar(fileName, tripleaJarName);
    }

    final String tripleaJarNameWithEngineVersion = getTripleaJarWithEngineVersionStringPath();
    final int locn = fileName.indexOf("triplea_" + tripleaJarNameWithEngineVersion + ".jar!");
    if (locn >= 0) {
      return new File(fileName.substring(0, locn - 1));
    }

    return getRootRelativeToClassFile(fileName);
  }


  public static String getGameRunnerFileLocation(final String runnerClassName) {
    final URL url = GameRunner.class.getResource(runnerClassName);
    String fileName = url.getFile();

    try {
      // Deal with spaces in the file name which would be url encoded
      fileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8.name());
    } catch (final UnsupportedEncodingException e) {
      ClientLogger.logError("Unsupported encoding of fileName: " + fileName + ", error: " + e.getMessage());
    }
    return fileName;
  }


  private static String getTripleaJarWithEngineVersionStringPath() {
    // TODO: This is begging for trouble since we call ClientFileSystem during the construction of
    // ClientContext. Though, we will at this point already have parsed the game engine version, so it is okay (but
    // brittle)
    final EngineVersion engine = ClientContext.engineVersion();
    final Version version = engine.getVersion();

    return "triplea_" + version.toStringFull("_") + ".jar!";
  }

  private static File getRootFolderRelativeToJar(final String fileName, final String tripleaJarName) {
    final String subString =
        fileName.substring("file:/".length() - (SystemProperties.isWindows() ? 0 : 1),
            fileName.indexOf(tripleaJarName) - 1);
    final File f = new File(subString).getParentFile();
    if (!f.exists()) {
      throw new IllegalStateException("File not found:" + f);
    }
    return f;
  }

  private static File getRootRelativeToClassFile(final String fileName) {
    File f = new File(fileName);

    // move up one directory for each package
    final int moveUpCount = GameRunner.class.getName().split("\\.").length + 1;
    for (int i = 0; i < moveUpCount; i++) {
      f = f.getParentFile();
    }

    // keep moving up one directory until we find the game_engine properties file that we expect to be at the root
    while (!folderContainsGamePropsFile(f)) {
      f = f.getParentFile();
    }

    if (!f.exists()) {
      System.err.println("Could not find root folder, does  not exist:" + f);
      return new File(System.getProperties().getProperty("user.dir"));
    }
    return f;
  }

  private static boolean folderContainsGamePropsFile(File folder) {
    if (!folder.isDirectory()) {
      folder = new File(folder.toString().substring(5));                // strip off "file:" prefix
    }
    final File[] files = folder.listFiles();
    final List<String> fileNames =
        Arrays.asList(files).stream().map(file -> file.getName()).collect(Collectors.toList());
    return fileNames.contains(GameEnginePropertyFileReader.GAME_ENGINE_PROPERTY_FILE);
  }

  public static boolean areWeOldExtraJar() {
    final URL url = GameRunner.class.getResource(GameRunner.class.getSimpleName() + ".class");
    String fileName = url.getFile();
    try {
      fileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8.name());
    } catch (final UnsupportedEncodingException e) {
      ClientLogger.logQuietly(e);
    }
    final String tripleaJarNameWithEngineVersion = getTripleaJarWithEngineVersionStringPath();
    if (fileName.contains(tripleaJarNameWithEngineVersion)) {
      final String subString = fileName.substring("file:/".length() - (SystemProperties.isWindows() ? 0 : 1),
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

  public static File getUserRootFolder() {
    final File userHome = new File(System.getProperties().getProperty("user.home"));
    final File rootDir = new File(new File(userHome, "Documents"), "triplea");
    return rootDir.exists() ? rootDir : new File(userHome, "triplea");
  }

  public static File getUserMapsFolder() {
    final String path = ClientContext.folderSettings().getDownloadedMapPath();


    final File mapsFolder = new File(path);
    if (!mapsFolder.exists()) {
      try {
        mapsFolder.mkdirs();
      } catch (final SecurityException e) {
        ClientLogger.logError(e);
      }
    }
    if (!mapsFolder.exists()) {
      ClientLogger.logError("Error, downloaded maps folder does not exist: " + mapsFolder.getAbsolutePath());
    }
    return mapsFolder;
  }

  /** Create a temporary file, checked exceptions are re-thrown as unchecked. */
  public static File createTempFile() {
    try {
      return File.createTempFile("triplea", "tmp");
    } catch (final IOException e) {
      throw new IllegalStateException("Failed to create a temporary file", e);
    }
  }
}
