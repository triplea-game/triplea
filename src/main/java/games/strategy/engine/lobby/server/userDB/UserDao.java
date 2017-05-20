package games.strategy.engine.lobby.server.userDB;

import games.strategy.engine.framework.startup.ui.InGameLobbyWatcher;
import games.strategy.util.MD5Crypt;
import games.strategy.util.Util;

public interface UserDao {
  /**
   * @return if this user is valid.
   */
  static String validate(String userName, String email, String hashedPassword) {
    if (email == null || !Util.isMailValid(email)) {
      return "Invalid email address";
    }
    if (hashedPassword == null || hashedPassword.length() < 3 || !hashedPassword.startsWith(MD5Crypt.MAGIC)) {
      return "Invalid password";
    }
    return validateUserName(userName);
  }

  static String validateUserName(String userName) {
    // is this a valid user?
    if (userName == null || !userName.matches("[0-9a-zA-Z_-]+") || userName.length() <= 2) {
      return "Usernames must be at least 3 characters long and can only contain alpha numeric characters, -, and _";
    }
    if (userName.contains(InGameLobbyWatcher.LOBBY_WATCHER_NAME)) {
      return InGameLobbyWatcher.LOBBY_WATCHER_NAME + " cannot be part of a name";
    }
    if (userName.toLowerCase().contains("admin")) {
      return "Username can't contain the word admin";
    }
    return null;
  }


  String getPassword(String userName);

  boolean doesUserExist(String userName);

  void updateUser(String name, String email, String hashedPassword, boolean admin);

  void createUser(String name, String email, String hashedPassword, boolean admin);

  boolean login(String userName, String hashedPassword);

  DBUser getUser(String userName);
}
