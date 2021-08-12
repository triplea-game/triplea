package org.triplea.http.client.maps.listing;

import java.util.Optional;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@Builder
@EqualsAndHashCode
public class MapTag {
  /** The human readable name of the tag */
  String name;

  /** The actual value of the tag */
  String value;

  /**
   * Tag type determines how the value is interpreted and rendered. The value should be an element
   * of {@code MapTagType}
   */
  String type;

  /**
   * The ordering to display this map tag in relative to other map tags. Lower values should be
   * displayed first. displayOrder is greater than zero.
   */
  int displayOrder;

  //  public MapTagType getType() {
  //    return MapTagType.valueOf(type);
  //  }

  public String getValue() {
    return Optional.ofNullable(value).orElse("");
  }
}
