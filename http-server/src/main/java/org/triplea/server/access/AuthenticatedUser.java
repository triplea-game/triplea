package org.triplea.server.access;

import com.google.common.base.Preconditions;
import java.security.Principal;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Getter;
import org.triplea.lobby.server.db.data.UserRole;

/**
 * AuthenticatedUser is anyone that has successfully logged in to the lobby, whether via anonymous
 * account or registered account. Because we must extend Principle, we have a no-op 'getName' method
 * that throws an {@code UnsupportedOperationException} if accessed.
 */
@Builder
public class AuthenticatedUser implements Principal {
  @Getter @Nullable private final Integer userId;
  @Getter @Nonnull private final String userRole;
  @Getter @Nullable private final String apiKey;

  @Override
  public String getName() {
    throw new UnsupportedOperationException("Name lookup is not done on authentication.");
  }

  public int getUserIdOrThrow() {
    Preconditions.checkState(
        !userRole.equals(UserRole.ANONYMOUS), "Anonymous users always have a null user id");
    return Optional.ofNullable(userId)
        .orElseThrow(() -> new AssertionError("Error, expected to have a non-null user id"));
  }
}
