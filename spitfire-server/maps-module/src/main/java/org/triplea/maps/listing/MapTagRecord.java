package org.triplea.maps.listing;

import lombok.Builder;
import lombok.Getter;
import org.jdbi.v3.core.mapper.reflect.ColumnName;
import org.triplea.http.client.maps.listing.MapTag;

@Getter
public class MapTagRecord {
  private final String mapName;
  private final String tagName;
  private final String value;
  private final int displayOrder;

  @Builder
  public MapTagRecord(
      @ColumnName("map_name") final String mapName,
      @ColumnName("tag_name") final String tagName,
      @ColumnName("tag_value") final String value,
      @ColumnName("display_order") final int displayOrder) {
    this.mapName = mapName;
    this.tagName = tagName;
    this.value = value;
    this.displayOrder = displayOrder;
  }

  public MapTag toMapTag() {
    return MapTag.builder().name(tagName).value(value).displayOrder(displayOrder).build();
  }
}
