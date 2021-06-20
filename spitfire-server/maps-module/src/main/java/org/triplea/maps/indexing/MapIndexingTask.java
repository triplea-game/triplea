package org.triplea.maps.indexing;

import com.google.common.annotations.VisibleForTesting;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nonnull;
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
class MapIndexingTask implements Function<MapRepoListing, Optional<MapIndexingResult>> {
  private static final int DESCRIPTION_COLUMN_DATABASE_MAX_LENGTH = 3000;

  /* Function that uses github API to map a {repoName -> lastCommitDate} */
  @Nonnull private final Function<String, Instant> lastCommitDateFetcher;

  /* Function to download content as a string and log an info message if not found. */
  @Setter(value = AccessLevel.PACKAGE, onMethod_ = @VisibleForTesting)
  @Builder.Default
  private Function<URI, String> downloadFunction =
      uri -> ContentDownloader.downloadAsString(uri).orElse(null);

  @Override
  public Optional<MapIndexingResult> apply(final MapRepoListing mapRepoListing) {
    final String mapName = readMapNameFromYaml(mapRepoListing).orElse(null);
    if (mapName == null) {
      return Optional.empty();
    }

    final Instant lastCommitDate = lastCommitDateFetcher.apply(mapRepoListing.getName());
    if (lastCommitDate == null) {
      log.warn(
          "Could not index map: {}, unable to fetch last commit date", mapRepoListing.getUri());
      return Optional.empty();
    }

    String description = downloadDescription(mapRepoListing).orElse(null);
    if (description == null) {
      description =
          String.format(
              "No description available for: %s"
                  + "Contact the map author and request they add a 'description.html' file",
              mapRepoListing.getUri());
    } else if (description.length() > DESCRIPTION_COLUMN_DATABASE_MAX_LENGTH) {
      description =
          String.format(
              "The description for this map is too long at %s characters. Max length is %s."
                  + "Contact the map author for: %s"
                  + ", and request they reduce the length of the file 'description.html'",
              description.length(),
              DESCRIPTION_COLUMN_DATABASE_MAX_LENGTH,
              mapRepoListing.getUri());
    }

    return Optional.of(
        MapIndexingResult.builder()
            .mapName(mapName)
            .mapRepoUri(mapRepoListing.getUri().toString())
            .lastCommitDate(lastCommitDate)
            .description(description)
            .build());
  }

  private Optional<String> downloadDescription(final MapRepoListing mapRepoListing) {
    final String descriptionUri =
        mapRepoListing.getUri().toString() + "/blob/master/description.html?raw=true";
    return ContentDownloader.downloadAsString(URI.create(descriptionUri));
  }

  /**
   * Determines the expected location of a map.yml file, downloads it, reads and returns the
   * 'map_name' attribute. Returns null if the file could not be found or otherwise could not be
   * read.
   */
  private Optional<String> readMapNameFromYaml(final MapRepoListing mapRepoListing) {
    final URI mapYmlUri =
        URI.create(mapRepoListing.getUri().toString() + "/blob/master/map.yml?raw=true");

    final String mapYamlContents = downloadFunction.apply(mapYmlUri);
    if (mapYamlContents == null) {
      log.warn("Could not index, missing map.yml. Expected URI: {}", mapYmlUri);
      return Optional.empty();
    }

    // parse and return the 'map_name' attribute from the YML file we just downloaded
    try {
      final Map<String, Object> mapYamlData = YamlReader.readMap(mapYamlContents);
      return Optional.of((String) mapYamlData.get("map_name"));
    } catch (final ClassCastException
        | YamlReader.InvalidYamlFormatException
        | NullPointerException e) {
      log.error("Invalid map.yml data found at URI: {}, error: {}", mapYmlUri, e.getMessage());
      return Optional.empty();
    }
  }
}
