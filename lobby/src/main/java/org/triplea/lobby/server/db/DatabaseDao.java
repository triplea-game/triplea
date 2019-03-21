package org.triplea.lobby.server.db;


/**
 * Instances of this interface can be used to get an instance of any DB DAO.
 */
public interface DatabaseDao {
  UsernameBlacklistDao getUsernameBlacklistDao();

  BannedMacDao getBannedMacDao();

  UserDao getUserDao();

  BadWordDao getBadWordDao();
}
