package org.triplea.maps.indexing;

import com.google.common.annotations.VisibleForTesting;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.triplea.http.client.github.MapRepoListing;
import org.triplea.io.ContentDownloader;
import org.triplea.yaml.YamlReader;

/**
 * Given a map repo name and URI, reads pertinent indexing information.
 *
 * <ul>
 *   <li>mapName: read from map.yml found in the repository
 *   <li>lastCommitDate: github API is queried for the repo's master branch last commit date.
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
class MapIndexer implements Function<MapRepoListing, Optional<MapIndexResult>> {
  /* Function that uses github API to map a {repoName -> lastCommitDate} */
  @Nonnull private final Function<String, Instant> lastCommitDateFetcher;

  /* Function to download content as a string and log an info message if not found. */
  @Setter(value = AccessLevel.PACKAGE, onMethod_ = @VisibleForTesting)
  @Builder.Default
  private Function<URI, String> downloadFunction =
      uri -> ContentDownloader.downloadAsString(uri).orElse(null);

  @Override
  public Optional<MapIndexResult> apply(final MapRepoListing mapRepoListing) {
    final String mapName = readMapNameFromYaml(mapRepoListing);
    if (mapName == null) {
      return Optional.empty();
    }

    final Instant lastCommitDate = lastCommitDateFetcher.apply(mapRepoListing.getName());
    if (lastCommitDate == null) {
      return Optional.empty();
    }

    return Optional.of(
        MapIndexResult.builder()
            .mapName(mapName)
            .mapRepoUri(mapRepoListing.getUri().toString())
            .lastCommitDate(lastCommitDate)
            .build());
  }

  /**
   * Determines the expected location of a map.yml file, downloads it, reads and returns the
   * 'map_name' attribute. Returns null if the file could not be found or otherwise could not be
   * read.
   */
  @VisibleForTesting
  @Nullable
  String readMapNameFromYaml(final MapRepoListing mapRepoListing) {
    final URI mapYmlUri =
        URI.create(mapRepoListing.getUri().toString() + "/blob/master/map.yml?raw=true");

    final String mapYamlContents = downloadFunction.apply(mapYmlUri);
    if (mapYamlContents == null) {
      return null;
    }

    // parse and return the 'map_name' attribute from the YML file we just downloaded
    try {
      final Map<String, Object> mapYamlData = YamlReader.readMap(mapYamlContents);
      return (String) mapYamlData.get("map_name");
    } catch (final ClassCastException
        | YamlReader.InvalidYamlFormatException
        | NullPointerException e) {
      log.error("Invalid map.yml data found at URI: {}, error: {}", mapYmlUri, e.getMessage());
      return null;
    }
  }
}
