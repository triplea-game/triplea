package games.strategy.engine.lobby.server.db;

import games.strategy.engine.lobby.server.userDB.DBUser;

public interface UserDao {

  HashedPassword getPassword(String userName);

  boolean doesUserExist(String userName);

  void updateUser(DBUser user, HashedPassword password);
  
  void updateUser(DBUser user, String password);

  void createUser(DBUser user, HashedPassword password);

  void createUser(DBUser user, String password);

  boolean login(String userName, HashedPassword password);

  boolean login(String userName, String password);

  DBUser getUserByName(String userName);
}
