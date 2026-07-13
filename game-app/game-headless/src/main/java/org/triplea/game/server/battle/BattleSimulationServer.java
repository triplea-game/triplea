package org.triplea.game.server.battle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import games.strategy.triplea.delegate.battle.simulation.BattleAction;
import games.strategy.triplea.delegate.battle.simulation.BattleEnvironment;
import games.strategy.triplea.delegate.battle.simulation.BattleObservation;
import games.strategy.triplea.delegate.battle.simulation.BattleResetRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

/** Newline-delimited JSON server for UI-independent battle simulation. */
public final class BattleSimulationServer {
  private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
  private static final List<String> COMMANDS =
      List.of("ping", "schema", "reset", "legalActions", "step");

  private BattleSimulationServer() {}

  public static void main(final String[] args) throws IOException {
    final Optional<BattleEnvironment> environment =
        ServiceLoader.load(BattleEnvironment.class).findFirst();
    try (var reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        var writer = new PrintWriter(System.out, true, StandardCharsets.UTF_8)) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (!line.isBlank()) {
          writer.println(GSON.toJson(handle(line, environment)));
        }
      }
    }
  }

  static Response handle(final String requestLine, final Optional<BattleEnvironment> environment) {
    try {
      final JsonObject request = JsonParser.parseString(requestLine).getAsJsonObject();
      final String command = request.get("command").getAsString();
      final JsonObject data =
          request.has("data") && request.get("data").isJsonObject()
              ? request.getAsJsonObject("data")
              : new JsonObject();
      return switch (command) {
        case "ping" ->
            Response.success(
                "pong", Map.of("schemaVersion", BattleObservation.CURRENT_SCHEMA_VERSION));
        case "schema" ->
            Response.success(
                "schema",
                Map.of(
                    "schemaVersion",
                    BattleObservation.CURRENT_SCHEMA_VERSION,
                    "commands",
                    COMMANDS,
                    "environmentAvailable",
                    environment.isPresent()));
        case "reset" ->
            withEnvironment(
                environment,
                value ->
                    Response.success(
                        "observation",
                        Map.of(
                            "observation",
                            value.reset(GSON.fromJson(data, BattleResetRequest.class)))));
        case "legalActions" ->
            withEnvironment(
                environment, value -> Response.success("legalActions", value.legalActions()));
        case "step" ->
            withEnvironment(
                environment,
                value ->
                    Response.success("step", value.step(GSON.fromJson(data, BattleAction.class))));
        default -> Response.error("unknown command: " + command);
      };
    } catch (final RuntimeException e) {
      return Response.error(e.getClass().getSimpleName() + ": " + e.getMessage());
    }
  }

  private static Response withEnvironment(
      final Optional<BattleEnvironment> environment, final EnvironmentCommand command) {
    return environment
        .map(command::execute)
        .orElseGet(
            () ->
                Response.error(
                    "no BattleEnvironment service is installed; "
                        + "ping and schema remain available"));
  }

  @FunctionalInterface
  private interface EnvironmentCommand {
    Response execute(BattleEnvironment environment);
  }

  record Response(boolean ok, String type, Object data, String error) {
    static Response success(final String type, final Object data) {
      return new Response(true, type, data, null);
    }

    static Response error(final String error) {
      return new Response(false, "error", null, error);
    }
  }
}
