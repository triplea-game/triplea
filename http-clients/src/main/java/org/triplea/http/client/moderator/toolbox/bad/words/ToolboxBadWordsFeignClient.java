package org.triplea.http.client.moderator.toolbox.bad.words;

import java.util.List;
import java.util.Map;

import org.triplea.http.client.HttpConstants;

import feign.HeaderMap;
import feign.Headers;
import feign.RequestLine;

interface ToolboxBadWordsFeignClient {
  @RequestLine("POST " + ToolboxBadWordsClient.BAD_WORD_REMOVE_PATH)
  @Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
  void removeBadWord(@HeaderMap Map<String, Object> headerMap, String word);

  @RequestLine("POST " + ToolboxBadWordsClient.BAD_WORD_ADD_PATH)
  @Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
  void addBadWord(@HeaderMap Map<String, Object> headerMap, String word);

  @RequestLine("GET " + ToolboxBadWordsClient.BAD_WORD_GET_PATH)
  @Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
  List<String> getBadWords(@HeaderMap Map<String, Object> headerMap);
}
