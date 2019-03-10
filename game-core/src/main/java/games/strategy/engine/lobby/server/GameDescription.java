package games.strategy.engine.lobby.server;

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

// TODO: move this class to lobby.common upon next lobby-incompatible release; it is shared between client and server

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
