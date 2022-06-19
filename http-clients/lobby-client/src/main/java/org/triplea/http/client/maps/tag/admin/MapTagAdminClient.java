package org.triplea.http.client.maps.tag.admin;

import java.net.URI;
import java.util.List;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.GenericServerResponse;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.lobby.AuthenticationHeaders;

/** Http client for 'map tag' administrative functionality. EG: updating a maps tag value. */
public class MapTagAdminClient {
  public static final String GET_MAP_TAGS_META_DATA_PATH = "/maps/list-tags";
  public static final String UPDATE_MAP_TAG_PATH = "/maps/update-tag";

  private final MapTagAdminFeignClient mapTagAdminClient;

  public MapTagAdminClient(final URI mapsServerUri, final ApiKey apiKey) {
    mapTagAdminClient =
        HttpClient.newClient(
            MapTagAdminFeignClient.class,
            mapsServerUri,
            new AuthenticationHeaders(apiKey).createHeaders());
  }

  public static MapTagAdminClient newClient(final URI mapsServerUri, final ApiKey apiKey) {
    return new MapTagAdminClient(mapsServerUri, apiKey);
  }

  public List<MapTagMetaData> fetchTagsMetaData() {
    return mapTagAdminClient.fetchAllowedMapTagValues();
  }

  public GenericServerResponse updateMapTag(final UpdateMapTagRequest updateMapTagRequest) {
    return mapTagAdminClient.updateMapTag(updateMapTagRequest);
  }
}
