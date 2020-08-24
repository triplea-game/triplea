package org.triplea.db.dao.api.key;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.IpAddressParser;

@ExtendWith(MockitoExtension.class)
class GameHostingApiKeyDaoWrapperTest {

  @Mock private GameHostingApiKeyDao gameHostApiKeyDao;

  @Mock private Function<ApiKey, String> keyHashingFunction;

  @Mock private Supplier<ApiKey> keyMaker;

  @InjectMocks private GameHostingApiKeyDaoWrapper gameHostingApiKeyDaoWrapper;

  @Nested
  class IsKeyValid {
    @Test
    void valid() {
      when(keyHashingFunction.apply(ApiKey.of("valid-key"))).thenReturn("hashed-valid-key");
      when(gameHostApiKeyDao.keyExists("hashed-valid-key")).thenReturn(true);

      assertThat(gameHostingApiKeyDaoWrapper.isKeyValid(ApiKey.of("valid-key")), is(true));
    }

    @Test
    void notValid() {
      when(keyHashingFunction.apply(ApiKey.of("not-valid-key"))).thenReturn("hashed-not-valid-key");
      when(gameHostApiKeyDao.keyExists("hashed-not-valid-key")).thenReturn(false);

      assertThat(gameHostingApiKeyDaoWrapper.isKeyValid(ApiKey.of("not-valid-key")), is(false));
    }
  }

  @Nested
  class NewGameHostKey {
    @Test
    void verifyCreatingNewKey() {
      when(keyMaker.get()).thenReturn(ApiKey.of("api-key"));
      when(keyHashingFunction.apply(ApiKey.of("api-key"))).thenReturn("hashed-key");
      when(gameHostApiKeyDao.insertKey("hashed-key", "1.1.1.1")).thenReturn(1);

      final ApiKey result =
          gameHostingApiKeyDaoWrapper.newGameHostKey(IpAddressParser.fromString("1.1.1.1"));

      assertThat(result, is(ApiKey.of("api-key")));
    }
  }
}
