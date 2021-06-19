package org.triplea.io;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.triplea.java.Interruptibles;
import org.triplea.java.StringUtils;

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
@Slf4j
public final class ContentDownloader implements CloseableDownloader {
  private final CloseableHttpClient httpClient;

  @Getter(onMethod_ = @Override)
  private final InputStream stream;

  private final CloseableHttpResponse response;

  public ContentDownloader(final URI uri) throws IOException {
    this(HttpClients.custom().disableCookieManagement().build(), uri, proxyUpdate -> {});
  }

  public ContentDownloader(final URI uri, final Consumer<HttpGet> proxySettings)
      throws IOException {
    this(HttpClients.custom().disableCookieManagement().build(), uri, proxySettings);
  }

  /**
   * On construction we send the HTTP request and set as member variables everything that needs to
   * be closed. Clients can then request the retrieved input stream, then on close we'll close up
   * all objects that need closing.
   *
   * @throws IOException Thrown if there are any communication errors, badly formatted response, or
   *     if the returned status code is not a 200.
   */
  @VisibleForTesting
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

  public static Optional<String> downloadAsString(final URI uri) {
    return downloadAsStringWithProxy(uri, null);
  }

  public static Optional<String> downloadAsStringWithProxy(
      final URI uri, @Nullable final HttpHost proxy) {
    try {
      return Optional.of(download(uri, StringUtils::readFully, proxy));
    } catch (final DownloadException e) {
      if (e.isServerError()) {
        log.error(
            "Contact TripleA for support, Error downloading: {}, "
                + "server error. Server status: {}, error: {}",
            uri,
            e.getStatusCode(),
            e.getMessage());
        return Optional.empty();
      } else {
        log.warn(
            "Check internent connection. Error downloading: {}, status: {}, error: {}",
            uri,
            e.getStatusCode(),
            e.getMessage());
        return Optional.empty();
      }
    }
  }

  /**
   * Downloads content from a given URI and runs a function on the downloaded content and returns
   * that functions output.
   *
   * @param uri The URI to download
   * @param streamProcessor Function to process the downloaded input.
   * @return Result of the stream processing function
   * @throws DownloadException Thrown if there are any problems during download.
   */
  private static <T> T download(
      final URI uri, final Function<InputStream, T> streamProcessor, @Nullable final HttpHost proxy)
      throws DownloadException {

    final HttpGet request = new HttpGet(uri);
    Optional.ofNullable(proxy)
        .ifPresent(
            proxyHost ->
                request.setConfig(
                    RequestConfig.copy(request.getConfig()).setProxy(proxyHost).build()));

    try (CloseableHttpClient httpClient = HttpClients.custom().disableCookieManagement().build();
        CloseableHttpResponse response = httpClient.execute(request)) {
      final int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != HttpStatus.SC_OK) {
        throw new DownloadException(
            statusCode, String.format("Unexpected status code (%d)", statusCode));
      }
      final HttpEntity entity =
          Optional.ofNullable(response.getEntity())
              .orElseThrow(() -> new DownloadException(statusCode, "Entity is missing"));
      try (InputStream stream = entity.getContent()) {
        return streamProcessor.apply(stream);
      } catch (final IOException e) {
        throw new DownloadException(statusCode, e);
      }
    } catch (final IOException e) {
      throw new DownloadException(0, e);
    }
  }

  /** Thrown if there are any problems downloading or reading downloaded content. */
  @Getter
  public static class DownloadException extends Exception {
    private static final long serialVersionUID = -4049197878908223175L;
    private final int statusCode;

    DownloadException(final int statusCode, final String message) {
      super(message);
      this.statusCode = statusCode;
    }

    DownloadException(final int statusCode, final Exception cause) {
      super(cause);
      this.statusCode = statusCode;
    }

    boolean isServerError() {
      return statusCode >= 500;
    }
  }
}
