package games.strategy.triplea.delegate.remote;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IPersistentDelegate;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.RemoteActionCode;
import games.strategy.net.Messengers;
import games.strategy.triplea.delegate.TechAdvance;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;
import org.triplea.java.collections.IntegerMap;
import org.triplea.util.Triple;

/** Remote interface for EditDelegate. */
public interface IEditDelegate extends IRemote, IPersistentDelegate {
  /**
   * Registers the typed handlers for this delegate's converted methods. Registration is
   * unconditional: the registry overwrites any prior handler, refreshing per-game captures on a
   * later game.
   */
  static void registerHandlers(final Messengers messengers) {
    messengers.registerMessageHandler(
        SetEditModeRequest.TYPE,
        (request, implementor) -> {
          ((IEditDelegate) implementor).setEditMode(request.isEditMode());
          return new SetEditModeResponse();
        });
  }

  @RemoteActionCode(9)
  boolean getEditMode();

  @RemoteActionCode(12)
  void setEditMode(boolean editMode);

  /** Typed request to toggle edit mode. */
  @AllArgsConstructor
  class SetEditModeRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5320048576544676221L;

    public static final MessageType<SetEditModeRequest> TYPE =
        MessageType.of(SetEditModeRequest.class);

    @Getter private final boolean editMode;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed acknowledgement that edit mode was toggled. */
  class SetEditModeResponse implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5320048576544676222L;

    public static final MessageType<SetEditModeResponse> TYPE =
        MessageType.of(SetEditModeResponse.class);

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  @RemoteActionCode(11)
  @Nullable
  String removeUnits(Territory t, Collection<Unit> units);

  @RemoteActionCode(2)
  @Nullable
  String addUnits(Territory t, Collection<Unit> units);

  @RemoteActionCode(6)
  @Nullable
  String changeTerritoryOwner(Territory t, GamePlayer player);

  @RemoteActionCode(13)
  @Nullable
  String changeResource(GamePlayer player, String resourceName, int newTotal);

  @RemoteActionCode(1)
  @Nullable
  String addTechAdvance(GamePlayer player, Collection<TechAdvance> advance);

  @RemoteActionCode(10)
  @Nullable
  String removeTechAdvance(GamePlayer player, Collection<TechAdvance> advance);

  @RemoteActionCode(8)
  @Nullable
  String changeUnitHitDamage(IntegerMap<Unit> unitDamageMap, Territory territory);

  @RemoteActionCode(7)
  @Nullable
  String changeUnitBombingDamage(IntegerMap<Unit> unitDamageMap, Territory territory);

  @RemoteActionCode(0)
  @Nullable
  String addComment(String message);

  @RemoteActionCode(4)
  @Nullable
  String changePoliticalRelationships(
      Collection<Triple<GamePlayer, GamePlayer, RelationshipType>> relationshipChanges);
}
