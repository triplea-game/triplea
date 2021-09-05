package org.triplea.maps.tags;

import com.google.common.base.Strings;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jdbi.v3.core.Jdbi;
import org.triplea.http.client.maps.tag.admin.MapTagMetaData;
import org.triplea.http.client.maps.tag.admin.UpdateMapTagRequest;

@AllArgsConstructor
public class MapTagAdminModule {

  private final MapTagsDao mapTagsDao;

  public static MapTagAdminModule build(final Jdbi jdbi) {
    return new MapTagAdminModule(new MapTagsDao(jdbi));
  }

  /**
   * Updates (upserts or deletes) the tag value on a given map.
   *
   * <p>Delete Case: If the new tag value is an empty string or null, then the tag value is deleted
   * from the map.
   *
   * <p>Insert Case: If the tag value is non-empty and the tag does not exist for the map, the tag
   * is created.
   *
   * <p>Update Case: If the tag exists for a given map, it is updated to a new value.
   */
  public UpdateMapTagResult updateMapTag(final UpdateMapTagRequest updateMapTagRequest) {
    // if new tag value is empty or null, then delete the map tag
    if (Strings.nullToEmpty(updateMapTagRequest.getNewTagValue()).isBlank()) {
      mapTagsDao.deleteMapTag(updateMapTagRequest.getMapName(), updateMapTagRequest.getTagName());
      return UpdateMapTagResult.builder()
          .success(true)
          .message(
              String.format(
                  "Deleted: %s : %s",
                  updateMapTagRequest.getMapName(), updateMapTagRequest.getTagName()))
          .build();
    } else {
      return mapTagsDao.upsertMapTag(
          updateMapTagRequest.getMapName(),
          updateMapTagRequest.getTagName(),
          updateMapTagRequest.getNewTagValue());
    }
  }

  /**
   * Queries DB to get the full set of tags and their allowed values. Results are returned by
   * display order. An empty string is automatically added to the set of allowed values for each
   * tag.
   */
  public List<MapTagMetaData> fetchMapTags() {

    // aggregate tuples of:
    //    <name, displayorder, value1>,
    //    <name, displayorder, value2>
    // into:
    //     <name, displayorder, [ "", value1, value2 ]>

    // create a map where we will aggregate by tag name
    final Map<String, MapTagMetaDataBuilder> builders = new HashMap<>();

    for (final MapTagMetaDataRecord mapTagMetaDataRecord : mapTagsDao.fetchAllTagsMetaData()) {
      if (builders.containsKey(mapTagMetaDataRecord.getName())) {
        builders
            .get(mapTagMetaDataRecord.getName())
            .addAllowedValue(mapTagMetaDataRecord.getAllowedValue());
      } else {
        builders.put(
            mapTagMetaDataRecord.getName(), new MapTagMetaDataBuilder(mapTagMetaDataRecord));
      }
    }

    return builders.values().stream()
        .sorted(Comparator.comparing(MapTagMetaDataBuilder::getDisplayOrder))
        .map(MapTagMetaDataBuilder::toMapTagMetaData)
        .collect(Collectors.toList());
  }

  private static class MapTagMetaDataBuilder {
    private final String name;
    @Getter private final int displayOrder;
    private final Set<String> allowedValues;

    MapTagMetaDataBuilder(final MapTagMetaDataRecord mapTagMetaDataRecord) {
      this.name = mapTagMetaDataRecord.getName();
      this.displayOrder = mapTagMetaDataRecord.getDisplayOrder();
      allowedValues = new HashSet<>();
      allowedValues.add("");
      allowedValues.add(mapTagMetaDataRecord.getAllowedValue());
    }

    void addAllowedValue(final String allowedValue) {
      allowedValues.add(allowedValue);
    }

    MapTagMetaData toMapTagMetaData() {
      return MapTagMetaData.builder()
          .tagName(name)
          .displayOrder(displayOrder)
          .allowedValues(allowedValues.stream().sorted().collect(Collectors.toList()))
          .build();
    }
  }
}
