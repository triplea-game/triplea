package games.strategy.triplea.delegate.remote.typed;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.player.PlayerBridge;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.triplea.delegate.remote.IEditDelegate;
import java.util.ArrayList;
import java.util.Collection;
import javax.annotation.Nullable;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;
import org.triplea.java.collections.IntegerMap;
import org.triplea.util.Triple;

/**
 * Typed-message-backed {@link IEditDelegate} handed to the edit UI in place of the reflective
 * proxy. Each business method forwards to the persistent {@code "edit"} delegate over the messenger
 * latch via {@link PlayerBridge#invokePersistentDelegate}, so the UI call sites are unchanged. Only
 * the business methods the UI invokes are supported; {@link #getEditMode()} is never called through
 * the remote and stays unsupported.
 */
public class TypedEditDelegate implements IEditDelegate {
  private static final String EDIT = "edit";

  private final PlayerBridge playerBridge;

  public TypedEditDelegate(final PlayerBridge playerBridge) {
    this.playerBridge = playerBridge;
  }

  @Override
  public boolean getEditMode() {
    throw new UnsupportedOperationException(
        "getEditMode is never invoked through the edit remote; the map panel reads edit mode from"
            + " game properties instead");
  }

  @Override
  public void setEditMode(final boolean editMode) {
    playerBridge.invokePersistentDelegate(
        EDIT, new SetEditModeRequest(editMode), SetEditModeResponse.TYPE);
  }

  @Override
  @Nullable
  public String removeUnits(final Territory t, final Collection<Unit> units) {
    return edit(new RemoveUnitsRequest(t, new ArrayList<>(units)));
  }

  @Override
  @Nullable
  public String addUnits(final Territory t, final Collection<Unit> units) {
    return edit(new AddUnitsRequest(t, new ArrayList<>(units)));
  }

  @Override
  @Nullable
  public String changeTerritoryOwner(final Territory t, final GamePlayer player) {
    return edit(new ChangeTerritoryOwnerRequest(t, player));
  }

  @Override
  @Nullable
  public String changeResource(
      final GamePlayer player, final String resourceName, final int newTotal) {
    return edit(new ChangeResourceRequest(player, resourceName, newTotal));
  }

  @Override
  @Nullable
  public String addTechAdvance(final GamePlayer player, final Collection<TechAdvance> advance) {
    return edit(new AddTechAdvanceRequest(player, new ArrayList<>(advance)));
  }

  @Override
  @Nullable
  public String removeTechAdvance(final GamePlayer player, final Collection<TechAdvance> advance) {
    return edit(new RemoveTechAdvanceRequest(player, new ArrayList<>(advance)));
  }

  @Override
  @Nullable
  public String changeUnitHitDamage(
      final IntegerMap<Unit> unitDamageMap, final Territory territory) {
    return edit(new ChangeUnitHitDamageRequest(unitDamageMap, territory));
  }

  @Override
  @Nullable
  public String changeUnitBombingDamage(
      final IntegerMap<Unit> unitDamageMap, final Territory territory) {
    return edit(new ChangeUnitBombingDamageRequest(unitDamageMap, territory));
  }

  @Override
  @Nullable
  public String addComment(final String message) {
    return edit(new AddCommentRequest(message));
  }

  @Override
  @Nullable
  public String changePoliticalRelationships(
      final Collection<Triple<GamePlayer, GamePlayer, RelationshipType>> relationshipChanges) {
    return edit(new ChangePoliticalRelationshipsRequest(new ArrayList<>(relationshipChanges)));
  }

  @Nullable
  private String edit(final WebSocketMessage request) {
    final EditResponse response =
        playerBridge.invokePersistentDelegate(EDIT, request, EditResponse.TYPE);
    return response == null ? null : response.getError();
  }
}
