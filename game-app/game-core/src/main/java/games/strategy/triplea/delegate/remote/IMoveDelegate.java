package games.strategy.triplea.delegate.remote;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.MoveDescription;
import games.strategy.engine.data.Territory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.RemoteActionCode;
import games.strategy.engine.message.wire.EntityRef;
import games.strategy.engine.posted.game.pbem.PbemMessagePoster;
import games.strategy.net.Messengers;
import games.strategy.triplea.delegate.UndoableMove;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

/** Remote interface for MoveDelegate. */
public interface IMoveDelegate
    extends IAbstractMoveDelegate<UndoableMove>, IAbstractForumPosterDelegate {
  /**
   * Registers the typed handlers for this delegate's converted methods, guarded for idempotency.
   * The move description rides the Java wire (as it did under the reflective path); territory
   * results ride as {@link EntityRef}s that the caller resolves against its own game data.
   */
  static void registerHandlers(final Messengers messengers, final GameData gameData) {
    if (!messengers.hasTypedMessageHandler(PerformMoveRequest.TYPE)) {
      messengers.registerMessageHandler(
          PerformMoveRequest.TYPE,
          (request, implementor) ->
              new PerformMoveResponse(
                  ((IMoveDelegate) implementor).performMove(request.getMove()).orElse(null)));
    }
    if (!messengers.hasTypedMessageHandler(GetAirCantLandForPlayerRequest.TYPE)) {
      messengers.registerMessageHandler(
          GetAirCantLandForPlayerRequest.TYPE,
          (request, implementor) ->
              TerritoriesResponse.of(
                  ((IMoveDelegate) implementor)
                      .getTerritoriesWhereAirCantLand(
                          request.getPlayer().resolvePlayer(gameData))));
    }
    if (!messengers.hasTypedMessageHandler(GetAirCantLandRequest.TYPE)) {
      messengers.registerMessageHandler(
          GetAirCantLandRequest.TYPE,
          (request, implementor) ->
              TerritoriesResponse.of(
                  ((IMoveDelegate) implementor).getTerritoriesWhereAirCantLand()));
    }
    if (!messengers.hasTypedMessageHandler(GetUnitsCantFightRequest.TYPE)) {
      messengers.registerMessageHandler(
          GetUnitsCantFightRequest.TYPE,
          (request, implementor) ->
              TerritoriesResponse.of(
                  ((IMoveDelegate) implementor).getTerritoriesWhereUnitsCantFight()));
    }
  }

  /**
   * Performs the specified move.
   *
   * @param move - the move to perform.
   * @return an error message if the move can't be made, null otherwise
   */
  @RemoteActionCode(13)
  Optional<String> performMove(MoveDescription move);

  /** Typed request to perform a move (the move description rides the Java wire). */
  @AllArgsConstructor
  class PerformMoveRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5320048576544678221L;

    public static final MessageType<PerformMoveRequest> TYPE =
        MessageType.of(PerformMoveRequest.class);

    @Getter private final MoveDescription move;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed reply with an error string, or null when the move succeeded. */
  @AllArgsConstructor
  class PerformMoveResponse implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5320048576544678222L;

    public static final MessageType<PerformMoveResponse> TYPE =
        MessageType.of(PerformMoveResponse.class);

    @Getter @Nullable private final String error;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /**
   * Get what air units must move before the end of the players turn.
   *
   * @param player referring player ID
   * @return a list of territories with air units that must move of player ID
   */
  @RemoteActionCode(9)
  Collection<Territory> getTerritoriesWhereAirCantLand(GamePlayer player);

  /** Typed request for the territories where a given player's air units cannot land. */
  @AllArgsConstructor
  class GetAirCantLandForPlayerRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5320048576544678223L;

    public static final MessageType<GetAirCantLandForPlayerRequest> TYPE =
        MessageType.of(GetAirCantLandForPlayerRequest.class);

    @Getter private final EntityRef player;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  @RemoteActionCode(8)
  Collection<Territory> getTerritoriesWhereAirCantLand();

  /** Typed request for the territories where air units cannot land. */
  class GetAirCantLandRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5320048576544678224L;

    public static final MessageType<GetAirCantLandRequest> TYPE =
        MessageType.of(GetAirCantLandRequest.class);

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /**
   * Get what units must have combat ability.
   *
   * @return a list of Territories with units that can't fight
   */
  @RemoteActionCode(10)
  Collection<Territory> getTerritoriesWhereUnitsCantFight();

  /** Typed request for the territories where units cannot fight. */
  class GetUnitsCantFightRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5320048576544678225L;

    public static final MessageType<GetUnitsCantFightRequest> TYPE =
        MessageType.of(GetUnitsCantFightRequest.class);

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /**
   * Shared typed reply carrying a set of territories as {@link EntityRef}s; the caller resolves
   * them against its own game data.
   */
  @AllArgsConstructor
  class TerritoriesResponse implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5320048576544678226L;

    public static final MessageType<TerritoriesResponse> TYPE =
        MessageType.of(TerritoriesResponse.class);

    @Getter private final List<EntityRef> territories;

    static TerritoriesResponse of(final Collection<Territory> territories) {
      return new TerritoriesResponse(
          territories.stream().map(EntityRef::of).collect(Collectors.toList()));
    }

    /** Resolves the carried references to live territories against the given game data. */
    public Collection<Territory> resolve(final GameData gameData) {
      return territories.stream()
          .map(ref -> ref.resolveTerritory(gameData))
          .collect(Collectors.toList());
    }

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  @RemoteActionCode(17)
  @Override
  void setHasPostedTurnSummary(boolean hasPostedTurnSummary);

  @RemoteActionCode(14)
  @Override
  boolean postTurnSummary(PbemMessagePoster poster, String title);

  @RemoteActionCode(19)
  @Override
  @Nullable
  String undoMove(int moveIndex);

  @RemoteActionCode(5)
  @Override
  List<UndoableMove> getMovesMade();

  @RemoteActionCode(11)
  @Override
  void initialize(String name, String displayName);

  @RemoteActionCode(16)
  @Override
  void setDelegateBridgeAndPlayer(IDelegateBridge delegateBridge);

  @RemoteActionCode(18)
  @Override
  void start();

  @RemoteActionCode(1)
  @Override
  void end();

  @RemoteActionCode(6)
  @Override
  String getName();

  @RemoteActionCode(3)
  @Override
  String getDisplayName();

  @RemoteActionCode(2)
  @Override
  IDelegateBridge getBridge();

  @RemoteActionCode(15)
  @Override
  Serializable saveState();

  @RemoteActionCode(12)
  @Override
  void loadState(Serializable state);

  @RemoteActionCode(7)
  @Override
  Class<? extends IRemote> getRemoteType();

  @RemoteActionCode(0)
  @Override
  boolean delegateCurrentlyRequiresUserInput();
}
