package org.triplea.maps.tags;

import java.util.List;
import lombok.AllArgsConstructor;
import org.jdbi.v3.core.Jdbi;
import org.triplea.maps.listing.MapTagRecord;

@AllArgsConstructor
public class MapTagsDao {
  private final Jdbi jdbi;

  /** Fetches the set of map tag values attached to a given map. */
  public List<MapTagRecord> fetchAllMapTags() {
    final String query =
        "select"
            + "    mi.map_name map_name,"
            + "    mt.name tag_name,"
            + "    mt.display_order,"
            + "    mtav.value tag_value"
            + "  from map_tag_value mtv"
            + "  join map_tag mt on mt.id = mtv.map_tag_id"
            + "  join map_tag_allowed_value mtav on mtav.id = mtv.map_tag_allowed_value_id"
            + "  join map_index mi on mi.id = mtv.map_index_id"
            + "  order by mi.map_name, mt.display_order";
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(query) //
                .mapTo(MapTagRecord.class)
                .list());
  }

  /** Returns the full set of tag names and all allowed values. */
  public List<MapTagMetaDataRecord> fetchAllTagsMetaData() {
    final String queryString =
        "select mt.name, mt.display_order, mtav.value allowed_value "
            + "from map_tag mt "
            + "join map_tag_allowed_value mtav on mtav.map_tag_id = mt.id "
            + "order by mt.display_order, mtav.value";

    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(queryString) //
                .mapTo(MapTagMetaDataRecord.class)
                .list());
  }

  /**
   * Deletes a tag value on a given map.
   *
   * @param mapName The map whose tag will be deleted.
   * @param tagName The name of the tag to delete.
   */
  UpdateMapTagResult deleteMapTag(final String mapName, final String tagName) {
    final String query =
        "delete from map_tag_value "
            + "where "
            + "  map_index_id = (select id from map_index where map_name = ?) "
            + "  and map_tag_id = (select id from map_tag where name = ?)";

    jdbi.withHandle(handle -> handle.execute(query, mapName, tagName));

    return UpdateMapTagResult.builder()
        .success(true)
        .message(String.format("Deleted: %s : %s", mapName, tagName))
        .build();
  }

  /**
   * Upserts a given tag for a given map. Returns a failure result if the tag value is invalid or if
   * the map name cannot be found. Updates to the same value return a 'success'.
   *
   * @param mapName The name of the map to be tagged (map_index.map_name)
   * @param tagName The name of the tag to upsert
   * @param tagValue The value of the tag
   */
  UpdateMapTagResult upsertMapTag(
      final String mapName, final String tagName, final String tagValue) {

    final Integer tagAllowedValueId = lookupTagValueId(tagName, tagValue);
    if (tagAllowedValueId == null) {
      return UpdateMapTagResult.builder()
          .success(false)
          .message(String.format("%s is not a valid tag value for tag %s", tagValue, tagName))
          .build();
    }

    final Integer mapId = lookupMapId(mapName);
    if (mapId == null) {
      return UpdateMapTagResult.builder()
          .success(false)
          .message(String.format("Unable to find map with name %s", mapName))
          .build();
    }

    // run the upsert
    final String upsert =
        String.format(
            "insert into map_tag_value("
                + "   map_tag_id, "
                + "   map_index_id, "
                + "   map_tag_allowed_value_id)"
                + " values("
                + "   (select id from map_tag where name = :tagName),"
                + "   %s,"
                + "   %s"
                + " )"
                + " on conflict(map_tag_id, map_index_id)\n"
                + " do update set map_tag_allowed_value_id = %s",
            mapId, tagAllowedValueId, tagAllowedValueId);

    jdbi.withHandle(
        handle ->
            handle
                .createUpdate(upsert)
                .bind("tagName", tagName)
                .bind("mapName", mapName)
                .execute());

    return UpdateMapTagResult.builder()
        .success(true)
        .message(String.format("%s tagged with %s = %s", mapName, tagName, tagValue))
        .build();
  }

  private Integer lookupTagValueId(final String tagName, final String tagValue) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(
                    "select mtav.id "
                        + "from map_tag_allowed_value mtav "
                        + "join map_tag mt on mt.id = mtav.map_tag_id "
                        + "where mt.name = :tagName and mtav.value = :tagValue")
                .bind("tagName", tagName)
                .bind("tagValue", tagValue)
                .mapTo(Integer.class)
                .findOne()
                .orElse(null));
  }

  private Integer lookupMapId(final String mapName) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery("select id from map_index where map_name = :mapName")
                .bind("mapName", mapName)
                .mapTo(Integer.class)
                .findOne()
                .orElse(null));
  }
}
