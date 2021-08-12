package org.triplea.maps.listing;

import lombok.Builder;
import org.jdbi.v3.core.mapper.reflect.ColumnName;
import org.triplea.http.client.maps.listing.MapTag;

public class MapTagRecord {
  private final String name;
  private final String value;
  private final String type;
  private final int displayOrder;

  @Builder
  public MapTagRecord(
      @ColumnName("name") final String name,
      @ColumnName("tag_value") final String value,
      @ColumnName("type") final String type,
      @ColumnName("display_order") final int displayOrder) {
    this.name = name;
    this.value = value;
    this.type = type;
    this.displayOrder = displayOrder;
  }

  public MapTag toMapTag() {
    return MapTag.builder().name(name).value(value).type(type).displayOrder(displayOrder).build();
  }
}
