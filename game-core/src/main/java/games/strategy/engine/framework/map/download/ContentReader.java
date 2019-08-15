package games.strategy.engine.framework.map.download;
// TODO: move to package games.strategy.engine.framework.map.download.client

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.framework.system.HttpProxy;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import lombok.AllArgsConstructor;
import lombok.extern.java.Log;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.triplea.java.function.ThrowingFunction;

/** Provides methods to download files via HTTP. */
@Log
@AllArgsConstructor
public final class ContentReader {
  private final Supplier<CloseableHttpClient> httpClientFactory;

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
      return Optional.of(downloadInternal(uri, action::apply));
    } catch (final IOException e) {
      log.log(Level.SEVERE, "Error while downloading file", e);
      return Optional.empty();
    }
  }

  @VisibleForTesting
  static <T> T download(
      final String uri,
      final ThrowingFunction<InputStream, T, IOException> action,
      final CloseableHttpClient client)
      throws IOException {
    final HttpGet request = new HttpGet(uri);
    HttpProxy.addProxy(request);

    try (CloseableHttpResponse response = client.execute(request)) {
      final int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != HttpStatus.SC_OK) {
        throw new IOException(String.format("Unexpected status code (%d)", statusCode));
      }

      final HttpEntity entity =
          Optional.ofNullable(response.getEntity())
              .orElseThrow(() -> new IOException("Entity is missing"));

      try (InputStream stream = entity.getContent()) {
        return action.apply(stream);
      }
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

    try (FileOutputStream os = new FileOutputStream(file)) {
      downloadInternal(
          uri, is -> os.getChannel().transferFrom(Channels.newChannel(is), 0L, Long.MAX_VALUE));
    }
  }

  /**
   * Downloads the resource at the specified URI using the configured httpClientFactory.
   *
   * @see #download(String, ThrowingFunction)
   * @param uri The resource URI; must not be {@code null}.
   * @param action The action to perform using the give InputStream; must not be {@code null}.
   * @throws IOException If an error occurs during the download.
   */
  private <T> T downloadInternal(
      final String uri, final ThrowingFunction<InputStream, T, IOException> action)
      throws IOException {
    try (CloseableHttpClient client = httpClientFactory.get()) {
      return download(uri, action, client);
    }
  }
}
