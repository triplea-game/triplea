package org.triplea.lobby.server.api.key;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.engine.lobby.PlayerName;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.ApiKey;
import org.triplea.lobby.server.db.dao.ApiKeyDao;
import org.triplea.lobby.server.db.dao.UserJdbiDao;

@ExtendWith(MockitoExtension.class)
class ApiKeyGeneratorTest {

  private static final PlayerName PLAYER_NAME = PlayerName.of("Why does the gold scream?");
  private static final ApiKey API_KEY = ApiKey.of("Yuck, mighty amnesty!");
  private static final int USER_ID = 10_000;

  @Mock private ApiKeyDao apiKeyDao;
  @Mock private UserJdbiDao userJdbiDao;
  private Supplier<ApiKey> keyMaker = () -> API_KEY;

  private ApiKeyGenerator apiKeyGenerator;

  @BeforeEach
  void setup() {
    apiKeyGenerator =
        ApiKeyGenerator.builder()
            .apiKeyDao(apiKeyDao)
            .userDao(userJdbiDao)
            .keyMaker(keyMaker)
            .build();
  }

  @Test
  void verifyApiKeyGenerator() {
    assertThat(
        "Simple check that we get no errors and two keys generated are not equal",
        ApiKeyGenerator.createKeyMaker().get(),
        not(equalTo(ApiKeyGenerator.createKeyMaker().get())));
  }

  @Test
  void generateKeyForRegisteredUser() {
    when(userJdbiDao.lookupUserIdByName(PLAYER_NAME.getValue())).thenReturn(Optional.of(USER_ID));

    final ApiKey result = apiKeyGenerator.apply(PLAYER_NAME);

    assertThat(result, is(API_KEY));
    verify(apiKeyDao).storeKey(USER_ID, API_KEY.getValue());
  }

  @Test
  void generateKeyForAnonymousUser() {
    when(userJdbiDao.lookupUserIdByName(PLAYER_NAME.getValue())).thenReturn(Optional.empty());

    final ApiKey result = apiKeyGenerator.apply(PLAYER_NAME);

    assertThat(result, is(API_KEY));
    verify(apiKeyDao).storeKey(null, API_KEY.getValue());
  }
}
