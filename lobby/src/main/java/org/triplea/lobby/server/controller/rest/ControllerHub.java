package org.triplea.lobby.server.controller.rest;

import static spark.Spark.path;

import org.triplea.lobby.server.config.LobbyConfiguration;
import org.triplea.lobby.server.db.Database;

/**
 * Main Routing class to connect all Controllers together.
 */
public class ControllerHub {

  public static void initializeControllers(final LobbyConfiguration configuration) {
    final Database database = new Database(configuration);
    path("/api", new ModeratorActionController(database)::initializeRoutes);
  }
}
