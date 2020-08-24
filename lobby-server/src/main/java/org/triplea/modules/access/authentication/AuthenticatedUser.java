package org.triplea.modules.access.authentication;

import com.google.common.base.Preconditions;
import java.security.Principal;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.triplea.db.dao.user.role.UserRole;
import org.triplea.domain.data.ApiKey;

/**
 * AuthenticatedUser is anyone that has successfully logged in to the lobby, whether via anonymous
 * account or registered account. Because we must extend Principle, we have a no-op 'getName' method
 * that throws an {@code UnsupportedOperationException} if accessed.
 */
@Builder
@EqualsAndHashCode
public class AuthenticatedUser implements Principal {
  @Getter @Nullable private final Integer userId;
  @Getter @Nonnull private final String userRole;
  @Getter @Nonnull private final ApiKey apiKey;
  @Nullable private final String name;

  @Nullable
  @Override
  public String getName() {
    Preconditions.checkState(
        (name == null) == userRole.equals(UserRole.HOST),
        "All user roles will have a name except the 'HOST' role (lobby watcher)");
    return name;
  }

  public int getUserIdOrThrow() {
    Preconditions.checkState(
        userId != null, "This method is called when we expect user id to not be null");
    Preconditions.checkState(
        !userRole.equals(UserRole.ANONYMOUS),
        "Integrity check, anonymous user role implies null user id, userId = " + userId);
    Preconditions.checkState(
        !userRole.equals(UserRole.HOST),
        "Integrity check, host user role implies null user id, userId = " + userId);
    return userId;
  }
}
