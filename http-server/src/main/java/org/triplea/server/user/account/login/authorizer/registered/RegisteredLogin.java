package org.triplea.server.user.account.login.authorizer.registered;

import com.google.common.base.Preconditions;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import lombok.Builder;
import org.triplea.domain.data.PlayerName;
import org.triplea.http.client.lobby.login.LoginRequest;

@Builder
public class RegisteredLogin implements Predicate<LoginRequest> {

  @Nonnull private final BiPredicate<PlayerName, String> passwordCheck;
  @Nonnull private final BiPredicate<PlayerName, String> legacyPasswordCheck;
  @Nonnull private final BiConsumer<PlayerName, String> legacyPasswordUpdater;

  @Override
  public boolean test(final LoginRequest loginRequest) {
    Preconditions.checkNotNull(loginRequest);
    Preconditions.checkNotNull(loginRequest.getName());
    Preconditions.checkNotNull(loginRequest.getPassword());

    final var playerName = PlayerName.of(loginRequest.getName());

    if (passwordCheck.test(playerName, loginRequest.getPassword())) {
      return true;
    }

    if (legacyPasswordCheck.test(playerName, loginRequest.getPassword())) {
      legacyPasswordUpdater.accept(playerName, loginRequest.getPassword());
      return true;
    }

    return false;
  }
}
