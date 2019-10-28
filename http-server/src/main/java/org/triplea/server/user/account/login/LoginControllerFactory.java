package org.triplea.server.user.account.login;

import lombok.experimental.UtilityClass;
import org.jdbi.v3.core.Jdbi;
import org.triplea.lobby.server.db.dao.TempPasswordDao;
import org.triplea.lobby.server.db.dao.UserJdbiDao;
import org.triplea.server.user.account.login.authorizer.BCryptHashVerifier;
import org.triplea.server.user.account.login.authorizer.anonymous.AnonymousLoginFactory;
import org.triplea.server.user.account.login.authorizer.registered.RegisteredLoginFactory;
import org.triplea.server.user.account.login.authorizer.temp.password.TempPasswordLogin;

@UtilityClass
public class LoginControllerFactory {

  public static LoginController buildController(final Jdbi jdbi) {
    final UserJdbiDao userJdbiDao = jdbi.onDemand(UserJdbiDao.class);

    return LoginController.builder()
        .loginModule(
            LoginModule.builder()
                .userJdbiDao(userJdbiDao)
                .apiKeyGenerator(ApiKeyGenerator.builder().build())
                .anonymousLogin(AnonymousLoginFactory.build(jdbi))
                .tempPasswordLogin(
                    TempPasswordLogin.builder()
                        .tempPasswordDao(jdbi.onDemand(TempPasswordDao.class))
                        .passwordChecker(new BCryptHashVerifier())
                        .build())
                .registeredLogin(RegisteredLoginFactory.build(jdbi))
                .build())
        .build();
  }
}
