package org.triplea.maps.tags;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItem;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.ExpectedDataSet;
import com.github.database.rider.junit5.DBUnitExtension;
import lombok.AllArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.maps.MapsModuleDatabaseTestSupport;

@DataSet(value = "map_index.yml,map_tag_value.yml", useSequenceFiltering = false)
@ExtendWith(MapsModuleDatabaseTestSupport.class)
@ExtendWith(DBUnitExtension.class)
@AllArgsConstructor
class MapTagsDaoTest {

  private final MapTagsDao mapTagsDao;

  @Test
  void verifyFetchTagsForGivenMap() {
    final var mapTags = mapTagsDao.fetchAllMapTags();
    assertThat(mapTags, hasSize(3));
    assertThat(mapTags.get(0).getMapName(), is("map-name"));
    assertThat(mapTags.get(0).getTagName(), is("Category"));
    assertThat(mapTags.get(0).getDisplayOrder(), is(1));
    assertThat(mapTags.get(0).getValue(), is("BEST"));

    assertThat(mapTags.get(1).getMapName(), is("map-name"));
    assertThat(mapTags.get(1).getTagName(), is("Rating"));
    assertThat(mapTags.get(1).getDisplayOrder(), is(2));
    assertThat(mapTags.get(1).getValue(), is("1"));

    assertThat(mapTags.get(2).getMapName(), is("map-name-2"));
    assertThat(mapTags.get(2).getTagName(), is("Rating"));
    assertThat(mapTags.get(2).getDisplayOrder(), is(2));
    assertThat(mapTags.get(2).getValue(), is("1"));
  }

  @DisplayName("Run a query to get all tags, then check that we have each expected result rows.")
  @Test
  void fetchAllTagsMetaData() {
    final var tags = mapTagsDao.fetchAllTagsMetaData();

    assertThat(
        tags,
        hasItem(
            MapTagMetaDataRecord.builder()
                .displayOrder(1)
                .name("Category")
                .allowedValue("AWESOME")
                .build()));
    assertThat(
        tags,
        hasItem(
            MapTagMetaDataRecord.builder()
                .displayOrder(1)
                .name("Category")
                .allowedValue("GOOD")
                .build()));
    assertThat(
        tags,
        hasItem(
            MapTagMetaDataRecord.builder()
                .displayOrder(1)
                .name("Category")
                .allowedValue("BEST")
                .build()));

    assertThat(
        tags,
        hasItem(
            MapTagMetaDataRecord.builder()
                .displayOrder(2)
                .name("Rating")
                .allowedValue("1")
                .build()));
    assertThat(
        tags,
        hasItem(
            MapTagMetaDataRecord.builder()
                .displayOrder(2)
                .name("Rating")
                .allowedValue("2")
                .build()));
  }

  @Test
  @ExpectedDataSet("expected/map_tag_value_after_tag_deleted.yml")
  void deleteMapTag() {
    mapTagsDao.deleteMapTag("map-name", "Category");
  }

  @DisplayName("Update an existing tag value to a new value")
  @Test
  @ExpectedDataSet("expected/map_tag_value_after_update_to_new_value.yml")
  void upsertMapTagNewValue() {
    final var result = mapTagsDao.upsertMapTag("map-name", "Category", "AWESOME");
    assertThat(result.isSuccess(), is(true));
  }

  @DisplayName("Insert a new tag value")
  @Test
  @ExpectedDataSet(value = "expected/map_tag_value_after_insert_new_value.yml")
  void upsertMapTagAddingNewTag() {
    // map-name-2 does not have a 'Category' tag
    final var result = mapTagsDao.upsertMapTag("map-name-2", "Category", "GOOD");
    assertThat(result.isSuccess(), is(true));
  }

  @DisplayName("Update a tag to an illegal value is a no-op error")
  @Test
  /* No changes expected to database */
  @ExpectedDataSet("map_tag_value.yml")
  void upsertMapTagToNonAllowed() {
    final var result = mapTagsDao.upsertMapTag("map-name", "Category", "BAD-VALUE");
    assertThat(result.isSuccess(), is(false));
  }

  @DisplayName("Update a tag to the wrong allowed value, the allowed value for a different tag")
  @Test
  /* No changes expected to database */
  @ExpectedDataSet("map_tag_value.yml")
  void upsertMapTagToWrongAllowedValue() {
    final var result = mapTagsDao.upsertMapTag("map-name", "Category", "1");
    assertThat(result.isSuccess(), is(false));
  }

  @DisplayName("Update a non-existing tag is a no-op error")
  @Test
  /* No changes expected to database */
  @ExpectedDataSet("map_tag_value.yml")
  void upsertMapTagToNonExistentTag() {
    final var result = mapTagsDao.upsertMapTag("map-name", "Bad-Tag-Name", "ANY-VALUE");
    assertThat(result.isSuccess(), is(false));
  }

  @DisplayName("Updating the tag of a non-existing map is a no-op error")
  @Test
  /* No changes expected to database */
  @ExpectedDataSet("map_tag_value.yml")
  void upsertMapTagWhereMapDoesNotExist() {
    final var result = mapTagsDao.upsertMapTag("bad-map-name", "Category", "GOOD");
    assertThat(result.isSuccess(), is(false));
  }
}
