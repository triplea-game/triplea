package org.triplea.io;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.experimental.UtilityClass;

/**
 * Class with method to extract a zip file with check against "Zip Slip" vulnerability
 * (https://snyk.io/research/zip-slip-vulnerability). Exceptions types are used to indicate errors
 * reading the zip file or alternatively errors accessing and writing to file system.
 */
@UtilityClass
public class ZipExtractor {
  /** Arbitrary maximum depth to prevent infinite loops on maliciously crafted zip files. */
  private static final int MAX_DEPTH = 10;

  /** Indicates there was an error reading the zip file (zip file is invalid). */
  public static class ZipReadException extends RuntimeException {
    private static final long serialVersionUID = -17268314166178535L;

    ZipReadException(final Path fileZip, final Exception e) {
      super("Error reading zip file: " + fileZip.toAbsolutePath() + ", " + e.getMessage(), e);
    }
  }

  public static class ZipSecurityException extends RuntimeException {
    private static final long serialVersionUID = 4558259887205500763L;

    ZipSecurityException(final Path zipEntry) {
      super(zipEntry + " is a forbidden path");
    }
  }

  /**
   * Indicates there was an error accessing or writing to the file system (zip is okay, but
   * extration failed).
   */
  public static class FileSystemException extends RuntimeException {
    private static final long serialVersionUID = -7396074067935705710L;

    FileSystemException(final Path fileZip, final Exception e) {
      super("Error extracting zip file: " + fileZip.toAbsolutePath() + ", " + e.getMessage(), e);
    }
  }

  /**
   * Unzips a given zip file into a destination directory. If the destination directory does not
   * exist it will be created. If the zip file is detected to be malicious, this method will be a
   * no-op.
   *
   * @param fileZip The file to be unzipped (must be suffixed .zip).
   * @param destDir The directory to unzip into.
   * @throws ZipReadException Thrown if the target zip file is invalid and could not be read.
   * @throws FileSystemException Thrown if there is an error during extraction.
   */
  public static void unzipFile(final Path fileZip, final Path destDir)
      throws ZipReadException, FileSystemException {
    Preconditions.checkArgument(
        fileZip.toString().endsWith(".zip"),
        "Illegal arg, must be a zip file: " + fileZip.toAbsolutePath());
    if (Files.exists(destDir)) {
      Preconditions.checkArgument(
          Files.isDirectory(destDir),
          "Illegal arg, destination directory must be a directory: " + destDir.toAbsolutePath());
    } else {
      try {
        Files.createDirectories(destDir);
      } catch (final IOException e) {
        throw new IllegalStateException(
            "Error, was not able to create directory: " + destDir.toAbsolutePath(), e);
      }
    }

    // iterate over each zip entry and write to a corresponding file
    // Note: We can switch to the single-param version of newFileSystem() once on Java 13+.
    try (FileSystem zipFileSystem = FileSystems.newFileSystem(fileZip, (ClassLoader) null)) {
      final Path zipRoot = zipFileSystem.getRootDirectories().iterator().next();
      try (Stream<Path> files = Files.walk(zipRoot, MAX_DEPTH)) {
        for (final Path zipEntry : files.collect(Collectors.toList())) {
          unzipZipEntry(destDir, zipRoot, zipEntry);
        }
      } catch (final IOException e) {
        throw new FileSystemException(fileZip, e);
      }
    } catch (final IOException e) {
      throw new ZipReadException(fileZip, e);
    }
  }

  /**
   * Writes a given zip entry to a corresponding file. The file may be specified in directories that
   * have not yet been created and those directories will be created.
   *
   * @throws FileSystemException thrown if there are any errors creating needed destination
   *     directories or writing the extracted file itself.
   * @throws ZipSecurityException thrown a zip entry is requested to be written outside of the
   *     destination directory (EG: zip path is something like "../../../bin/bash")
   * @throws IOException thrown if there are errors writing the extracted zip contents.
   */
  private static void unzipZipEntry(final Path destDir, final Path zipRoot, final Path zipEntry)
      throws IOException {
    final Path newFile = newFile(destDir, zipRoot, zipEntry);
    if (Files.isDirectory(zipEntry)) {
      if (!Files.exists(newFile)) {
        Files.createDirectory(newFile);
      }
    } else {
      Files.copy(zipEntry, newFile, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  /**
   * Extracts the given 'zipEntry' file into the destination directory. This method ensures that the
   * zipEntry is created *in* the destination directory and that a hand-crafted zip with a path like
   * "../../../usr/bin/bash" is not written outside of the target folder.
   *
   * @throws ZipSecurityException thrown a zip entry is requested to be written outside of the
   *     destination directory (EG: zip path is something like "../../../bin/bash")
   * @throws FileSystemException thrown if there are errors accessing the file system (used to
   *     determine what the final path will be for a given zip entry which may require queries to
   *     the file system.)
   */
  private static Path newFile(final Path destinationDir, final Path zipRoot, final Path zipEntry) {
    // We swap file systems here, from the path within the zip to the path on the local machine
    final Path destFile = destinationDir.resolve(zipRoot.relativize(zipEntry).toString());

    // ensure that the destination to write is within the target folder. Avoids
    // 'zip slip' vulnerability: https://snyk.io/research/zip-slip-vulnerability
    if (!destFile.normalize().startsWith(destinationDir.normalize())) {
      throw new ZipSecurityException(zipEntry);
    }

    return destFile;
  }
}
