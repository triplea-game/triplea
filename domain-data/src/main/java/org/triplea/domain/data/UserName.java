package org.triplea.domain.data;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.io.Serializable;
import java.util.Optional;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * AKA username, represents the display name of a player. This is the name used when taking a
 * game-seat or when chatting.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
public class UserName implements Serializable {
  @VisibleForTesting static final int MAX_LENGTH = 40;

  private static final long serialVersionUID = 8356372044000232198L;
  private static final int MIN_LENGTH = 3;
  @Getter private final String value;

  public static UserName of(final String name) {
    Preconditions.checkNotNull(name);
    return new UserName(name);
  }

  @Override
  public String toString() {
    return getValue();
  }

  public static boolean isValid(final String username) {
    return Optional.ofNullable(validate(username)).isEmpty();
  }

  /**
   * Checks if a username is syntactical valid.
   *
   * @return Error message if username is not valid, otherwise returns an error message.
   */
  public static @Nullable String validate(final String username) {
    if ((username == null) || (username.length() < MIN_LENGTH)) {
      return "Name is too short (minimum " + MIN_LENGTH + " characters)";
    } else if (username.length() > MAX_LENGTH) {
      return "Name is too long (maximum " + MAX_LENGTH + " characters)";
    } else if (!username.matches("[a-zA-Z][0-9a-zA-Z_-]+")) {
      return "Name can only contain alphanumeric characters, hyphens (-), and underscores (_)";
    }
    return null;
  }
}
