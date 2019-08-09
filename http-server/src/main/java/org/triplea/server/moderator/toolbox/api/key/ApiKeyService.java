package org.triplea.server.moderator.toolbox.api.key;

import com.google.common.base.Preconditions;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.Builder;
import org.triplea.http.client.moderator.toolbox.api.key.ApiKeyData;
import org.triplea.lobby.server.db.dao.ModeratorApiKeyDao;

@Builder
class ApiKeyService {
  @Nonnull private ModeratorApiKeyDao apiKeyDao;

  List<ApiKeyData> getKeys(final int moderatorUserId) {
    return apiKeyDao.getKeysByUserId(moderatorUserId).stream()
        .map(
            daoData ->
                ApiKeyData.builder()
                    .publicId(daoData.getPublicId())
                    .lastUsed(daoData.getLastUsed())
                    .lastUsedIp(daoData.getLastUsedByHostAddress())
                    .build())
        .collect(Collectors.toList());
  }

  void deleteKey(final String keyId) {
    Preconditions.checkState(apiKeyDao.deleteKey(keyId) == 1);
  }
}
