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
import org.triplea.db.dao.api.key.ApiKeyDaoWrapper;
import org.triplea.db.dao.api.key.ApiKeyLookupRecord;
import org.triplea.db.data.UserRole;
import org.triplea.domain.data.ApiKey;
import org.triplea.modules.TestData;

@ExtendWith(MockitoExtension.class)
class ApiKeyAuthenticatorTest {
  private static final ApiKey API_KEY = TestData.API_KEY;

  private static final ApiKeyLookupRecord PLAYER_DATA =
      ApiKeyLookupRecord.builder()
          .username("player-name")
          .role(UserRole.PLAYER)
          .userId(100)
          .build();

  private static final ApiKeyLookupRecord HOST_RECORD =
      ApiKeyLookupRecord.builder().role(UserRole.HOST).build();

  private static final ApiKeyLookupRecord ANONYMOUS_USER_RECORD =
      ApiKeyLookupRecord.builder().username("anonymous-user-name").role(UserRole.ANONYMOUS).build();

  @Mock private ApiKeyDaoWrapper apiKeyDao;

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
      final Optional<AuthenticatedUser> result, final ApiKeyLookupRecord apiKeyLookupRecord) {
    assertThat(
        result,
        isPresentAndIs(
            AuthenticatedUser.builder()
                .apiKey(API_KEY)
                .userId(apiKeyLookupRecord.getUserId())
                .name(apiKeyLookupRecord.getUsername())
                .userRole(apiKeyLookupRecord.getRole())
                .build()));
  }

  // TODO: Project#12 When host keys are split from user keys, we should never get a host
  // record from lobby api keys.
  @Test
  void hostKeyFound() {
    when(apiKeyDao.lookupByApiKey(API_KEY)).thenReturn(Optional.of(HOST_RECORD));

    final Optional<AuthenticatedUser> result = authenticator.authenticate(API_KEY.getValue());

    verify(result, HOST_RECORD);
  }

  @Test
  void anonymousUserKeyFound() {
    when(apiKeyDao.lookupByApiKey(API_KEY)).thenReturn(Optional.of(ANONYMOUS_USER_RECORD));

    final Optional<AuthenticatedUser> result = authenticator.authenticate(API_KEY.getValue());

    verify(result, ANONYMOUS_USER_RECORD);
  }
}
