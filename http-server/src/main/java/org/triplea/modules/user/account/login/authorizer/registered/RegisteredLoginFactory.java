package org.triplea.modules.user.account.login.authorizer.registered;

import java.util.function.Predicate;
import lombok.experimental.UtilityClass;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.dao.UserJdbiDao;
import org.triplea.http.client.lobby.login.LoginRequest;
import org.triplea.modules.user.account.PasswordBCrypter;
import org.triplea.modules.user.account.login.authorizer.BCryptHashVerifier;
import org.triplea.modules.user.account.login.authorizer.legacy.LegacyPasswordCheck;
import org.triplea.modules.user.account.login.authorizer.legacy.LegacyPasswordUpdate;

@UtilityClass
public class RegisteredLoginFactory {

  public static Predicate<LoginRequest> build(final Jdbi jdbi) {
    final UserJdbiDao userJdbiDao = jdbi.onDemand(UserJdbiDao.class);

    return RegisteredLogin.builder()
        .legacyPasswordUpdater(
            LegacyPasswordUpdate.builder()
                .passwordBcrypter(new PasswordBCrypter())
                .userJdbiDao(userJdbiDao)
                .build())
        .legacyPasswordCheck(LegacyPasswordCheck.builder().userJdbiDao(userJdbiDao).build())
        .passwordCheck(
            PasswordCheck.builder()
                .passwordVerifier(new BCryptHashVerifier())
                .userJdbiDao(userJdbiDao)
                .build())
        .build();
  }
}
