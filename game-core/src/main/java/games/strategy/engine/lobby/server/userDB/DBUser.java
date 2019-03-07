// CHECKSTYLE-OFF: PackageName
// move this class to lobby.common upon next lobby-incompatible release; it is shared between client and server

package games.strategy.engine.lobby.server.userDB;

import java.io.Serializable;

import org.triplea.java.StringUtils;
import org.triplea.lobby.common.LobbyConstants;

import com.google.common.annotations.VisibleForTesting;

import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * A lobby user.
 */
@EqualsAndHashCode
@SuppressWarnings("checkstyle:AbbreviationAsWordInName") // rename upon next lobby-incompatible release
@ToString
public final class DBUser implements Serializable {
  private static final long serialVersionUID = -5289923058375302916L;

  @SuppressWarnings("checkstyle:MemberName") // rename upon next lobby-incompatible release
  private final String m_name;
  @SuppressWarnings("checkstyle:MemberName") // rename upon next lobby-incompatible release
  private final String m_email;
  private final Role userRole;

  public DBUser(final UserName name, final UserEmail email, final Role role) {
    this.m_name = name.userName;
    this.m_email = email.userEmail;
    this.userRole = role;
  }

  /**
   * The user's role within the lobby.
   */
  public enum Role {
    NOT_ADMIN, ADMIN
  }

  /** User name value object with validation methods. */
  public static class UserName {
    @VisibleForTesting
    public static final int MAX_LENGTH = 40;
    private static final int MIN_LENGTH = 3;

    public final String userName;

    public UserName(final String userName) {
      this.userName = userName;
    }

    String validate() {
      if ((userName == null) || (userName.length() < MIN_LENGTH)) {
        return "Name is too short (minimum " + MIN_LENGTH + " characters)";
      } else if (userName.length() > MAX_LENGTH) {
        return "Name is too long (maximum " + MAX_LENGTH + " characters)";
      } else if (!userName.matches("[0-9a-zA-Z_-]+")) {
        return "Name can only contain alphanumeric characters, hyphens (-), and underscores (_)";
      } else if (userName.toLowerCase().contains(LobbyConstants.LOBBY_WATCHER_NAME.toLowerCase())) {
        return LobbyConstants.LOBBY_WATCHER_NAME + " cannot be part of a name";
      } else if (userName.toLowerCase().contains(LobbyConstants.ADMIN_USERNAME.toLowerCase())) {
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
      if (userEmail == null || userEmail.isEmpty() || !StringUtils.isMailValid(userEmail)) {
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
