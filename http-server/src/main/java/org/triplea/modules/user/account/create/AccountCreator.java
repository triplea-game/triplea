package org.triplea.modules.user.account.create;

import java.util.function.Function;
import javax.annotation.Nonnull;
import lombok.Builder;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.dao.UserJdbiDao;
import org.triplea.http.client.lobby.login.CreateAccountRequest;
import org.triplea.http.client.lobby.login.CreateAccountResponse;
import org.triplea.java.Postconditions;
import org.triplea.modules.user.account.PasswordBCrypter;

/**
 * Responsible to execute a 'create account' request. We should already have validated the request
 * and just need to store the new account in database.
 */
@Builder
class AccountCreator implements Function<CreateAccountRequest, CreateAccountResponse> {
  @Nonnull private final UserJdbiDao userJdbiDao;
  @Nonnull private final Function<String, String> passwordEncryptor;

  public static AccountCreator build(final Jdbi jdbi) {
    return AccountCreator.builder()
        .userJdbiDao(jdbi.onDemand(UserJdbiDao.class))
        .passwordEncryptor(new PasswordBCrypter())
        .build();
  }

  @Override
  public CreateAccountResponse apply(final CreateAccountRequest createAccountRequest) {
    final String cryptedPassword = passwordEncryptor.apply(createAccountRequest.getPassword());

    final int rowCount =
        userJdbiDao.createUser(
            createAccountRequest.getUsername(), createAccountRequest.getEmail(), cryptedPassword);
    Postconditions.assertState(rowCount == 1);
    return CreateAccountResponse.SUCCESS_RESPONSE;
  }
}
