package games.strategy.engine.framework.map.download;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.io.IoUtils;
import lombok.extern.java.Log;

/**
 * Downloads a map index file, parses it and returns a <code>List</code> of <code>DownloadFileDescription</code>.
 */
@Log
public class DownloadRunnable {
  private final String urlString;

  public DownloadRunnable(final String urlString) {
    this.urlString = urlString;
  }

  /**
   * Returns a parsed list of parsed downloadable maps. If initialized with a URL then we will do a network fetch and
   * parse those contents, otherwise (for testing) we assume a local file reference and parse that.
   */
  public Optional<List<DownloadFileDescription>> getDownloads() {
    return beginsWithHttpProtocol(urlString) ? downloadFile() : Optional.of(readLocalFile());
  }

  private static boolean beginsWithHttpProtocol(final String urlString) {
    return urlString.startsWith("http://") || urlString.startsWith("https://");
  }

  private Optional<List<DownloadFileDescription>> downloadFile() {
    try {
      final Path tempFile = ClientFileSystemHelper.newTempFile().toPath();
      tempFile.toFile().deleteOnExit();
      DownloadConfiguration.contentReader().downloadToFile(urlString, tempFile.toFile());
      final byte[] contents = Files.readAllBytes(tempFile);
      return Optional.of(IoUtils.readFromMemory(contents, DownloadFileParser::parse));
    } catch (final IOException e) {
      log.log(Level.SEVERE, "Error - check internet connection, unable to download list of maps.", e);
      return Optional.empty();
    }
  }

  private List<DownloadFileDescription> readLocalFile() {
    final Path targetFile = Paths.get(urlString);
    try (InputStream inputStream = Files.newInputStream(targetFile)) {
      final List<DownloadFileDescription> downloads = DownloadFileParser.parse(inputStream);
      checkNotNull(downloads);
      return downloads;
    } catch (final IOException e) {
      log.log(Level.SEVERE, "Failed to read file at: " + targetFile.toAbsolutePath(), e);
      return new ArrayList<>();
    }
  }
}
