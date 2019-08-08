package org.triplea.http.client.moderator.toolbox.api.key;

import static com.google.common.base.Preconditions.checkArgument;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.HttpInteractionException;
import org.triplea.http.client.moderator.toolbox.ApiKeyPassword;
import org.triplea.http.client.moderator.toolbox.NewApiKey;
import org.triplea.http.client.moderator.toolbox.ToolboxHttpHeaders;

/**
 * Http client intended for interaction with server to manage API keys. This includes view,
 * validate, create and delete.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ToolboxApiKeyClient {
  public static final String GET_API_KEYS = "/moderator-toolbox/get-api-keys";
  public static final String DELETE_API_KEY = "/moderator-toolbox/delete-api-key";
  public static final String GENERATE_SINGLE_USE_KEY_PATH =
      "/moderator-toolbox/generate-single-use-key";
  public static final String VALIDATE_API_KEY_PATH = "/moderator-toolbox/validate-api-key";
  public static final String RESET_LOCKOUTS_PATH = "/moderator-toolbox/reset-lockouts";

  public static final String SINGLE_USE_KEY_PARAM = "singleUseKey";
  public static final String NEW_API_KEY_PASSWORD_PARAM = "newApiKey";

  private final ToolboxHttpHeaders toolboxHttpHeaders;
  private final ToolboxApiKeyFeignClient client;

  public static ToolboxApiKeyClient newClient(
      final URI serverUri, final ApiKeyPassword apiKeyPassword) {
    return new ToolboxApiKeyClient(
        new ToolboxHttpHeaders(apiKeyPassword),
        new HttpClient<>(ToolboxApiKeyFeignClient.class, serverUri).get());
  }

  /**
   * Sends an API key for validation. Note, this is not the single-use key initially issued but a
   * moderators long-term API key that is stored in client settings.
   *
   * @return Returns empty if success, otherwise returns an error message.
   */
  public Optional<String> validateApiKey() {
    try {
      client.validateApiKey(toolboxHttpHeaders.createHeaders());
      return Optional.empty();
    } catch (final HttpInteractionException e) {
      return Optional.of(e.getMessage());
    }
  }

  public List<ApiKeyData> getApiKeys() {
    return client.getApiKeys(toolboxHttpHeaders.createHeaders());
  }

  public void deleteApiKey(final String keyIdToDelete) {
    checkArgument(keyIdToDelete != null);
    client.deleteApiKey(toolboxHttpHeaders.createHeaders(), keyIdToDelete);
  }

  public NewApiKey generateSingleUseKey() {
    return client.generateSingleUseKey(toolboxHttpHeaders.createHeaders());
  }

  /** To only be used in non-prod (support testing). */
  public void clearLockouts() {
    client.clearLockouts();
  }
}
