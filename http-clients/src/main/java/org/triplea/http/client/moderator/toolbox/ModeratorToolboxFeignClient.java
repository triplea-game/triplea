package org.triplea.http.client.moderator.toolbox;

import java.util.List;
import java.util.Map;

import org.triplea.http.client.HttpConstants;

import feign.HeaderMap;
import feign.Headers;
import feign.RequestLine;

/**
 * Http client for moderator 'toolbox' actions. The toolbox is essentially a set of windows
 * that show lobby table data and provide CRUD operations to moderators. For example
 * with the toolbox moderators should be able to update the list of restricted usernames,
 * view players that have joined the lobby, add and remove player bans, etc.
 * <p>
 * This set of http methods should all require an API key passed via headers to authenticate
 * the moderator issuing the request.
 * </p>
 * <p>
 * Rate-limiting: of note, the backend implementation should be careful to apply rate limiting
 * to any/all endpoints that take an API key so as to avoid brute-force attacks to try and crack
 * an API key value.
 * </p>
 */
interface ModeratorToolboxFeignClient {
  @RequestLine("POST " + ModeratorToolboxClient.VALIDATE_API_KEY_PATH)
  @Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
  String validateApiKey(@HeaderMap Map<String, Object> headerMap);

  @RequestLine("POST " + ModeratorToolboxClient.BAD_WORD_REMOVE_PATH)
  @Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
  String removeBadWord(@HeaderMap Map<String, Object> headerMap, String word);

  @RequestLine("POST " + ModeratorToolboxClient.BAD_WORD_ADD_PATH)
  @Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
  String addBadWord(@HeaderMap Map<String, Object> headerMap, String word);

  @RequestLine("GET " + ModeratorToolboxClient.BAD_WORD_GET_PATH)
  @Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
  List<String> getBadWords(@HeaderMap Map<String, Object> headerMap);
}
