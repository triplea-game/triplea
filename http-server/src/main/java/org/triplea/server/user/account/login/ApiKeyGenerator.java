package org.triplea.server.user.account.login;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.function.Function;
import javax.annotation.Nonnull;
import lombok.Builder;
import org.triplea.domain.data.ApiKey;
import org.triplea.lobby.server.db.dao.api.key.ApiKeyDaoWrapper;

// TODO: Project#12 test-me
@Builder
class ApiKeyGenerator implements Function<LoginRecord, ApiKey> {

  @Nonnull private final ApiKeyDaoWrapper apiKeyDaoWrapper;

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
