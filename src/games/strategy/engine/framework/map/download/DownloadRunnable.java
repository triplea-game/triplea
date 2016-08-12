package games.strategy.engine.framework.map.download;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.ClientFileSystemHelper;

/** Used to download triplea_maps.xml */
// TODO: make this class immutable, drop the volatile modifiers
public class DownloadRunnable implements Runnable {
  private static Map<String,File> downloadCache = Maps.newHashMap();
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
   if(downloads == null) {
      return new ArrayList<>();
    } else {
      return downloads;
    }
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

  public static boolean beginsWithHttpProtocol(String urlString) {
    return urlString.startsWith("http://") || urlString.startsWith("https://");
  }

  private void downloadFile() {
    try {
      if( !downloadCache.containsKey(urlString)) {
        File tempFile = ClientFileSystemHelper.createTempFile();
        DownloadUtils.downloadFile(urlString, tempFile);
        downloadCache.put(urlString, tempFile);
      }
      File f = downloadCache.get(urlString);
      contents = Files.readAllBytes(f.toPath());
    } catch (IOException e) {
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
