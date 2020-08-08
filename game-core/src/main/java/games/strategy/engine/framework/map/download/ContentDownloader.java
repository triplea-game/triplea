package games.strategy.engine.framework.map.download;

import games.strategy.engine.framework.system.HttpProxy;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.Getter;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.triplea.java.Interruptibles;

/**
 * Downloads content from HTTP resources. Meant to be used with a try-with-resources block, eg:
 *
 * <pre>
 *   try(CloseableDownloader downloader = new ContentDownloader(uri)) {
 *     InputStream stream = downloader.getStream();
 *     // read and process stream, stream is auto-closed for you.
 *   }
 * </pre>
 *
 * To process and read the input stream in one-go, use {@see ContentReader}. Use this class if you
 * want direct access to an InputStream. This is useful if you want to be able to change the source
 * of the input stream.
 *
 * <p>Warning: The input stream provided by this class can only be consumed once (property of input
 * streams, the input stream is not reset in any way after reading it).
 */
public final class ContentDownloader implements CloseableDownloader {
  private final CloseableHttpClient httpClient;

  @Getter(onMethod_ = @Override)
  private final InputStream stream;

  private final CloseableHttpResponse response;

  public ContentDownloader(final URI uri) throws IOException {
    this(HttpClients.custom().disableCookieManagement().build(), uri, HttpProxy::addProxy);
  }

  /**
   * On construction we send the HTTP request and set as member variables everything that needs to
   * be closed. Clients can then request the retrieved input stream, then on close we'll close up
   * all objects that need closing.
   *
   * @throws IOException Thrown if there are any communication errors, badly formatted response, or
   *     if the returned status code is not a 200.
   */
  ContentDownloader(
      final CloseableHttpClient httpClient, final URI uri, final Consumer<HttpGet> proxySettings)
      throws IOException {
    this.httpClient = httpClient;
    final HttpGet request = new HttpGet(uri);
    proxySettings.accept(request);
    response = downloadWithSingleRetryOnError(request);

    final int statusCode = response.getStatusLine().getStatusCode();
    if (statusCode != HttpStatus.SC_OK) {
      throw new IOException(String.format("Unexpected status code (%d)", statusCode));
    }

    final HttpEntity entity =
        Optional.ofNullable(response.getEntity())
            .orElseThrow(() -> new IOException("Entity is missing"));
    stream = entity.getContent();
  }

  private CloseableHttpResponse downloadWithSingleRetryOnError(final HttpGet request)
      throws IOException {
    try {
      return httpClient.execute(request);
    } catch (final IOException e) {
      // short back-off before a single retry
      Interruptibles.sleep(1000L);
      return httpClient.execute(request);
    }
  }

  @Override
  public void close() throws IOException {
    stream.close();
    response.close();
    httpClient.close();
  }
}
