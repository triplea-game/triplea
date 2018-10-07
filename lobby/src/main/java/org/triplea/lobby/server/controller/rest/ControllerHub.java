package org.triplea.lobby.server.controller.rest;

import static spark.Spark.exception;
import static spark.Spark.path;

import org.triplea.lobby.server.config.LobbyConfiguration;
import org.triplea.lobby.server.controller.rest.exception.BadAuthenticationException;
import org.triplea.lobby.server.controller.rest.exception.InsufficientRightsException;
import org.triplea.lobby.server.controller.rest.exception.InvalidParameterException;
import org.triplea.lobby.server.db.Database;

import com.github.openjson.JSONObject;
import com.google.common.collect.ImmutableMap;

/**
 * Main Routing class to connect all Controllers together.
 */
public class ControllerHub {

  private static final ImmutableMap<Class<? extends RuntimeException>, Integer> statusCodeMapping = ImmutableMap.of(
      BadAuthenticationException.class, 401,
      InsufficientRightsException.class, 401,
      InvalidParameterException.class, 422
  );

  private static String exceptionToJson(final RuntimeException exception) {
    return new JSONObject(ImmutableMap.of(
        "status", "Error",
        "type", exception.getClass().getName(),
        "message", exception.getMessage()
    )).toString();
  }

  public static void initializeControllers(final LobbyConfiguration configuration) {
    final Database database = new Database(configuration);
    statusCodeMapping.forEach((exceptionClass, status) -> exception(exceptionClass, (exception, req, res) -> {
      res.status(status);
      res.body(exceptionToJson(exception));
    }));

    path("/api", new ModeratorActionController(database)::initializeRoutes);
  }
}
