package org.triplea.lobby.server.db;

/** Data access object for the users table. */
public interface UserDao {

  /** Returns null if the user does not exist. */
  HashedPassword getPassword(String username);

  boolean doesUserExist(String username);

  void createUser(String name, String email, HashedPassword password);

  boolean login(String username, String password);

  boolean isAdmin(String username);
}
