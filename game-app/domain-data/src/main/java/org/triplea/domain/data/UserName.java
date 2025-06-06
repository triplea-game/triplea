package org.triplea.domain.data;

import com.google.common.base.Preconditions;
import java.io.Serial;
import java.io.Serializable;
import java.util.Optional;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.Nls;

/**
 * AKA username, represents the display name of a player. This is the name used when taking a
 * game-seat or when chatting.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
public class UserName implements Serializable {

  @Serial private static final long serialVersionUID = 8356372044000232198L;
  private final String value;

  public static UserName of(final String name) {
    Preconditions.checkNotNull(name);
    return new UserName(name);
  }

  @Override
  public String toString() {
    return getValue();
  }

  public static boolean isValid(final @Nullable String username) {
    return validate(username).isEmpty();
  }

  /**
   * Checks if a username is syntactical valid.
   *
   * @return Error message if username is not valid, otherwise returns an error message.
   */
  public static Optional<@Nls String> validate(final @Nullable String username) {
    if ((username == null) || (username.length() < LobbyConstants.USERNAME_MIN_LENGTH)) {
      return Optional.of(
          "Name is too short (minimum %d characters)"
              .formatted(LobbyConstants.USERNAME_MIN_LENGTH));
    } else if (username.length() > LobbyConstants.USERNAME_MAX_LENGTH) {
      return Optional.of(
          "Name is too long (maximum %d characters)".formatted(LobbyConstants.USERNAME_MAX_LENGTH));
    } else if (!username.matches("[a-zA-Z][0-9a-zA-Z_-]+")) {
      return Optional.of(
          "Name can only contain alphanumeric characters, hyphens (-), and underscores (_)");
    }
    return Optional.empty();
  }
}
