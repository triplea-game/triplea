package org.triplea.modules.access.authentication;

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
import org.triplea.db.dao.api.key.GameHostingApiKeyDaoWrapper;
import org.triplea.db.dao.api.key.PlayerApiKeyDaoWrapper;
import org.triplea.db.dao.api.key.PlayerApiKeyLookupRecord;
import org.triplea.db.dao.user.role.UserRole;
import org.triplea.domain.data.ApiKey;
import org.triplea.modules.TestData;

@ExtendWith(MockitoExtension.class)
class ApiKeyAuthenticatorTest {
  private static final ApiKey API_KEY = TestData.API_KEY;

  private static final PlayerApiKeyLookupRecord PLAYER_DATA =
      PlayerApiKeyLookupRecord.builder()
          .username("player-name")
          .userRole(UserRole.PLAYER)
          .userId(100)
          .apiKeyId(123)
          .playerChatId("chat-id")
          .build();

  @Mock private PlayerApiKeyDaoWrapper apiKeyDao;
  @Mock private GameHostingApiKeyDaoWrapper gameHostingApiKeyDaoWrapper;

  @InjectMocks private ApiKeyAuthenticator authenticator;

  @Test
  void keyNotFound() {
    when(apiKeyDao.lookupByApiKey(API_KEY)).thenReturn(Optional.empty());
    when(gameHostingApiKeyDaoWrapper.isKeyValid(API_KEY)).thenReturn(false);

    final Optional<AuthenticatedUser> result = authenticator.authenticate(API_KEY.getValue());

    assertThat(result, isEmpty());
  }

  @Test
  void playerKeyFound() {
    when(apiKeyDao.lookupByApiKey(API_KEY)).thenReturn(Optional.of(PLAYER_DATA));

    final Optional<AuthenticatedUser> result = authenticator.authenticate(API_KEY.getValue());

    assertThat(
        result,
        isPresentAndIs(
            AuthenticatedUser.builder()
                .apiKey(API_KEY)
                .userId(PLAYER_DATA.getUserId())
                .name(PLAYER_DATA.getUsername())
                .userRole(PLAYER_DATA.getUserRole())
                .build()));
  }

  @Test
  void hostKeyFound() {
    when(apiKeyDao.lookupByApiKey(API_KEY)).thenReturn(Optional.empty());
    when(gameHostingApiKeyDaoWrapper.isKeyValid(API_KEY)).thenReturn(true);

    final Optional<AuthenticatedUser> result = authenticator.authenticate(API_KEY.getValue());

    assertThat(
        result,
        isPresentAndIs(
            AuthenticatedUser.builder().apiKey(API_KEY).userRole(UserRole.HOST).build()));
  }
}
