package games.strategy.engine.framework.map.download;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.google.common.annotations.VisibleForTesting;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.framework.system.HttpProxy;

public final class DownloadUtils {
  private static final Map<String, Long> downloadLengthCache = new HashMap<>();

  private DownloadUtils() {}

  static Optional<Long> getDownloadLength(final String uri) {
    if (!downloadLengthCache.containsKey(uri)) {
      final Optional<Long> length = getDownloadLengthWithoutCache(uri);
      if (length.isPresent()) {
        downloadLengthCache.put(uri, length.get());
      } else {
        return Optional.empty();
      }
    }
    return Optional.of(downloadLengthCache.get(uri));
  }

  private static Optional<Long> getDownloadLengthWithoutCache(final String uri) {
    try (final CloseableHttpClient client = newHttpClient()) {
      return getLengthOfResourceAt(uri, client);
    } catch (final IOException e) {
      ClientLogger.logQuietly(String.format("failed to get download length for '%s'", uri), e);
      return Optional.empty();
    }
  }

  private static CloseableHttpClient newHttpClient() {
    return HttpClients.custom()
        .setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build())
        .build();
  }

  /**
   * Gets the length of the resource at the specified URI.
   *
   * @param uri The resource URI; must not be {@code null}.
   * @param client The client through which to connect; must not be {@code null}.
   *
   * @return The resource length or empty if the resource length is unknown; never {@code null}.
   *
   * @throws IOException If an error occurs while attempting to get the resource length.
   */
  @VisibleForTesting
  static Optional<Long> getLengthOfResourceAt(
      final String uri,
      final CloseableHttpClient client) throws IOException {
    try (final CloseableHttpResponse response = client.execute(newHttpHeadRequest(uri))) {
      final int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != HttpStatus.SC_OK) {
        throw new IOException(String.format("unexpected status code (%d)", statusCode));
      }

      final Header header = response.getFirstHeader(HttpHeaders.CONTENT_LENGTH);
      // NB: it is legal for a server to respond with "Transfer-Encoding: chunked" instead of Content-Length
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

  static URL toURL(String url) {
    try {
      return new URL(url);
    } catch (MalformedURLException e) {
      throw new IllegalStateException("Invalid URL: " + url, e);
    }
  }

  static void downloadFile(URL url, File targetFile) throws IOException {
    FileOutputStream fos = new FileOutputStream(targetFile);
    url = getUrlFollowingRedirects(url);
    ReadableByteChannel rbc = Channels.newChannel(url.openStream());
    fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
    fos.close();
  }

  public static void downloadFile(String urlString, File targetFile) throws IOException {
    downloadFile(getUrlFollowingRedirects(urlString), targetFile);
  }

  private static URL getUrlFollowingRedirects(String possibleRedirectionUrl) throws IOException {
    URL url = new URL(possibleRedirectionUrl);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    int status = conn.getResponseCode();
    if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM
        || status == HttpURLConnection.HTTP_SEE_OTHER) {
      // update the URL if we were redirected
      url = new URL(conn.getHeaderField("Location"));
    }
    return url;
  }

  private static URL getUrlFollowingRedirects(URL url) throws IOException {
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    int status = conn.getResponseCode();
    if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM
        || status == HttpURLConnection.HTTP_SEE_OTHER) {
      // update the URL if we were redirected
      url = new URL(conn.getHeaderField("Location"));
    }
    return url;
  }
}
