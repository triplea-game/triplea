package org.triplea.db.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class UserRoleTest {

  @ParameterizedTest
  @ValueSource(strings = {UserRole.MODERATOR, UserRole.ADMIN})
  void isModerator(final String role) {
    assertThat(role + " is a moderator", UserRole.isModerator(role), is(true));
  }

  @ParameterizedTest
  @ValueSource(strings = {UserRole.PLAYER, UserRole.HOST, UserRole.ANONYMOUS})
  void isNotModerator(final String role) {
    assertThat(role + " is _not_ a moderator", UserRole.isModerator(role), is(false));
  }
}
