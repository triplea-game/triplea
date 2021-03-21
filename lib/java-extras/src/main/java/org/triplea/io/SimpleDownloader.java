package org.triplea.io;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Optional;
import java.util.function.Function;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

/**
 * Bare-bones utility class to download content from a specific URI and process the downloaded
 * content.
 */
@UtilityClass
public final class SimpleDownloader {

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
  public static <T> T downloadAndExecute(
      final URI uri, final Function<InputStream, T> streamProcessor) throws DownloadException {

    final HttpGet request = new HttpGet(uri);
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
}
