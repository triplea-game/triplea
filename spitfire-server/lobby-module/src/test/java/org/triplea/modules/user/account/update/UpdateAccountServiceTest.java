package org.triplea.modules.user.account.update;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.db.dao.user.UserJdbiDao;

@ExtendWith(MockitoExtension.class)
class UpdateAccountServiceTest {

  private static final int USER_ID = 100;
  private static final String PASSWORD = "password-value";
  private static final String HASHED_PASSWORD = "hashed-password-value";
  private static final String EMAIL = "email";

  @Mock private UserJdbiDao userJdbiDao;
  @Mock private Function<String, String> passwordEncrypter;

  private UpdateAccountService userAccountService;

  @BeforeEach
  void setUp() {
    userAccountService =
        UpdateAccountService.builder()
            .userJdbiDao(userJdbiDao)
            .passwordEncrpter(passwordEncrypter)
            .build();
  }

  @Test
  void changePassword() {
    when(passwordEncrypter.apply(PASSWORD)).thenReturn(HASHED_PASSWORD);
    when(userJdbiDao.updatePassword(USER_ID, HASHED_PASSWORD)).thenReturn(1);

    userAccountService.changePassword(USER_ID, PASSWORD);

    verify(userJdbiDao).updatePassword(USER_ID, HASHED_PASSWORD);
  }

  @Test
  void fetchEmail() {
    when(userJdbiDao.fetchEmail(USER_ID)).thenReturn(EMAIL);

    final String result = userAccountService.fetchEmail(USER_ID);

    assertThat(result, is(EMAIL));
  }

  @Test
  void changeEmail() {
    when(userJdbiDao.updateEmail(USER_ID, EMAIL)).thenReturn(1);

    userAccountService.changeEmail(USER_ID, EMAIL);

    verify(userJdbiDao).updateEmail(USER_ID, EMAIL);
  }
}
