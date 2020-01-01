package org.triplea.server.user.account.login.authorizer.legacy;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.domain.data.UserName;
import org.triplea.lobby.server.db.dao.UserJdbiDao;

@ExtendWith(MockitoExtension.class)
class LegacyPasswordUpdateTest {
  private static final UserName PLAYER_NAME = UserName.of("player-name");
  private static final int USER_ID = 100;

  private static final String PASSWORD = "password";
  private static final String HASHED_PASSWORD = "hashed-password";

  @Mock private UserJdbiDao userJdbiDao;
  @Mock private Function<String, String> passwordBcrypter;

  private LegacyPasswordUpdate legacyPasswordUpdate;

  @BeforeEach
  void setup() {
    legacyPasswordUpdate =
        LegacyPasswordUpdate.builder()
            .userJdbiDao(userJdbiDao)
            .passwordBcrypter(passwordBcrypter)
            .build();
  }

  @Test
  void verifyInteractions() {
    when(userJdbiDao.lookupUserIdByName(PLAYER_NAME.getValue())).thenReturn(Optional.of(USER_ID));
    when(passwordBcrypter.apply(PASSWORD)).thenReturn(HASHED_PASSWORD);
    when(userJdbiDao.updatePassword(USER_ID, HASHED_PASSWORD)).thenReturn(1);

    legacyPasswordUpdate.accept(PLAYER_NAME, PASSWORD);

    verify(userJdbiDao).updatePassword(USER_ID, HASHED_PASSWORD);
  }
}
