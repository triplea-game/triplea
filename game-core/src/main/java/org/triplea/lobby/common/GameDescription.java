package org.triplea.lobby.common;

import games.strategy.net.INode;
import games.strategy.net.Node;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.Arrays;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;
import org.triplea.domain.data.LobbyGame;
import org.triplea.game.server.HeadlessGameServer;

/**
 * Immutable Data class being used to send information about the current game state to the lobby.
 */
// See https://github.com/google/error-prone/pull/1195 and
// https://github.com/rzwitserloot/lombok/issues/737
@SuppressWarnings("ReferenceEquality")
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class GameDescription implements Serializable {
  private static final long serialVersionUID = 508593169141567546L;

  /** Represents the game states displayed to users looking at the list of available lobby games. */
  @AllArgsConstructor
  public enum GameStatus {
    LAUNCHING("Launching"),

    IN_PROGRESS("In Progress"),

    WAITING_FOR_PLAYERS("Waiting For Players");

    private final String displayName;

    @Override
    public String toString() {
      return displayName;
    }
  }

  private final INode hostedBy;
  private final Instant startDateTime;
  @Wither private final String gameName;
  @Wither private final int playerCount;
  @Wither private final int round;
  @Wither private final GameStatus status;
  @Wither private final String comment;
  @Wither private final boolean passworded;
  @Wither private final String gameVersion;

  public boolean isBot() {
    return hostedBy.getName().startsWith(HeadlessGameServer.BOT_GAME_HOST_NAME_PREFIX)
        && HeadlessGameServer.BOT_GAME_HOST_COMMENT.equals(comment);
  }

  /**
   * Parsing method to convert a given {@code LobbyGame} into an equivalent {@code GameDescription}.
   */
  public static GameDescription fromLobbyGame(final LobbyGame lobbyGame) {
    try {
      return GameDescription.builder()
          .comment(lobbyGame.getComments())
          .hostedBy(
              new Node(
                  lobbyGame.getHostName(),
                  InetAddress.getByName(lobbyGame.getHostAddress()),
                  lobbyGame.getHostPort()))
          .startDateTime(Instant.ofEpochMilli(lobbyGame.getEpochMilliTimeStarted()))
          .gameName(lobbyGame.getMapName())
          .gameVersion(lobbyGame.getMapVersion())
          .status(
              Arrays.stream(GameStatus.values())
                  .filter(s -> s.toString().equals(lobbyGame.getStatus()))
                  .findFirst()
                  .orElseThrow(
                      () ->
                          new IllegalArgumentException(
                              "Unknown game status: " + lobbyGame.getStatus())))
          .passworded(lobbyGame.getPassworded())
          .playerCount(lobbyGame.getPlayerCount())
          .round(lobbyGame.getGameRound())
          .build();
    } catch (final UnknownHostException e) {
      throw new IllegalArgumentException("Error parsing hostname: " + lobbyGame.getHostAddress());
    }
  }

  /** Conversion method to convert the current object into an equivalent {@code LobbyGame}. */
  public LobbyGame toLobbyGame() {
    return LobbyGame.builder()
        .comments(comment)
        .hostName(hostedBy.getName())
        .hostAddress(hostedBy.getAddress().getHostAddress())
        .hostPort(hostedBy.getPort())
        .epochMilliTimeStarted(startDateTime.toEpochMilli())
        .mapName(gameName)
        .mapVersion(gameVersion)
        .status(status.toString())
        .passworded(passworded)
        .playerCount(playerCount)
        .gameRound(round)
        .build();
  }
}
