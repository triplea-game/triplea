package org.triplea.server.user.account.login.authorizer.anonymous;

import java.util.Optional;
import java.util.function.Function;
import lombok.experimental.UtilityClass;
import org.jdbi.v3.core.Jdbi;
import org.triplea.domain.data.PlayerName;
import org.triplea.lobby.server.db.dao.UserJdbiDao;

@UtilityClass
public class AnonymousLoginFactory {

  public static Function<PlayerName, Optional<String>> build(final Jdbi jdbi) {
    return AnonymousLogin.builder().userJdbiDao(jdbi.onDemand(UserJdbiDao.class)).build();
  }
}
