package org.triplea.io;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.experimental.UtilityClass;

/** A collection of useful methods related to files. */
@UtilityClass
public final class FileUtils {

  /**
   * Creates a new file with a parent folder and any number of child folders. This is a convenience
   * method to concatenate the path together with an OS specific file separator.
   */
  public static File newFile(final String parentDir, final String... childDirs) {
    final List<String> dirs = new ArrayList<>();
    dirs.add(parentDir);
    dirs.addAll(List.of(childDirs));
    return new File(String.join(File.separator, dirs));
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
}
