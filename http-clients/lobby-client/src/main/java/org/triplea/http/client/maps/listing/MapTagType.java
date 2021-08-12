package org.triplea.http.client.maps.listing;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MapTagType {
  public static final MapTagType STRING = new MapTagType("STRING");
  public static final MapTagType STAR = new MapTagType("STAR");

  private final String name;

  @Override
  public String toString() {
    return name;
  }

  public static MapTagType valueOf(final String mapTagName) {
    if (STAR.name.equalsIgnoreCase(mapTagName)) {
      return STAR;
    } else {
      // default type is STRING. Default is in place to allow older clients to handle newer types.
      return STRING;
    }
  }
}
