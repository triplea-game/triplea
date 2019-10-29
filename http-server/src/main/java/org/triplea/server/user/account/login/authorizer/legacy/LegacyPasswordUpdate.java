package org.triplea.server.user.account.login.authorizer.legacy;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.util.function.BiConsumer;
import java.util.function.Function;
import javax.annotation.Nonnull;
import lombok.Builder;
import org.triplea.domain.data.PlayerName;
import org.triplea.java.Postconditions;
import org.triplea.lobby.server.db.dao.UserJdbiDao;

// TODO: Md5-Deprecation - This class can be removed when MD5 is removed
@Builder
public class LegacyPasswordUpdate implements BiConsumer<PlayerName, String> {

  @Nonnull private final UserJdbiDao userJdbiDao;
  @Nonnull private final Function<String, String> passwordBcrypter;

  @Override
  public void accept(final PlayerName playerName, final String password) {
    Preconditions.checkNotNull(Strings.emptyToNull(password));
    Preconditions.checkNotNull(playerName);

    final int id =
        userJdbiDao
            .lookupUserIdByName(playerName.getValue())
            .orElseThrow(() -> new IllegalArgumentException("No user id found for: " + playerName));
    Postconditions.assertState(id > 0);

    final int updateCount = userJdbiDao.updatePassword(id, passwordBcrypter.apply(password));
    Postconditions.assertState(updateCount == 1, "Password update failed: " + playerName);
  }
}
