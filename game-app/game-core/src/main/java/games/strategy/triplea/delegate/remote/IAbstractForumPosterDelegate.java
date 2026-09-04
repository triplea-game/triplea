package games.strategy.triplea.delegate.remote;

import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.RemoteActionCode;
import games.strategy.engine.posted.game.pbem.PbemMessagePoster;
import games.strategy.net.Messengers;
import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

/**
 * Logic for posting a save game to a forum. Supplements other game logic at points where it makes
 * sense to record a save game (e.g. at the end of a game turn).
 */
public interface IAbstractForumPosterDelegate extends IRemote, IDelegate {
  /**
   * Registers the typed handlers for this delegate's converted methods, guarded for idempotency.
   */
  static void registerHandlers(final Messengers messengers) {
    if (!messengers.hasTypedMessageHandler(GetHasPostedTurnSummaryRequest.TYPE)) {
      messengers.registerMessageHandler(
          GetHasPostedTurnSummaryRequest.TYPE,
          (request, implementor) ->
              new GetHasPostedTurnSummaryResponse(
                  ((IAbstractForumPosterDelegate) implementor).getHasPostedTurnSummary()));
    }
  }

  @RemoteActionCode(9)
  boolean postTurnSummary(PbemMessagePoster poster, String title);

  @RemoteActionCode(12)
  void setHasPostedTurnSummary(boolean hasPostedTurnSummary);

  @RemoteActionCode(4)
  boolean getHasPostedTurnSummary();

  /** Typed request asking whether the turn summary has been posted. */
  class GetHasPostedTurnSummaryRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5320048576544675221L;

    public static final MessageType<GetHasPostedTurnSummaryRequest> TYPE =
        MessageType.of(GetHasPostedTurnSummaryRequest.class);

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed reply carrying whether the turn summary has been posted. */
  @AllArgsConstructor
  class GetHasPostedTurnSummaryResponse implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5320048576544675222L;

    public static final MessageType<GetHasPostedTurnSummaryResponse> TYPE =
        MessageType.of(GetHasPostedTurnSummaryResponse.class);

    @Getter private final boolean hasPosted;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  @RemoteActionCode(7)
  @Override
  void initialize(String name, String displayName);

  @RemoteActionCode(11)
  @Override
  void setDelegateBridgeAndPlayer(IDelegateBridge delegateBridge);

  @RemoteActionCode(13)
  @Override
  void start();

  @RemoteActionCode(1)
  @Override
  void end();

  @RemoteActionCode(5)
  @Override
  String getName();

  @RemoteActionCode(3)
  @Override
  String getDisplayName();

  @RemoteActionCode(2)
  @Override
  IDelegateBridge getBridge();

  @RemoteActionCode(10)
  @Override
  Serializable saveState();

  @RemoteActionCode(8)
  @Override
  void loadState(Serializable state);

  @RemoteActionCode(6)
  @Override
  Class<? extends IRemote> getRemoteType();

  @RemoteActionCode(0)
  @Override
  boolean delegateCurrentlyRequiresUserInput();
}
