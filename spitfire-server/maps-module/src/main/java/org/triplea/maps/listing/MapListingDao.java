package org.triplea.maps.listing;

import java.util.List;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

public interface MapListingDao {
  @SqlQuery(
      "select"
          + "    m.map_name,"
          + "    m.repo_url,"
          + "    m.description,"
          + "    m.last_commit_date,"
          + "    c.name as category_name"
          + "  from map_index m"
          + "  join map_category c on c.id = m.category_id"
          + "  order by m.map_name")
  List<MapListingRecord> fetchMapListings();
}
