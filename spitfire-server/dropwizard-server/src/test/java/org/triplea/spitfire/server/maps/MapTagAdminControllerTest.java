package org.triplea.spitfire.server.maps;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;

import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.GenericServerResponse;
import org.triplea.http.client.maps.listing.MapsClient;
import org.triplea.http.client.maps.tag.admin.MapTagAdminClient;
import org.triplea.http.client.maps.tag.admin.MapTagMetaData;
import org.triplea.http.client.maps.tag.admin.UpdateMapTagRequest;
import org.triplea.spitfire.server.ControllerIntegrationTest;

class MapTagAdminControllerTest extends ControllerIntegrationTest {
  private final URI localhost;
  private final MapTagAdminClient client;

  MapTagAdminControllerTest(final URI localhost) {
    this.localhost = localhost;
    this.client = MapTagAdminClient.newClient(localhost, ControllerIntegrationTest.MODERATOR);
  }

  @Test
  void requiresAuthentication() {
    assertNotAuthorized(
        ControllerIntegrationTest.NOT_MODERATORS,
        apiKey -> MapTagAdminClient.newClient(localhost, apiKey),
        MapTagAdminClient::fetchAllowedMapTagValues,
        client ->
            client.updateMapTag(
                UpdateMapTagRequest.builder()
                    .tagName("tag")
                    .mapName("map")
                    .newTagValue("value")
                    .build()));
  }

  @Test
  void fetchMapTagMetaData() {
    final List<MapTagMetaData> mapTagMetaData = client.fetchAllowedMapTagValues();

    assertThat(mapTagMetaData, hasSize(2));
    for (final MapTagMetaData item : mapTagMetaData) {
      assertThat(item.getTagName(), is(notNullValue()));
      assertThat(item.getAllowedValues(), is(not(empty())));
      assertThat(item.getDisplayOrder() > 0, is(true));
    }
  }

  /**
   * In this test we:<br>
   * - fetch a map listing<br>
   * - update a tag value of the first map listing<br>
   * - fetch map listing & verify tag value is updated<br>
   */
  @Test
  void updateMapTagValue() {
    assertThat(
        "Verify an initial state in database", getMapTagValue("map-name", "Rating"), is("2"));

    final GenericServerResponse serverResponse =
        client.updateMapTag(
            UpdateMapTagRequest.builder()
                .mapName("map-name")
                .tagName("Rating")
                .newTagValue("1")
                .build());
    assertThat(
        "expecting tag to be updated successfully: " + serverResponse,
        serverResponse.isSuccess(),
        is(true));

    assertThat(
        "Verify tag value is now updated compared to the initial database state",
        getMapTagValue("map-name", "Rating"),
        is("1"));
  }

  @SuppressWarnings("SameParameterValue")
  private String getMapTagValue(final String mapName, final String tagName) {
    // Get all maps listing
    final var mapListing = MapsClient.newClient(localhost).fetchMapListing();

    // find specific map from listing
    final var map =
        mapListing.stream()
            .filter(m -> m.getMapName().equals(mapName))
            .findFirst()
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Unable to find map: " + mapName + ", in: " + mapListing));

    // return tag value
    return map.getTagValue(tagName);
  }
}
