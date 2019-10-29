package org.triplea.server.user.account.login.authorizer.anonymous;

import com.google.common.base.Preconditions;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nonnull;
import lombok.Builder;
import org.triplea.domain.data.PlayerName;
import org.triplea.lobby.server.db.dao.UserJdbiDao;

@Builder
class AnonymousLogin implements Function<PlayerName, Optional<String>> {
  @Nonnull private final UserJdbiDao userJdbiDao;

  // TODO: Project#12 bad-words check
  // TODO: Project#12 banned username check

  @Override
  public Optional<String> apply(final PlayerName playerName) {
    Preconditions.checkNotNull(playerName);
    // TODO: Project#12 check if chat has playerName
    return userJdbiDao.lookupUserIdByName(playerName.getValue()).isEmpty()
        ? Optional.empty()
        : Optional.of("Name is already in use, please choose another");
  }
}
