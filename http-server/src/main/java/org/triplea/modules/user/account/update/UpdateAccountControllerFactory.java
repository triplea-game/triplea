package org.triplea.modules.user.account.update;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.dao.UserJdbiDao;
import org.triplea.modules.user.account.PasswordBCrypter;

/** Creates instances of UserAccountController. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UpdateAccountControllerFactory {

  /** Instantiates controller with dependencies. */
  public static UpdateAccountController buildController(final Jdbi jdbi) {
    return UpdateAccountController.builder()
        .userAccountService(
            UpdateAccountService.builder()
                .userJdbiDao(jdbi.onDemand(UserJdbiDao.class))
                .passwordEncrpter(new PasswordBCrypter())
                .build())
        .build();
  }
}
