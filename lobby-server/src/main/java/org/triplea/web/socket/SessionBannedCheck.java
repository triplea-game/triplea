package org.triplea.web.socket;

import com.google.common.annotations.VisibleForTesting;
import java.util.function.Predicate;
import javax.websocket.Session;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.dao.user.ban.UserBanDao;

@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @VisibleForTesting)
public class SessionBannedCheck implements Predicate<Session> {
  private final UserBanDao userBanDao;

  public static SessionBannedCheck build(final Jdbi jdbi) {
    return new SessionBannedCheck(jdbi.onDemand(UserBanDao.class));
  }

  @Override
  public boolean test(final Session session) {
    final String ip = InetExtractor.extract(session.getUserProperties()).getHostAddress();
    return userBanDao.isBannedByIp(ip);
  }
}
