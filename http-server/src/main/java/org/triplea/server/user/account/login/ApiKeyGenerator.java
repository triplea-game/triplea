package org.triplea.server.user.account.login;

import java.util.function.Function;
import lombok.Builder;
import org.triplea.domain.data.ApiKey;

@Builder
class ApiKeyGenerator implements Function<LoginRecord, ApiKey> {

  @Override
  public ApiKey apply(final LoginRecord loginRecord) {
    // TODO: Project#12 implement
    throw new UnsupportedOperationException("TODO");
  }
}
