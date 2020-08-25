package org.triplea.maps.listing;

import java.util.List;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

interface MapListingDao {
  @SqlQuery(
      "select"
          + "    m.id,"
          + "    m.url,"
          + "    m.name,"
          + "    m.description,"
          + "    m.version,"
          + "    c.name as category_name,"
          + "    m.preview_image_url"
          + "  from map m"
          + "  join category c on c.id = m.category_id"
          + "  order by m.name")
  List<MapListingRecord> fetchMapListings();
}
