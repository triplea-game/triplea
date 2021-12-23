package org.triplea.io;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import lombok.experimental.UtilityClass;

/** Util class to wrap & simplify interactions with {@code java.nio.file.Path} */
@UtilityClass
public class PathUtils {

  public URL toUrl(Path path) {
    try {
      return path.toUri().toURL();
    } catch (final MalformedURLException e) {
      throw new IllegalArgumentException(
          String.format("Error creating file system path: %s, %s", path, e.getMessage()), e);
    }
  }
}
