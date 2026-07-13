package org.triplea.game.server.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import games.strategy.triplea.delegate.battle.simulation.BattleEnvironment;
import games.strategy.triplea.delegate.battle.simulation.BattleObservation;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import org.junit.jupiter.api.Test;

class BattleSimulationServerTest {
  @Test
  void pingReturnsSchemaVersionWithoutEnvironment() {
    final BattleSimulationServer.Response response =
        BattleSimulationServer.handle("{\"command\":\"ping\",\"data\":{}}", Optional.empty());

    assertTrue(response.ok());
    assertEquals("pong", response.type());
    assertEquals(
        Map.of("schemaVersion", BattleObservation.CURRENT_SCHEMA_VERSION), response.data());
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

  @Test
  void savedGameEnvironmentIsRegisteredThroughServiceLoader() {
    final boolean registered =
        ServiceLoader.load(BattleEnvironment.class).stream()
            .map(ServiceLoader.Provider::type)
            .anyMatch(SavedGameBattleEnvironment.class::equals);

    assertTrue(registered);
  }
}
