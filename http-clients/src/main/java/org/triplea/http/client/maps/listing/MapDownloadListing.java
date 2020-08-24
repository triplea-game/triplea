package org.triplea.http.client.maps.listing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@AllArgsConstructor
@Getter
public class MapDownloadListing {
  private final String url;
  private final String description;
  private final String mapName;
  private final String version;
  private final String downloadType;
  private final String mapCategory;
  private final String img;
}
