package org.triplea.http.client.maps.listing;

import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@AllArgsConstructor
@Getter
public class MapDownloadListing {
  /** URL where the map can be downloaded. */
  @Nonnull private final String downloadUrl;
  /** URL of the preview image of the map. */
  private final String previewImageUrl;

  @Nonnull private final String mapName;
  private final Long lastCommitDateEpochMilli;
  @Nonnull private final String mapCategory;
  /** HTML description of the map. */
  @Nonnull private final String description;
  /** @deprecated use lastCommitDateEpochMilli and file time stamps instead. */
  @Deprecated private final Integer version;
}
