package org.triplea.lobby.server.db;

import org.triplea.lobby.server.db.dao.ModeratorAuditHistoryDao;
import org.triplea.lobby.server.db.dao.UserLookupDao;

/** Instances of this interface can be used to get an instance of any DB DAO. */
public interface DatabaseDao {
  UsernameBlacklistDao getUsernameBlacklistDao();

  UserBanDao getBannedMacDao();

  UserDao getUserDao();

  UserLookupDao getUserLookupDao();

  AccessLogDao getAccessLogDao();

  BadWordDao getBadWordDao();

  ModeratorAuditHistoryDao getModeratorAuditHistoryDao();
}
