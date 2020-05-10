package org.triplea.modules.access.authentication;

import com.google.common.annotations.VisibleForTesting;
import io.dropwizard.auth.Authenticator;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.dao.api.key.GameHostingApiKeyDaoWrapper;
import org.triplea.db.dao.api.key.PlayerApiKeyDaoWrapper;
import org.triplea.db.dao.user.role.UserRole;
import org.triplea.domain.data.ApiKey;

/**
 * Verifies a 'bearer' token API key is valid. This means checking if the key is in database, if so
 * we return an {@code AuthenticatedUser} otherwise optional. Anonymous users will have a null
 * user-id and role of 'ANONYMOUS'.
 */
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @VisibleForTesting)
public class ApiKeyAuthenticator implements Authenticator<String, AuthenticatedUser> {

  private final PlayerApiKeyDaoWrapper apiKeyDaoWrapper;
  private final GameHostingApiKeyDaoWrapper gameHostingApiKeyDaoWrapper;

  public static ApiKeyAuthenticator build(final Jdbi jdbi) {
    return new ApiKeyAuthenticator(
        PlayerApiKeyDaoWrapper.build(jdbi), GameHostingApiKeyDaoWrapper.build(jdbi));
  }

  @Override
  public Optional<AuthenticatedUser> authenticate(final String apiKey) {
    final ApiKey key = ApiKey.of(apiKey);
    return apiKeyDaoWrapper
        .lookupByApiKey(key)
        .map(
            userData ->
                AuthenticatedUser.builder()
                    .userId(userData.getUserId())
                    .name(userData.getUsername())
                    .userRole(userData.getRole())
                    .apiKey(key)
                    .build())
        .or(
            () ->
                gameHostingApiKeyDaoWrapper.isKeyValid(key)
                    ? Optional.of(
                        AuthenticatedUser.builder() //
                            .userRole(UserRole.HOST)
                            .apiKey(key)
                            .build())
                    : Optional.empty());
  }
}
