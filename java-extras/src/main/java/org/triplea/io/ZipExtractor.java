package org.triplea.io;

import com.google.common.base.Preconditions;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * Class with method to extract a zip file with check against "Zip Slip" vulnerability
 * (https://snyk.io/research/zip-slip-vulnerability). Exceptions types are used to indicate errors
 * reading the zip file or alternatively errors accessing and writing to file system.
 */
@UtilityClass
@Slf4j
public class ZipExtractor {
  /** Indicates there was an error reading the zip file (zip file is invalid). */
  public static class ZipReadException extends RuntimeException {
    private static final long serialVersionUID = -17268314166178535L;

    ZipReadException(final File fileZip, final Exception e) {
      super("Error reading zip file: " + fileZip.getAbsolutePath() + ", " + e.getMessage(), e);
    }
  }

  /**
   * Indicates there was an error accessing or writing to the file system (zip is okay, but
   * extration failed).
   */
  public static class FileSystemException extends RuntimeException {
    private static final long serialVersionUID = -7396074067935705710L;

    FileSystemException(final File fileZip, final Exception e) {
      super("Error extracting zip file: " + fileZip.getAbsolutePath() + ", " + e.getMessage(), e);
    }

    FileSystemException(final Exception e) {
      super("Error accessing file system: " + e.getMessage(), e);
    }

    FileSystemException(final String message) {
      super(message);
    }

    public FileSystemException(final String message, final Exception e) {
      super(message, e);
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
  public static void unzipFile(final File fileZip, final File destDir)
      throws ZipReadException, FileSystemException {
    Preconditions.checkArgument(
        fileZip.getName().endsWith(".zip"),
        "Illegal arg, must be a zip file: " + fileZip.getAbsolutePath());
    if (destDir.exists()) {
      Preconditions.checkArgument(
          destDir.isDirectory(),
          "Illegal arg, destination directory must be a directory: " + destDir.getAbsolutePath());
    } else {
      Preconditions.checkState(
          destDir.mkdirs(),
          "Error, was not able to create directory: " + destDir.getAbsolutePath());
    }

    // iterate over each zip entry and write to a corresponding file
    try (ZipFile zipFile = new ZipFile(fileZip)) {
      for (final ZipEntry zipEntry : zipFile.stream().collect(Collectors.toList())) {
        try {
          unzipZipEntry(destDir, zipFile, zipEntry);
        } catch (final IOException e) {
          throw new FileSystemException(fileZip, e);
        }
      }
    } catch (final IOException e) {
      throw new ZipReadException(fileZip, e);
    } catch (final ZipSecurityException e) {
      log.error(
          "Malicious zip file detected: "
              + fileZip.getAbsolutePath()
              + ", please report this to TripleA and delete the zip file");
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
  private static void unzipZipEntry(
      final File destDir, final ZipFile zipFile, final ZipEntry zipEntry) throws IOException {
    final File newFile = newFile(destDir, zipEntry);
    if (zipEntry.isDirectory()) {
      if (!newFile.isDirectory() && !newFile.mkdirs()) {
        throw new FileSystemException("Failed to create directory " + newFile);
      }
    } else {
      // fix for Windows-created archives
      final File parent = newFile.getParentFile();
      if (!parent.isDirectory() && !parent.mkdirs()) {
        throw new FileSystemException("Failed to create directory " + parent);
      }

      try (FileOutputStream fos = new FileOutputStream(newFile)) {
        fos.write(zipFile.getInputStream(zipEntry).readAllBytes());
      }
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
  private static File newFile(final File destinationDir, final ZipEntry zipEntry) {
    final File destFile = new File(destinationDir, zipEntry.getName());

    try {
      final String destDirPath = destinationDir.getCanonicalPath();
      final String destFilePath = destFile.getCanonicalPath();

      // ensure that the destination to write is within the target folder. Avoids
      // 'zip slip' vulnerability: https://snyk.io/research/zip-slip-vulnerability
      if (!destFilePath.startsWith(destDirPath + File.separator)) {
        throw new ZipSecurityException(zipEntry);
      }

      return destFile;
    } catch (final IOException e) {
      throw new FileSystemException(e);
    }
  }

  private static class ZipSecurityException extends RuntimeException {
    private static final long serialVersionUID = 4558259887205500763L;

    ZipSecurityException(final ZipEntry zipEntry) {
      throw new IllegalArgumentException("Malicious path: " + zipEntry);
    }
  }
}
