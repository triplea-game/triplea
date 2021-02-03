package org.triplea.maps.indexing;

import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Value;

/**
 * Data object representing a map index entry. URI is the location of the map repo, name and version
 * are read from the 'map.yml' file located in the map repo.
 */
@Builder
@Value
class MapIndexResult {
  @Nonnull String mapName;
  @Nonnull String mapRepoUri;
  @Nonnull Integer mapVersion;
}
