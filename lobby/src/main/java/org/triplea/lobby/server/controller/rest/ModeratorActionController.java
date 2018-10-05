package org.triplea.lobby.server.controller.rest;

import static spark.Spark.before;
import static spark.Spark.halt;
import static spark.Spark.path;
import static spark.Spark.post;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.triplea.lobby.server.db.Database;
import org.triplea.lobby.server.db.HashedPassword;
import org.triplea.lobby.server.db.UserController;

import com.google.common.base.Splitter;

import games.strategy.engine.lobby.server.userDB.DBUser;
import spark.Request;
import spark.Response;

class ModeratorActionController {
  private final UserController controller;

  ModeratorActionController(final Database database) {
    this.controller = new UserController(database);
  }


  void initializeRoutes() {
    path("/moderate", () -> {
      before("/*", this::authorize);
      post("/:username/set-permission/:rank", this::setRole);
    });
  }

  static Map<String, String> parseBodyToMap(final String body) {
    return Splitter.on('&').splitToList(body)
        .stream()
        .map(Splitter.on('=')::splitToList)
        .collect(Collectors.toMap(list -> list.get(0), list -> list.get(1)));
  }

  void authorize(final Request req, final Response res) {
    if (req.requestMethod().equals("POST")) {
      final Map<String, String> bodyParams = parseBodyToMap(req.body());
      final String username = bodyParams.get("username");
      final String password = bodyParams.get("password");
      if (username == null || password == null) {
        halt(401, "{ code: \"Error\", reason: \"Missing parameter\"}");
        return;
      }
      final DBUser user = controller.getUserByName(username);
      if (user == null) {
        halt(401, "{ code: \"Error\", reason: \"Invalid Username\"}");
        return;
      }
      if (!user.isAdmin()) {
        halt(401, "{ code: \"Error\", reason: \"Not a moderator\"}");
        return;
      }
      if (!controller.login(username, new HashedPassword(password))) {
        halt(401, "{ code: \"Error\", reason: \"Invalid Password\"}");
        return;
      }
      return;
    }
    halt(405, "{ code: \"Error\", reason: \"Request must be a POST Request\"}");
  }

  String setRole(final Request req, final Response res) {
    final DBUser user = controller.getUserByName(req.params(":username"));
    if (user != null) {
      try {
        final DBUser.Role rank = DBUser.Role.valueOf(req.params(":rank"));
        controller.makeAdmin(new DBUser(new DBUser.UserName(user.getName()),
            new DBUser.UserEmail(user.getEmail()), rank));
        return "{ code: \"Success\" }";
      } catch (final IllegalArgumentException e) {
        return "{ code: \"Error\", reason: \"Rank does not exist\"}";
      }
    }
    return "{ code: \"Error\", reason: \"User does not exist\"}";
  }
}
