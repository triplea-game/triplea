package games.strategy.engine.lobby.server.userDB;

import java.util.Arrays;
import java.util.Collection;

import com.google.common.annotations.VisibleForTesting;

import games.strategy.engine.framework.startup.ui.InGameLobbyWatcher;
import games.strategy.util.Util;

/*
 * Note, this class is called by reflection: .
 * TODO: annotate this class and others to identify them. Longer term drop the reflection.
 */
public class DBUser {
  private final UserName userName;
  private final UserEmail userEmail;
  private final Role userRole;

  @VisibleForTesting
  static final Collection<String> forbiddenNameParts =
      Arrays.asList(InGameLobbyWatcher.LOBBY_WATCHER_NAME, "admin");

  public enum Role {
    NOT_ADMIN, ADMIN
  }

  /** Value object with validation methods. */
  public static class UserName {
    public final String userName;

    public UserName(final String userName) {
      this.userName = userName;
    }

    String validate() {
      if (userName == null || !userName.matches("[0-9a-zA-Z_-]+") || userName.length() <= 2) {
        return "Names must be at least 3 characters long and can only contain alpha numeric characters, -, and _";
      }
      if (userName.toLowerCase().contains(InGameLobbyWatcher.LOBBY_WATCHER_NAME.toLowerCase())) {
        return InGameLobbyWatcher.LOBBY_WATCHER_NAME + " cannot be part of a name";
      }
      if (userName.toLowerCase().contains("admin")) {
        return "Name can't contain the word admin";
      }
      return null;
    }
  }



  public static class UserEmail {
    public final String userEmail;

    public UserEmail(final String userEmail) {
      this.userEmail = userEmail;
    }

    String validate() {
      if (userEmail == null || userEmail.isEmpty() || !Util.isMailValid(userEmail)) {
        return "Invalid email address";
      }
      return null;
    }
  }

  /**
   *  Convenience constructor for non-admin users.
   */
  public DBUser(final UserName name, final UserEmail email) {
    this(name, email, Role.NOT_ADMIN);
  }


  public String getName() {
    return userName.userName;
  }

  public String getEmail() {
    return userEmail.userEmail;
  }

  public boolean isAdmin() {
    return userRole == Role.ADMIN;
  }

  /**
   * An all-args constructor.
   */
  public DBUser(final UserName name, final UserEmail email, final Role role) {
    this.userName = name;
    this.userEmail = email;
    this.userRole = role;
  }

  public boolean isValid() {
    return getValidationErrorMessage() == null;
  }

  public static boolean isValidUserName(final String userName) {
    return new UserName(userName).validate() == null;
  }

  /**
   * Returns an error message String if there are validation errors, otherwise null.
   */
  public String getValidationErrorMessage() {
    if (userName.validate() == null && userEmail.validate() == null) {
      return null;
    }
    return userName.validate() + " " + userEmail.validate();
  }

  /**
   * Example usage:
   * <pre><code>
   *   String proposedUserName = getUserInput();
   *   if(!DBUser.isValidUserName(proposedUserName)) {
   *     String validationErrorMessage =  DBUser.getUserNameValidationErrorMessage(proposedUserName();
   *     showMessageToUser("User name is invalid: " + validationErrorMessage);
   *   }
   * </code>
   * </pre>
   * @return Assuming an invalid user name - returns an error message String.
   * @throws IllegalStateException if the username is valid. Only call this method if 'isValidUserName()' return false
   */
  public static String getUserNameValidationErrorMessage(final String userName) {
    return new UserName(userName).validate();
  }


  @Override
  public String toString() {
    return "name: " + userName.userName
        + ", email: " + userEmail.userEmail
        + ", role: " + userRole;
  }
}
