package org.triplea.maps.tags;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

@Getter
@ToString
@EqualsAndHashCode
public class MapTagMetaDataRecord {
  private final String name;
  private final int displayOrder;
  private final String allowedValue;

  @Builder
  public MapTagMetaDataRecord(
      @ColumnName("name") final String name,
      @ColumnName("display_order") final int displayOrder,
      @ColumnName("allowed_value") final String allowedValue) {
    this.name = name;
    this.displayOrder = displayOrder;
    this.allowedValue = allowedValue;
  }
}
