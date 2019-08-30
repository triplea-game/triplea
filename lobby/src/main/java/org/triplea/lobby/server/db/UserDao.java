package org.triplea.lobby.server.db;

/** Data access object for the users table. */
public interface UserDao {

  /** Returns null if the user does not exist. */
  HashedPassword getPassword(String username);

  boolean doesUserExist(String username);

  void updateUser(String name, String email, HashedPassword password);

  void createUser(String name, String email, HashedPassword password);

  boolean login(String username, String password);

  String getUserEmailByName(String username);

  boolean isAdmin(String username);
}
