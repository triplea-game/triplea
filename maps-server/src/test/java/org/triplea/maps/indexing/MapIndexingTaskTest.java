package org.triplea.maps.indexing;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.github.GithubApiClient;

@ExtendWith(MockitoExtension.class)
class MapIndexingTaskTest {

  static final MapIndexResult MAP_INDEX_RESULT =
      MapIndexResult.builder().mapVersion(7).mapName("map-name").mapRepoUri("http://repo").build();

  @Mock MapIndexDao mapIndexDao;
  @Mock GithubApiClient githubApiClient;
  @Mock Function<URI, Optional<MapIndexResult>> mapIndexer;

  MapIndexingTask mapIndexingTask;

  @BeforeEach
  void setup() {
    mapIndexingTask =
        MapIndexingTask.builder()
            .githubOrgName("ORG_NAME")
            .mapIndexDao(mapIndexDao)
            .githubApiClient(githubApiClient)
            .mapIndexer(mapIndexer)
            .build();
  }

  @Test
  @DisplayName(
      "Verify we fetch repos, run indexer, and then process results on non-empty indexed repos")
  void runMapIndexing() {
    when(githubApiClient.listRepositories("ORG_NAME"))
        .thenReturn(List.of(URI.create("https://uri-1"), URI.create("https://uri-2")));
    when(mapIndexer.apply(URI.create("https://uri-1"))).thenReturn(Optional.of(MAP_INDEX_RESULT));
    when(mapIndexer.apply(URI.create("https://uri-2"))).thenReturn(Optional.empty());

    mapIndexingTask.run();

    verify(mapIndexDao).removeMapsNotIn(List.of("https://uri-1", "https://uri-2"));
    verify(mapIndexDao).upsert(MAP_INDEX_RESULT);
  }
}
