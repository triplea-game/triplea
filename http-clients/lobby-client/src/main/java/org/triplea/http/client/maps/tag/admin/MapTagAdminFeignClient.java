package org.triplea.http.client.maps.tag.admin;

import feign.HeaderMap;
import feign.Headers;
import feign.RequestLine;
import java.util.List;
import java.util.Map;
import org.triplea.http.client.GenericServerResponse;
import org.triplea.http.client.HttpConstants;

@Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
public interface MapTagAdminFeignClient {
  @RequestLine("GET " + MapTagAdminClient.GET_MAP_TAGS_META_DATA_PATH)
  List<MapTagMetaData> fetchAllowedMapTagValues(@HeaderMap Map<String, Object> systemIdHeaders);

  @RequestLine("POST " + MapTagAdminClient.UPDATE_MAP_TAG_PATH)
  GenericServerResponse updateMapTag(
      @HeaderMap Map<String, Object> systemIdHeaders, UpdateMapTagRequest updateMapTagRequest);
}
