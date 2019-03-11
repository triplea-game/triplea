package org.triplea.lobby.server.db;


/**
 * Instances of this interface can be used to get an instance of any DB DAO.
 */
public interface DatabaseDao {
  BannedUsernameDao getBannedUsernameDao();

  BannedMacDao getBannedMacDao();

  UserDao getUserDao();

  MutedUsernameDao getMutedUsernameDao();

  MutedMacDao getMutedMacDao();

  BadWordDao getBadWordDao();
}
