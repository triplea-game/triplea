package org.triplea.server.user.account.login.authorizer.registered;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.util.function.BiPredicate;
import javax.annotation.Nonnull;
import lombok.Builder;
import org.triplea.domain.data.PlayerName;
import org.triplea.lobby.server.db.dao.UserJdbiDao;

@Builder
public class PasswordCheck implements BiPredicate<PlayerName, String> {

  @Nonnull private final UserJdbiDao userJdbiDao;
  @Nonnull private final BiPredicate<String, String> passwordVerifier;

  @Override
  public boolean test(final PlayerName playerName, final String password) {
    Preconditions.checkNotNull(Strings.emptyToNull(password));
    return userJdbiDao
        .getPassword(playerName.getValue())
        .map(dbPassword -> passwordVerifier.test(password, dbPassword))
        .orElse(false);
  }
}
