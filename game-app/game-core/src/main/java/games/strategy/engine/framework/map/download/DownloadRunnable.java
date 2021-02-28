package games.strategy.engine.framework.map.download;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Downloads a map index file, parses it and returns a <code>List</code> of <code>
 * DownloadFileDescription</code>.
 */
@Slf4j
public class DownloadRunnable {

  private DownloadRunnable() {}

  /**
   * Parses a file at the given URL into a List of {@link DownloadFileDescription}s. If an error
   * occurs this will return an empty list.
   */
  public static List<DownloadFileDescription> download(final String url) {
    return DownloadConfiguration.contentReader()
        .download(url, DownloadFileParser::parse)
        .orElseGet(List::of);
  }

  /**
   * Parses a file at the given {@link Path} into a List of {@link DownloadFileDescription}s. If an
   * error occurs this will return an empty list.
   */
  public static List<DownloadFileDescription> readLocalFile(final Path path) {
    try (InputStream inputStream = Files.newInputStream(path)) {
      return DownloadFileParser.parse(inputStream);
    } catch (final IOException e) {
      log.error("Failed to read file at: " + path.toAbsolutePath(), e);
      return List.of();
    }
  }
}
