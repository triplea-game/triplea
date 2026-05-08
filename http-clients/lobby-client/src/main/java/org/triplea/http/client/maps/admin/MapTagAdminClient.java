package org.triplea.http.client.maps.admin;

import feign.RequestLine;
import java.net.URI;
import java.util.List;
import org.triplea.http.client.ClientIdentifiers;
import org.triplea.http.client.GenericServerResponse;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.ServerPaths;
import org.triplea.http.client.lobby.maps.admin.MapTagMetaData;

/** Http client for 'map tag' administrative functionality. EG: updating a maps tag value. */
public interface MapTagAdminClient {

  static MapTagAdminClient newClient(
      final URI mapsServerUri, final ClientIdentifiers clientIdentifiers) {
    return HttpClient.newClient(
        MapTagAdminClient.class, mapsServerUri, clientIdentifiers.createHeaders());
  }

  @RequestLine("GET " + ServerPaths.GET_MAP_TAGS_META_DATA_PATH)
  List<MapTagMetaData> fetchAllowedMapTagValues();

  @RequestLine("POST " + ServerPaths.UPDATE_MAP_TAG_PATH)
  GenericServerResponse updateMapTag(
      org.triplea.http.client.lobby.maps.admin.UpdateMapTagRequest updateMapTagRequest);
}
