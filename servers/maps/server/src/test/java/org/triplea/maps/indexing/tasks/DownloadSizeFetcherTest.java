package org.triplea.maps.indexing.tasks;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.github.MapRepoListing;
import org.triplea.test.common.StringToInputStream;

class DownloadSizeFetcherTest {

  private static final MapRepoListing mapRepoListing =
      MapRepoListing.builder().name("repo name").htmlUrl("htttps://fake-uri").build();

  /**
   * In this test we stub the downloaded content to be a fixed size string. We then verify the
   * reported download size matches teh byte length of the string.
   */
  @Test
  @DisplayName("Happy case, return content size downloaded")
  void returnContentSizeDownloaded() {
    final DownloadSizeFetcher downloadSizeFetcher = new DownloadSizeFetcher();
    final String contentsString = "this is a test";
    downloadSizeFetcher.setDownloadFunction(
        uri -> StringToInputStream.asInputStream(contentsString));

    final Optional<Long> result = downloadSizeFetcher.apply(mapRepoListing);

    assertThat(
        "The 'contentString' was 'fake' downloaded, reported download size should match the "
            + "byte length of that string.",
        result,
        isPresentAndIs((long) contentsString.getBytes(StandardCharsets.UTF_8).length));
  }

  @Test
  @DisplayName("Error case, error during download returns empty optional")
  void returnEmptyOnErrorDownloading() {
    final DownloadSizeFetcher downloadSizeFetcher = new DownloadSizeFetcher();
    downloadSizeFetcher.setDownloadFunction(
        uri -> {
          throw new IOException("test");
        });

    final Optional<Long> result = downloadSizeFetcher.apply(mapRepoListing);

    assertThat(result, isEmpty());
  }
}
