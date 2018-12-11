package games.strategy.engine.framework.map.download;
// TODO: move to package games.strategy.engine.framework.map.download.client

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import lombok.extern.java.Log;

/**
 * Provides methods to download files via HTTP.
 *
 * @deprecated Use either {@code ContentReader} or {@code DownloadLengthReader} instead.
 */
@Log
@Deprecated
// TODO: testing, break up DownloadUtilsTest to test this component individually from DownloadUtils.
public final class DownloadUtils {

  @VisibleForTesting static ContentReader contentReader;
  @VisibleForTesting static DownloadLengthReader downloadLengthReader;

  static {
    final Supplier<CloseableHttpClient> httpClientSupplier =
        () -> HttpClients.custom().disableCookieManagement().build();
    contentReader = new ContentReader(httpClientSupplier);
    downloadLengthReader = new DownloadLengthReader(httpClientSupplier);
  }

  private DownloadUtils() {}

  /**
   * Gets the download length for the resource at the specified URI.
   *
   * <p>This method is thread safe.
   *
   * @param uri The resource URI; must not be {@code null}.
   * @return The download length (in bytes) or empty if unknown; never {@code null}.
   */
  static Optional<Long> getDownloadLength(final String uri) {
    return downloadLengthReader.getDownloadLength(uri);
  }

  /**
   * Creates a temp file, downloads the contents of a target uri to that file, returns the file.
   *
   * @param uri The URI whose contents will be downloaded
   */
  public static FileDownloadResult downloadToFile(final String uri) {
    return contentReader.downloadToFile(uri);
  }

  /** The result of a file download request. */
  public static class FileDownloadResult {
    public final boolean wasSuccess;
    public final File downloadedFile;

    public static final FileDownloadResult FAILURE = new FileDownloadResult();

    public static FileDownloadResult success(final File downloadedFile) {
      return new FileDownloadResult(downloadedFile);
    }

    private FileDownloadResult() {
      wasSuccess = false;
      downloadedFile = null;
    }

    private FileDownloadResult(final File downloadedFile) {
      Preconditions.checkState(
          downloadedFile.exists(),
          "Error, file does not exist: " + downloadedFile.getAbsolutePath());
      this.downloadedFile = downloadedFile;
      this.wasSuccess = true;
    }
  }

  /**
   * Downloads the resource at the specified URI to the specified file.
   *
   * @param uri The resource URI; must not be {@code null}.
   * @param file The file that will receive the resource; must not be {@code null}.
   * @throws IOException If an error occurs during the download.
   */
  static void downloadToFile(final String uri, final File file) throws IOException {
    checkNotNull(uri);
    checkNotNull(file);
    contentReader.downloadToFile(uri, file);
  }
}
