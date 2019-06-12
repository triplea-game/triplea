package org.triplea.http.client.moderator.toolbox;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.triplea.http.client.HttpClient;
import org.triplea.http.client.HttpInteractionException;

import com.google.common.base.Preconditions;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * Wrapper around moderator toolbox feign client. This wrapper handles exceptions and returns
 * a success string result for most methods, otherwise the return value is any error message
 * returned back from the server.
 */
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class ModeratorToolboxClient {

  /**
   * Success string, any simple GET/POST method returning a string value other than this
   * value is returning an error message.
   */
  public static final String SUCCESS = "SUCCESS";

  public static final int API_KEY_PASSWORD_MIN_LENGTH = 4;
  public static final int API_KEY_PASSWORD_MAX_LENGTH = 64;

  public static final String VALIDATE_API_KEY_PATH = "/moderator-toolbox/validate-api-key";
  public static final String REGISTER_API_KEY_PATH = "/moderator-toolbox/register-api-key";
  public static final String BAD_WORD_ADD_PATH = "/moderator-toolbox/bad-words/add";
  public static final String BAD_WORD_REMOVE_PATH = "/moderator-toolbox/bad-words/remove";
  public static final String BAD_WORD_GET_PATH = "/moderator-toolbox/bad-words/get";
  public static final String AUDIT_HISTORY_PATH = "/moderator-toolbox/audit-history/lookup";
  public static final String API_KEY_HEADER = "moderator-api-key";
  public static final String API_KEY_PASSWORD_HEADER = "api-key-password";

  public static final String ROW_START_PARAM = "rowStart";
  public static final String ROW_COUNT_PARAM = "rowCount";


  public static final String SINGLE_USE_KEY_PARAM = "singleUseKey";
  public static final String NEW_API_KEY_PASSWORD_PARAM = "newApiKey";

  private final ModeratorToolboxFeignClient client;
  private final String apiKeyPassword;


  /**
   * Creates a ModeratorToolboxClient, used by moderators interact with the server
   * to view/add/remove database data (eg: player bans, bad words table).
   */
  public static ModeratorToolboxClient newClient(final URI uri, final String password) {
    Preconditions.checkNotNull(uri);
    Preconditions.checkNotNull(password);

    return new ModeratorToolboxClient(
        new HttpClient<>(ModeratorToolboxFeignClient.class, uri).get(),
        password);
  }

  /**
   * Sends a new single-use key API to server for validation. If things go well we'll get back a new
   * API key that should be stored in client settings. The password sent along with this API key should
   * be sent along with the new API key for future validations.
   */
  public RegisterApiKeyResult registerNewKey(final String singleUseKey) {
    checkArgument(singleUseKey != null && !singleUseKey.isEmpty());
    checkState(apiKeyPassword != null);

    try {
      return client.registerKey(
          RegisterApiKeyParam.builder()
              .singleUseKey(singleUseKey)
              .newPassword(apiKeyPassword)
              .build());
    } catch (final RuntimeException e) {
      return RegisterApiKeyResult.builder()
          .errorMessage(e.getMessage())
          .build();
    }
  }

  /**
   * Sends an API key for validation. Note, this is not the single-use key initially issued but a moderators
   * long-term API key that is stored in client settings.
   */
  public String validateApiKey(final String apiKey) {
    checkArgument(apiKey != null && !apiKey.isEmpty());

    try {
      return client.validateApiKey(createHeaders(apiKey));
    } catch (final RuntimeException e) {
      return e.getMessage();
    }
  }

  public String removeBadWord(final UpdateBadWordsArg badWordArgs) {
    checkArgument(badWordArgs != null);
    try {
      return client.removeBadWord(createHeaders(badWordArgs.getApiKey()), badWordArgs.getBadWord());
    } catch (final HttpInteractionException e) {
      return e.getMessage();
    }
  }

  private Map<String, Object> createHeaders(final String apiKey) {
    final Map<String, Object> headerMap = new HashMap<>();
    headerMap.put(API_KEY_HEADER, apiKey);
    headerMap.put(API_KEY_PASSWORD_HEADER, apiKeyPassword);
    return headerMap;
  }

  public String addBadWord(final UpdateBadWordsArg addBadWordArgs) {
    checkArgument(addBadWordArgs != null);
    try {
      return client.addBadWord(createHeaders(addBadWordArgs.getApiKey()), addBadWordArgs.getBadWord());
    } catch (final RuntimeException e) {
      return e.getMessage();
    }
  }

  /**
   * Returns list of bad words present in the bad words table.
   *
   * @param apiKey Moderator API key used to validate request is from an authorized moderator user.
   * @throws HttpInteractionException thrown if there are any HTTP errors or a non-200 return code.
   */
  public List<String> getBadWords(final String apiKey) {
    checkArgument(apiKey != null && !apiKey.isEmpty());

    return client.getBadWords(createHeaders(apiKey));
  }

  /**
   * Method to lookup moderator audit history events with paging.
   */
  public List<ModeratorEvent> lookupModeratorEvents(final LookupModeratorEventsArgs lookupModeratorEventsArgs) {
    checkArgument(!lookupModeratorEventsArgs.getApiKey().isEmpty());
    checkArgument(lookupModeratorEventsArgs.getRowStart() >= 0);
    checkArgument(lookupModeratorEventsArgs.getRowCount() >= 1);

    return client.lookupModeratorEvents(
        createHeaders(lookupModeratorEventsArgs.getApiKey()),
        lookupModeratorEventsArgs.getRowStart(),
        lookupModeratorEventsArgs.getRowCount());
  }
}
