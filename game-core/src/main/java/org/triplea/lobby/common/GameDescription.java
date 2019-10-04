package org.triplea.lobby.common;

import games.strategy.net.INode;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;
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
  private final String hostName;
  @Wither private final String comment;
  @Wither private final boolean passworded;
  @Wither private final String gameVersion;

  public boolean isBot() {
    return hostName.startsWith(HeadlessGameServer.BOT_GAME_HOST_NAME_PREFIX)
        && HeadlessGameServer.BOT_GAME_HOST_COMMENT.equals(comment);
  }

  public String getFormattedBotStartTime() {
    return startDateTime == null
        ? ""
        : new DateTimeFormatterBuilder()
            .appendLocalized(null, FormatStyle.SHORT)
            .toFormatter()
            .format(LocalDateTime.ofInstant(startDateTime, ZoneOffset.systemDefault()));
  }
}
