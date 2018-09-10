package org.triplea.lobby.server;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public final class UserTest {
  @Test
  public void shouldBeEquatableAndHashable() {
    EqualsVerifier.forClass(User.class).verify();
  }
}
