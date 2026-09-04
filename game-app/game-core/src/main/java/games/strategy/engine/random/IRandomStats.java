package games.strategy.engine.random;

import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.RemoteName;
import games.strategy.engine.message.wire.OpaqueBlob;
import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

/** A service that generates dice statistics for each player in the game. */
public interface IRandomStats extends IRemote {
  RemoteName RANDOM_STATS_REMOTE_NAME =
      new RemoteName(
          "games.strategy.engine.random.RandomStats.RANDOM_STATS_REMOTE_NAME", IRandomStats.class);

  /**
   * Identifies the purpose for which dice are rolled. Used to group dice statistics into various
   * buckets.
   */
  enum DiceType {
    COMBAT,
    BOMBING,
    NONCOMBAT,
    TECH,
    ENGINE
  }

  RandomStatsDetails getRandomStats(int diceSides);

  /** Typed request for the current dice statistics computed over the given number of dice sides. */
  @AllArgsConstructor
  class GetRandomStatsRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 4048960159135690181L;

    public static final MessageType<GetRandomStatsRequest> TYPE =
        MessageType.of(GetRandomStatsRequest.class);

    @Getter private final int diceSides;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /**
   * Typed reply carrying the computed dice statistics. The stats graph overlaps game entities, so
   * it rides as an {@link OpaqueBlob} of its Java-serialized form rather than as typed wire fields.
   */
  @AllArgsConstructor
  class GetRandomStatsResponse implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 8624139207764158022L;

    public static final MessageType<GetRandomStatsResponse> TYPE =
        MessageType.of(GetRandomStatsResponse.class);

    private final OpaqueBlob randomStats;

    public static GetRandomStatsResponse of(final RandomStatsDetails randomStats) {
      return new GetRandomStatsResponse(OpaqueBlob.of(randomStats));
    }

    public RandomStatsDetails getRandomStats() {
      return randomStats.read(RandomStatsDetails.class);
    }

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }
}
