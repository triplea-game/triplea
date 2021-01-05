package org.triplea.maps.listing;

import lombok.Builder;
import lombok.Getter;
import org.jdbi.v3.core.mapper.reflect.ColumnName;
import org.triplea.http.client.maps.listing.MapDownloadListing;

public class MapListingRecord {

  @Getter private final int id;
  private final String url;
  private final String name;
  private final String description;
  private final String version;
  private final String categoryName;
  private final String previewImageUrl;

  @Builder
  public MapListingRecord(
      @ColumnName("id") final int id,
      @ColumnName("url") final String url,
      @ColumnName("name") final String name,
      @ColumnName("description") final String description,
      @ColumnName("version") final String version,
      @ColumnName("category_name") final String categoryName,
      @ColumnName("preview_image_url") final String previewImageUrl) {
    this.id = id;
    this.url = url;
    this.name = name;
    this.description = description;
    this.version = version;
    this.categoryName = categoryName;
    this.previewImageUrl = previewImageUrl;
  }

  MapDownloadListing toMapDownloadListing() {
    return MapDownloadListing.builder()
        .url(url)
        .mapName(name)
        .description(description)
        .version(version)
        .mapCategory(categoryName)
        .previewImage(previewImageUrl)
        .build();
  }
}
