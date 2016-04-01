package games.strategy.engine.framework.map.download;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.io.FileUtils;

import com.google.common.base.Throwables;

public class DownloadUtils {
  public static int getDownloadLength(final URL url) {
    try {
      final HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
      final int responseCode = httpConn.getResponseCode();
      // always check HTTP response code first
      if (responseCode == HttpURLConnection.HTTP_OK) {
        return httpConn.getContentLength();
      } else {
        return -1;
      }
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public static URL toURL(final String url) {
    try {
      return new URL(url);
    } catch (final MalformedURLException e) {
      throw new IllegalStateException("Invalid URL: " + url, e);
    }
  }

  public static boolean beginsWithHttpProtocol(final String urlString) {
    return urlString.startsWith("http://") || urlString.startsWith("https://");
  }

  public static void downloadFile(final String urlString, final File targetFile) throws IOException {
    try {
      final URL url = getUrlFollowingRedirects(urlString);

      FileUtils.copyURLToFile(url, targetFile);
    } catch (final MalformedURLException e) {
      throw Throwables.propagate(e);
    }

  }

  private static URL getUrlFollowingRedirects(final String possibleRedirectionUrl) throws IOException {
    URL url = new URL(possibleRedirectionUrl);
    final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    final int status = conn.getResponseCode();
    if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM
        || status == HttpURLConnection.HTTP_SEE_OTHER) {
      // update the URL if we were redirected
      url = new URL(conn.getHeaderField("Location"));
    }
    return url;
  }

}
