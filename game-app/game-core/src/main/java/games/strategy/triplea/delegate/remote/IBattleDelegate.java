package games.strategy.triplea.delegate.remote;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.RemoteActionCode;
import games.strategy.engine.message.wire.EntityRef;
import games.strategy.net.Messengers;
import games.strategy.triplea.delegate.battle.IBattle;
import games.strategy.triplea.delegate.battle.IBattle.BattleType;
import games.strategy.triplea.delegate.data.BattleListing;
import java.io.Serial;
import java.io.Serializable;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;
import org.triplea.java.RemoveOnNextMajorRelease;

/** Logic for querying and fighting pending battles. */
public interface IBattleDelegate extends IRemote, IDelegate {
  /**
   * Registers the typed handlers for this delegate's converted methods. Registration is
   * unconditional: the registry overwrites any prior handler, so re-registering on a later game
   * refreshes the {@code gameData} captured below.
   */
  static void registerHandlers(final Messengers messengers, final GameData gameData) {
    messengers.registerMessageHandler(
        GetBattleListingRequest.TYPE,
        (request, implementor) ->
            new GetBattleListingResponse(((IBattleDelegate) implementor).getBattleListing()));
    messengers.registerMessageHandler(
        FightBattleRequest.TYPE,
        (request, implementor) ->
            new FightBattleResponse(
                ((IBattleDelegate) implementor)
                    .fightBattle(
                        request.getWhere().resolveTerritory(gameData),
                        request.isBombing(),
                        request.getType())));
    messengers.registerMessageHandler(
        GetCurrentBattleRequest.TYPE,
        (request, implementor) ->
            new GetCurrentBattleResponse(((IBattleDelegate) implementor).getCurrentBattle()));
  }

  /** Returns the battles currently waiting to be fought. */
  @RemoteActionCode(3)
  BattleListing getBattleListing();

  /** Typed request asking for the pending battle listing. */
  class GetBattleListingRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5320048576544673221L;

    public static final MessageType<GetBattleListingRequest> TYPE =
        MessageType.of(GetBattleListingRequest.class);

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /**
   * Typed reply carrying the pending battle listing. The listing rides the Java wire as it did
   * under the reflective path, so it has no Gson fixture.
   */
  @AllArgsConstructor
  class GetBattleListingResponse implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5320048576544673222L;

    public static final MessageType<GetBattleListingResponse> TYPE =
        MessageType.of(GetBattleListingResponse.class);

    @Getter private final BattleListing battleListing;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed request to fight the battle in a given territory. */
  @AllArgsConstructor
  class FightBattleRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5320048576544673223L;

    public static final MessageType<FightBattleRequest> TYPE =
        MessageType.of(FightBattleRequest.class);

    @Getter private final EntityRef where;
    @Getter private final boolean bombing;
    @Getter private final BattleType type;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed reply with an error string, or null when the battle was fought successfully. */
  @AllArgsConstructor
  class FightBattleResponse implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5320048576544673224L;

    public static final MessageType<FightBattleResponse> TYPE =
        MessageType.of(FightBattleResponse.class);

    @Getter @Nullable private final String error;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /**
   * Fight the battle in the given country.
   *
   * @param where - where to fight
   * @param bombing - fight a bombing raid
   * @return an error string if the battle could not be fought or an error occurred, null otherwise
   */
  @RemoveOnNextMajorRelease("Remove 'boolean bombing' parameter")
  @RemoteActionCode(2)
  String fightBattle(Territory where, boolean bombing, BattleType type);

  /**
   * Returns the current battle if there is one, or null if there is no current battle in progress.
   */
  @RemoteActionCode(5)
  IBattle getCurrentBattle();

  /** Typed request for the battle currently in progress, if any. */
  class GetCurrentBattleRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5320048576544673225L;

    public static final MessageType<GetCurrentBattleRequest> TYPE =
        MessageType.of(GetCurrentBattleRequest.class);

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /**
   * Typed reply carrying the current battle, or null when none is in progress. The battle rides the
   * Java wire, so this has no Gson fixture.
   */
  @AllArgsConstructor
  class GetCurrentBattleResponse implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5320048576544673226L;

    public static final MessageType<GetCurrentBattleResponse> TYPE =
        MessageType.of(GetCurrentBattleResponse.class);

    @Getter @Nullable private final IBattle currentBattle;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  @RemoteActionCode(9)
  @Override
  void initialize(String name, String displayName);

  @RemoteActionCode(12)
  @Override
  void setDelegateBridgeAndPlayer(IDelegateBridge delegateBridge);

  @RemoteActionCode(13)
  @Override
  void start();

  @RemoteActionCode(1)
  @Override
  void end();

  @RemoteActionCode(7)
  @Override
  String getName();

  @RemoteActionCode(6)
  @Override
  String getDisplayName();

  @RemoteActionCode(4)
  @Override
  IDelegateBridge getBridge();

  @RemoteActionCode(11)
  @Override
  Serializable saveState();

  @RemoteActionCode(10)
  @Override
  void loadState(Serializable state);

  @RemoteActionCode(8)
  @Override
  Class<? extends IRemote> getRemoteType();

  @RemoteActionCode(0)
  @Override
  boolean delegateCurrentlyRequiresUserInput();
}
