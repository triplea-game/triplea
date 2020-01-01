package org.triplea.server.user.account.login.authorizer.anonymous;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresent;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.domain.data.UserName;
import org.triplea.lobby.server.db.dao.UserJdbiDao;
import org.triplea.server.lobby.chat.event.processing.Chatters;

@ExtendWith(MockitoExtension.class)
class AnonymousLoginTest {
  private static final UserName PLAYER_NAME = UserName.of("Player");

  @Mock private UserJdbiDao userJdbiDao;
  @Mock private Chatters chatters;

  private AnonymousLogin anonymousLogin;

  @BeforeEach
  void setup() {
    anonymousLogin = AnonymousLogin.builder().userJdbiDao(userJdbiDao).chatters(chatters).build();
  }

  @Test
  void nameIsInUse() {
    when(chatters.hasPlayer(PLAYER_NAME)).thenReturn(true);

    final Optional<String> result = anonymousLogin.apply(PLAYER_NAME);

    assertThat(result, isPresent());
  }

  @Test
  void nameIsRegistered() {
    when(userJdbiDao.lookupUserIdByName(PLAYER_NAME.getValue())).thenReturn(Optional.of(1));

    final Optional<String> result = anonymousLogin.apply(PLAYER_NAME);

    assertThat(result, isPresent());
  }

  @Test
  void allowLogin() {
    when(chatters.hasPlayer(PLAYER_NAME)).thenReturn(false);
    when(userJdbiDao.lookupUserIdByName(PLAYER_NAME.getValue())).thenReturn(Optional.empty());

    final Optional<String> result = anonymousLogin.apply(PLAYER_NAME);

    assertThat(result, isEmpty());
  }
}
