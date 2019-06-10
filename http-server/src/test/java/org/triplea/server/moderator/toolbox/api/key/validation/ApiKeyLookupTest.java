package org.triplea.server.moderator.toolbox.api.key.validation;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.lobby.server.db.ApiKeyDao;

@ExtendWith(MockitoExtension.class)
class ApiKeyLookupTest {

  private static final String API_KEY = "api-key";
  private static final String HASHED_KEY = "hashed-key";
  private static final int MODERATOR_ID = 999;


  @Mock
  private ApiKeyDao apiKeyDao;
  @Mock
  private Function<String, String> hashFunction;

  @InjectMocks
  private ApiKeyLookup apiKeyLookup;


  @Test
  void keyNotFound() {
    when(hashFunction.apply(API_KEY)).thenReturn(HASHED_KEY);
    when(apiKeyDao.lookupModeratorIdByApiKey(HASHED_KEY)).thenReturn(Optional.empty());

    assertThat(apiKeyLookup.apply(API_KEY), isEmpty());
  }

  @Test
  void keyFound() {
    when(hashFunction.apply(API_KEY)).thenReturn(HASHED_KEY);
    when(apiKeyDao.lookupModeratorIdByApiKey(HASHED_KEY)).thenReturn(Optional.of(MODERATOR_ID));

    assertThat(apiKeyLookup.apply(API_KEY), isPresentAndIs(MODERATOR_ID));
  }
}
