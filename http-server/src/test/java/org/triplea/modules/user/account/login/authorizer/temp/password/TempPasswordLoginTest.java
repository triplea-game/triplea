package org.triplea.modules.user.account.login.authorizer.temp.password;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.function.BiPredicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.db.dao.temp.password.TempPasswordDao;
import org.triplea.http.client.lobby.login.LoginRequest;

@ExtendWith(MockitoExtension.class)
class TempPasswordLoginTest {
  private static final String TEMP_PASSWORD_FROM_DB = "from-db";

  private static final LoginRequest LOGIN_REQUEST =
      LoginRequest.builder().name("login-name").password("incoming-password").build();

  @Mock private TempPasswordDao tempPasswordDao;
  @Mock private BiPredicate<String, String> passwordChecker;

  private TempPasswordLogin tempPasswordLogin;

  @BeforeEach
  void setUp() {
    tempPasswordLogin =
        TempPasswordLogin.builder()
            .passwordChecker(passwordChecker)
            .tempPasswordDao(tempPasswordDao)
            .build();
  }

  @Test
  void userHasNoTempPassword() {
    when(tempPasswordDao.fetchTempPassword(LOGIN_REQUEST.getName())).thenReturn(Optional.empty());

    final boolean result = tempPasswordLogin.test(LOGIN_REQUEST);

    assertThat(result, is(false));
  }

  @Test
  void hasPasswordButMismatches() {
    when(tempPasswordDao.fetchTempPassword(LOGIN_REQUEST.getName()))
        .thenReturn(Optional.of(TEMP_PASSWORD_FROM_DB));
    when(passwordChecker.test(LOGIN_REQUEST.getPassword(), TEMP_PASSWORD_FROM_DB))
        .thenReturn(false);

    final boolean result = tempPasswordLogin.test(LOGIN_REQUEST);

    assertThat(result, is(false));
  }

  @Test
  void tempPasswordMatches() {
    when(tempPasswordDao.fetchTempPassword(LOGIN_REQUEST.getName()))
        .thenReturn(Optional.of(TEMP_PASSWORD_FROM_DB));
    when(passwordChecker.test(LOGIN_REQUEST.getPassword(), TEMP_PASSWORD_FROM_DB)).thenReturn(true);

    final boolean result = tempPasswordLogin.test(LOGIN_REQUEST);

    assertThat(result, is(true));
    verify(tempPasswordDao).invalidateTempPasswords(LOGIN_REQUEST.getName());
  }
}
