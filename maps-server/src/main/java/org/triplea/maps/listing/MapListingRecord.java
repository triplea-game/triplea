package org.triplea.maps.listing;

import lombok.Builder;
import org.jdbi.v3.core.mapper.reflect.ColumnName;
import org.triplea.http.client.maps.listing.MapDownloadListing;

public class MapListingRecord {
  private final String url;
  private final String name;
  private final String version;
  private final String categoryName;

  @Builder
  public MapListingRecord(
      @ColumnName("repo_url") final String url,
      @ColumnName("map_name") final String name,
      @ColumnName("version") final String version,
      @ColumnName("category_name") final String categoryName) {
    this.url = url;
    this.name = name;
    this.version = version;
    this.categoryName = categoryName;
  }

  MapDownloadListing toMapDownloadListing() {
    return MapDownloadListing.builder()
        .url(url)
        .mapName(name)
        .version(version)
        .mapCategory(categoryName)
        .build();
  }
}
