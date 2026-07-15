package org.triplea.game.server.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import games.strategy.triplea.delegate.battle.simulation.BattleEnvironment;
import games.strategy.triplea.delegate.battle.simulation.BattleEpisodeLog;
import games.strategy.triplea.delegate.battle.simulation.BattleObservation;
import games.strategy.triplea.delegate.reinforcement.FixedReinforcementObservation;
import games.strategy.triplea.delegate.strategic.simulation.StrategicAction;
import games.strategy.triplea.delegate.strategic.simulation.StrategicDecisionDomain;
import games.strategy.triplea.delegate.strategic.simulation.StrategicEnvironment;
import games.strategy.triplea.delegate.strategic.simulation.StrategicObservation;
import games.strategy.triplea.delegate.strategic.simulation.StrategicPhase;
import games.strategy.triplea.delegate.strategic.simulation.StrategicResetRequest;
import games.strategy.triplea.delegate.strategic.simulation.StrategicStepResult;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import org.junit.jupiter.api.Test;

class BattleSimulationServerTest {
  @Test
  void pingReturnsBattleAndStrategicSchemaVersionsWithoutEnvironment() {
    final BattleSimulationServer.Response response =
        BattleSimulationServer.handle("{\"command\":\"ping\",\"data\":{}}", Optional.empty());

    assertTrue(response.ok());
    assertEquals("pong", response.type());
    assertEquals(
        Map.of(
            "schemaVersion",
            BattleObservation.CURRENT_SCHEMA_VERSION,
            "strategicSchemaVersion",
            StrategicObservation.CURRENT_SCHEMA_VERSION),
        response.data());
    assertNull(response.error());
  }

  @Test
  void schemaListsBattleAndStrategicCommands() {
    final BattleSimulationServer.Response response =
        BattleSimulationServer.handle("{\"command\":\"schema\",\"data\":{}}", Optional.empty());

    assertTrue(response.ok());
    assertEquals("schema", response.type());
    final Map<?, ?> data = (Map<?, ?>) response.data();
    assertEquals(BattleEpisodeLog.CURRENT_LOG_SCHEMA_VERSION, data.get("episodeLogSchemaVersion"));
    assertEquals(StrategicObservation.CURRENT_SCHEMA_VERSION, data.get("strategicSchemaVersion"));
    final List<?> commands = (List<?>) data.get("commands");
    assertTrue(commands.containsAll(List.of("episodeLog", "replay", "batch")));
    assertTrue(
        commands.containsAll(List.of("strategicReset", "strategicLegalActions", "strategicStep")));
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
  void strategicResetUsesStrategicEnvironment() {
    final FakeStrategicEnvironment environment = new FakeStrategicEnvironment();

    final BattleSimulationServer.Response response =
        BattleSimulationServer.handle(
            "{\"command\":\"strategicReset\",\"data\":{\"scenarioPath\":\"scenario.tsvg\",\"seed\":41,\"player\":\"Blue\"}}",
            Optional.empty(),
            Optional.of(environment));

    assertTrue(response.ok());
    assertEquals("strategicObservation", response.type());
    assertEquals(41, environment.request.seed());
    assertEquals("Blue", environment.request.player());
  }

  @Test
  void savedGameEnvironmentsAreRegisteredThroughServiceLoader() {
    final boolean battleRegistered =
        ServiceLoader.load(BattleEnvironment.class).stream()
            .map(ServiceLoader.Provider::type)
            .anyMatch(SavedGameBattleEnvironment.class::equals);
    final boolean strategicRegistered =
        ServiceLoader.load(StrategicEnvironment.class).stream()
            .map(ServiceLoader.Provider::type)
            .anyMatch(SavedGameStrategicEnvironment.class::equals);

    assertTrue(battleRegistered);
    assertTrue(strategicRegistered);
  }

  private static final class FakeStrategicEnvironment implements StrategicEnvironment {
    private StrategicResetRequest request;

    @Override
    public StrategicObservation reset(final StrategicResetRequest request) {
      this.request = request;
      return observation(request.seed(), request.player());
    }

    @Override
    public List<StrategicAction> legalActions() {
      return List.of();
    }

    @Override
    public StrategicStepResult step(final StrategicAction action) {
      throw new UnsupportedOperationException();
    }

    private static StrategicObservation observation(final long seed, final String player) {
      return new StrategicObservation(
          StrategicObservation.CURRENT_SCHEMA_VERSION,
          seed,
          1,
          player,
          "",
          StrategicPhase.COMPLETE,
          StrategicDecisionDomain.COMPLETE,
          List.of(),
          new FixedReinforcementObservation(1, player, 1, 0, List.of(), List.of()),
          List.of(),
          null,
          true);
    }
  }
}
