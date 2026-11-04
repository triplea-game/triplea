package org.triplea.http.client.lobby.maps.listing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MapDownloadItemTest {

  @Test
  @DisplayName("A tag with several values shows all of them, other tags left out")
  void getTagValueJoinsAllValuesOfTheTag() {
    final MapDownloadItem map =
        givenMapWithTags(
            List.of(
                MapTag.builder().name("Genre").value("Fantasy").build(),
                MapTag.builder().name("Quality").value("Good").build(),
                MapTag.builder().name("Genre").value("WW2").build()));

    assertThat(map.getTagValue("genre"))
        .as("both genre values, in server order, tag name ignoring case")
        .isEqualTo("Fantasy, WW2");
    assertThat(map.getTagValue("Quality")).as("a single value is unchanged").isEqualTo("Good");
  }

  @Test
  @DisplayName("A map built without tags gives an empty value instead of throwing")
  void getTagValueWithoutTagsIsEmpty() {
    final MapDownloadItem map = givenMapWithTags(null);

    assertThat(map.getTagValue("Genre")).as("null mapTags counts as no tags").isEmpty();
  }

  private static MapDownloadItem givenMapWithTags(final List<MapTag> mapTags) {
    return MapDownloadItem.builder()
        .downloadUrl("https://download")
        .previewImageUrl("https://preview")
        .mapName("map")
        .lastCommitDateEpochMilli(0L)
        .description("description")
        .mapTags(mapTags)
        .downloadSizeInBytes(0L)
        .build();
  }
}
