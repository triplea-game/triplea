package org.triplea.maps.indexing;

import java.time.Instant;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Value;

/**
 * Data object representing a map index entry. URI is the location of the map repo, name and version
 * are read from the 'map.yml' file located in the map repo.
 */
@Builder
@Value
public class MapIndexingResult {
  /** Name of the map as found in the 'map.yml' file located in the map repo. */
  @Nonnull String mapName;

  /** URI to the repo, typically something like: "https://github.com/triplea-maps/test-map/" */
  @Nonnull String mapRepoUri;

  /** Date of the most recent commit to master. */
  @Nonnull Instant lastCommitDate;

  /**
   * Data that is parsed from description.html. If the file is missing or if contents are two long
   * then a message should be inserted stated that the map author needs to fix the description.html
   * file.
   */
  String description;
}
