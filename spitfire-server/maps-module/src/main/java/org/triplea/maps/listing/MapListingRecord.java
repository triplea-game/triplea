package org.triplea.maps.listing;

import java.time.Instant;
import lombok.Builder;
import org.jdbi.v3.core.mapper.reflect.ColumnName;
import org.triplea.http.client.maps.listing.MapDownloadListing;

public class MapListingRecord {
  private final String name;
  private final String url;
  private final String description;
  private final Instant lastCommitDate;
  private final String categoryName;

  @Builder
  public MapListingRecord(
      @ColumnName("map_name") final String name,
      @ColumnName("repo_url") final String url,
      @ColumnName("description") final String description,
      @ColumnName("last_commit_date") final Instant lastCommitDate,
      @ColumnName("category_name") final String categoryName) {
    this.url = url;
    this.name = name;
    this.lastCommitDate = lastCommitDate;
    this.categoryName = categoryName;
    this.description = description;
  }

  MapDownloadListing toMapDownloadListing() {
    return MapDownloadListing.builder()
        .url(url)
        .mapName(name)
        .lastCommitDateEpochMilli(lastCommitDate.toEpochMilli())
        .mapCategory(categoryName)
        .description(description)
        .build();
  }
}
