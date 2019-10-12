package org.triplea.server.access;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresent;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.lobby.server.db.dao.ApiKeyDao;
import org.triplea.lobby.server.db.data.ApiKeyUserData;
import org.triplea.server.TestData;

@ExtendWith(MockitoExtension.class)
class ApiKeyAuthenticatorTest {
  private static final String API_KEY = TestData.API_KEY.getValue();

  private static final ApiKeyUserData USER_DATA =
      ApiKeyUserData.builder().role("role").userId(100).build();

  @Mock private ApiKeyDao apiKeyDao;

  @InjectMocks private ApiKeyAuthenticator authenticator;

  @Test
  void keyNotFound() {
    when(apiKeyDao.lookupByApiKey(API_KEY)).thenReturn(Optional.empty());

    final Optional<AuthenticatedUser> result = authenticator.authenticate(API_KEY);

    assertThat(result, isEmpty());
  }

  @Test
  void keyFound() {
    when(apiKeyDao.lookupByApiKey(API_KEY)).thenReturn(Optional.of(USER_DATA));

    final Optional<AuthenticatedUser> result = authenticator.authenticate(API_KEY);

    assertThat(result, isPresent());
    assertThat(result.get().getUserId(), is(USER_DATA.getUserId()));
    assertThat(result.get().getUserRole(), is(USER_DATA.getRole()));
  }
}
