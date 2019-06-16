package org.triplea.http.client.moderator.toolbox.register.key;

import java.net.URI;

import org.triplea.http.client.HttpClient;
import org.triplea.http.client.HttpInteractionException;
import org.triplea.http.client.moderator.toolbox.ApiKeyPassword;
import org.triplea.http.client.moderator.toolbox.ToolboxHttpHeaders;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * Http client to 'register' a single use key. This is the process of sending a single-use key and a new
 * api key password to the backend. The backend will 'consume' the single-use key, mark it as used, generate
 * a new key salted with the password provided and will then pass the new key to the front-end. Note, the backend
 * does not store the new key value, it hashes it with the password. Through this one-time process, once the
 * key leaves the backend server, only the front-end client will know the key value.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ToolboxRegisterNewKeyClient {

  public static final String REGISTER_API_KEY_PATH = "/moderator-toolbox/register-api-key";

  private final ToolboxRegisterNewKeyFeignClient client;

  public static ToolboxRegisterNewKeyClient newClient(final URI serverUri) {
    return new ToolboxRegisterNewKeyClient(
        new HttpClient<>(ToolboxRegisterNewKeyFeignClient.class, serverUri).get());
  }

  /**
   * Sends a new single-use key API to server for validation. If things go well we'll get back a new
   * API key that should be stored in client settings. The password sent along with this API key should
   * be sent along with the new API key for future validations.
   */
  public RegisterApiKeyResult registerNewKey(final ApiKeyPassword apiKeyPassword) {
    final ToolboxHttpHeaders headers = new ToolboxHttpHeaders(apiKeyPassword);

    try {
      return client.registerKey(headers.createHeaders());
    } catch (final HttpInteractionException e) {
      return RegisterApiKeyResult.builder()
          .errorMessage(e.getMessage())
          .build();
    }
  }
}
