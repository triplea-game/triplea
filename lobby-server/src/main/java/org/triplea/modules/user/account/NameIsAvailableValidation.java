package org.triplea.modules.user.account;

import com.google.common.annotations.VisibleForTesting;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.dao.user.UserJdbiDao;

@AllArgsConstructor(access = AccessLevel.PROTECTED, onConstructor_ = @VisibleForTesting)
public class NameIsAvailableValidation implements Function<String, Optional<String>> {

  @Nonnull private final UserJdbiDao userJdbiDao;

  public static NameIsAvailableValidation build(final Jdbi jdbi) {
    return new NameIsAvailableValidation(jdbi.onDemand(UserJdbiDao.class));
  }

  @Override
  public Optional<String> apply(final String playerName) {
    return userJdbiDao.lookupUserIdByName(playerName.trim()).isPresent()
        ? Optional.of("That name is already taken, please choose another")
        : Optional.empty();
  }
}
