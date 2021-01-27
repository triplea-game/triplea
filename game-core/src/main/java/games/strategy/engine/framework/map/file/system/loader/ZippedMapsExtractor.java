package games.strategy.engine.framework.map.file.system.loader;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import games.strategy.engine.ClientFileSystemHelper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.triplea.io.FileUtils;
import org.triplea.io.ZipExtractor;
import org.triplea.io.ZipExtractor.FileSystemException;
import org.triplea.io.ZipExtractor.ZipReadException;

/**
 * Responsible to find downloaded maps and unzip any that are zipped. Any 'bad' map zips that we
 * fail to unzip will be moved into a bad-zip folder.
 */
@Builder
@Slf4j
public class ZippedMapsExtractor {
  private static final String ZIP_EXTENSION = ".zip";

  /**
   * Callback to be invoked if we find any zip files. The task passed to the progress indicator will
   * be the unzip task.
   */
  private final Consumer<Runnable> progressIndicator;

  /** Path to where downloaded maps can be found. */
  private final Path downloadedMapsFolder;

  /**
   * Finds all map zips, extracts them and then removes the original zip. If any zipped files are
   * found, then the progressIndicator is invoked with a callback that will execute the unzip task.
   */
  public void unzipMapFiles() {
    final Collection<File> zippedMaps = findAllZippedMapFiles();
    if (zippedMaps.isEmpty()) {
      return;
    }
    progressIndicator.accept(
        () ->
            zippedMaps.forEach(
                mapZip -> {
                  try {
                    unzipMap(mapZip);
                  } catch (final ZipReadException zipReadException) {
                    // Problem reading the zip, move it to a folder so that the user does
                    // not repeatedly see an error trying to read this zip.
                    moveBadZip(mapZip)
                        .ifPresent(
                            newLocation ->
                                log.warn(
                                    "Error extracting map zip: "
                                        + mapZip.getAbsolutePath()
                                        + ", zip has been moved to: "
                                        + newLocation.toFile().getAbsolutePath(),
                                    zipReadException));
                  } catch (final FileSystemException | IOException e) {
                    // Thrown if we are are out of disk space or have file system access issues.
                    // Do not move the zip file to a bad-zip folder as that operation could also
                    // fail.
                    log.warn("Error extracting map zip: " + mapZip + ", " + e.getMessage(), e);
                  }
                }));
  }

  private Collection<File> findAllZippedMapFiles() {
    return FileUtils.listFiles(downloadedMapsFolder.toFile()).stream()
        .filter(File::isFile)
        .filter(file -> file.getName().toLowerCase().endsWith(ZIP_EXTENSION))
        .collect(Collectors.toList());
  }

  /**
   * Unzips are target map file into the downloaded maps folder, deletes the zip file after
   * extraction. Extracted files are first extracted to a temporary location before being moved into
   * the downloaded maps folder. This temporary location is to help avoid intermediate results if
   * for example we run out of disk space while extracting.
   *
   * @param mapZip The map zip file to be extracted to the downloaded maps folder.
   */
  public static void unzipMap(final File mapZip) throws IOException {
    Preconditions.checkArgument(mapZip.isFile(), mapZip.getAbsolutePath());
    Preconditions.checkArgument(mapZip.exists(), mapZip.getAbsolutePath());
    Preconditions.checkArgument(mapZip.getName().endsWith(".zip"), mapZip.getAbsolutePath());

    final String extractionFolderName = createExtractionFolderName(mapZip.getName());
    final Path extractionTarget =
        ClientFileSystemHelper.getUserMapsFolder().toPath().resolve(extractionFolderName);

    final boolean mapIsAlreadyExtracted = extractionTarget.toFile().exists();
    if (mapIsAlreadyExtracted) {
      // no-op, we would not have expected for the map zip to have exist
      return;
    }

    log.info(
        "Extracting map zip: {} -> {}",
        mapZip.getAbsolutePath(),
        extractionTarget.toAbsolutePath());

    // extract into a temp folder first
    final Path tempFolder = Files.createTempDirectory("triplea-unzip");
    ZipExtractor.unzipFile(mapZip, tempFolder.toFile());

    // extraction done, now move the extracted folder to target location
    Files.move(tempFolder, extractionTarget);

    // move properties file if it exists
    final Path propertiesFile = mapZip.toPath().resolveSibling(mapZip.getName() + ".properties");
    if (propertiesFile.toFile().exists()) {
      Files.move(
          propertiesFile, extractionTarget.resolveSibling(extractionFolderName + ".properties"));
    }

    final boolean successfullyExtracted = extractionTarget.toFile().exists();
    if (successfullyExtracted) {
      mapZip.delete();
    }
  }

  /**
   * Removes the '.zip' or '-master.zip' suffix from map names if present. <br>
   * EG: 'map-name-master.zip' -> 'map-name'
   */
  @VisibleForTesting
  static String createExtractionFolderName(final String mapZipName) {
    String newName = mapZipName;
    if (newName.endsWith(".zip")) {
      newName = newName.substring(0, newName.length() - ".zip".length());
    }
    if (newName.endsWith("-master")) {
      newName = newName.substring(0, newName.length() - "-master".length());
    }
    return newName;
  }

  /**
   * Moves a target zip file into a 'bad-zip' folder. This is to prevent the file from being picked
   * up in future unzip operations and cause repeated warning messages to users.
   *
   * @return Returns the new location of the file, returns an empty if the file move operation
   *     failed.
   */
  private Optional<Path> moveBadZip(final File mapZip) {
    final Path badZipFolder = downloadedMapsFolder.resolve("bad-zips");
    if (!badZipFolder.toFile().mkdirs()) {
      log.error(
          "Unable to create folder: "
              + badZipFolder.toFile().getAbsolutePath()
              + ", please report this to TripleA and create the folder manually.");
      return Optional.empty();
    }
    try {
      final Path newLocation = badZipFolder.resolve(mapZip.getName());
      Files.move(mapZip.toPath(), newLocation);
      return Optional.of(newLocation);
    } catch (final IOException e) {
      log.error(
          "Failed to move file: "
              + mapZip.getAbsolutePath()
              + ", to: "
              + badZipFolder.toFile().getAbsolutePath(),
          e);
      return Optional.empty();
    }
  }
}
