package org.triplea.modules.user.account.login.authorizer.registered;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.function.BiPredicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.db.dao.user.UserJdbiDao;
import org.triplea.http.client.lobby.login.LoginRequest;

@ExtendWith(MockitoExtension.class)
class PasswordCheckTest {
  private static final String PLAYER_NAME = "player-name";
  private static final String PASSWORD = "plaintext-pass";
  private static final String DB_PASSWORD = "db-pass";

  @Mock private UserJdbiDao userJdbiDao;

  @Mock private BiPredicate<String, String> bcryptCheck;

  private PasswordCheck passwordCheck;

  @BeforeEach
  void setUp() {
    passwordCheck =
        PasswordCheck.builder().userJdbiDao(userJdbiDao).passwordVerifier(bcryptCheck).build();
  }

  @Test
  void userHasNoPassword() {
    when(userJdbiDao.getPassword(PLAYER_NAME)).thenReturn(Optional.empty());

    final boolean result =
        passwordCheck.test(LoginRequest.builder().name(PLAYER_NAME).password(PASSWORD).build());

    assertThat(result, is(false));
  }

  @Test
  void incorrectPassword() {
    whenPasswordIsValid(false);

    final boolean result =
        passwordCheck.test(LoginRequest.builder().name(PLAYER_NAME).password(PASSWORD).build());

    assertThat(result, is(false));
  }

  private void whenPasswordIsValid(final boolean isValid) {
    when(userJdbiDao.getPassword(PLAYER_NAME)).thenReturn(Optional.of(DB_PASSWORD));
    when(bcryptCheck.test(PASSWORD, DB_PASSWORD)).thenReturn(isValid);
  }

  @Test
  void correctPassword() {
    whenPasswordIsValid(true);

    final boolean result =
        passwordCheck.test(LoginRequest.builder().name(PLAYER_NAME).password(PASSWORD).build());

    assertThat(result, is(true));
  }
}
