package org.triplea.maps.indexing.tasks;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.junit.jupiter.api.Test;
import org.triplea.http.client.github.MapRepoListing;

class PreviewUriCalculatorTest {
  @Test
  void verifyDownloadUriCalculation() {
    final var mapRepoListing =
        MapRepoListing.builder()
            .htmlUrl("https://github.com/triplea-maps/test-map")
            .name("repo name")
            .build();

    final String result = new PreviewUriCalculator().apply(mapRepoListing);

    assertThat(
        result, is("https://github.com/triplea-maps/test-map/blob/master/preview.png?raw=true"));
  }
}
