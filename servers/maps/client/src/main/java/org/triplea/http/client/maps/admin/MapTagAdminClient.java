package org.triplea.http.client.maps.admin;

import feign.RequestLine;
import java.net.URI;
import java.util.List;
import org.triplea.http.client.GenericServerResponse;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.lib.ClientIdentifiers;

/** Http client for 'map tag' administrative functionality. EG: updating a maps tag value. */
public interface MapTagAdminClient {
  String GET_MAP_TAGS_META_DATA_PATH = "/maps/list-tags";
  String UPDATE_MAP_TAG_PATH = "/maps/update-tag";

  static MapTagAdminClient newClient(
      final URI mapsServerUri, final ClientIdentifiers clientIdentifiers) {
    return HttpClient.newClient(
        MapTagAdminClient.class, mapsServerUri, clientIdentifiers.createHeaders());
  }

  @RequestLine("GET " + MapTagAdminClient.GET_MAP_TAGS_META_DATA_PATH)
  List<MapTagMetaData> fetchAllowedMapTagValues();

  @RequestLine("POST " + MapTagAdminClient.UPDATE_MAP_TAG_PATH)
  GenericServerResponse updateMapTag(UpdateMapTagRequest updateMapTagRequest);
}
