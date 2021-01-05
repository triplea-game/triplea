package org.triplea.maps.server.db;

import java.util.Collection;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.jdbi.v3.core.mapper.RowMapperFactory;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.triplea.maps.listing.MapListingRecord;

@UtilityClass
public class RowMappers {

  public Collection<RowMapperFactory> rowMappers() {
    return List.of(ConstructorMapper.factory(MapListingRecord.class));
  }
}
