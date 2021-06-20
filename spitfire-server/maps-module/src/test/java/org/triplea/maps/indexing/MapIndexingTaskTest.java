package org.triplea.maps.indexing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.github.MapRepoListing;

class MapIndexingTaskTest {

  private static final Instant instant = Instant.now();

  @Test
  @DisplayName("On successful indexing, data is aggregated correctly")
  void verifyMapIndexingHappyCase() {
    final var mapIndexingTask =
        MapIndexingTask.builder()
            .lastCommitDateFetcher(repoListing -> Optional.of(instant))
            .skipMapIndexingCheck((mapRepoListing, instant1) -> false)
            .mapNameReader(mapRepoListing -> Optional.of("map name"))
            .mapDescriptionReader(mapRepoListing -> "description")
            .build();

    final var mapIndexingResult =
        mapIndexingTask
            .apply(MapRepoListing.builder().htmlUrl("http://url").name("repo name").build())
            .orElseThrow(() -> new IllegalStateException("Unexpected empty result, check logs.."));

    assertThat(mapIndexingResult.getMapName(), is("map name"));
    assertThat(mapIndexingResult.getLastCommitDate(), is(instant));
    assertThat(mapIndexingResult.getDescription(), is("description"));
    assertThat(mapIndexingResult.getMapRepoUri(), is("http://url"));
  }

  @Test
  @DisplayName("No result if there is a failure getting last commit date from repo")
  void verifyNoResultIfLastCommitDateCannotBeObtained() {
    final var mapIndexingTask =
        MapIndexingTask.builder()
            .lastCommitDateFetcher(repoListing -> Optional.empty())
            .skipMapIndexingCheck((mapRepoListing, instant1) -> false)
            .mapNameReader(mapRepoListing -> Optional.of("map name"))
            .mapDescriptionReader(mapRepoListing -> "description")
            .build();

    final var mapIndexingResult =
        mapIndexingTask
            .apply(MapRepoListing.builder().htmlUrl("http://url").name("repo name").build())
            .orElse(null);

    assertThat(
        "No value indicates we skipped indexing, because last commit date fetcher"
            + "return an empty we expect indexing to have been skipped.",
        mapIndexingResult,
        is(nullValue()));
  }

  @Test
  @DisplayName("No result if there skip check returns true")
  void verifyNoResultIfIndexingIsSkipped() {
    final var mapIndexingTask =
        MapIndexingTask.builder()
            .lastCommitDateFetcher(repoListing -> Optional.of(instant))
            .skipMapIndexingCheck((mapRepoListing, instant1) -> true)
            .mapNameReader(mapRepoListing -> Optional.of("map name"))
            .mapDescriptionReader(mapRepoListing -> "description")
            .build();

    final var mapIndexingResult =
        mapIndexingTask
            .apply(MapRepoListing.builder().htmlUrl("http://url").name("repo name").build())
            .orElse(null);

    assertThat(
        "No value indicates we skipped indexing, because skip check returned true"
            + " we expect indexing to have been skipped.",
        mapIndexingResult,
        is(nullValue()));
  }
}
