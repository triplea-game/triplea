package org.triplea.game.server.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class BattleSimulationServerTest {
  @Test
  void pingReturnsSchemaVersionWithoutEnvironment() {
    final BattleSimulationServer.Response response =
        BattleSimulationServer.handle("{\"command\":\"ping\",\"data\":{}}", Optional.empty());

    assertTrue(response.ok());
    assertEquals("pong", response.type());
    assertEquals(Map.of("schemaVersion", 1), response.data());
    assertNull(response.error());
  }

  @Test
  void resetWithoutEnvironmentReturnsStructuredError() {
    final BattleSimulationServer.Response response =
        BattleSimulationServer.handle(
            "{\"command\":\"reset\",\"data\":{\"scenarioPath\":\"scenario.xml\",\"seed\":1}}",
            Optional.empty());

    assertFalse(response.ok());
    assertEquals("error", response.type());
    assertNull(response.data());
    assertTrue(response.error().contains("no BattleEnvironment service is installed"));
  }
}
