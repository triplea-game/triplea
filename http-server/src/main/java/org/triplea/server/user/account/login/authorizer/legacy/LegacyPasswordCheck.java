package org.triplea.server.user.account.login.authorizer.legacy;

import java.util.function.BiPredicate;
import lombok.Builder;
import lombok.NonNull;
import org.triplea.domain.data.PlayerName;
import org.triplea.lobby.server.db.dao.UserJdbiDao;

@Builder
public class LegacyPasswordCheck implements BiPredicate<PlayerName, String> {

  @NonNull private final UserJdbiDao userJdbiDao;

  @Override
  public boolean test(final PlayerName playerName, final String plainTextPassword) {
    return userJdbiDao
        .getLegacyPassword(playerName.getValue())
        .map(
            legacyDbPassword -> {
              final String salt = Md5Crypt.getSalt(legacyDbPassword);
              final String cryptedPlaintext = Md5Crypt.hash(plainTextPassword, salt);
              return cryptedPlaintext.equals(legacyDbPassword);
            })
        .orElse(false);
  }
}
