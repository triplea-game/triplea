package org.triplea.maps.indexing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.StringContains.containsString;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.github.MapRepoListing;

/**
 * Test that does a live indexing (goes over network to github) of 'triplea-maps/test-map'. We'll
 * build an indexer and then run indexing on the test map. The test map will be in a known state and
 * we'll then verify the returned indexing results are as expected.
 */
@Disabled
public class MapIndexingIntegrationTest {

  @Test
  void runIndexingOnTestMap() {
    final MapIndexingTask mapIndexingTaskRunner =
        MapsIndexingObjectFactory.mapIndexingTask(
            MapsIndexingObjectFactory.githubApiClient(
                "triplea-maps", "https://api.github.com", null),
            (repo, repoLastCommitDate) -> false);

    final MapIndexingResult result =
        mapIndexingTaskRunner
            .apply(
                MapRepoListing.builder()
                    .name("test-map")
                    .htmlUrl("https://github.com/triplea-maps/test-map")
                    .build())
            .orElseThrow(
                () -> new AssertionError("Expected a result to be returned, check logs for cause"));

    assertThat(result.getMapRepoUri(), is("https://github.com/triplea-maps/test-map"));
    assertThat("Map name is read from map.yml", result.getMapName(), is("Test Map"));
    assertThat(
        "Last commit date is parsed from an API call",
        result.getLastCommitDate(),
        is(notNullValue()));
    assertThat(
        "Description is downloaded from description.html",
        result.getDescription(),
        containsString("<br><b><em>by test</em></b>"));

    assertThat(
        result.getDownloadUri(),
        is("https://github.com/triplea-maps/test-map/archive/refs/heads/master.zip"));

    assertThat(result.getMapDownloadSizeInBytes() > 0, is(true));
  }
}
