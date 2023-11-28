package org.triplea.maps.indexing.tasks;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.triplea.http.client.github.MapRepoListing;
import org.triplea.io.ContentDownloader;
import org.triplea.io.FileUtils;
import org.triplea.java.function.ThrowingFunction;

/**
 * Given a map repo, determines the map download size.
 *
 * <p>Implementation note: download size is determined by downloading the entire file at the given
 * URI to a temp file and then returning the size of that temp file. The temp file is then deleted.
 */
@Slf4j
public class DownloadSizeFetcher implements Function<MapRepoListing, Optional<Long>> {

  @Setter(value = AccessLevel.PACKAGE, onMethod_ = @VisibleForTesting)
  private ThrowingFunction<URI, InputStream, IOException> downloadFunction =
      (uri -> new ContentDownloader(uri).getStream());

  @Override
  public Optional<Long> apply(final MapRepoListing mapRepoListing) {
    final URI uri = URI.create(new DownloadUriCalculator().apply(mapRepoListing));

    log.info("Checking file size, downloading: " + uri);
    final Path tempFile = FileUtils.createTempFile().orElse(null);
    if (tempFile == null) {
      return Optional.empty();
    }

    // download the file at the given URI, the copy method will return the file sizes in bytes.
    try {
      final long fileSize =
          Files.copy(downloadFunction.apply(uri), tempFile, StandardCopyOption.REPLACE_EXISTING);
      FileUtils.delete(tempFile);
      return Optional.of(fileSize);
    } catch (final IOException e) {
      log.error("Error downloading: {}, {}", uri, e.getMessage(), e);
      FileUtils.delete(tempFile);
      return Optional.empty();
    }
  }
}
