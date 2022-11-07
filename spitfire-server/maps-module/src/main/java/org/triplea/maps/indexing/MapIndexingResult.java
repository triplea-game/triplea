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

  /**
   * URI to download the map:
   * "https://github.com/triplea-maps/test-map/archive/refs/heads/master.zip"
   */
  @Nonnull String downloadUri;

  /**
   * URI to download preview image, eg:
   * https://raw.githubusercontent.com/triplea-maps/napoleonic_empires/master/preview.png
   */
  @Nonnull String previewImageUri;

  /** The size of the map download in bytes. */
  @Nonnull Long mapDownloadSizeInBytes;

  /** Date of the most recent commit to master. */
  @Nonnull Instant lastCommitDate;

  /**
   * Description data parsed from description.html. If description file is missing or too long, then
   * this field will continue an error message with details on how to fix the missing description.
   */
  @Nonnull String description;
}
