package org.triplea.modules.user.account.login.authorizer.registered;

import com.google.common.base.Preconditions;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import lombok.Builder;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.dao.user.UserJdbiDao;
import org.triplea.http.client.lobby.login.LoginRequest;
import org.triplea.java.ArgChecker;
import org.triplea.modules.user.account.PasswordBCrypter;

@Builder
public class PasswordCheck implements Predicate<LoginRequest> {

  @Nonnull private final UserJdbiDao userJdbiDao;
  @Nonnull private final BiPredicate<String, String> passwordVerifier;

  public static PasswordCheck build(final Jdbi jdbi) {
    return PasswordCheck.builder()
        .passwordVerifier(PasswordBCrypter::verifyHash)
        .userJdbiDao(jdbi.onDemand(UserJdbiDao.class))
        .build();
  }

  @Override
  public boolean test(final LoginRequest loginRequest) {
    Preconditions.checkNotNull(loginRequest);
    ArgChecker.checkNotEmpty(loginRequest.getName());
    ArgChecker.checkNotEmpty(loginRequest.getPassword());
    return userJdbiDao
        .getPassword(loginRequest.getName())
        .map(dbPassword -> passwordVerifier.test(loginRequest.getPassword(), dbPassword))
        .orElse(false);
  }
}
