package games.strategy.engine.lobby.server.userDB;

public interface UserDao {

  HashedPassword getPassword(String userName);

  boolean doesUserExist(String userName);

  void updateUser(DbUser user, HashedPassword password);

  void createUser(DbUser user, HashedPassword password);

  boolean login(String userName, HashedPassword password);

  DbUser getUserByName(String userName);
}
