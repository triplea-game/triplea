package org.triplea.game.server.battle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import games.strategy.triplea.delegate.battle.simulation.BattleAction;
import games.strategy.triplea.delegate.battle.simulation.BattleBatchRequest;
import games.strategy.triplea.delegate.battle.simulation.BattleEnvironment;
import games.strategy.triplea.delegate.battle.simulation.BattleEpisodeLog;
import games.strategy.triplea.delegate.battle.simulation.BattleObservation;
import games.strategy.triplea.delegate.battle.simulation.BattleResetRequest;
import games.strategy.triplea.delegate.strategic.simulation.StrategicAction;
import games.strategy.triplea.delegate.strategic.simulation.StrategicEnvironment;
import games.strategy.triplea.delegate.strategic.simulation.StrategicObservation;
import games.strategy.triplea.delegate.strategic.simulation.StrategicResetRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

/** Newline-delimited JSON server for UI-independent battle and strategic simulation. */
public final class BattleSimulationServer {
  private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
  private static final List<String> COMMANDS =
      List.of(
          "ping",
          "schema",
          "reset",
          "legalActions",
          "step",
          "episodeLog",
          "replay",
          "batch",
          "strategicReset",
          "strategicLegalActions",
          "strategicStep");

  private BattleSimulationServer() {}

  public static void main(final String[] args) throws IOException {
    final Optional<BattleEnvironment> battleEnvironment =
        ServiceLoader.load(BattleEnvironment.class).findFirst();
    final Optional<StrategicEnvironment> strategicEnvironment =
        ServiceLoader.load(StrategicEnvironment.class).findFirst();
    try (var reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        var writer = new PrintWriter(System.out, true, StandardCharsets.UTF_8)) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (!line.isBlank()) {
          writer.println(GSON.toJson(handle(line, battleEnvironment, strategicEnvironment)));
        }
      }
    }
  }

  static Response handle(final String requestLine, final Optional<BattleEnvironment> environment) {
    return handle(requestLine, environment, Optional.empty());
  }

  static Response handle(
      final String requestLine,
      final Optional<BattleEnvironment> battleEnvironment,
      final Optional<StrategicEnvironment> strategicEnvironment) {
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
                "pong",
                Map.of(
                    "schemaVersion",
                    BattleObservation.CURRENT_SCHEMA_VERSION,
                    "strategicSchemaVersion",
                    StrategicObservation.CURRENT_SCHEMA_VERSION));
        case "schema" ->
            Response.success(
                "schema",
                Map.of(
                    "schemaVersion",
                    BattleObservation.CURRENT_SCHEMA_VERSION,
                    "strategicSchemaVersion",
                    StrategicObservation.CURRENT_SCHEMA_VERSION,
                    "episodeLogSchemaVersion",
                    BattleEpisodeLog.CURRENT_LOG_SCHEMA_VERSION,
                    "commands",
                    COMMANDS,
                    "environmentAvailable",
                    battleEnvironment.isPresent(),
                    "strategicEnvironmentAvailable",
                    strategicEnvironment.isPresent()));
        case "reset" ->
            withBattleEnvironment(
                battleEnvironment,
                value ->
                    Response.success(
                        "observation",
                        Map.of(
                            "observation",
                            value.reset(GSON.fromJson(data, BattleResetRequest.class)))));
        case "legalActions" ->
            withBattleEnvironment(
                battleEnvironment, value -> Response.success("legalActions", value.legalActions()));
        case "step" ->
            withBattleEnvironment(
                battleEnvironment,
                value ->
                    Response.success("step", value.step(GSON.fromJson(data, BattleAction.class))));
        case "episodeLog" ->
            withBattleEnvironment(
                battleEnvironment, value -> Response.success("episodeLog", value.episodeLog()));
        case "replay" ->
            withBattleEnvironment(
                battleEnvironment,
                value ->
                    Response.success(
                        "replay", value.replay(GSON.fromJson(data, BattleEpisodeLog.class))));
        case "batch" ->
            withBattleEnvironment(
                battleEnvironment,
                value ->
                    Response.success(
                        "batch", value.batch(GSON.fromJson(data, BattleBatchRequest.class))));
        case "strategicReset" ->
            withStrategicEnvironment(
                strategicEnvironment,
                value ->
                    Response.success(
                        "strategicObservation",
                        Map.of("observation", value.reset(parseStrategicResetRequest(data)))));
        case "strategicLegalActions" ->
            withStrategicEnvironment(
                strategicEnvironment,
                value -> Response.success("strategicLegalActions", value.legalActions()));
        case "strategicStep" ->
            withStrategicEnvironment(
                strategicEnvironment,
                value ->
                    Response.success(
                        "strategicStep", value.step(GSON.fromJson(data, StrategicAction.class))));
        default -> Response.error("unknown command: " + command);
      };
    } catch (final RuntimeException e) {
      return Response.error(e.getClass().getSimpleName() + ": " + e.getMessage());
    }
  }

  private static StrategicResetRequest parseStrategicResetRequest(final JsonObject data) {
    final int maxActions =
        data.has("maxActions")
            ? data.get("maxActions").getAsInt()
            : StrategicResetRequest.DEFAULT_MAX_ACTIONS;
    return new StrategicResetRequest(
        data.get("scenarioPath").getAsString(),
        data.get("seed").getAsLong(),
        data.get("player").getAsString(),
        maxActions);
  }

  private static Response withBattleEnvironment(
      final Optional<BattleEnvironment> environment, final BattleEnvironmentCommand command) {
    return environment
        .map(command::execute)
        .orElseGet(
            () ->
                Response.error(
                    "no BattleEnvironment service is installed; "
                        + "ping, schema, and strategic commands remain available"));
  }

  private static Response withStrategicEnvironment(
      final Optional<StrategicEnvironment> environment, final StrategicEnvironmentCommand command) {
    return environment
        .map(command::execute)
        .orElseGet(
            () ->
                Response.error(
                    "no StrategicEnvironment service is installed; "
                        + "ping, schema, and battle commands remain available"));
  }

  @FunctionalInterface
  private interface BattleEnvironmentCommand {
    Response execute(BattleEnvironment environment);
  }

  @FunctionalInterface
  private interface StrategicEnvironmentCommand {
    Response execute(StrategicEnvironment environment);
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
