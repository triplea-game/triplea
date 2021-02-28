package org.triplea.web.socket;

import com.google.common.annotations.VisibleForTesting;
import java.net.InetAddress;
import java.util.function.Predicate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.dao.user.ban.UserBanDao;

@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @VisibleForTesting)
public class SessionBannedCheck implements Predicate<InetAddress> {
  private final UserBanDao userBanDao;

  public static SessionBannedCheck build(final Jdbi jdbi) {
    return new SessionBannedCheck(jdbi.onDemand(UserBanDao.class));
  }

  @Override
  public boolean test(final InetAddress remoteAddress) {
    final String ip = remoteAddress.getHostAddress();
    return userBanDao.isBannedByIp(ip);
  }
}
