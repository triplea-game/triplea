package org.triplea.server.user.account.login;

import lombok.experimental.UtilityClass;
import org.jdbi.v3.core.Jdbi;
import org.triplea.lobby.server.db.dao.TempPasswordDao;
import org.triplea.lobby.server.db.dao.UserJdbiDao;
import org.triplea.lobby.server.db.dao.access.log.AccessLogDao;
import org.triplea.lobby.server.db.dao.api.key.ApiKeyDaoWrapper;
import org.triplea.server.lobby.chat.event.processing.Chatters;
import org.triplea.server.user.account.login.authorizer.BCryptHashVerifier;
import org.triplea.server.user.account.login.authorizer.anonymous.AnonymousLoginFactory;
import org.triplea.server.user.account.login.authorizer.registered.RegisteredLoginFactory;
import org.triplea.server.user.account.login.authorizer.temp.password.TempPasswordLogin;

@UtilityClass
public class LoginControllerFactory {

  public static LoginController buildController(final Jdbi jdbi, final Chatters chatters) {
    final UserJdbiDao userJdbiDao = jdbi.onDemand(UserJdbiDao.class);

    return LoginController.builder()
        .loginModule(
            LoginModule.builder()
                .userJdbiDao(userJdbiDao)
                .accessLogUpdater(
                    AccessLogUpdater.builder()
                        .accessLogDao(jdbi.onDemand(AccessLogDao.class))
                        .build())
                .apiKeyGenerator(
                    ApiKeyGenerator.builder().apiKeyDaoWrapper(new ApiKeyDaoWrapper(jdbi)).build())
                .anonymousLogin(AnonymousLoginFactory.build(jdbi, chatters))
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
