package org.triplea.http.client.lobby.moderator.toolbox.words;

import feign.FeignException;
import java.net.URI;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.lobby.AuthenticationHeaders;

/** Http client class for fetching the list of bad words and adding and removing them. */
@Slf4j
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ToolboxBadWordsClient {
  public static final String BAD_WORD_ADD_PATH = "/moderator-toolbox/bad-words/add";
  public static final String BAD_WORD_REMOVE_PATH = "/moderator-toolbox/bad-words/remove";
  public static final String BAD_WORD_GET_PATH = "/moderator-toolbox/bad-words/get";

  private final ToolboxBadWordsFeignClient client;

  public static ToolboxBadWordsClient newClient(final URI serverUri, final ApiKey apiKey) {
    return new ToolboxBadWordsClient(
        HttpClient.newClient(
            ToolboxBadWordsFeignClient.class,
            serverUri,
            new AuthenticationHeaders(apiKey).createHeaders()));
  }

  public void removeBadWord(final String badWord) {
    client.removeBadWord(badWord);
  }

  public void addBadWord(final String badWord) {
    client.addBadWord(badWord);
  }

  /**
   * Returns list of bad words present in the bad words table. On error, logs an error message and
   * returns an empty list.
   */
  public List<String> getBadWords() {
    try {
      return client.getBadWords();
    } catch (final FeignException e) {
      log.error("Failed to fetch list of bad words", e);
      return List.of();
    }
  }
}
