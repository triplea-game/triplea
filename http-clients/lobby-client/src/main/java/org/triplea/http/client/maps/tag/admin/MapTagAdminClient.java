package org.triplea.http.client.maps.tag.admin;

import java.net.URI;
import java.util.List;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.AuthenticationHeaders;
import org.triplea.http.client.GenericServerResponse;
import org.triplea.http.client.HttpClient;

/** Http client for 'map tag' administrative functionality. EG: updating a maps tag value. */
public class MapTagAdminClient {
  public static final String GET_MAP_TAGS_META_DATA_PATH = "/maps/list-tags";
  public static final String UPDATE_MAP_TAG_PATH = "/maps/update-tag";

  private final MapTagAdminFeignClient mapTagAdminClient;
  private final ApiKey apiKey;

  public MapTagAdminClient(final URI mapsServerUri, final ApiKey apiKey) {
    mapTagAdminClient = new HttpClient<>(MapTagAdminFeignClient.class, mapsServerUri).get();
    this.apiKey = apiKey;
  }

  public static MapTagAdminClient newClient(final URI mapsServerUri, final ApiKey apiKey) {
    return new MapTagAdminClient(mapsServerUri, apiKey);
  }

  public List<MapTagMetaData> fetchTagsMetaData() {
    return mapTagAdminClient.fetchAllowedMapTagValues(
        new AuthenticationHeaders(apiKey).createHeaders());
  }

  public GenericServerResponse updateMapTag(final UpdateMapTagRequest updateMapTagRequest) {
    return mapTagAdminClient.updateMapTag(
        new AuthenticationHeaders(apiKey).createHeaders(), updateMapTagRequest);
  }
}
