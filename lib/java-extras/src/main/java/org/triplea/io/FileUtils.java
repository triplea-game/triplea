package org.triplea.io;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.MalformedInputException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

/** A collection of useful methods related to files. */
@UtilityClass
@Slf4j
public final class FileUtils {

  public static Path newTempFolder() {
    try {
      return Files.createTempDirectory("triplea");
    } catch (final IOException e) {
      throw new FileSystemException(e);
    }
  }

  /**
   * Calculates the size behind a path.
   *
   * @param path to file or folder
   * @return file size in byters or the sum of bytes of the files contained in the folder
   * @throws IOException if an I/O error is thrown by a visitor method traversing the folder path
   */
  public static long getByteSizeFromPath(Path path) throws IOException {
    final long byteSize;
    if (path.toFile().isFile()) {
      byteSize = Files.size(path);
    } else {
      byteSize = getFolderSize(path);
    }
    return byteSize;
  }

  /**
   * Calculates the folder size in bytes.
   *
   * @param folderPath assumed folder path
   * @return sum of bytes of the files contained in the folder
   * @throws IOException if an I/O error is thrown by a visitor method
   */
  private static long getFolderSize(Path folderPath) throws IOException {
    final long[] size = {0};

    Files.walkFileTree(
        folderPath,
        new SimpleFileVisitor<>() {
          @Override
          public @NotNull FileVisitResult visitFile(
              @NotNull Path file, @NotNull BasicFileAttributes attrs) {
            size[0] += attrs.size();
            return FileVisitResult.CONTINUE;
          }
        });

    return size[0];
  }

  private static class FileSystemException extends RuntimeException {
    private static final long serialVersionUID = -2046259158805830577L;

    FileSystemException(final IOException e) {
      super("File system exception (check available disk space), " + e.getMessage(), e);
    }
  }

  /**
   * Returns a collection of abstract pathnames denoting the files and directories in the specified
   * directory.
   *
   * @param directory The directory whose files are to be listed.
   * @return An immutable collection of files. If {@code directory} does not denote a directory, the
   *     collection will be empty.
   * @see Files#list(Path)
   */
  public static Collection<Path> listFiles(final Path directory) {
    checkNotNull(directory);
    try (Stream<Path> stream = Files.list(directory)) {
      return stream.collect(Collectors.toList());
    } catch (final IOException exception) {
      log.error("Failed to list Files in directory " + directory, exception);
      return List.of();
    }
  }

  public static URL toUrl(final Path file) {
    try {
      return file.toUri().toURL();
    } catch (final MalformedURLException e) {
      throw new IllegalStateException(
          "Invalid conversion from file to URL, file: " + file.toAbsolutePath(), e);
    }
  }

  /**
   * Searches a file system starting from a given directory looking for a file or directory with a
   * matching name. If multiple matching paths are found, returns the one closest to the root (via
   * minimum length of the absolute path).
   *
   * @param searchRoot The directory whose contents we will search (and subdirectories).
   * @param maxDepth The maximum number of subdirectories to search. Zero means only search the
   *     'searchRoot' directory.
   * @param fileName The name of the file to be search for.
   * @return A file matching the given name or empty if not found.
   */
  public Optional<Path> findClosestToRoot(
      final Path searchRoot, final int maxDepth, final String fileName) {
    return find(searchRoot, maxDepth, fileName).stream().findAny();
  }

  /**
   * Searches a file system starting from a given directory looking for files or directories with
   * matching names. The resulting list will be in ascending order by absolute path length.
   *
   * @param searchRoot The directory whose contents we will search (and subdirectories).
   * @param maxDepth The maximum number of subdirectories to search. Zero means only search the
   *     'searchRoot' directory.
   * @param fileName The name of the file to be search for.
   * @return A list of files matching the given name or an empty list if not.
   */
  public List<Path> find(final Path searchRoot, final int maxDepth, final String fileName) {
    Preconditions.checkArgument(Files.isDirectory(searchRoot), searchRoot.toAbsolutePath());
    Preconditions.checkArgument(Files.exists(searchRoot), searchRoot.toAbsolutePath());
    Preconditions.checkArgument(maxDepth > -1);
    Preconditions.checkArgument(!fileName.isBlank());
    try (Stream<Path> files = Files.walk(searchRoot, maxDepth)) {
      return files
          .filter(f -> f.getFileName().toString().equals(fileName))
          // Sort by path length (shortest to longest), so that the ordering is deterministic and
          // paths closer to the root are earlier in the list.
          .sorted(Comparator.comparingInt(f -> f.toAbsolutePath().toString().length()))
          .collect(Collectors.toList());
    } catch (final IOException e) {
      log.error(
          "Unable to access files in: " + searchRoot.toAbsolutePath() + ", " + e.getMessage(), e);
      return List.of();
    }
  }

