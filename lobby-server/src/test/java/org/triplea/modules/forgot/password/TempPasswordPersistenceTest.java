package org.triplea.modules.forgot.password;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.db.dao.temp.password.TempPasswordDao;
import org.triplea.http.client.forgot.password.ForgotPasswordRequest;

@ExtendWith(MockitoExtension.class)
class TempPasswordPersistenceTest {
  private static final String USERNAME = "user";
  private static final String PASSWORD = "pass";
  private static final String HASHED_PASS = "hashed";
  private static final String BCRYPTED_PASS = "bcrypted";
  private static final String EMAIL = "email";

  @Mock private TempPasswordDao tempPasswordDao;
  @Mock private Function<String, String> passwordHasher;
  @Mock private Function<String, String> hashedPasswordBcrypter;

  private TempPasswordPersistence tempPasswordPersistence;

  @BeforeEach
  void setUp() {
    tempPasswordPersistence =
        new TempPasswordPersistence(tempPasswordDao, passwordHasher, hashedPasswordBcrypter);
  }

  @Test
  void storeTempPasswordUsernameNotFound() {
    givenInsertResult(false);

    assertThat(
        tempPasswordPersistence.storeTempPassword(
            ForgotPasswordRequest.builder().username(USERNAME).email(EMAIL).build(), PASSWORD),
        is(false));
  }

  private void givenInsertResult(final boolean result) {
    when(passwordHasher.apply(PASSWORD)).thenReturn(HASHED_PASS);
    when(hashedPasswordBcrypter.apply(HASHED_PASS)).thenReturn(BCRYPTED_PASS);
    when(tempPasswordDao.insertTempPassword(USERNAME, EMAIL, BCRYPTED_PASS)).thenReturn(result);
  }

  @Test
  void storeTempPassword() {
    givenInsertResult(true);

    assertThat(
        tempPasswordPersistence.storeTempPassword(
            ForgotPasswordRequest.builder().username(USERNAME).email(EMAIL).build(), PASSWORD),
        is(true));
  }
}
