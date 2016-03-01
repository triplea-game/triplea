package games.strategy.engine.framework.map.download;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import com.google.common.collect.Maps;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.ClientFileSystemHelper;

/** Used to download triplea_maps.xml */
public class DownloadRunnable implements Runnable {
  private static Map<URL, File> downloadCache = Maps.newHashMap();
  private final String urlString;
  private final boolean parse;
  private volatile byte[] contents;
  private volatile String error;
  private volatile List<DownloadFileDescription> downloads;

  public DownloadRunnable(final String urlString) {
    super();
    this.urlString = urlString;
    parse = true;
  }

  public byte[] getContents() {
    return contents;
  }

  public List<DownloadFileDescription> getDownloads() {
    return downloads;
  }

  public String getError() {
    return error;
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

      if (!downloadCache.containsKey(url)) {
        File tempFile = ClientFileSystemHelper.createTempFile();
        FileUtils.copyURLToFile(url, tempFile);
        downloadCache.put(url, tempFile);
      }
      File f = downloadCache.get(url);
      contents = Files.readAllBytes(f.toPath());
    } catch (final Exception e) {
      error = e.getMessage();
      return;
    }

    if (parse) {
      try {
        downloads = DownloadFileParser.parse(new ByteArrayInputStream(getContents()));
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
      downloads = DownloadFileParser.parse(new ByteArrayInputStream(getContents()));
      checkNotNull(downloads);
    } catch (IOException e) {
      ClientLogger.logError("Failed to read file at: " + targetFile.getAbsolutePath(), e);
    }
  }

}
