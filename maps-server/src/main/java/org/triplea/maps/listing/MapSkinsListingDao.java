package org.triplea.maps.listing;

import java.util.List;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

interface MapSkinsListingDao {
  @SqlQuery(
      "select"
          + "    map_id,"
          + "    version,"
          + "    skin_name,"
          + "    url,"
          + "    preview_image_url,"
          + "    description"
          + "  from map_skin"
          + "  order by skin_name;")
  List<MapSkinRecord> fetchMapSkinsListings();
}
