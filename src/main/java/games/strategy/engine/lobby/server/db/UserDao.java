package games.strategy.engine.lobby.server.db;

import games.strategy.engine.lobby.server.userDB.DBUser;

public interface UserDao {

  HashedPassword getPassword(String username);

  HashedPassword getMd5Password(String username);

  boolean doesUserExist(String username);

  void updateUser(DBUser user, HashedPassword password);

  void createUser(DBUser user, HashedPassword password);

  boolean login(String username, HashedPassword password);

  DBUser getUserByName(String username);
}
