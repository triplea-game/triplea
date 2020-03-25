package org.triplea.modules.user.account.login;

import java.util.function.Consumer;
import javax.annotation.Nonnull;
import lombok.Builder;
import org.triplea.db.dao.access.log.AccessLogDao;
import org.triplea.java.Postconditions;

@Builder
class AccessLogUpdater implements Consumer<LoginRecord> {

  @Nonnull private final AccessLogDao accessLogDao;

  @Override
  public void accept(final LoginRecord loginRecord) {
    final int updateCount =
        loginRecord.isRegistered()
            ? accessLogDao.insertRegisteredUserRecord(
                loginRecord.getUserName().getValue(),
                loginRecord.getIp(),
                loginRecord.getSystemId().getValue())
            : accessLogDao.insertAnonymousUserRecord(
                loginRecord.getUserName().getValue(),
                loginRecord.getIp(),
                loginRecord.getSystemId().getValue());

    Postconditions.assertState(updateCount == 1);
  }
}
