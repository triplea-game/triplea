package games.strategy.engine.lobby.server.db;

import games.strategy.engine.lobby.server.userDB.DBUser;

public interface UserDao {

  /**
   * @return null if the user does not exist.
   */
  HashedPassword getPassword(String username);

  /**
   * @return null if the user does not exist or the user has no legacy password stored.
   */
  HashedPassword getLegacyPassword(String username);

  boolean doesUserExist(String username);

  void updateUser(DBUser user, HashedPassword password);

  void createUser(DBUser user, HashedPassword password);

  boolean login(String username, HashedPassword password);

  DBUser getUserByName(String username);
}
