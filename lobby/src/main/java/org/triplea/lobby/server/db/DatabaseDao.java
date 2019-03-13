package org.triplea.lobby.server.db;


/**
 * Instances of this interface can be used to get an instance of any DB DAO.
 */
public interface DatabaseDao {
  PlayerNameBlackListDao getPlayerNameBlackListDao();

  BannedMacDao getBannedMacDao();

  UserDao getUserDao();

  MutedMacDao getMutedMacDao();

  BadWordDao getBadWordDao();
}
