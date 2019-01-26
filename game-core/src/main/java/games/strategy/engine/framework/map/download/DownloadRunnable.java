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

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import games.strategy.engine.framework.system.HttpProxy;
import lombok.extern.java.Log;

/**
 * Downloads a map index file, parses it and returns a <code>List</code> of <code>DownloadFileDescription</code>.
 */
@Log
public class DownloadRunnable {
  private final String locator;

  public DownloadRunnable(final String locator) {
    this.locator = locator;
  }

  /**
   * Returns a parsed list of parsed downloadable maps. If initialized with a URL then we will do a network fetch and
   * parse those contents, otherwise (for testing) we assume a local file reference and parse that.
   */
  public Optional<List<DownloadFileDescription>> getDownloads() {
    return beginsWithHttpProtocol() ? downloadFile() : Optional.of(readLocalFile());
  }

  private boolean beginsWithHttpProtocol() {
    return locator.startsWith("http://") || locator.startsWith("https://");
  }

  private Optional<List<DownloadFileDescription>> downloadFile() {
    try (CloseableHttpClient client = HttpClients.custom().disableCookieManagement().build()) {
      final HttpGet request = new HttpGet(locator);
      HttpProxy.addProxy(request);
      try (CloseableHttpResponse response = client.execute(request)) {
        final int status = response.getStatusLine().getStatusCode();
        if (status != HttpStatus.SC_OK) {
          log.log(Level.WARNING, "Invalid map link '" + locator + "'. Server returned " + status);
          return Optional.empty();
        }
        return Optional.of(DownloadFileParser.parse(response.getEntity().getContent()));
      }
    } catch (final IOException e) {
      log.log(Level.SEVERE, "Error while downloading map download info file.");
      return Optional.empty();
    }
  }

  private List<DownloadFileDescription> readLocalFile() {
    final Path targetFile = Paths.get(locator);
    try (InputStream inputStream = Files.newInputStream(targetFile)) {
      return checkNotNull(DownloadFileParser.parse(inputStream));
    } catch (final IOException e) {
      log.log(Level.SEVERE, "Failed to read file at: " + targetFile.toAbsolutePath(), e);
      return new ArrayList<>();
    }
  }
}