  /**
   * Recursively searches current folder and parent folders for a given file name.
   *
   * @return Path to file or empty if not found.
   */
  public Optional<Path> findFileInParentFolders(final Path searchRoot, final String fileName) {
    if (searchRoot == null) {
      return Optional.empty();
    }

    if (Files.exists(searchRoot.resolve(fileName))) {
      return Optional.of(searchRoot.resolve(fileName));
    } else {
      return findFileInParentFolders(searchRoot.getParent(), fileName);
    }
  }

  /**
   * Method to conveniently open an input stream reading a file and run a function on that input
   * stream producing some return value.
   *
   * @param file The file to be read.
   * @param inputStreamFunction Function to be run on the input stream (if able to open).
   * @return Return value of the inputStream function.
   * @throws UnableToReadFileException thrown if there are exceptions generated when opening the
   *     input stream.
   * @throws IllegalArgumentException thrown if input file does not exist or is not a file.
   */
  public <T> T openInputStream(
      final Path file, final Function<InputStream, T> inputStreamFunction) {
    Preconditions.checkArgument(Files.exists(file), file.toAbsolutePath());
    Preconditions.checkArgument(Files.isReadable(file), file.toAbsolutePath());

    try (InputStream inputStream = Files.newInputStream(file)) {
      return inputStreamFunction.apply(inputStream);
    } catch (final IOException e) {
      throw new UnableToReadFileException(file, e);
    }
  }

  private static class UnableToReadFileException extends RuntimeException {
    private static final long serialVersionUID = -3739909439458686372L;

    UnableToReadFileException(final Path file, final IOException e) {
      super("Unable to open file: " + file.toAbsolutePath() + ", " + e.getMessage(), e);
    }
  }

  public static Collection<Path> findXmlFiles(final Path mapFolder, final int maxXmlSearchDepth) {
    try (Stream<Path> files = Files.walk(mapFolder, maxXmlSearchDepth)) {
      return files
          .filter(Predicate.not(Files::isDirectory))
          .filter(file -> file.getFileName().toString().endsWith(".xml"))
          .collect(Collectors.toList());
    } catch (final IOException e) {
      throw new FileSystemReadError(mapFolder, e);
    }
  }

  private static class FileSystemReadError extends RuntimeException {
    private static final long serialVersionUID = -1962042508193702048L;

    FileSystemReadError(final Path file, final IOException e) {
      super("Error reading files in: " + file.toAbsolutePath(), e);
    }
  }

  /**
   * Reads and returns the contents of a given file. Returns empty if the file does not exist or if
   * there were any errors reading the file. Character encodings allowed: UTf-8, ISO_8859_1
   */
  public static Optional<String> readContents(final Path fileToRead) {
    if (!Files.exists(fileToRead)) {
      return Optional.empty();
    }

    // try to read the file with default character encoding, if fails then fallback
    // to UTF-8 then try ISO-8859_1
    try {
      try {
        return Optional.of(Files.readString(fileToRead));
      } catch (final MalformedInputException e) {
        log.info(
            "Warning: file was not saved as UTF-8, some characters may not render:  {}, {}",
            fileToRead.toAbsolutePath(),
            e.getMessage());
      }

      try {
        return Optional.of(Files.readString(fileToRead, Charsets.ISO_8859_1));
      } catch (final MalformedInputException e) {
        log.warn(
            "Bad file encoding: "
                + fileToRead.toAbsolutePath()
                + ", contact the map maker and ask them to save this file as UTF-8");
        return Optional.empty();
      }
    } catch (final IOException e) {
      log.error("Error reading file: {}, {}", fileToRead.toAbsolutePath(), e.getMessage(), e);
    }
    return Optional.empty();
  }

  public static void writeToFile(final Path fileToWrite, final String contents) {
    try {
      Files.writeString(fileToWrite, contents);
    } catch (final IOException e) {
      log.error("Failed to write file: {}, {}", fileToWrite.toAbsolutePath(), e.getMessage(), e);
    }
  }

