package games.strategy.engine;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.framework.system.SystemProperties;
import games.strategy.triplea.settings.ClientSetting;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.Optional;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.triplea.game.ApplicationContext;
import org.triplea.io.FileUtils;
import org.triplea.util.Services;

/** Provides methods to work with common file locations in a client installation. */
@Slf4j
public final class ClientFileSystemHelper {
  private ClientFileSystemHelper() {}

  /**
   * Returns top-most, or the root folder for the TripleA installation folder.
   *
   * @return Folder that is the 'root' of the tripleA binary installation. This folder and contents
   *     contains the versioned content downloaded and initially installed. This is in contrast to
   *     the user root folder that is not replaced between installations.
   * @throws IllegalStateException If the root folder cannot be located.
   */
  public static Path getRootFolder() {
    try {
      return FileUtils.findFileInParentFolders(getCodeSourceFolder(), ".triplea-root")
          .orElseThrow(() -> new IllegalStateException("Unable to locate root folder"))
          .getParent();
    } catch (final IOException e) {
      throw new IllegalStateException("Unable to locate root folder", e);
    }
  }

  private static Path getCodeSourceFolder() throws IOException {
    final ApplicationContext applicationContext = Services.loadAny(ApplicationContext.class);
    final @Nullable CodeSource codeSource =
        applicationContext.getMainClass().getProtectionDomain().getCodeSource();
    if (codeSource == null) {
      throw new IOException("code source is not available");
    }

    final Path codeSourceLocation;
    try {
      codeSourceLocation = Path.of(codeSource.getLocation().toURI());
    } catch (final URISyntaxException e) {
      throw new IOException("code source location URI is malformed", e);
    }

    // code source location is either a jar file (installation) or a folder (dev environment)
    return Files.isDirectory(codeSourceLocation)
        ? codeSourceLocation
        : codeSourceLocation.getParent();
  }

  /**
   * TripleA stores two folders, one for user content that survives between game installs, and a
   * second that contains binaries. This method returns the 'user folder', which contains maps and
   * save games.
   *
   * @return Folder where tripleA 'user data' is stored between game installations. This folder
   *     would contain as some examples: save games, downloaded maps. This location is currently not
   *     configurable (ideally we would allow this to be set during install perhaps).
   */
  public static Path getUserRootFolder() {
    final Path userHome = Path.of(SystemProperties.getUserHome());
    final Path rootDir = userHome.resolve("Documents").resolve("triplea");
    return Files.exists(rootDir) ? rootDir : userHome.resolve("triplea");
  }

  /**
   * Returns location of the folder containing downloaded TripleA maps. The folder will be created
   * if it does not exist.
   *
   * @return Folder where maps are downloaded and stored. Default location is relative to users home
   *     folder and not the engine install folder, this allows it to be retained between engine
   *     installations. Users can override this location in settings.
   */
  public static Path getUserMapsFolder() {
    return getUserMapsFolder(ClientFileSystemHelper::getUserRootFolder);
  }

  @VisibleForTesting
  static Path getUserMapsFolder(final Supplier<Path> userHomeRootFolderSupplier) {
    final Path defaultDownloadedMapsFolder =
        userHomeRootFolderSupplier.get().resolve("downloadedMaps");

    // make sure folder override location is valid, if not notify user and reset it.
    final Optional<Path> path = ClientSetting.mapFolderOverride.getValue();
    if (path.isPresent() && (!Files.exists(path.get()) || !Files.isWritable(path.get()))) {
      ClientSetting.mapFolderOverride.resetValue();
      log.warn(
          "Invalid map override setting folder does not exist or cannot be written: {}\n"
              + "Reverting to use default map folder location: {}",
          path.get().toAbsolutePath(),
          defaultDownloadedMapsFolder.toAbsolutePath());
    }

    final Path mapsFolder =
        ClientSetting.mapFolderOverride
            .getValue()
            .orElseGet(() -> userHomeRootFolderSupplier.get().resolve("downloadedMaps"));

    if (!Files.exists(mapsFolder)) {
      try {
        Files.createDirectories(mapsFolder);
      } catch (final IOException e) {
        log.error(
            "Error, could not create map download folder: {}", mapsFolder.toAbsolutePath(), e);
      }
    }
    return mapsFolder;
  }
}
