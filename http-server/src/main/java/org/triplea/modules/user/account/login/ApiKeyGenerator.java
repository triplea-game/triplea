package org.triplea.modules.user.account.login;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.function.Function;
import javax.annotation.Nonnull;
import lombok.Builder;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.dao.api.key.ApiKeyDaoWrapper;
import org.triplea.domain.data.ApiKey;

// TODO: Project#12 test-me
@Builder
class ApiKeyGenerator implements Function<LoginRecord, ApiKey> {

  @Nonnull private final ApiKeyDaoWrapper apiKeyDaoWrapper;

  public static ApiKeyGenerator build(final Jdbi jdbi) {
    return ApiKeyGenerator.builder() //
        .apiKeyDaoWrapper(ApiKeyDaoWrapper.build(jdbi))
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
