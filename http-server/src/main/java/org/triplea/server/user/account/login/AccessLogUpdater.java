package org.triplea.server.user.account.login;

import java.util.function.Consumer;
import lombok.Builder;

@Builder
class AccessLogUpdater implements Consumer<LoginRecord> {

  @Override
  public void accept(final LoginRecord loginRecord) {
    // TODO: Project#12 Implement
    throw new UnsupportedOperationException("TODO");
  }
}
