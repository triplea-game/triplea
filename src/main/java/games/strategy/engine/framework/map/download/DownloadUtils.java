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

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.google.common.annotations.VisibleForTesting;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.framework.system.HttpProxy;

public final class DownloadUtils {
  private static Map<URL, Integer> downloadLengthCache = new HashMap<>();

  private DownloadUtils() {}

  static Optional<Integer> getDownloadLength(URL url) {
    if (!downloadLengthCache.containsKey(url)) {
      Optional<Integer> length = getDownloadLengthWithoutCache(url);
      if (length.isPresent()) {
        downloadLengthCache.put(url, length.get());
      } else {
        return Optional.empty();
      }
    }
    return Optional.of(downloadLengthCache.get(url));
  }

  private static Optional<Integer> getDownloadLengthWithoutCache(final URL url) {
    try (final CloseableHttpClient client = newHttpClient()) {
      return getLengthOfResourceAt(url.toString(), client);
    } catch (final IOException e) {
      ClientLogger.logQuietly(String.format("failed to get download length for '%s'", url), e);
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
  static Optional<Integer> getLengthOfResourceAt(
      final String uri,
      final CloseableHttpClient client) throws IOException {
    try (final CloseableHttpResponse response = client.execute(newHttpGetRequest(uri))) {
      final int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != HttpStatus.SC_OK) {
        throw new IOException(String.format("unexpected status code (%d)", statusCode));
      }

      final HttpEntity entity = response.getEntity();
      if (entity == null) {
        throw new IOException("entity does not exist");
      }

      final long length = entity.getContentLength();
      if (length > Integer.MAX_VALUE) {
        throw new IOException("content length exceeds Integer.MAX_VALUE");
      }

      return (length >= 0L) ? Optional.of((int) length) : Optional.empty();
    }
  }

  private static HttpRequestBase newHttpGetRequest(final String uri) {
    final HttpGet request = new HttpGet(uri);
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
