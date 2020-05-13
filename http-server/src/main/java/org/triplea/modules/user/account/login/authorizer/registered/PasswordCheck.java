package org.triplea.modules.user.account.login.authorizer.registered;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.util.function.BiPredicate;
import javax.annotation.Nonnull;
import lombok.Builder;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.dao.user.UserJdbiDao;
import org.triplea.domain.data.UserName;
import org.triplea.modules.user.account.login.authorizer.BCryptHashVerifier;

@Builder
public class PasswordCheck implements BiPredicate<UserName, String> {

  @Nonnull private final UserJdbiDao userJdbiDao;
  @Nonnull private final BiPredicate<String, String> passwordVerifier;

  public static PasswordCheck build(final Jdbi jdbi) {
    return PasswordCheck.builder()
        .passwordVerifier(new BCryptHashVerifier())
        .userJdbiDao(jdbi.onDemand(UserJdbiDao.class))
        .build();
  }

  @Override
  public boolean test(final UserName userName, final String password) {
    Preconditions.checkNotNull(Strings.emptyToNull(password));
    return userJdbiDao
        .getPassword(userName.getValue())
        .map(dbPassword -> passwordVerifier.test(password, dbPassword))
        .orElse(false);
  }
}
