package org.triplea.maps.indexing;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.triplea.io.CloseableDownloader;
import org.triplea.io.ContentDownloader;
import org.triplea.yaml.YamlReader;

/**
 * Given a map URI, attempts to fetch the 'map.yml' file from the URI and if available generates a
 * {@code MapIndexResult}.
 */

// TODO: test-me
@Slf4j
class MapIndexer implements Function<URI, Optional<MapIndexResult>> {
  @Override
  public Optional<MapIndexResult> apply(final URI uri) {
    final URI mapYmlUri = URI.create(uri.toString() + "/map.yml?raw=true");

    try (CloseableDownloader downloader = new ContentDownloader(mapYmlUri)) {
      final InputStream stream = downloader.getStream();

      final Map<String, Object> mapYamlData = YamlReader.readMap(stream);

      return Optional.of(
          MapIndexResult.builder()
              .mapRepoUri(uri.toString())
              .mapName((String) mapYamlData.get("map_name"))
              .mapVersion((Integer) mapYamlData.get("version"))
              .build());

    } catch (final IOException e) {
      log.warn("Failed to read map.yml file at: " + mapYmlUri + ", " + e.getMessage());
      return Optional.empty();
    }
  }
}
