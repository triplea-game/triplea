package games.strategy.engine.framework.map.download;
// TODO: move to package games.strategy.engine.framework.map.download.client

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.util.function.Supplier;
import java.util.logging.Level;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;

import com.google.common.annotations.VisibleForTesting;

import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.framework.system.HttpProxy;
import lombok.AllArgsConstructor;
import lombok.extern.java.Log;

/** Provides methods to download files via HTTP. */
@Log
@AllArgsConstructor
// TODO: testing, break up DownloadUtilsTest to test this component individually from DownloadUtils.
final class ContentReader {
  private final Supplier<CloseableHttpClient> httpClientFactory;

  /**
   * Creates a temp file, downloads the contents of a target uri to that file, returns the file.
   *
   * @param uri The URI whose contents will be downloaded
   */
  DownloadUtils.FileDownloadResult downloadToFile(final String uri) {
    final File file = ClientFileSystemHelper.newTempFile();
    file.deleteOnExit();
    try {
      downloadToFile(uri, file);
      return DownloadUtils.FileDownloadResult.success(file);
    } catch (final IOException e) {
      log.log(
          Level.SEVERE,
          "Failed to download: "
              + uri
              + ", will attempt to use backup values where available. "
              + "Please check your network connection.",
          e);
      return DownloadUtils.FileDownloadResult.FAILURE;
    }
  }

  /**
   * Downloads the resource at the specified URI to the specified file.
   *
   * @param uri The resource URI; must not be {@code null}.
   * @param file The file that will receive the resource; must not be {@code null}.
   * @throws IOException If an error occurs during the download.
   */
  void downloadToFile(final String uri, final File file) throws IOException {
    checkNotNull(uri);
    checkNotNull(file);

    try (FileOutputStream os = new FileOutputStream(file);
        CloseableHttpClient client = httpClientFactory.get()) {
      downloadToFile(uri, os, client);
    }
  }

  @VisibleForTesting
  static void downloadToFile(
      final String uri, final FileOutputStream os, final CloseableHttpClient client)
      throws IOException {
    try (CloseableHttpResponse response = client.execute(newHttpGetRequest(uri))) {
      final int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != HttpStatus.SC_OK) {
        throw new IOException(String.format("unexpected status code (%d)", statusCode));
      }

      final HttpEntity entity = response.getEntity();
      if (entity == null) {
        throw new IOException("entity is missing");
      }

      os.getChannel().transferFrom(Channels.newChannel(entity.getContent()), 0L, Long.MAX_VALUE);
    }
  }

  private static HttpRequestBase newHttpGetRequest(final String uri) {
    final HttpGet request = new HttpGet(uri);
    HttpProxy.addProxy(request);
    return request;
  }
}
