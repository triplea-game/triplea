package org.triplea.maps.indexing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.github.GithubApiClient;
import org.triplea.http.client.github.MapRepoListing;

/**
 * Test that does a live indexing (goes over network to github) of 'triplea-maps/test-map'. We'll
 * build an indexer and then run indexing on the test map. The test map will be in a known state and
 * we'll then verify the returned indexing results are as expected.
 */
public class MapIndexingIntegrationTest {

  @Test
  void runIndexingOnTestMap() {
    final MapIndexingTask mapIndexingTaskRunner =
        MapIndexingTask.builder()
            .lastCommitDateFetcher(
                MapsIndexingObjectFactory.lastCommitDateFetcher(
                    GithubApiClient.builder().uri(URI.create("https://api.github.com")).build(),
                    "triplea-maps"))
            .build();

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
  }
}
