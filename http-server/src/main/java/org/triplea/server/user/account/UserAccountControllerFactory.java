package org.triplea.server.user.account;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jdbi.v3.core.Jdbi;
import org.triplea.lobby.server.db.dao.UserJdbiDao;

/** Creates instances of UserAccountController. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UserAccountControllerFactory {

  /** Instantiates controller with dependencies. */
  public static UserAccountController buildController(final Jdbi jdbi) {
    return UserAccountController.builder()
        .userAccountService(
            UserAccountService.builder()
                .userJdbiDao(jdbi.onDemand(UserJdbiDao.class))
                .passwordEncrpter(new PasswordBCrypter())
                .build())
        .build();
  }
}
