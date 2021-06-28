package games.strategy.engine.framework.map.download;

import java.util.Arrays;
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
  private final MapCategory mapCategory;
  private final String img;

  enum MapCategory {
    BEST("High Quality"),

    GOOD("Good Quality"),

    DEVELOPMENT("In Development"),

    EXPERIMENTAL("Experimental");

    final String outputLabel;

    MapCategory(final String label) {
      outputLabel = label;
    }

    @Override
    public String toString() {
      return outputLabel;
    }

    private static MapCategory fromString(final String category) {
      return Arrays.stream(values())
          .filter(mapCategory -> mapCategory.outputLabel.equalsIgnoreCase(category))
          .findAny()
          .orElse(EXPERIMENTAL);
    }
  }

  public static DownloadFileDescription ofMapDownloadListing(
      final MapDownloadListing mapDownloadListing) {
    return DownloadFileDescription.builder()
        .url(mapDownloadListing.getUrl())
        .mapName(mapDownloadListing.getMapName())
        .description(mapDownloadListing.getDescription())
        // TODO: PROJECT#17 replace with latest commit date
        .version(1)
        .mapCategory(MapCategory.fromString(mapDownloadListing.getMapCategory()))
        .build();
  }
}
