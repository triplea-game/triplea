package org.triplea.maps.indexing.tasks;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.github.MapRepoListing;

class MapNameReaderTest {

  @Test
  void readMapName() {
    final var mapNameReader =
        MapNameReader.builder().downloadFunction(uri -> "map_name: The Map Name").build();

    final Optional<String> result =
        mapNameReader.apply(
            MapRepoListing.builder().name("repo name").htmlUrl("http://repo").build());

    assertThat(result, isPresentAndIs("The Map Name"));
  }

  @Test
  void readMapNameErrorCaseWithNoMapNameRead() {
    final var mapNameReader = MapNameReader.builder().downloadFunction(uri -> null).build();

    final Optional<String> result =
        mapNameReader.apply(
            MapRepoListing.builder().name("repo name").htmlUrl("http://repo").build());

    assertThat(result, isEmpty());
  }
}
