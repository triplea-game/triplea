package games.strategy.engine.framework.map.download;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
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

  private DownloadRunnable() {}

  public static List<DownloadFileDescription> download(final String url) {
    try (CloseableHttpClient client = HttpClients.custom().disableCookieManagement().build()) {
      final HttpGet request = new HttpGet(url);
      HttpProxy.addProxy(request);
      try (CloseableHttpResponse response = client.execute(request)) {
        final int status = response.getStatusLine().getStatusCode();
        if (status != HttpStatus.SC_OK) {
          log.warning("Invalid map link '" + url + "'. Server returned " + status);
          return Collections.emptyList();
        }
        return DownloadFileParser.parse(response.getEntity().getContent());
      }
    } catch (final IOException e) {
      log.log(Level.SEVERE, "Error while downloading map download info.", e);
      return Collections.emptyList();
    }
  }

  public static List<DownloadFileDescription> readLocalFile(final Path path) {
    try (InputStream inputStream = Files.newInputStream(path)) {
      return DownloadFileParser.parse(inputStream);
    } catch (final IOException e) {
      log.log(Level.SEVERE, "Failed to read file at: " + path.toAbsolutePath(), e);
      return Collections.emptyList();
    }
  }
}
