package org.triplea.maps.indexing;

import com.google.common.annotations.VisibleForTesting;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.triplea.io.ContentDownloader;
import org.triplea.yaml.YamlReader;

/**
 * Given a map URI, attempts to fetch the 'map.yml' file from the URI and if available generates a
 * {@code MapIndexResult}.
 */
@Slf4j
class MapIndexer implements Function<URI, Optional<MapIndexResult>> {
  @Override
  public Optional<MapIndexResult> apply(final URI uri) {
    final URI mapYmlUri = URI.create(uri.toString() + "/map.yml?raw=true");
    return ContentDownloader.downloadAndExecute(
        mapYmlUri, mapYmlContentStream -> indexMapYmlContent(mapYmlUri, mapYmlContentStream));
  }

  /**
   * Reads the input stream for map index YAML information, returns null if any data is missing or
   * formatting is bad.
   */
  @VisibleForTesting
  @Nullable
  static MapIndexResult indexMapYmlContent(final URI mapYmlUri, final InputStream inputStream) {
    try {
      final Map<String, Object> mapYamlData = YamlReader.readMap(inputStream);
      return MapIndexResult.builder()
          .mapRepoUri(mapYmlUri.toString())
          .mapName((String) mapYamlData.get("map_name"))
          .mapVersion((Integer) mapYamlData.get("version"))
          .build();
    } catch (final ClassCastException
        | YamlReader.InvalidYamlFormatException
        | NullPointerException e) {
      log.error("Invalid map.yml data found at URI: {}, error: {}", mapYmlUri, e.getMessage());
      return null;
    }
  }
}
