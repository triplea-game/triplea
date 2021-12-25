package org.triplea.maps.listing;

import java.util.List;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

public interface MapListingDao {
  /** Returns all maps registered listed in database. */
  @SqlQuery(
      "select"
          + "    m.map_name,"
          + "    m.download_url,"
          + "    m.download_size_bytes,"
          + "    m.preview_image_url,"
          + "    m.description,"
          + "    m.last_commit_date"
          + "  from map_index m"
          + "  order by m.map_name")
  List<MapListingRecord> fetchMapListings();
}
