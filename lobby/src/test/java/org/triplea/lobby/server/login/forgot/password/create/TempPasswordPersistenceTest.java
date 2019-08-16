package org.triplea.lobby.server.login.forgot.password.create;

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
import org.triplea.lobby.server.db.HashedPassword;
import org.triplea.lobby.server.db.dao.TempPasswordDao;

@ExtendWith(MockitoExtension.class)
class TempPasswordPersistenceTest {
  private static final String USERNAME = "user";
  private static final String PASSWORD = "pass";
  private static final HashedPassword HASHED_PASS = new HashedPassword("hashed");
  private static final String BCRYPTED_PASS = "bcrypted";

  @Mock private TempPasswordDao tempPasswordDao;
  @Mock private Function<String, HashedPassword> passwordHasher;
  @Mock private Function<HashedPassword, String> hashedPasswordBcrypter;

  private TempPasswordPersistence tempPasswordPersistence;

  @BeforeEach
  void setup() {
    tempPasswordPersistence =
        new TempPasswordPersistence(tempPasswordDao, passwordHasher, hashedPasswordBcrypter);
  }

  @Test
  void storeTempPasswordUsernameNotFound() {
    givenInsertResult(false);

    assertThat(tempPasswordPersistence.storeTempPassword(USERNAME, PASSWORD), is(false));

    verify(tempPasswordDao).invalidateTempPasswords(USERNAME);
  }

  private void givenInsertResult(final boolean result) {
    when(passwordHasher.apply(PASSWORD)).thenReturn(HASHED_PASS);
    when(hashedPasswordBcrypter.apply(HASHED_PASS)).thenReturn(BCRYPTED_PASS);
    when(tempPasswordDao.insertTempPassword(USERNAME, BCRYPTED_PASS)).thenReturn(result);
  }

  @Test
  void storeTempPassword() {
    givenInsertResult(true);

    assertThat(tempPasswordPersistence.storeTempPassword(USERNAME, PASSWORD), is(true));

    verify(tempPasswordDao).invalidateTempPasswords(USERNAME);
  }
}
