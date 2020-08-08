package org.triplea.http.client.lobby.moderator.toolbox.words;

import java.net.URI;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.AuthenticationHeaders;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.HttpInteractionException;

/** Http client class for fetching the list of bad words and adding and removing them. */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ToolboxBadWordsClient {
  public static final String BAD_WORD_ADD_PATH = "/moderator-toolbox/bad-words/add";
  public static final String BAD_WORD_REMOVE_PATH = "/moderator-toolbox/bad-words/remove";
  public static final String BAD_WORD_GET_PATH = "/moderator-toolbox/bad-words/get";

  private final AuthenticationHeaders authenticationHeaders;
  private final ToolboxBadWordsFeignClient client;

  public static ToolboxBadWordsClient newClient(final URI serverUri, final ApiKey apiKey) {
    return new ToolboxBadWordsClient(
        new AuthenticationHeaders(apiKey),
        new HttpClient<>(ToolboxBadWordsFeignClient.class, serverUri).get());
  }

  public void removeBadWord(final String badWord) {
    client.removeBadWord(authenticationHeaders.createHeaders(), badWord);
  }

  public void addBadWord(final String badWord) {
    client.addBadWord(authenticationHeaders.createHeaders(), badWord);
  }

  /**
   * Returns list of bad words present in the bad words table.
   *
   * @throws HttpInteractionException thrown if there are any HTTP errors or a non-200 return code.
   */
  public List<String> getBadWords() {
    return client.getBadWords(authenticationHeaders.createHeaders());
  }
}
