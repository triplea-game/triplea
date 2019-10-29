package org.triplea.server.user.account.login.authorizer.anonymous;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresent;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.domain.data.PlayerName;
import org.triplea.lobby.server.db.dao.UserJdbiDao;

@ExtendWith(MockitoExtension.class)
class AnonymousLoginTest {
  private static final PlayerName PLAYER_NAME = PlayerName.of("Player");

  @Mock private UserJdbiDao userJdbiDao;

  private AnonymousLogin anonymousLogin;

  @BeforeEach
  void setup() {
    anonymousLogin = AnonymousLogin.builder().userJdbiDao(userJdbiDao).build();
  }

  @Test
  void nameIsRegistered() {
    when(userJdbiDao.lookupUserIdByName(PLAYER_NAME.getValue())).thenReturn(Optional.of(1));

    final Optional<String> result = anonymousLogin.apply(PLAYER_NAME);

    assertThat(result, isPresent());
  }
}
