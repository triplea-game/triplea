package games.strategy.engine.lobby.server.userDB;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

import com.google.common.annotations.VisibleForTesting;

import games.strategy.engine.framework.startup.ui.InGameLobbyWatcher;
import games.strategy.util.Util;

/*
 * Note, the DBUser data type is passed between lobby and client.
 * TODO: annotate this class and others to identify them. Longer term drop the reflection.
 */
public final class DBUser implements Serializable {
  private static final long serialVersionUID = -5289923058375302916L;

  private final String m_name;
  private final String m_email;
  private final Role userRole;


  @VisibleForTesting
  static final Collection<String> forbiddenNameParts = Arrays.asList(InGameLobbyWatcher.LOBBY_WATCHER_NAME, "admin");

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
      if ((userName == null) || !userName.matches("[0-9a-zA-Z_-]+") || (userName.length() <= 2)) {
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
      if ((userEmail == null) || userEmail.isEmpty() || !Util.isMailValid(userEmail)) {
        return "Invalid email address";
      }
      return null;
    }
  }

  /**
   * Convenience constructor for non-admin users.
   */
  public DBUser(final UserName name, final UserEmail email) {
    this(name, email, Role.NOT_ADMIN);
  }


  public String getName() {
    return m_name;
  }

  public String getEmail() {
    return m_email;
  }

  public boolean isAdmin() {
    return userRole == Role.ADMIN;
  }

  /**
   * An all-args constructor.
   */
  public DBUser(final UserName name, final UserEmail email, final Role role) {
    this.m_name = name.userName;
    this.m_email = email.userEmail;
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
    if ((new UserName(m_name).validate() == null) && (new UserEmail(m_email).validate() == null)) {
      return null;
    }
    return new UserName(m_name).validate() + " " + new UserEmail(m_email).validate();
  }

  /**
   * Example usage:
   *
   * <pre>
   * <code>
   *   String proposedUserName = getUserInput();
   *   if(!DBUser.isValidUserName(proposedUserName)) {
   *     String validationErrorMessage =  DBUser.getUserNameValidationErrorMessage(proposedUserName();
   *     showMessageToUser("User name is invalid: " + validationErrorMessage);
   *   }
   * </code>
   * </pre>
   *
   * @return Assuming an invalid user name - returns an error message String.
   * @throws IllegalStateException if the username is valid. Only call this method if 'isValidUserName()' return false
   */
  public static String getUserNameValidationErrorMessage(final String userName) {
    return new UserName(userName).validate();
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this) {
      return true;
    } else if (!(obj instanceof DBUser)) {
      return false;
    }

    final DBUser other = (DBUser) obj;
    return Objects.equals(m_email, other.m_email)
        && Objects.equals(m_name, other.m_name)
        && (userRole == other.userRole);
  }

  @Override
  public int hashCode() {
    return Objects.hash(m_email, m_name, userRole);
  }

  @Override
  public String toString() {
    return "name: " + m_name
        + ", email: " + m_email
        + ", role: " + userRole;
  }
}
