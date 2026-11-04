package org.triplea.http.client.lobby.maps.listing;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Represents a map that can be downloaded. This data object contains information on how to render a
 * preview and description of the map, as well as the needed URLs to download the map itself.
 */
@Builder
@AllArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
public class MapDownloadItem {
  /** URL where the map can be downloaded. */
  @Nonnull private final String downloadUrl;

  /** URL of the preview image of the map. */
  @Nonnull private final String previewImageUrl;

  @Nonnull private final String mapName;
  @Nonnull private final Long lastCommitDateEpochMilli;

  /** HTML description of the map. */
  @Nonnull private final String description;

  /** Additional meta data about the map, eg: categories, rating, etc... */
  private final List<MapTag> mapTags;

  @Nonnull private final Long downloadSizeInBytes;

  /**
   * Returns the values of all tags with this name, comma separated, as a map can have several
   * values per tag (eg two genres). If the tag is not found, an empty string is returned instead.
   */
  @Nonnull
  public String getTagValue(final String tagName) {
    return getMapTags().stream()
        .filter(tag -> tag.getName().equalsIgnoreCase(tagName))
        .map(MapTag::getValue)
        .collect(Collectors.joining(", "));
  }

  public List<MapTag> getMapTags() {
    return Optional.ofNullable(mapTags).orElse(List.of());
  }
}
