package games.strategy.engine.framework.map.download;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.triplea.http.client.maps.listing.MapDownloadListing;

/**
 * This class represents the essential data for downloading a TripleA map. Where to get it, where to
 * install it, version, etc..
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
@Builder
@AllArgsConstructor
public final class DownloadFileDescription {
  @EqualsAndHashCode.Include private final String url;
  private final String description;
  private final String mapName;
  private final Integer version;
  private final String mapCategory;
  private final String img;

  public static DownloadFileDescription ofMapDownloadListing(
      final MapDownloadListing mapDownloadListing) {
    return DownloadFileDescription.builder()
        .url(mapDownloadListing.getUrl())
        .mapName(mapDownloadListing.getMapName())
        .description(mapDownloadListing.getDescription())
        // TODO: PROJECT#17 replace with latest commit date
        .version(1)
        .mapCategory(mapDownloadListing.getMapCategory())
        .build();
  }
}
