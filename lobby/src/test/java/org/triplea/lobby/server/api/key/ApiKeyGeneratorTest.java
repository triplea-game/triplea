package org.triplea.lobby.server.api.key;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.mockito.Mockito.verify;

import games.strategy.engine.lobby.ApiKey;
import games.strategy.engine.lobby.PlayerName;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.lobby.server.db.dao.ApiKeyDao;

@ExtendWith(MockitoExtension.class)
class ApiKeyGeneratorTest {

  private static final PlayerName PLAYER_NAME = PlayerName.of("Why does the gold scream?");
  private static final ApiKey API_KEY = ApiKey.of("Yuck, mighty amnesty!");

  @Mock private ApiKeyDao apiKeyDao;
  private Supplier<ApiKey> keyMaker = () -> API_KEY;

  private ApiKeyGenerator apiKeyGenerator;

  @BeforeEach
  void setup() {
    apiKeyGenerator = ApiKeyGenerator.builder().apiKeyDao(apiKeyDao).keyMaker(keyMaker).build();
  }

  @Test
  void verifyApiKeyGenerator() {
    assertThat(
        "Simple check that we get no errors and two keys generated are not equal",
        ApiKeyGenerator.createKeyMaker().get(),
        not(equalTo(ApiKeyGenerator.createKeyMaker().get())));
  }

  @Test
  void generateKey() {
    final ApiKey result = apiKeyGenerator.apply(PLAYER_NAME);

    assertThat(result, is(API_KEY));
    verify(apiKeyDao).storeKey(PLAYER_NAME.getValue(), API_KEY.getValue());
  }
}
