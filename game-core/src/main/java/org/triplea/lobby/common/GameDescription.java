package org.triplea.lobby.common;

import java.io.Serializable;
import java.time.Instant;

import org.triplea.game.server.HeadlessGameServer;

import games.strategy.net.INode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Wither;

/**
 * Immutable Data class being used to send information about the
 * current game state to the lobby.
 */
// See https://github.com/google/error-prone/pull/1195 and https://github.com/rzwitserloot/lombok/issues/737
@SuppressWarnings("ReferenceEquality")
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
@ToString
public class GameDescription implements Serializable {
  private static final long serialVersionUID = 508593169141567546L;

  /**
   * Represents the game states displayed to users looking at the list of available lobby games.
   */
  @AllArgsConstructor
  public enum GameStatus {
    LAUNCHING("Launching"), IN_PROGRESS("In Progress"), WAITING_FOR_PLAYERS("Waiting For Players");

    private final String displayName;

    @Override
    public String toString() {
      return displayName;
    }
  }

  private final INode hostedBy;
  private final Instant startDateTime;
  @Wither
  private final String gameName;
  @Wither
  private final int playerCount;
  @Wither
  private final int round;
  @Wither
  private final GameStatus status;
  private final String hostName;
  @Wither
  private final String comment;
  @Wither
  private final boolean passworded;
  @Wither
  private final String gameVersion;

  public boolean isBot() {
    return hostName.startsWith(HeadlessGameServer.BOT_GAME_HOST_NAME_PREFIX)
        && HeadlessGameServer.BOT_GAME_HOST_COMMENT.equals(comment);
  }
}
