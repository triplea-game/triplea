package org.triplea.modules.user.account.create;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.db.dao.UserJdbiDao;
import org.triplea.http.client.lobby.login.CreateAccountRequest;
import org.triplea.http.client.lobby.login.CreateAccountResponse;

@ExtendWith(MockitoExtension.class)
class AccountCreatorTest {
  private static final String CRYPTED_PASSWORD = "crypted-password";
  private static final CreateAccountRequest CREATE_ACCOUNT_REQUEST =
      CreateAccountRequest.builder().username("user").email("email").password("password").build();

  @Mock private UserJdbiDao userJdbiDao;
  @Mock private Function<String, String> passwordEncryptor;
  @InjectMocks private AccountCreator accountCreator;

  @Test
  void createAccount() {
    when(passwordEncryptor.apply(CREATE_ACCOUNT_REQUEST.getPassword()))
        .thenReturn(CRYPTED_PASSWORD);
    when(userJdbiDao.createUser(
            CREATE_ACCOUNT_REQUEST.getUsername(),
            CREATE_ACCOUNT_REQUEST.getEmail(),
            CRYPTED_PASSWORD))
        .thenReturn(1);

    final CreateAccountResponse result = accountCreator.apply(CREATE_ACCOUNT_REQUEST);

    assertThat(result, is(CreateAccountResponse.SUCCESS_RESPONSE));
  }
}
