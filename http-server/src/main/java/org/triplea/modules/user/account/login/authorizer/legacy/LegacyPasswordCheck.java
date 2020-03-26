package org.triplea.modules.user.account.login.authorizer.legacy;

import java.util.function.BiPredicate;
import lombok.Builder;
import lombok.NonNull;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.dao.UserJdbiDao;
import org.triplea.domain.data.UserName;

@Builder
public class LegacyPasswordCheck implements BiPredicate<UserName, String> {

  @NonNull private final UserJdbiDao userJdbiDao;

  public static LegacyPasswordCheck build(final Jdbi jdbi) {
    return LegacyPasswordCheck.builder() //
        .userJdbiDao(jdbi.onDemand(UserJdbiDao.class))
        .build();
  }

  @Override
  public boolean test(final UserName userName, final String plainTextPassword) {
    return userJdbiDao
        .getLegacyPassword(userName.getValue())
        .map(
            legacyDbPassword -> {
              final String salt = Md5Crypt.getSalt(legacyDbPassword);
              final String cryptedPlaintext = Md5Crypt.hash(plainTextPassword, salt);
              return cryptedPlaintext.equals(legacyDbPassword);
            })
        .orElse(false);
  }
}
