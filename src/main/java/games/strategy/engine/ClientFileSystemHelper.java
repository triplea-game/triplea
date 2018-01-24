package games.strategy.engine;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import com.google.common.annotations.VisibleForTesting;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.config.client.GameEnginePropertyReader;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.system.SystemProperties;
import games.strategy.io.FileUtils;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.settings.GameSetting;

/**
 * Provides methods to work with common file locations in a client installation.
 */
public final class ClientFileSystemHelper {

  private ClientFileSystemHelper() {}

  /**
   * Returns top-most, or the root folder for the TripleA installation folder.
   *
   * @return Folder that is the 'root' of the tripleA binary installation. This folder and
   *         contents contains the versioned content downloaded and initially installed. This is
   *         in contrast to the user root folder that is not replaced between installations.
   */
  public static File getRootFolder() {
    final String classFilePath = getGameRunnerClassFilePath();

    final String jarFileName = String.format("triplea-%s-all.jar!", ClientContext.engineVersion().getExactVersion());
    if (classFilePath.contains(jarFileName)) {
      return getRootFolderRelativeToJar(classFilePath, jarFileName);
    }

    return getRootFolderRelativeToClassFile(classFilePath);
  }

  private static String getGameRunnerClassFilePath() {
    final String runnerClassName = GameRunner.class.getSimpleName() + ".class";
    final URL url = GameRunner.class.getResource(runnerClassName);
    final String path = url.getFile();
    try {
      // Deal with spaces in the file name which would be url encoded
      return URLDecoder.decode(path, StandardCharsets.UTF_8.name());
    } catch (final UnsupportedEncodingException e) {
      throw new AssertionError("platform does not support UTF-8 charset", e);
    }
  }

  private static File getRootFolderRelativeToJar(final String path, final String jarFileName) {
    final String subString = path.substring(
        "file:/".length() - (SystemProperties.isWindows() ? 0 : 1),
        path.indexOf(jarFileName) - 1);
    final File f = new File(subString).getParentFile();
    if (!f.exists()) {
      throw new IllegalStateException("File not found:" + f);
    }
    return f;
  }

  private static File getRootFolderRelativeToClassFile(final String path) {
    File f = new File(path);

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
      return new File(SystemProperties.getUserDir());
    }
    return f;
  }

  private static boolean folderContainsGamePropsFile(final File folder) {
    return FileUtils.listFiles(folder).stream()
        .map(File::getName)
        .anyMatch(it -> GameEnginePropertyReader.GAME_ENGINE_PROPERTIES_FILE.equals(it));
  }

  /**
   * TripleA stores two folders, one for user content that survives between game installs,
   * and a second that contains binaries. This method returns the 'user folder', which contains
   * maps and save games.
   *
   * @return Folder where tripleA 'user data' is stored between game installations. This folder
   *         would contain as some examples: save games, downloaded maps. This location is currently
   *         not configurable (ideally we would allow this to be set during install perhaps).
   */
  public static File getUserRootFolder() {
    final File userHome = new File(SystemProperties.getUserHome());
    final File rootDir = new File(new File(userHome, "Documents"), "triplea");
    return rootDir.exists() ? rootDir : new File(userHome, "triplea");
  }

  /**
   * Returns location of the folder containing downloaded TripleA maps.
   *
   * @return Folder where maps are downloaded and stored. Default location is relative
   *         to users home folder and not the engine install folder, this allows it to be
   *         retained between engine installations. Users can override this location in settings.
   */
  public static File getUserMapsFolder() {
    final String path = getUserMapsFolderPath(ClientSetting.USER_MAPS_FOLDER_PATH, ClientSetting.MAP_FOLDER_OVERRIDE);
    final File mapsFolder = new File(path);
    if (!mapsFolder.exists()) {
      mapsFolder.mkdirs();
    }
    if (!mapsFolder.exists()) {
      ClientLogger.logError("Error, downloaded maps folder does not exist: " + mapsFolder.getAbsolutePath());
    }
    return mapsFolder;
  }

  @VisibleForTesting
  static String getUserMapsFolderPath(
      final GameSetting currentUserMapsFolderPathSetting,
      final GameSetting overrideUserMapsFolderPathSetting) {
    return overrideUserMapsFolderPathSetting.isSet()
        ? overrideUserMapsFolderPathSetting.value()
        : currentUserMapsFolderPathSetting.value();
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
