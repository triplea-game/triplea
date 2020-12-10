package org.triplea.http.client.maps.listing;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MapSkinListing {
  private final String url;
  private final String skinName;
  private final String description;
  private final String version;
  private final String previewImageUrl;
}
