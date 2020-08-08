package games.strategy.engine.framework.map.download;
// TODO: move to package games.strategy.engine.framework.map.download.client

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.framework.system.HttpProxy;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import java.util.logging.Level;
import lombok.AllArgsConstructor;
import lombok.extern.java.Log;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;

/** Can execute an http head request to determine the size of a file download from URI. */
@Log
@AllArgsConstructor
final class DownloadLengthReader {
  @VisibleForTesting
  final ConcurrentMap<String, Long> downloadLengthsByUri = new ConcurrentHashMap<>();

  private final Supplier<CloseableHttpClient> httpClientFactory;

  /**
   * Gets the download length for the resource at the specified URI.
   *
   * <p>This method is thread safe.
   *
   * @param uri The resource URI; must not be {@code null}.
   * @return The download length (in bytes) or empty if unknown; never {@code null}.
   */
  Optional<Long> getDownloadLength(final String uri) {
    return getDownloadLengthFromCache(uri, this::getDownloadLengthFromHost);
  }

  @VisibleForTesting
  Optional<Long> getDownloadLengthFromCache(
      final String uri, final DownloadLengthSupplier supplier) {
    return Optional.ofNullable(
        downloadLengthsByUri.computeIfAbsent(uri, k -> supplier.get(k).orElse(null)));
  }

  @VisibleForTesting
  interface DownloadLengthSupplier {
    Optional<Long> get(String uri);
  }

  private Optional<Long> getDownloadLengthFromHost(final String uri) {
    try (CloseableHttpClient client = httpClientFactory.get()) {
      return getDownloadLengthFromHost(uri, client);
    } catch (final IOException e) {
      log.log(
          Level.INFO, String.format("(Ignoring) Failed to get download length for '%s'", uri), e);
      return Optional.empty();
    }
  }

  @VisibleForTesting
  static Optional<Long> getDownloadLengthFromHost(
      final String uri, final CloseableHttpClient client) throws IOException {
    try (CloseableHttpResponse response = client.execute(newHttpHeadRequest(uri))) {
      final int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != HttpStatus.SC_OK) {
        throw new IOException(String.format("unexpected status code (%d)", statusCode));
      }

      final Header header = response.getFirstHeader(HttpHeaders.CONTENT_LENGTH);
      // NB: it is legal for a server to respond with "Transfer-Encoding: chunked" instead of
      // Content-Length
      if (header == null) {
        return Optional.empty();
      }

      final String encodedLength = header.getValue();
      if (encodedLength == null) {
        throw new IOException("content length header value is absent");
      }

      try {
        return Optional.of(Long.parseLong(encodedLength));
      } catch (final NumberFormatException e) {
        throw new IOException(e);
      }
    }
  }

  private static HttpRequestBase newHttpHeadRequest(final String uri) {
    final HttpHead request = new HttpHead(uri);
    HttpProxy.addProxy(request);
    return request;
  }
}
