package games.strategy.engine.lobby.client.login;

import lombok.Builder;
import lombok.Getter;
import org.triplea.domain.data.ApiKey;
import org.triplea.domain.data.UserName;

/**
 * Data object representing a successful login to lobby and the data we received back from the lobby
 * after logging in.
 */
@Builder
@Getter
public class LoginResult {
  /**
   * After a successful login the lobby generates and returns an API-Key. On subsequent transactions
   * with the lobby we can send this API-Key instead of logging in again.
   */
  private final ApiKey apiKey;
  /** The name of the user that has logged in. */
  private final UserName username;
  /**
   * Anonymous login is a login without an account, an unregistered login. No password is required.
   */
  private final boolean anonymousLogin;
  /**
   * Moderator flag indicates if the user is a moderator, this comes from a lookup on the lobby side
   * and the lobby tells us if this user is a moderator.
   */
  private final boolean moderator;
  /**
   * If the user is using a temporary password, the lobby will set the password change required flag
   * in its response to indicate the user should be prompted with a password change prompt.
   */
  private final boolean passwordChangeRequired;
}
