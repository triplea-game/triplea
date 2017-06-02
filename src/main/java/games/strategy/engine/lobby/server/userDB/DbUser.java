package games.strategy.engine.lobby.server.userDB;

import com.google.common.base.Preconditions;

import games.strategy.engine.framework.startup.ui.InGameLobbyWatcher;
import games.strategy.util.Util;

/**
 * Value object representing data needed to create a user in database.
 */
public class DbUser {
  public final String name;
  public final String email;
  public final boolean admin;

  public enum Role {
    NOT_ADMIN, ADMIN
  }

  public static class UserName {
    public final String value;

    public UserName(String name) {
      this.value = name;
    }
  }

  public static class UserEmail {
    public final String value;

    public UserEmail(String email) {
      this.value = email;
    }
  }

  /**
   *  Convenience constructor for non-admin users.
   */
  public DbUser(UserName name, UserEmail email) {
    this(name, email, Role.NOT_ADMIN);
  }


  /**
   * An all-args constructor.
   */
  public DbUser(UserName name, UserEmail email, Role role) {
    this.name = name.value;
    this.email = email.value;
    this.admin = (role == Role.ADMIN);
  }

  public boolean isValid() {
    return getValidationErrorMessage() == null;
  }

  public String getValidationErrorMessage() {
    if (email == null || email.isEmpty() || !Util.isMailValid(email)) {
      return "Invalid email address";
    }

    return userNameValidation(name);
  }

  public static boolean isValidUserName(String userName) {
    return userNameValidation(userName) != null;
  }

  /**
   * @return null if user name is valid otherwise returns an error message String.
   */
  public static String getUserNameValidationErrorMessage(String userName) {
    Preconditions.checkState(!isValidUserName(userName));
    return userNameValidation(userName);
  }

  private static String userNameValidation(String userName) {

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

  @Override
  public String toString() {
    return "name: " + name
        + ", email: " + email
        + ", admin? " + admin;
  }

}
