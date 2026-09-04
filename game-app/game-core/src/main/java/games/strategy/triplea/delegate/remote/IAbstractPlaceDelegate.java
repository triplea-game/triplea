package games.strategy.triplea.delegate.remote;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.RemoteActionCode;
import games.strategy.engine.message.wire.EntityRef;
import games.strategy.net.Messengers;
import games.strategy.triplea.delegate.UndoablePlacement;
import games.strategy.triplea.delegate.data.PlaceableUnits;
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
import org.triplea.java.RemoveOnNextMajorRelease;

/** Logic for placing units within a territory. */
public interface IAbstractPlaceDelegate extends IAbstractMoveDelegate<UndoablePlacement> {
  /**
   * Registers the typed handlers for this delegate's converted methods. Registration is
   * unconditional: the registry overwrites any prior handler, so re-registering on a later game
   * refreshes the {@code gameData} captured below. Units and the target territory ride as {@link
   * EntityRef}s resolved against the server's game data; the {@link PlaceableUnits} result rides
   * the Java wire (so it has no Gson fixture) and the air-cant-land territories reuse {@link
   * IMoveDelegate.TerritoriesResponse}.
   */
  static void registerHandlers(final Messengers messengers, final GameData gameData) {
    messengers.registerMessageHandler(
        PlaceUnitsRequest.TYPE,
        (request, implementor) ->
            new PlaceUnitsResponse(
                ((IAbstractPlaceDelegate) implementor)
                    .placeUnits(
                        request.resolveUnits(gameData),
                        request.getAt().resolveTerritory(gameData),
                        request.getBidMode())
                    .orElse(null)));
    messengers.registerMessageHandler(
        GetPlaceableUnitsRequest.TYPE,
        (request, implementor) ->
            new GetPlaceableUnitsResponse(
                ((IAbstractPlaceDelegate) implementor)
                    .getPlaceableUnits(
                        request.resolveUnits(gameData),
                        request.getAt().resolveTerritory(gameData))));
    messengers.registerMessageHandler(
        GetAirCantLandRequest.TYPE,
        (request, implementor) ->
            IMoveDelegate.TerritoriesResponse.of(
                ((IAbstractPlaceDelegate) implementor).getTerritoriesWhereAirCantLand()));
  }

  /**
   * Places the specified units in the specified territory.
   *
   * @param units units to place.
   * @param at territory to place
   * @return an error code if the placement was not successful
   */
  @RemoteActionCode(13)
  Optional<String> placeUnits(Collection<Unit> units, Territory at, BidMode bidMode);

  /** Typed request to place units in a territory. */
  @AllArgsConstructor
  class PlaceUnitsRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5320048576544679221L;

    public static final MessageType<PlaceUnitsRequest> TYPE =
        MessageType.of(PlaceUnitsRequest.class);

    @Getter private final List<EntityRef> units;
    @Getter private final EntityRef at;
    @Getter private final BidMode bidMode;

    List<Unit> resolveUnits(final GameData gameData) {
      return units.stream().map(ref -> ref.resolveUnit(gameData)).collect(Collectors.toList());
    }

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed reply with an error string, or null when the placement succeeded. */
  @AllArgsConstructor
  class PlaceUnitsResponse implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5320048576544679222L;

    public static final MessageType<PlaceUnitsResponse> TYPE =
        MessageType.of(PlaceUnitsResponse.class);

    @Getter @Nullable private final String error;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Convenience method for testing. Never called over the network. */
  default Optional<String> placeUnits(final Collection<Unit> units, final Territory at) {
    return placeUnits(units, at, BidMode.NOT_BID);
  }

  /** Indicates whether bidding is enabled during placement. No longer used. */
  @RemoveOnNextMajorRelease
  enum BidMode {
    BID,
    NOT_BID
  }

  /**
   * Query what units can be produced in a given territory. ProductionResponse may indicate an error
   * string that there can be no units placed in a given territory
   *
   * @param units place-able units
   * @param at referring territory
   * @return object that contains place-able units
   */
  @RemoteActionCode(6)
  PlaceableUnits getPlaceableUnits(Collection<Unit> units, Territory at);

  /** Typed request asking which of the given units can be placed in a territory. */
  @AllArgsConstructor
  class GetPlaceableUnitsRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5320048576544679223L;

    public static final MessageType<GetPlaceableUnitsRequest> TYPE =
        MessageType.of(GetPlaceableUnitsRequest.class);

    @Getter private final List<EntityRef> units;
    @Getter private final EntityRef at;

    List<Unit> resolveUnits(final GameData gameData) {
      return units.stream().map(ref -> ref.resolveUnit(gameData)).collect(Collectors.toList());
    }

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed reply carrying the placeable units (rides the Java wire). */
  @AllArgsConstructor
  class GetPlaceableUnitsResponse implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5320048576544679224L;

    public static final MessageType<GetPlaceableUnitsResponse> TYPE =
        MessageType.of(GetPlaceableUnitsResponse.class);

    @Getter private final PlaceableUnits placeableUnits;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /**
   * Returns the number of placements made so far. this is not the number of units placed, but the
   * number of times we have made successful placements.
   */
  @RemoteActionCode(7)
  int getPlacementsMade();

  /**
   * Get what air units must move before the end of the players turn.
   *
   * @return a list of Territories with air units that must move
   */
  @RemoteActionCode(9)
  Collection<Territory> getTerritoriesWhereAirCantLand();

  /** Typed request for the territories where air units cannot land during placement. */
  class GetAirCantLandRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5320048576544679225L;

    public static final MessageType<GetAirCantLandRequest> TYPE =
        MessageType.of(GetAirCantLandRequest.class);

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  @RemoteActionCode(17)
  @Override
  @Nullable
  String undoMove(int moveIndex);

  @RemoteActionCode(10)
  @Override
  void initialize(String name, String displayName);

  @RemoteActionCode(15)
  @Override
  void setDelegateBridgeAndPlayer(IDelegateBridge delegateBridge);

  @RemoteActionCode(16)
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

  @RemoteActionCode(14)
  @Override
  Serializable saveState();

  @RemoteActionCode(11)
  @Override
  void loadState(Serializable state);

  @RemoteActionCode(8)
  @Override
  Class<? extends IRemote> getRemoteType();

  @RemoteActionCode(0)
  @Override
  boolean delegateCurrentlyRequiresUserInput();
}
