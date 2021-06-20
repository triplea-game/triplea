package org.triplea.maps.indexing.tasks;

import com.google.common.annotations.VisibleForTesting;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.triplea.http.client.github.MapRepoListing;
import org.triplea.io.ContentDownloader;
import org.triplea.yaml.YamlReader;

@Slf4j
@Builder
public class MapNameReader implements Function<MapRepoListing, Optional<String>> {
  /* Function to download content as a string and log an info message if not found. */
  @Setter(value = AccessLevel.PACKAGE, onMethod_ = @VisibleForTesting)
  @Builder.Default
  private Function<URI, String> downloadFunction =
      uri -> ContentDownloader.downloadAsString(uri).orElse(null);

  /**
   * Determines the expected location of a map.yml file, downloads it, reads and returns the
   * 'map_name' attribute. Returns null if the file could not be found or otherwise could not be
   * read.
   */
  @Override
  public Optional<String> apply(final MapRepoListing mapRepoListing) {
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
