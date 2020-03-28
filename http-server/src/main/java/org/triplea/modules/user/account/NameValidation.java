package org.triplea.modules.user.account;

import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nonnull;
import lombok.Builder;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.dao.BadWordsDao;
import org.triplea.db.dao.UserJdbiDao;
import org.triplea.domain.data.UserName;

@Builder
public class NameValidation implements Function<String, Optional<String>> {

  @Nonnull private final Function<String, Optional<String>> syntaxValidation;
  @Nonnull private final BadWordsDao badWordsDao;
  @Nonnull private final UserJdbiDao userJdbiDao;

  public static NameValidation build(final Jdbi jdbi) {
    return NameValidation.builder()
        .userJdbiDao(jdbi.onDemand(UserJdbiDao.class))
        .syntaxValidation(name -> Optional.ofNullable(UserName.validate(name)))
        .badWordsDao(jdbi.onDemand(BadWordsDao.class))
        .build();
  }

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
