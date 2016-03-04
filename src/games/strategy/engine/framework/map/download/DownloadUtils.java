package games.strategy.engine.framework.map.download;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class DownloadUtils {
  public static int getDownloadLength(URL url) {
    try {
      HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
      int responseCode = httpConn.getResponseCode();
      // always check HTTP response code first
      if (responseCode == HttpURLConnection.HTTP_OK) {
        return httpConn.getContentLength();
      } else {
        return -1;
      }
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public static URL toURL(String url) {
    try {
      return new URL(url);
    } catch (MalformedURLException e) {
      throw new IllegalStateException("Invalid URL: " + url, e);
    }
  }

  public static boolean beginsWithHttpProtocol(String urlString) {
    return urlString.startsWith("http://") || urlString.startsWith("https://");
  }
}
