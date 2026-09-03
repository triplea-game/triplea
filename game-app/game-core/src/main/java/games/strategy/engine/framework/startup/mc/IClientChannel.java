package games.strategy.engine.framework.startup.mc;

import games.strategy.engine.framework.message.PlayerListing;
import games.strategy.engine.message.IChannelSubscriber;
import games.strategy.engine.message.RemoteActionCode;
import games.strategy.engine.message.RemoteName;
import games.strategy.net.INode;
import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

/**
 * A channel representing a remote client of a network game. Used by the server to notify clients of
 * various events.
 */
public interface IClientChannel extends IChannelSubscriber {
  RemoteName CHANNEL_NAME =
      new RemoteName(
          "games.strategy.engine.framework.ui.IClientChannel.CHANNEL", IClientChannel.class);

  @RemoteActionCode(2)
  void playerListingChanged(PlayerListing listing);

  /** Typed broadcast that the player listing changed. */
  @AllArgsConstructor
  class PlayerListingChangedMessage implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 4409921188735540221L;

    public static final MessageType<PlayerListingChangedMessage> TYPE =
        MessageType.of(PlayerListingChangedMessage.class);

    private final PlayerListing listing;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }

    public void invokeCallback(final IClientChannel clientChannel) {
      clientChannel.playerListingChanged(listing);
    }
  }

  /**
   * Invoked when all players have been selected. This event indicates the game is ready to start.
   *
   * @param players who is playing who.
   */
  @RemoteActionCode(0)
  void doneSelectingPlayers(byte[] gameData, Map<String, INode> players);

  /** Typed broadcast that all players have been selected and the game is ready to start. */
  @AllArgsConstructor
  class DoneSelectingPlayersMessage implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 1290048371266199210L;

    public static final MessageType<DoneSelectingPlayersMessage> TYPE =
        MessageType.of(DoneSelectingPlayersMessage.class);

    private final byte[] gameData;
    private final Map<String, INode> players;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }

    public void invokeCallback(final IClientChannel clientChannel) {
      clientChannel.doneSelectingPlayers(gameData, players);
    }
  }

  @RemoteActionCode(1)
  void gameReset();

  /** Typed broadcast that the game was reset. */
  class GameResetMessage implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 7712099048823310021L;

    public static final MessageType<GameResetMessage> TYPE = MessageType.of(GameResetMessage.class);

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }

    public void invokeCallback(final IClientChannel clientChannel) {
      clientChannel.gameReset();
    }
  }
}
