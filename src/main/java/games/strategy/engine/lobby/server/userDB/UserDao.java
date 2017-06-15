package games.strategy.engine.lobby.server.userDB;

public interface UserDao {

  HashedPassword getPassword(String userName);

  boolean doesUserExist(String userName);

  void updateUser(DBUser user, HashedPassword password);

  void createUser(DBUser user, HashedPassword password);

  boolean login(String userName, HashedPassword password);

  DBUser getUserByName(String userName);
}
