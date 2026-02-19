package org.triplea.http.client.maps.listing;

import java.util.List;
import java.util.Optional;
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
   * Finds a tag by name and returns its corresponding value. If the tag is not found or has a null
   * value, an empty string is returned instead.
   */
  @Nonnull
  public String getTagValue(final String tagName) {
    return mapTags.stream()
        .filter(tag -> tag.getName().equalsIgnoreCase(tagName))
        .findAny()
        .map(MapTag::getValue)
        .orElse("");
  }

  public List<MapTag> getMapTags() {
    return Optional.ofNullable(mapTags).orElse(List.of());
  }
}
