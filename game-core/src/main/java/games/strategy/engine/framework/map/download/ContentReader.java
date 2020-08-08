package games.strategy.engine.framework.map.download;
// TODO: move to package games.strategy.engine.framework.map.download.client

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.logging.Level;
import lombok.AllArgsConstructor;
import lombok.extern.java.Log;
import org.triplea.java.function.ThrowingFunction;

/**
 * Provides methods to download content via HTTP. Auto-closes all http client and stream resources.
 */
@Log
@AllArgsConstructor
public final class ContentReader {
  private final ThrowingFunction<URI, CloseableDownloader, IOException> downloaderFactory;

  /**
   * Downloads the resource at the specified URI, applies the given Function to it and returns the
   * result.
   *
   * @param uri The resource URI; must not be {@code null}.
   * @param action The action to perform using the give InputStream; must not be {@code null}.
   */
  public <T> Optional<T> download(
      final String uri, final ThrowingFunction<InputStream, T, IOException> action) {
    checkNotNull(uri);
    checkNotNull(action);

    try {
      return Optional.of(downloadAndApplyAction(uri, action));
    } catch (final IOException e) {
      log.log(Level.SEVERE, "Error while downloading file", e);
      return Optional.empty();
    }
  }

  private <T> T downloadAndApplyAction(
      final String uri, final ThrowingFunction<InputStream, T, IOException> action)
      throws IOException {

    try (CloseableDownloader closeableDownloader = downloaderFactory.apply(URI.create(uri))) {
      return action.apply(closeableDownloader.getStream());
    }
  }

  /**
   * Downloads the resource at the specified URI to the specified file.
   *
   * @param uri The resource URI; must not be {@code null}.
   * @param file The file that will receive the resource; must not be {@code null}.
   * @throws IOException If an error occurs during the download.
   */
  void downloadToFile(final String uri, final File file) throws IOException {
    checkNotNull(uri);
    checkNotNull(file);

    downloadAndApplyAction(
        uri, is -> Files.copy(is, file.toPath(), StandardCopyOption.REPLACE_EXISTING));
  }
}
