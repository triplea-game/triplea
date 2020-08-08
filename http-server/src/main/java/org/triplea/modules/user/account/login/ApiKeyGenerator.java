package org.triplea.modules.user.account.login;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.function.Function;
import javax.annotation.Nonnull;
import lombok.Builder;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.dao.api.key.PlayerApiKeyDaoWrapper;
import org.triplea.domain.data.ApiKey;

@Builder
class ApiKeyGenerator implements Function<LoginRecord, ApiKey> {

  @Nonnull private final PlayerApiKeyDaoWrapper apiKeyDaoWrapper;

  public static ApiKeyGenerator build(final Jdbi jdbi) {
    return ApiKeyGenerator.builder() //
        .apiKeyDaoWrapper(PlayerApiKeyDaoWrapper.build(jdbi))
        .build();
  }

  @Override
  public ApiKey apply(final LoginRecord loginRecord) {
    try {
      return apiKeyDaoWrapper.newKey(
          loginRecord.getUserName(),
          InetAddress.getByName(loginRecord.getIp()),
          loginRecord.getSystemId(),
          loginRecord.getPlayerChatId());
    } catch (final UnknownHostException e) {
      throw new IllegalStateException(
          "Unexpected exception for IP address: " + loginRecord.getIp(), e);
    }
  }
}
