package org.triplea.spitfire.server.maps;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.junit5.DBUnitExtension;
import java.net.URI;
import java.util.List;
import lombok.AllArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.HttpInteractionException;
import org.triplea.http.client.maps.listing.MapsClient;
import org.triplea.http.client.maps.tag.admin.MapTagAdminClient;
import org.triplea.http.client.maps.tag.admin.MapTagMetaData;
import org.triplea.http.client.maps.tag.admin.UpdateMapTagRequest;
import org.triplea.spitfire.server.AllowedUserRole;
import org.triplea.spitfire.server.SpitfireDatabaseTestSupport;
import org.triplea.spitfire.server.SpitfireServerTestExtension;

@AllArgsConstructor
@ExtendWith(SpitfireServerTestExtension.class)
@ExtendWith(SpitfireDatabaseTestSupport.class)
@ExtendWith(DBUnitExtension.class)
@DataSet(
    value = SpitfireServerTestExtension.LOBBY_USER_DATASET + ",map_index.yml,map_tag_value.yml",
    useSequenceFiltering = false)
class MapTagAdminControllerTest {
  private final URI localhost;

  @ParameterizedTest
  @ValueSource(strings = {AllowedUserRole.KeyValues.ANONYMOUS, AllowedUserRole.KeyValues.PLAYER})
  void fetchMapTagMetaDataRequiresAuthentication(final String disallowedRoleKey) {
    new MapTagAdminClient(localhost, ApiKey.of(disallowedRoleKey)).fetchTagsMetaData();
  }

  @Test
  void fetchMapTagMetaData() {
    final List<MapTagMetaData> mapTagMetaData =
        new MapTagAdminClient(localhost, AllowedUserRole.MODERATOR.getApiKey()).fetchTagsMetaData();

    assertThat(mapTagMetaData, hasSize(2));
    for (final MapTagMetaData item : mapTagMetaData) {
      assertThat(item.getTagName(), is(notNullValue()));
      assertThat(item.getAllowedValues(), is(not(empty())));
      assertThat(item.getDisplayOrder() > 0, is(true));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {AllowedUserRole.KeyValues.ANONYMOUS, AllowedUserRole.KeyValues.PLAYER})
  void updateMapTagValueRequiresAuthentication(final String disallowedRoleKey) {
    final HttpInteractionException exception =
        Assertions.assertThrows(
            HttpInteractionException.class,
            () ->
                new MapTagAdminClient(localhost, ApiKey.of(disallowedRoleKey))
                    .updateMapTag(
                        UpdateMapTagRequest.builder()
                            .mapName("map-name")
                            .tagName("tag-name")
                            .newTagValue("new-tag-value")
                            .build()));

    assertThat(exception.status(), is(403));
  }

  /**
   * In this test we fetch a map listing and verify a known map should have a known tag value. We
   * then send a request to update that tag value and we then repeat the listing request to verify
   * the tag value is updated.
   */
  @Test
  void updateMapTagValue() {
    assertThat(
        "Verify an initial state in database", getMapTagValue("map-name", "Rating"), is("2"));

    new MapTagAdminClient(localhost, ApiKey.of(AllowedUserRole.KeyValues.MODERATOR))
        .updateMapTag(
            UpdateMapTagRequest.builder()
                .mapName("map-name")
                .tagName("Rating")
                .newTagValue("1")
                .build());

    assertThat(
        "Verify tag value is now updated compared to the initial database state",
        getMapTagValue("map-name", "Rating"),
        is("1"));
  }

  @SuppressWarnings("SameParameterValue")
  private String getMapTagValue(final String mapName, final String tagName) {
    // Get map listing
    final var mapListing = new MapsClient(localhost).fetchMapDownloads();

    // find map
    final var map =
        mapListing.stream()
            .filter(m -> m.getMapName().equals(mapName))
            .findFirst()
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Unable to find map: " + mapName + ", in: " + mapListing));

    return map.getTagValue(tagName);
  }
}
