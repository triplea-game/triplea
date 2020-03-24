package org.triplea.modules.access.authentication;

import io.dropwizard.auth.Authenticator;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.triplea.db.dao.api.key.ApiKeyDaoWrapper;
import org.triplea.domain.data.ApiKey;

/**
 * Verifies a 'bearer' token API key is valid. This means checking if the key is in database, if so
 * we return an {@code AuthenticatedUser} otherwise optional. Anonymous users will have a null
 * user-id and role of 'ANONYMOUS'.
 */
@AllArgsConstructor
public class ApiKeyAuthenticator implements Authenticator<String, AuthenticatedUser> {

  private final ApiKeyDaoWrapper apiKeyDaoWrapper;

  @Override
  public Optional<AuthenticatedUser> authenticate(final String apiKey) {
    return apiKeyDaoWrapper
        .lookupByApiKey(ApiKey.of(apiKey))
        .map(
            userData ->
                AuthenticatedUser.builder()
                    .userId(userData.getUserId())
                    .name(userData.getUsername())
                    .userRole(userData.getRole())
                    .apiKey(ApiKey.of(apiKey))
                    .build());
  }
}
