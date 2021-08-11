package org.triplea.maps.listing;

import java.util.List;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

public interface MapListingDao {
  /** Returns all maps registered listed in database. */
  @SqlQuery(
      "select"
          + "    m.map_name,"
          + "    m.download_url,"
          + "    m.preview_image_url,"
          + "    m.description,"
          + "    m.last_commit_date"
          + "  from map_index m"
          + "  order by m.map_name")
  List<MapListingRecord> fetchMapListings();

  /** Fetchs map tags for a given map. */
  @SqlQuery(
      "select"
          + "    tt.name,"
          + "    tt.type,"
          + "    tt.display_order,"
          + "    mtv.tag_value"
          + "  from map_tag_values mtv"
          + "  join tag_type tt on tt.id = mtv.tag_type_id"
          + "  join map_index mi on mi.id = mtv.map_id"
          + "  where mi.map_name = :mapName"
          + "  order by tt.display_order")
  List<MapTagRecord> fetchMapTagsForMapName(@Bind("mapName") String mapName);
}
