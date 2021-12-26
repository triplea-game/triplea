package org.triplea.http.client.lobby.moderator.toolbox.words;

import java.net.URI;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.AuthenticationHeaders;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.HttpInteractionException;

/** Http client class for fetching the list of bad words and adding and removing them. */
@Slf4j
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ToolboxBadWordsClient {
  public static final String BAD_WORD_ADD_PATH = "/moderator-toolbox/bad-words/add";
  public static final String BAD_WORD_REMOVE_PATH = "/moderator-toolbox/bad-words/remove";
  public static final String BAD_WORD_GET_PATH = "/moderator-toolbox/bad-words/get";

  private final Map<String, Object> authenticationHeaders;
  private final ToolboxBadWordsFeignClient client;

  public static ToolboxBadWordsClient newClient(final URI serverUri, final ApiKey apiKey) {
    return new ToolboxBadWordsClient(
        new AuthenticationHeaders(apiKey).createHeaders(),
        new HttpClient<>(ToolboxBadWordsFeignClient.class, serverUri).get());
  }

  public void removeBadWord(final String badWord) {
    client.removeBadWord(authenticationHeaders, badWord);
  }

  public void addBadWord(final String badWord) {
    client.addBadWord(authenticationHeaders, badWord);
  }

  /**
   * Returns list of bad words present in the bad words table. On error, logs an error message and
   * returns an empty list.
   */
  public List<String> getBadWords() {
    try {
      return client.getBadWords(authenticationHeaders);
    } catch (final HttpInteractionException e) {
      log.error("Failed to fetch list of bad words", e);
      return List.of();
    }
  }
}
