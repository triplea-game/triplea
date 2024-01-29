package org.triplea.maps.tags;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.maps.admin.MapTagMetaData;
import org.triplea.http.client.maps.admin.UpdateMapTagRequest;

@ExtendWith(MockitoExtension.class)
class MapTagAdminModuleTest {

  @Mock MapTagsDao mapTagsDao;

  @InjectMocks MapTagAdminModule mapTagAdminModule;

  @DisplayName(
      "If we update a map tag to an empty or null value, "
          + "then we should delete the map tag value database row.")
  @ParameterizedTest
  @MethodSource
  void updateMapTagDeletionCases(final UpdateMapTagRequest updateMapTagRequest) {
    mapTagAdminModule.updateMapTag(updateMapTagRequest);

    verify(mapTagsDao)
        .deleteMapTag(updateMapTagRequest.getMapName(), updateMapTagRequest.getTagName());
  }

  @SuppressWarnings("unused")
  static List<UpdateMapTagRequest> updateMapTagDeletionCases() {
    return List.of(
        UpdateMapTagRequest.builder()
            .mapName("map-name")
            .tagName("tag-name")
            .newTagValue("")
            .build(),
        UpdateMapTagRequest.builder()
            .mapName("map-name")
            .tagName("tag-name")
            .newTagValue(null)
            .build());
  }

  @DisplayName("Updating a map tag value to a non-empty value will upsert into database.")
  @Test
  void updateMapTag() {
    final var updateMapTagRequest =
        UpdateMapTagRequest.builder()
            .mapName("map-name")
            .tagName("tag-name")
            .newTagValue("any-value")
            .build();

    mapTagAdminModule.updateMapTag(updateMapTagRequest);

    verify(mapTagsDao)
        .upsertMapTag(
            updateMapTagRequest.getMapName(),
            updateMapTagRequest.getTagName(),
            updateMapTagRequest.getNewTagValue());
  }

  @Test
  void fetchMapTags() {
    when(mapTagsDao.fetchAllTagsMetaData())
        .thenReturn(
            List.of(
                MapTagMetaDataRecord.builder()
                    .displayOrder(1)
                    .name("Category")
                    .allowedValue("AWESOME")
                    .build(),
                MapTagMetaDataRecord.builder()
                    .displayOrder(1)
                    .name("Category")
                    .allowedValue("GOOD")
                    .build(),
                MapTagMetaDataRecord.builder()
                    .displayOrder(2)
                    .name("Rating")
                    .allowedValue("1")
                    .build()));

    final var results = mapTagAdminModule.fetchMapTags();

    assertThat(
        results,
        hasItem(
            MapTagMetaData.builder()
                .tagName("Category")
                .displayOrder(1)
                // note that empty string has been added
                .allowedValues(List.of("", "AWESOME", "GOOD"))
                .build()));
    assertThat(
        results,
        hasItem(
            MapTagMetaData.builder()
                .tagName("Rating")
                .displayOrder(2)
                // note that empty string has been added
                .allowedValues(List.of("", "1"))
                .build()));
  }
}
