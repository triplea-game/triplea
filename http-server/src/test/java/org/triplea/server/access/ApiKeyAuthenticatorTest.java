package org.triplea.server.access;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.domain.data.ApiKey;
import org.triplea.lobby.server.db.dao.api.key.LobbyApiKeyDaoWrapper;
import org.triplea.lobby.server.db.data.ApiKeyUserData;
import org.triplea.lobby.server.db.data.UserRole;
import org.triplea.server.TestData;

@ExtendWith(MockitoExtension.class)
class ApiKeyAuthenticatorTest {
  private static final ApiKey API_KEY = TestData.API_KEY;

  private static final ApiKeyUserData PLAYER_DATA =
      ApiKeyUserData.builder().username("player-name").role(UserRole.PLAYER).userId(100).build();

  private static final ApiKeyUserData HOST_DATA =
      ApiKeyUserData.builder().role(UserRole.HOST).build();

  private static final ApiKeyUserData ANONYMOUS_USER_DATA =
      ApiKeyUserData.builder().username("anonymous-user-name").role(UserRole.ANONYMOUS).build();

  @Mock private LobbyApiKeyDaoWrapper apiKeyDao;

  @InjectMocks private ApiKeyAuthenticator authenticator;

  @Test
  void keyNotFound() {
    when(apiKeyDao.lookupByApiKey(API_KEY)).thenReturn(Optional.empty());

    final Optional<AuthenticatedUser> result = authenticator.authenticate(API_KEY.getValue());

    assertThat(result, isEmpty());
  }

  @Test
  void playerKeyFound() {
    when(apiKeyDao.lookupByApiKey(API_KEY)).thenReturn(Optional.of(PLAYER_DATA));

    final Optional<AuthenticatedUser> result = authenticator.authenticate(API_KEY.getValue());

    verify(result, PLAYER_DATA);
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private static void verify(
      final Optional<AuthenticatedUser> result, final ApiKeyUserData userData) {
    assertThat(
        result,
        isPresentAndIs(
            AuthenticatedUser.builder()
                .apiKey(API_KEY)
                .userId(userData.getUserId())
                .name(userData.getUsername())
                .userRole(userData.getRole())
                .build()));
  }

  @Test
  void hostKeyFound() {
    when(apiKeyDao.lookupByApiKey(API_KEY)).thenReturn(Optional.of(HOST_DATA));

    final Optional<AuthenticatedUser> result = authenticator.authenticate(API_KEY.getValue());

    verify(result, HOST_DATA);
  }

  @Test
  void anonymousUserKeyFound() {
    when(apiKeyDao.lookupByApiKey(API_KEY)).thenReturn(Optional.of(ANONYMOUS_USER_DATA));

    final Optional<AuthenticatedUser> result = authenticator.authenticate(API_KEY.getValue());

    verify(result, ANONYMOUS_USER_DATA);
  }
}
