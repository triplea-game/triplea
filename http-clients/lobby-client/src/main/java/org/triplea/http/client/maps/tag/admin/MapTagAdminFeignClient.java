package org.triplea.http.client.maps.tag.admin;

import feign.RequestLine;
import java.util.List;
import org.triplea.http.client.GenericServerResponse;

public interface MapTagAdminFeignClient {
  @RequestLine("GET " + MapTagAdminClient.GET_MAP_TAGS_META_DATA_PATH)
  List<MapTagMetaData> fetchAllowedMapTagValues();

  @RequestLine("POST " + MapTagAdminClient.UPDATE_MAP_TAG_PATH)
  GenericServerResponse updateMapTag(UpdateMapTagRequest updateMapTagRequest);
}
