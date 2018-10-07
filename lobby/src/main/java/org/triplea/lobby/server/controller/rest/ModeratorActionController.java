package org.triplea.lobby.server.controller.rest;

import static spark.Spark.before;
import static spark.Spark.halt;
import static spark.Spark.path;
import static spark.Spark.post;

import java.util.Map;
import java.util.stream.Collectors;

import org.triplea.lobby.server.controller.rest.exception.BadAuthenticationException;
import org.triplea.lobby.server.controller.rest.exception.InsufficientRightsException;
import org.triplea.lobby.server.controller.rest.exception.InvalidParameterException;
import org.triplea.lobby.server.db.Database;
import org.triplea.lobby.server.db.HashedPassword;
import org.triplea.lobby.server.db.UserController;

import com.github.openjson.JSONObject;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;

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
        throw new BadAuthenticationException("Missing parameter");
      }
      final DBUser user = controller.getUserByName(username);
      if (user == null) {
        throw new BadAuthenticationException("Invalid Username");
      }
      if (!user.isAdmin()) {
        throw new InsufficientRightsException("Not a moderator");
      }
      if (!controller.login(username, new HashedPassword(password))) {
        throw new BadAuthenticationException("Invalid Password");
      }
      return;
    }
    // Authentication credentials should not get passed as POST body parameters
    // but rather encoded in the header or something
    halt(405, "{ code: \"Error\", reason: \"Request must be a POST Request\"}");
  }

  String setRole(final Request req, final Response res) {
    final DBUser user = controller.getUserByName(req.params(":username"));
    if (user != null) {
      try {
        final DBUser.Role rank = DBUser.Role.valueOf(req.params(":rank"));
        controller.makeAdmin(new DBUser(new DBUser.UserName(user.getName()),
            new DBUser.UserEmail(user.getEmail()), rank));
        return new JSONObject(ImmutableMap.of("status", "Success")).toString();
      } catch (final IllegalArgumentException e) {
        throw new InvalidParameterException("Rank does not exist");
      }
    }
    throw new InvalidParameterException("User does not exist");
  }
}
