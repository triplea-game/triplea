package games.strategy.engine.lobby.client.login;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.triplea.live.servers.ServerProperties;

final class LobbyServerPropertiesTest {
  @Nested
  final class EqualsAndHashCodeTest {
    @Test
    void shouldBeEquatableAndHashable() {
      EqualsVerifier.forClass(ServerProperties.class).verify();
    }
  }
}
