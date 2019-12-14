package org.triplea.server.user.account;

import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nonnull;
import lombok.Builder;
import org.triplea.lobby.server.db.dao.BadWordsDao;
import org.triplea.lobby.server.db.dao.UserJdbiDao;

@Builder
public class NameValidation implements Function<String, Optional<String>> {

  @Nonnull private final Function<String, Optional<String>> syntaxValidation;
  @Nonnull private final BadWordsDao badWordsDao;
  @Nonnull private final UserJdbiDao userJdbiDao;

  @Override
  public Optional<String> apply(final String playerName) {
    return syntaxValidation
        .apply(playerName)
        .or(
            () ->
                badWordsDao.containsBadWord(playerName)
                    ? Optional.of("That is not a nice name")
                    : Optional.empty())
        .or(
            () ->
                userJdbiDao.lookupUserIdByName(playerName).isPresent()
                    ? Optional.of("That name is already taken, please choose another")
                    : Optional.empty());
  }
}
