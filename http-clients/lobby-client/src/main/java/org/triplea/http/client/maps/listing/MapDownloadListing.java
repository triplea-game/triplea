package org.triplea.http.client.maps.listing;

import java.util.List;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Builder
@AllArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
public class MapDownloadListing {
  /** URL where the map can be downloaded. */
  @Nonnull private final String downloadUrl;
  /** URL of the preview image of the map. */
  @Nonnull private final String previewImageUrl;

  @Nonnull private final String mapName;
  @Nonnull private final Long lastCommitDateEpochMilli;
  /** HTML description of the map. */
  @Nonnull private final String description;
  /** @deprecated use lastCommitDateEpochMilli and file time stamps instead. */
  @Deprecated private final Integer version;

  /** Mapping of {tag name -> tag value} */
  private final List<MapTag> mapTags;

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
}
