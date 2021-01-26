package org.triplea.io;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Preconditions;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/** A collection of useful methods related to files. */
@UtilityClass
@Slf4j
public final class FileUtils {

  /**
   * Creates a new file with a parent folder and any number of child folders. This is a convenience
   * method to concatenate the path together with an OS specific file separator.
   */
  public static File newFile(final String parentDir, final String... childDirs) {
    return Path.of(parentDir, childDirs).toFile();
  }

  /**
   * Returns a collection of abstract pathnames denoting the files and directories in the specified
   * directory.
   *
   * @param directory The directory whose files are to be listed.
   * @return An immutable collection of files. If {@code directory} does not denote a directory, the
   *     collection will be empty.
   * @see File#listFiles()
   */
  public static Collection<File> listFiles(final File directory) {
    checkNotNull(directory);
    return Optional.ofNullable(directory.listFiles()).map(List::of).orElseGet(List::of);
  }

  public static URL toUrl(final File file) {
    return toUrl(file.toPath());
  }

  public static URL toUrl(final Path file) {
    try {
      return file.toUri().toURL();
    } catch (final MalformedURLException e) {
      throw new IllegalStateException(
          "Invalid conversion from file to URL, file: " + file.toFile().getAbsolutePath(), e);
    }
  }

  // TODO: test
  /**
   * Searchs a file system for a specific file name starting from a given directory.
   *
   * @param searchRoot The directory whose contents we will search (and sub-directories)
   * @param maxDepth The maximum number of subdirectories to search. Zero means only search the
   *     'searchRoot' directory.
   * @param fileName The name of the file to be search for.
   * @return A file matching the given name or empty if not found.
   */
  public Optional<File> findFile(final File searchRoot, final int maxDepth, final String fileName) {
    Preconditions.checkArgument(searchRoot.isDirectory());
    Preconditions.checkArgument(searchRoot.exists());
    Preconditions.checkArgument(maxDepth > -1);
    Preconditions.checkArgument(!fileName.isBlank());
    try (Stream<Path> files = Files.walk(searchRoot.toPath(), maxDepth)) {
      return files
          .map(Path::toFile)
          .filter(File::isFile)
          .filter(f -> f.getName().equals(fileName))
          .findAny();
    } catch (final IOException e) {
      log.error(
          "Unable to access files in: " + searchRoot.getAbsolutePath() + ", " + e.getMessage(), e);
      return Optional.empty();
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
      final File file, final Function<InputStream, T> inputStreamFunction) {
    Preconditions.checkArgument(file.exists(), file.getAbsolutePath());
    Preconditions.checkArgument(file.isFile(), file.getAbsolutePath());

    try (FileInputStream inputStream = new FileInputStream(file)) {
      return inputStreamFunction.apply(inputStream);
    } catch (final IOException e) {
      throw new UnableToReadFileException(file, e);
    }
  }

  private static class UnableToReadFileException extends RuntimeException {
    private static final long serialVersionUID = -3739909439458686372L;

    UnableToReadFileException(final File file, final IOException e) {
      super("Unable to open file: " + file.getAbsolutePath() + ", " + e.getMessage(), e);
    }
  }

  public static Collection<File> findXmlFiles(final File mapFolder, final int maxXmlSearchDepth) {
    try (Stream<Path> files = Files.walk(mapFolder.toPath(), maxXmlSearchDepth)) {
      return files
          .map(Path::toFile)
          .filter(File::isFile)
          .filter(file -> file.getName().endsWith(".xml"))
          .collect(Collectors.toList());
    } catch (final IOException e) {
      throw new FileSystemReadError(mapFolder, e);
    }
  }

  private static class FileSystemReadError extends RuntimeException {
    private static final long serialVersionUID = -1962042508193702048L;

    FileSystemReadError(final File file, final IOException e) {
      super("Error reading files in: " + file.getAbsolutePath(), e);
    }
  }
}
