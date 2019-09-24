package org.triplea.http.client.lobby.moderator.toolbox.words;

import feign.HeaderMap;
import feign.Headers;
import feign.RequestLine;
import java.util.List;
import java.util.Map;
import org.triplea.http.client.HttpConstants;

@Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
interface ToolboxBadWordsFeignClient {
  @RequestLine("POST " + ToolboxBadWordsClient.BAD_WORD_REMOVE_PATH)
  void removeBadWord(@HeaderMap Map<String, Object> headerMap, String word);

  @RequestLine("POST " + ToolboxBadWordsClient.BAD_WORD_ADD_PATH)
  void addBadWord(@HeaderMap Map<String, Object> headerMap, String word);

  @RequestLine("GET " + ToolboxBadWordsClient.BAD_WORD_GET_PATH)
  List<String> getBadWords(@HeaderMap Map<String, Object> headerMap);
}
