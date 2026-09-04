package games.strategy.triplea.delegate.remote;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IPersistentDelegate;
import games.strategy.engine.message.IRemote;
import games.strategy.net.Messengers;
import games.strategy.triplea.delegate.TechAdvance;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
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
   * later game. Every business method returns an error string (null on success), so they all share
   * {@link EditResponse}. Game entities ride the Java wire as they did under the reflective path;
   * the real messenger's game-object streams resolve them to the server's live objects by name/id,
   * so the payloads carry no fixture.
   */
  static void registerHandlers(final Messengers messengers) {
    messengers.registerMessageHandler(
        SetEditModeRequest.TYPE,
        (request, implementor) -> {
          ((IEditDelegate) implementor).setEditMode(request.isEditMode());
          return new SetEditModeResponse();
        });
    messengers.registerMessageHandler(
        AddUnitsRequest.TYPE,
        (request, implementor) ->
            new EditResponse(
                ((IEditDelegate) implementor)
                    .addUnits(request.getTerritory(), request.getUnits())));
    messengers.registerMessageHandler(
        RemoveUnitsRequest.TYPE,
        (request, implementor) ->
            new EditResponse(
                ((IEditDelegate) implementor)
                    .removeUnits(request.getTerritory(), request.getUnits())));
    messengers.registerMessageHandler(
        ChangeTerritoryOwnerRequest.TYPE,
        (request, implementor) ->
            new EditResponse(
                ((IEditDelegate) implementor)
                    .changeTerritoryOwner(request.getTerritory(), request.getPlayer())));
    messengers.registerMessageHandler(
        ChangeResourceRequest.TYPE,
        (request, implementor) ->
            new EditResponse(
                ((IEditDelegate) implementor)
                    .changeResource(
                        request.getPlayer(), request.getResourceName(), request.getNewTotal())));
    messengers.registerMessageHandler(
        AddTechAdvanceRequest.TYPE,
        (request, implementor) ->
            new EditResponse(
                ((IEditDelegate) implementor)
                    .addTechAdvance(request.getPlayer(), request.getAdvances())));
    messengers.registerMessageHandler(
        RemoveTechAdvanceRequest.TYPE,
        (request, implementor) ->
            new EditResponse(
                ((IEditDelegate) implementor)
                    .removeTechAdvance(request.getPlayer(), request.getAdvances())));
    messengers.registerMessageHandler(
        AddCommentRequest.TYPE,
        (request, implementor) ->
            new EditResponse(((IEditDelegate) implementor).addComment(request.getMessage())));
    messengers.registerMessageHandler(
        ChangeUnitHitDamageRequest.TYPE,
        (request, implementor) ->
            new EditResponse(
                ((IEditDelegate) implementor)
                    .changeUnitHitDamage(request.getUnitDamageMap(), request.getTerritory())));
    messengers.registerMessageHandler(
        ChangeUnitBombingDamageRequest.TYPE,
        (request, implementor) ->
            new EditResponse(
                ((IEditDelegate) implementor)
                    .changeUnitBombingDamage(request.getUnitDamageMap(), request.getTerritory())));
    messengers.registerMessageHandler(
        ChangePoliticalRelationshipsRequest.TYPE,
        (request, implementor) ->
            new EditResponse(
                ((IEditDelegate) implementor)
                    .changePoliticalRelationships(request.getRelationshipChanges())));
  }

  boolean getEditMode();

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

  /**
   * Shared typed reply for every edit business method: an error string, or null when the edit was
   * applied. The error may itself carry game entities, so this rides the Java wire (no fixture).
   */
  @AllArgsConstructor
  class EditResponse implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5320048576544681001L;

    public static final MessageType<EditResponse> TYPE = MessageType.of(EditResponse.class);

    @Getter @Nullable private final String error;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  @Nullable
  String removeUnits(Territory t, Collection<Unit> units);

  /** Typed request to remove units from a territory (entities ride the Java wire). */
  @AllArgsConstructor
  class RemoveUnitsRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5320048576544681002L;

    public static final MessageType<RemoveUnitsRequest> TYPE =
        MessageType.of(RemoveUnitsRequest.class);

    @Getter private final Territory territory;
    @Getter private final List<Unit> units;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  @Nullable
  String addUnits(Territory t, Collection<Unit> units);

  /** Typed request to add units to a territory (the new units ride the Java wire). */
  @AllArgsConstructor
  class AddUnitsRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5320048576544681003L;

    public static final MessageType<AddUnitsRequest> TYPE = MessageType.of(AddUnitsRequest.class);

    @Getter private final Territory territory;
    @Getter private final List<Unit> units;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  @Nullable
  String changeTerritoryOwner(Territory t, GamePlayer player);

  /** Typed request to change a territory's owner (entities ride the Java wire). */
  @AllArgsConstructor
  class ChangeTerritoryOwnerRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5320048576544681004L;

    public static final MessageType<ChangeTerritoryOwnerRequest> TYPE =
        MessageType.of(ChangeTerritoryOwnerRequest.class);

    @Getter private final Territory territory;
    @Getter private final GamePlayer player;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  @Nullable
  String changeResource(GamePlayer player, String resourceName, int newTotal);

  /** Typed request to set a player's resource total (the player rides the Java wire). */
  @AllArgsConstructor
  class ChangeResourceRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5320048576544681005L;

    public static final MessageType<ChangeResourceRequest> TYPE =
        MessageType.of(ChangeResourceRequest.class);

    @Getter private final GamePlayer player;
    @Getter private final String resourceName;
    @Getter private final int newTotal;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  @Nullable
  String addTechAdvance(GamePlayer player, Collection<TechAdvance> advance);

  /** Typed request to grant tech advances to a player (entities ride the Java wire). */
  @AllArgsConstructor
  class AddTechAdvanceRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5320048576544681006L;

    public static final MessageType<AddTechAdvanceRequest> TYPE =
        MessageType.of(AddTechAdvanceRequest.class);

    @Getter private final GamePlayer player;
    @Getter private final List<TechAdvance> advances;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  @Nullable
  String removeTechAdvance(GamePlayer player, Collection<TechAdvance> advance);

  /** Typed request to revoke tech advances from a player (entities ride the Java wire). */
  @AllArgsConstructor
  class RemoveTechAdvanceRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5320048576544681007L;

    public static final MessageType<RemoveTechAdvanceRequest> TYPE =
        MessageType.of(RemoveTechAdvanceRequest.class);

    @Getter private final GamePlayer player;
    @Getter private final List<TechAdvance> advances;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  @Nullable
  String changeUnitHitDamage(IntegerMap<Unit> unitDamageMap, Territory territory);

  /** Typed request to set units' hit damage (the map and territory ride the Java wire). */
  @AllArgsConstructor
  class ChangeUnitHitDamageRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5320048576544681008L;

    public static final MessageType<ChangeUnitHitDamageRequest> TYPE =
        MessageType.of(ChangeUnitHitDamageRequest.class);

    @Getter private final IntegerMap<Unit> unitDamageMap;
    @Getter private final Territory territory;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  @Nullable
  String changeUnitBombingDamage(IntegerMap<Unit> unitDamageMap, Territory territory);

  /** Typed request to set units' bombing damage (the map and territory ride the Java wire). */
  @AllArgsConstructor
  class ChangeUnitBombingDamageRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5320048576544681009L;

    public static final MessageType<ChangeUnitBombingDamageRequest> TYPE =
        MessageType.of(ChangeUnitBombingDamageRequest.class);

    @Getter private final IntegerMap<Unit> unitDamageMap;
    @Getter private final Territory territory;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  @Nullable
  String addComment(String message);

  /** Typed request to add a comment to the game history. */
  @AllArgsConstructor
  class AddCommentRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5320048576544681010L;

    public static final MessageType<AddCommentRequest> TYPE =
        MessageType.of(AddCommentRequest.class);

    @Getter private final String message;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  @Nullable
  String changePoliticalRelationships(
      Collection<Triple<GamePlayer, GamePlayer, RelationshipType>> relationshipChanges);

  /** Typed request to change political relationships (the triples ride the Java wire). */
  @AllArgsConstructor
  class ChangePoliticalRelationshipsRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5320048576544681011L;

    public static final MessageType<ChangePoliticalRelationshipsRequest> TYPE =
        MessageType.of(ChangePoliticalRelationshipsRequest.class);

    @Getter
    private final List<Triple<GamePlayer, GamePlayer, RelationshipType>> relationshipChanges;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }
}
