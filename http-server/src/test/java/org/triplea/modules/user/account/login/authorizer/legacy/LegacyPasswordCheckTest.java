package org.triplea.modules.user.account.login.authorizer.legacy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.db.dao.user.UserJdbiDao;
import org.triplea.domain.data.UserName;

@ExtendWith(MockitoExtension.class)
class LegacyPasswordCheckTest {

  private static final String DB_LEGACY_PASSWORD = "$1$GTV9OtVd$WKt1JeqIasr4GlAJylq2A/";
  private static final String PLAINTEXT_PASSWORD = "legacy";

  private static final UserName PLAYER_NAME = UserName.of("user");

  @Mock private UserJdbiDao userJdbiDao;

  private LegacyPasswordCheck legacyPasswordCheck;

  @BeforeEach
  void setUp() {
    legacyPasswordCheck = LegacyPasswordCheck.builder().userJdbiDao(userJdbiDao).build();
  }

  @Test
  void noLegacyPassword() {
    when(userJdbiDao.getLegacyPassword(PLAYER_NAME.getValue())).thenReturn(Optional.empty());

    final boolean result = legacyPasswordCheck.test(PLAYER_NAME, PLAINTEXT_PASSWORD);

    assertThat(result, is(false));
  }

  @Test
  void badLegacyPassword() {
    when(userJdbiDao.getLegacyPassword(PLAYER_NAME.getValue()))
        .thenReturn(Optional.of(DB_LEGACY_PASSWORD));

    final boolean result = legacyPasswordCheck.test(PLAYER_NAME, "incorrect");

    assertThat(result, is(false));
  }

  @Test
  void validLegacyPassword() {
    when(userJdbiDao.getLegacyPassword(PLAYER_NAME.getValue()))
        .thenReturn(Optional.of(DB_LEGACY_PASSWORD));

    final boolean result = legacyPasswordCheck.test(PLAYER_NAME, PLAINTEXT_PASSWORD);

    assertThat(result, is(true));
  }
}
