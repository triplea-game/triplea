package org.triplea.http.client.lobby.moderator.toolbox.words;

import feign.RequestLine;
import java.net.URI;
import java.util.List;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.lobby.AuthenticationHeaders;

/** Http client class for fetching the list of bad words and adding and removing them. */
public interface ToolboxBadWordsClient {
  String BAD_WORD_ADD_PATH = "/lobby/moderator-toolbox/bad-words/add";
  String BAD_WORD_REMOVE_PATH = "/lobby/moderator-toolbox/bad-words/remove";
  String BAD_WORD_GET_PATH = "/lobby/moderator-toolbox/bad-words/get";

  static ToolboxBadWordsClient newClient(final URI serverUri, final ApiKey apiKey) {
    return HttpClient.newClient(
        ToolboxBadWordsClient.class, serverUri, new AuthenticationHeaders(apiKey).createHeaders());
  }

  @RequestLine("POST " + ToolboxBadWordsClient.BAD_WORD_REMOVE_PATH)
  void removeBadWord(String word);

  @RequestLine("POST " + ToolboxBadWordsClient.BAD_WORD_ADD_PATH)
  void addBadWord(String word);

  /**
   * Returns list of bad words present in the bad words table. On error, logs an error message and
   * returns an empty list.
   */
  @RequestLine("GET " + ToolboxBadWordsClient.BAD_WORD_GET_PATH)
  List<String> getBadWords();
}
