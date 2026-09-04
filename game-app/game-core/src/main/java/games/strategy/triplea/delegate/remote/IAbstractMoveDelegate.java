package games.strategy.triplea.delegate.remote;

import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.RemoteActionCode;
import games.strategy.net.Messengers;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

/**
 * Remote interface for MoveDelegate and PlaceDelegate.
 *
 * @param <T> The type of the move (typically {@code UndoableMove} or {@code UndoablePlacement}).
 */
public interface IAbstractMoveDelegate<T> extends IRemote, IDelegate {
  /**
   * Registers the typed handlers shared by every move/place delegate. Registration is
   * unconditional: the registry overwrites any prior handler, refreshing per-game captures on a
   * later game. The moves-made list rides the Java wire (as it did under the reflective path), so
   * its response has no Gson fixture. A single handler serves every delegate because each per-name
   * endpoint supplies its own implementor at dispatch.
   */
  static void registerHandlers(final Messengers messengers) {
    messengers.registerMessageHandler(
        GetMovesMadeRequest.TYPE,
        (request, implementor) ->
            new GetMovesMadeResponse(((IAbstractMoveDelegate<?>) implementor).getMovesMade()));
    messengers.registerMessageHandler(
        UndoMoveRequest.TYPE,
        (request, implementor) ->
            new UndoMoveResponse(
                ((IAbstractMoveDelegate<?>) implementor).undoMove(request.getMoveIndex())));
  }

  /**
   * Get the moves already made.
   *
   * @return A list of moves already made.
   */
  @RemoteActionCode(4)
  List<T> getMovesMade();

  /** Typed request asking for the moves already made. */
  class GetMovesMadeRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5320048576544677221L;

    public static final MessageType<GetMovesMadeRequest> TYPE =
        MessageType.of(GetMovesMadeRequest.class);

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed reply carrying the moves already made (the list rides the Java wire). */
  @AllArgsConstructor
  class GetMovesMadeResponse implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5320048576544677222L;

    public static final MessageType<GetMovesMadeResponse> TYPE =
        MessageType.of(GetMovesMadeResponse.class);

    @Getter private final List<?> movesMade;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /**
   * Undoes the move at the specified index.
   *
   * @param moveIndex - an index in the list getMovesMade.
   * @return an error string if the move could not be undone, null otherwise
   */
  @RemoteActionCode(12)
  @Nullable
  String undoMove(int moveIndex);

  /** Typed request to undo the move at a given index. */
  @AllArgsConstructor
  class UndoMoveRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5320048576544677223L;

    public static final MessageType<UndoMoveRequest> TYPE = MessageType.of(UndoMoveRequest.class);

    @Getter private final int moveIndex;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed reply with an error string, or null when the move was undone. */
  @AllArgsConstructor
  class UndoMoveResponse implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5320048576544677224L;

    public static final MessageType<UndoMoveResponse> TYPE = MessageType.of(UndoMoveResponse.class);

    @Getter @Nullable private final String error;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  @RemoteActionCode(7)
  @Override
  void initialize(String name, String displayName);

  @RemoteActionCode(10)
  @Override
  void setDelegateBridgeAndPlayer(IDelegateBridge delegateBridge);

  @RemoteActionCode(11)
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

  @RemoteActionCode(9)
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