  /**
   * Utility to delete file specified by the given path. This method handles any needed logging if
   * the delete fails.
   */
  public static void delete(final Path pathToDelete) {
    try {
      Files.delete(pathToDelete);
    } catch (final IOException e) {
      log.error("Failed to delete file: {}, {}", pathToDelete.toAbsolutePath(), e.getMessage(), e);
    }
  }

  /**
   * Creates a temp file, logs and returns an empty optional if there is a problem creating the temp
   * file.
   */
  public static Optional<Path> createTempFile() {
    try {
      return Optional.of(Files.createTempFile("triplea-temp-file", ".temp"));
    } catch (final IOException e) {
      log.error("Failed to create temp file: {}", e.getMessage(), e);
      return Optional.empty();
    }
  }

  public static void deleteDirectory(final Path path) throws IOException {
    org.apache.commons.io.FileUtils.deleteDirectory(path.toFile());
  }

  /**
   * Does an overwrite of one folder onto another and rolls back if there were errors. The rollback
   * is done by first moving the destination folder to a backup location. If there are any errors
   * then we delete whatever we copied and move the backup location back to the destination
   * location.
   *
   * <p>If the destination folder does not exist then this behaves like a folder move.
   *
   * @param src The folder to be moved.
   * @param dest A folder that will be erased and replaced by the contents of 'src'.
   * @return True if the move operation succeed, false if not. If the operation does not succeed,
   *     this method will log the details.
   */
  public static boolean replaceFolder(final Path src, final Path dest) {
    return replaceFolder(src, dest, new FileMoveOperation());
  }

  @VisibleForTesting
  static class FileMoveOperation {
    void move(final Path src, final Path dest) throws IOException {
      Files.move(src, dest);
    }
  }

  @VisibleForTesting
  static boolean replaceFolder(
      final Path src, final Path dest, final FileMoveOperation fileMoveOperation) {

    if (!Files.exists(dest)) {
      // no folder exists at the destination, this is just a move and not a replace
      try {
        fileMoveOperation.move(src, dest);
      } catch (final IOException e) {
        log.warn(
            "Failed to move {} to {}. <br>"
                + "Check that the destination folder is not owned by an administrator. <br>"
                + "Error message: {}",
            src.toAbsolutePath(),
            dest.toAbsolutePath(),
            e.getMessage(),
            e);
      }
      return true;
    }

    // otherwise, create a backup of the destination folder before we replace it
    final Path backupFolder;
    try {
      backupFolder = Files.createTempDirectory("temp-dir").resolve(dest.getFileName());
    } catch (final IOException e) {
      log.warn("Failed to create temp folder: " + e.getMessage(), e);
      return false;
    }

    try {
      // make a complete backup by moving the dest folder to back up
      fileMoveOperation.move(dest, backupFolder);

      // do the folder move
      fileMoveOperation.move(src, dest);

      // folder replace was a success, clean up the backup folder
      deleteDirectory(backupFolder);

      return true;
    } catch (final IOException e) {
      log.warn(
          "Unable to replace folder: {} <br/>"
              + "Are you low on disk space?<br/>"
              + " Is the destination folder owned by administrator but you"
              + " are running TripleA as a non-administrator?",
          dest.toAbsolutePath());

      // anything that exists at 'dest' is a failed copy and can be cleaned up
      try {
        if (Files.exists(dest)) {
          deleteDirectory(dest);
        }
        // restore the backup folder
        fileMoveOperation.move(backupFolder, dest);
      } catch (final IOException e2) {
        log.error(
            "Failed to rollback, failed to restore backup folder: {}, to: {}",
            backupFolder.toAbsolutePath(),
            dest.toAbsolutePath());
      }
      return false;
    }
  }

  /**
   * Returns the file system 'last modified' time stamp for a given path. Returns an empty if the
   * path does not exist or if there are any errors reading the last modified time stamp.
   */
  public static Optional<Instant> getLastModified(final Path path) {
    if (!Files.exists(path)) {
      return Optional.empty();
    }

    try {
      return Optional.of(Files.getLastModifiedTime(path).toInstant());
    } catch (final IOException e) {
      log.error("Unable to read file system at: " + path + ", " + e.getMessage(), e);
      return Optional.empty();
    }
  }
}
