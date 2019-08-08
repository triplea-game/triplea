package org.triplea.server.moderator.toolbox.api.key;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.moderator.toolbox.api.key.ApiKeyData;
import org.triplea.lobby.server.db.dao.ModeratorApiKeyDao;
import org.triplea.lobby.server.db.data.ApiKeyDaoData;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {
  private static final String KEY_ID = "a key id";
  private static final int MODERATOR_ID = 6363;
  private static final ApiKeyDaoData API_KEY_DAO_DATA =
      ApiKeyDaoData.builder()
          .lastUsed(Instant.now())
          .lastUsedByHostAddress("last used host")
          .publicId("public id")
          .build();

  @Mock private ModeratorApiKeyDao moderatorApiKeyDao;

  @InjectMocks private ApiKeyService apiKeyService;

  @Test
  void getKeys() {
    when(moderatorApiKeyDao.getKeysByUserId(MODERATOR_ID))
        .thenReturn(singletonList(API_KEY_DAO_DATA));

    final List<ApiKeyData> results = apiKeyService.getKeys(MODERATOR_ID);

    assertThat(results, hasSize(1));
    assertThat(results.get(0).getLastUsed(), is(API_KEY_DAO_DATA.getLastUsed()));
    assertThat(results.get(0).getPublicId(), is(API_KEY_DAO_DATA.getPublicId()));
    assertThat(results.get(0).getLastUsedIp(), is(API_KEY_DAO_DATA.getLastUsedByHostAddress()));
  }

  @Test
  void deleteKeyThrowsIfNothingDeleted() {
    when(moderatorApiKeyDao.deleteKey(KEY_ID)).thenReturn(0);

    assertThrows(IllegalStateException.class, () -> apiKeyService.deleteKey(KEY_ID));
  }

  @Test
  void deleteKey() {
    when(moderatorApiKeyDao.deleteKey(KEY_ID)).thenReturn(1);
    apiKeyService.deleteKey(KEY_ID);
  }
}
