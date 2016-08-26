package games.strategy.engine.framework.map.download;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.ClientFileSystemHelper;

/**
 * Downlaods a map index file, parses it and returns a <code>List</code> of <code>DownloadFileDescription</code>
 */
class DownloadRunnable {
  private final String urlString;

  DownloadRunnable(final String urlString) {
    super();
    this.urlString = urlString;
  }


  List<DownloadFileDescription> getDownloads() {
    if (beginsWithHttpProtocol(urlString)) {
      return downloadFile();
    } else {
      return readLocalFile();
    }
  }

  private static boolean beginsWithHttpProtocol(String urlString) {
    return urlString.startsWith("http://") || urlString.startsWith("https://");
  }

  private List<DownloadFileDescription> downloadFile() {
    try {
      File tempFile = ClientFileSystemHelper.createTempFile();
      DownloadUtils.downloadFile(urlString, tempFile);
      byte[] contents = Files.readAllBytes(tempFile.toPath());
      return DownloadFileParser.parse(new ByteArrayInputStream(contents));
    } catch (IOException e) {
      ClientLogger.logError("Error - will show an empty list of downloads. Failed to get files from: " + urlString, e);
      return new ArrayList<>();
    }
  }

  private List<DownloadFileDescription> readLocalFile() {
    File targetFile = new File(ClientFileSystemHelper.getRootFolder(), urlString);
    try {
      byte[] contents = Files.readAllBytes(targetFile.toPath());
      List<DownloadFileDescription> downloads = DownloadFileParser.parse(new ByteArrayInputStream(contents));
      checkNotNull(downloads);
      return downloads;
    } catch (IOException e) {
      ClientLogger.logError("Failed to read file at: " + targetFile.getAbsolutePath(), e);
      return new ArrayList<>();
    }
  }
}
