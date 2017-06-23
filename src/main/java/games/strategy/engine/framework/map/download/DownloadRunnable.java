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
 * Downloads a map index file, parses it and returns a <code>List</code> of <code>DownloadFileDescription</code>.
 */
public class DownloadRunnable {
  private final String urlString;

  public DownloadRunnable(final String urlString) {
    super();
    this.urlString = urlString;
  }


  /**
   * Returns a parsed list of parsed downloadable maps. If initialized with a URL
   * then we will do a network fetch and parse those contents, otherwise (for testing)
   * we assume a local file reference and parse that.
   */
  public List<DownloadFileDescription> getDownloads() {
    if (beginsWithHttpProtocol(urlString)) {
      return downloadFile();
    } else {
      return readLocalFile();
    }
  }

  private static boolean beginsWithHttpProtocol(final String urlString) {
    return urlString.startsWith("http://") || urlString.startsWith("https://");
  }

  private List<DownloadFileDescription> downloadFile() {
    try {
      final File tempFile = ClientFileSystemHelper.createTempFile();
      tempFile.deleteOnExit();
      DownloadUtils.downloadToFile(urlString, tempFile);
      final byte[] contents = Files.readAllBytes(tempFile.toPath());
      return DownloadFileParser.parse(new ByteArrayInputStream(contents));
    } catch (final IOException e) {
      ClientLogger.logError("Error - will show an empty list of downloads. Failed to get files from: " + urlString, e);
      return new ArrayList<>();
    }
  }

  private List<DownloadFileDescription> readLocalFile() {
    final File targetFile = new File(ClientFileSystemHelper.getRootFolder(), urlString);
    try {
      final byte[] contents = Files.readAllBytes(targetFile.toPath());
      final List<DownloadFileDescription> downloads = DownloadFileParser.parse(new ByteArrayInputStream(contents));
      checkNotNull(downloads);
      return downloads;
    } catch (final IOException e) {
      ClientLogger.logError("Failed to read file at: " + targetFile.getAbsolutePath(), e);
      return new ArrayList<>();
    }
  }
}
