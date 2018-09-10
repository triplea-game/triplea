package games.strategy.engine.lobby.server.userDB;

import java.io.Serializable;

import org.triplea.lobby.common.LobbyConstants;

import games.strategy.util.Util;
import lombok.EqualsAndHashCode;
import lombok.ToString;

// TODO: move this class to lobby.common upon next incompatible release; it is shared between client and server

/**
 * A lobby user.
 */
@EqualsAndHashCode
@ToString
public final class DBUser implements Serializable {
  private static final long serialVersionUID = -5289923058375302916L;

  private final String m_name;
  private final String m_email;
  private final Role userRole;

  /**
   * The user's role within the lobby.
   */
  public enum Role {
    NOT_ADMIN, ADMIN
  }

  /** User name value object with validation methods. */
  public static class UserName {
    public final String userName;

    public UserName(final String userName) {
      this.userName = userName;
    }

    String validate() {
      if (userName == null || !userName.matches("[0-9a-zA-Z_-]+") || userName.length() <= 2) {
        return "Names must be at least 3 characters long and can only contain alpha numeric characters, -, and _";
      }
      if (userName.toLowerCase().contains(LobbyConstants.LOBBY_WATCHER_NAME.toLowerCase())) {
        return LobbyConstants.LOBBY_WATCHER_NAME + " cannot be part of a name";
      }
      if (userName.toLowerCase().contains(LobbyConstants.ADMIN_USERNAME.toLowerCase())) {
        return "Name can't contain the word " + LobbyConstants.ADMIN_USERNAME;
      }
      return null;
    }
  }

  /**
   * User email value object with validation methods.
   */
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
    if (new UserName(m_name).validate() == null && new UserEmail(m_email).validate() == null) {
      return null;
    }
    return new UserName(m_name).validate() + " " + new UserEmail(m_email).validate();
  }

  /**
   * Returns an error message String if {@code userName} is not valid; otherwise {@code null}. Example usage:
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
}
