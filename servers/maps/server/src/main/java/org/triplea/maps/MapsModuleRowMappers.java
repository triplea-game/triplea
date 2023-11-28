package org.triplea.maps;

import java.util.List;
import lombok.experimental.UtilityClass;
import org.jdbi.v3.core.mapper.RowMapperFactory;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.triplea.maps.listing.MapListingRecord;
import org.triplea.maps.listing.MapTagRecord;
import org.triplea.maps.tags.MapTagMetaDataRecord;

/** Utility to get connections to the Postgres lobby database. */
@UtilityClass
public final class MapsModuleRowMappers {
  /**
   * Returns all row mappers. These are classes that map result set values to corresponding return
   * objects.
   */
  public static List<RowMapperFactory> rowMappers() {
    return List.of(
        ConstructorMapper.factory(MapListingRecord.class),
        ConstructorMapper.factory(MapTagRecord.class),
        ConstructorMapper.factory(MapTagMetaDataRecord.class));
  }
}
