package org.triplea.maps.listing;

import lombok.Builder;
import lombok.Getter;
import org.jdbi.v3.core.mapper.reflect.ColumnName;
import org.triplea.http.client.maps.listing.MapSkinListing;

public class MapSkinRecord {
  @Getter private final int mapId;
  private final String skinName;
  private final String url;
  private final String description;
  private final String version;
  private final String previewImageUrl;

  @Builder
  public MapSkinRecord(
      @ColumnName("map_id") final int mapId,
      @ColumnName("skin_name") final String skinName,
      @ColumnName("url") final String url,
      @ColumnName("description") final String description,
      @ColumnName("version") final String version,
      @ColumnName("preview_image_url") final String previewImageUrl) {
    this.mapId = mapId;
    this.skinName = skinName;
    this.url = url;
    this.description = description;
    this.version = version;
    this.previewImageUrl = previewImageUrl;
  }

  MapSkinListing toMapSkinListing() {
    return MapSkinListing.builder()
        .skinName(skinName)
        .url(url)
        .description(description)
        .version(version)
        .previewImageUrl(previewImageUrl)
        .build();
  }
}
