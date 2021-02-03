package org.triplea.maps.indexing;

import java.net.URI;
import java.util.Optional;
import java.util.function.Function;

/**
 * Given a map URI, attempts to fetch the 'map.yml' file from the URI
 * and if available generates a {@code MapIndexResult}.
 */
public class MapIndexer implements Function<URI, Optional<MapIndexResult>> {
  @Override
  public Optional<MapIndexResult> apply(final URI uri) {
    return null;
  }
}
