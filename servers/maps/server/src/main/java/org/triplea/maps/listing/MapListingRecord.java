package org.triplea.maps.listing;

import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import org.jdbi.v3.core.mapper.reflect.ColumnName;
import org.triplea.http.client.maps.listing.MapDownloadItem;
import org.triplea.http.client.maps.listing.MapTag;

@Getter
public class MapListingRecord {
  private final String name;
  private final String downloadUrl;
  private final String previewImageUrl;
  private final String description;
  private final Instant lastCommitDate;
  private final Long downloadSizeBytes;

  @Builder
  public MapListingRecord(
      @ColumnName("map_name") final String name,
      @ColumnName("download_url") final String downloadUrl,
      @ColumnName("preview_image_url") final String previewImageUrl,
      @ColumnName("description") final String description,
      @ColumnName("last_commit_date") final Instant lastCommitDate,
      @ColumnName("download_size_bytes") final Long downloadSizeBytes) {
    this.name = name;
    this.downloadUrl = downloadUrl;
    this.previewImageUrl = previewImageUrl;
    this.description = description;
    this.lastCommitDate = lastCommitDate;
    this.downloadSizeBytes = downloadSizeBytes;
  }

  public MapDownloadItem toMapDownloadItem(final List<MapTag> mapTags) {
    return MapDownloadItem.builder()
        .downloadUrl(downloadUrl)
        .downloadSizeInBytes(downloadSizeBytes)
        .previewImageUrl(previewImageUrl)
        .mapName(name)
        .lastCommitDateEpochMilli(lastCommitDate.toEpochMilli())
        .description(description)
        .mapTags(mapTags)
        .build();
  }
}
