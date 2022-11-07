package org.triplea.maps.indexing;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.github.MapRepoListing;

class MapIndexingTaskTest {

  private static final Instant instant = Instant.now();

  private static final MapRepoListing mapRepoListing =
      MapRepoListing.builder().htmlUrl("http://url").name("repo name").build();

  @Test
  @DisplayName("On successful indexing, data is aggregated correctly")
  void verifyMapIndexingHappyCase() {
    final var mapIndexingTask =
        MapIndexingTask.builder()
            .lastCommitDateFetcher(repoListing -> Optional.of(instant))
            .skipMapIndexingCheck((mapRepoListing, instant1) -> false)
            .mapNameReader(mapRepoListing -> Optional.of("map name"))
            .mapDescriptionReader(mapRepoListing -> "description")
            .downloadSizeFetcher(mapRepoListing -> Optional.of(10L))
            .build();

    final var mapIndexingResult =
        mapIndexingTask
            .apply(mapRepoListing)
            .orElseThrow(() -> new IllegalStateException("Unexpected empty result, check logs.."));

    assertThat(mapIndexingResult.getMapName(), is("map name"));
    assertThat(mapIndexingResult.getLastCommitDate(), is(instant));
    assertThat(mapIndexingResult.getDescription(), is("description"));
    assertThat(mapIndexingResult.getMapRepoUri(), is(mapRepoListing.getUri().toString()));
    assertThat(
        mapIndexingResult.getDownloadUri(),
        is(mapRepoListing.getUri().toString() + "/archive/refs/heads/master.zip"));
    assertThat(mapIndexingResult.getMapDownloadSizeInBytes(), is(10L));
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
            .downloadSizeFetcher(mapRepoListing -> Optional.of(10L))
            .build();

    final var mapIndexingResult = mapIndexingTask.apply(mapRepoListing);

    assertThat(
        "No value indicates we skipped indexing, because last commit date fetcher"
            + "return an empty we expect indexing to have been skipped.",
        mapIndexingResult,
        isEmpty());
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
            .downloadSizeFetcher(mapRepoListing -> Optional.of(10L))
            .build();

    final var mapIndexingResult = mapIndexingTask.apply(mapRepoListing);

    assertThat(
        "No value indicates we skipped indexing, because skip check returned true"
            + " we expect indexing to have been skipped.",
        mapIndexingResult,
        isEmpty());
  }

  @Test
  @DisplayName("No result if we fail to download")
  void verifyNoResultIfDownloadFails() {
    final var mapIndexingTask =
        MapIndexingTask.builder()
            .lastCommitDateFetcher(repoListing -> Optional.of(instant))
            .skipMapIndexingCheck((mapRepoListing, instant1) -> false)
            .mapNameReader(mapRepoListing -> Optional.of("map name"))
            .mapDescriptionReader(mapRepoListing -> "description")
            .downloadSizeFetcher(mapRepoListing -> Optional.empty())
            .build();

    final var mapIndexingResult = mapIndexingTask.apply(mapRepoListing);

    assertThat(
        "Download size fetcher returned empty, failure to download, indexing skipped",
        mapIndexingResult,
        isEmpty());
  }
}
