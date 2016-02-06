package games.strategy.engine.framework.mapDownload;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;

import com.google.common.io.Resources;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.ClientFileSystemHelper;

public class DownloadRunnable implements Runnable {
  private final String urlString;
  private final boolean parse;
  private volatile byte[] contents;
  private volatile String error;
  private volatile List<DownloadFileDescription> downloads;

  public DownloadRunnable(final String urlString) {
    super();
    this.urlString = urlString;
    parse = false;
  }

  public DownloadRunnable(final String urlString, final boolean parseToo) {
    super();
    this.urlString = urlString;
    parse = parseToo;
  }

  public byte[] getContents() {
    return contents;
  }

  public void setContents(final byte[] contents) {
    this.contents = contents;
  }

  public List<DownloadFileDescription> getDownloads() {
    return downloads;
  }

  public void setDownloads(final List<DownloadFileDescription> downloads) {
    this.downloads = downloads;
  }

  public String getError() {
    return error;
  }

  public void setError(final String error) {
    this.error = error;
  }

  @Override
  public void run() {
    if (beginsWithHttpProtocol(urlString)) {
      downloadFile();
    } else {
      readLocalFile();
    }
  }

  private static boolean beginsWithHttpProtocol(String urlString) {
    return urlString.startsWith("http://") || urlString.startsWith("https://");
  }

  private void downloadFile() {
    try {
      final URL url = getUrlFollowingRedirects(urlString);
      contents = Resources.asByteSource(url).read();
    } catch (final Exception e) {
      error = e.getMessage();
      return;
    }

    if (parse) {
      try {
        downloads = DownloadFileParser.parse(new ByteArrayInputStream(getContents()), urlString);
        if (downloads == null || downloads.isEmpty()) {
          error = "No games listed.";
        }
      } catch (final Exception e) {
        error = e.getMessage();
      }
    }
  }

  private static URL getUrlFollowingRedirects(String possibleRedirectionUrl) throws Exception {
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


  private void readLocalFile() {
    File targetFile = new File(ClientFileSystemHelper.getRootFolder(), urlString);
    try {
      contents = Files.readAllBytes(targetFile.toPath());
      downloads = DownloadFileParser.parse(new ByteArrayInputStream(getContents()), urlString);
    } catch (IOException e) {
      ClientLogger.logError("Failed to read file at: " + targetFile.getAbsolutePath(), e);
    }
  }

}
