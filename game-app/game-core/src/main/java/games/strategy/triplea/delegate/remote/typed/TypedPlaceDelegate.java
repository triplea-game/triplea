package games.strategy.triplea.delegate.remote.typed;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.message.wire.EntityRef;
import games.strategy.engine.player.PlayerBridge;
import games.strategy.triplea.delegate.UndoablePlacement;
import games.strategy.triplea.delegate.data.PlaceableUnits;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Typed-message-backed {@link IAbstractPlaceDelegate} handed to the AI in place of the reflective
 * current-delegate proxy. The AI's real placement commits (getPlaceableUnits/placeUnits) forward
 * over the messenger latch; the AI's local-simulation validation calls the real place delegate
 * directly (from the cloned game data) and never reaches this adapter, so the remaining methods
 * stay unsupported. Units and the target territory exist on both nodes and ride as {@link
 * EntityRef}s the server resolves.
 */
public class TypedPlaceDelegate extends AbstractTypedCurrentDelegate
    implements IAbstractPlaceDelegate {
  public TypedPlaceDelegate(final PlayerBridge playerBridge) {
    super(playerBridge);
  }

  @Override
  public Optional<String> placeUnits(
      final Collection<Unit> units, final Territory at, final BidMode bidMode) {
    return Optional.ofNullable(
        invokeCurrent(
                new PlaceUnitsRequest(toRefs(units), EntityRef.of(at), bidMode),
                PlaceUnitsResponse.TYPE)
            .getError());
  }

  @Override
  public PlaceableUnits getPlaceableUnits(final Collection<Unit> units, final Territory at) {
    return invokeCurrent(
            new GetPlaceableUnitsRequest(toRefs(units), EntityRef.of(at)),
            GetPlaceableUnitsResponse.TYPE)
        .getPlaceableUnits();
  }

  private static List<EntityRef> toRefs(final Collection<Unit> units) {
    return units.stream().map(EntityRef::of).collect(Collectors.toList());
  }

  @Override
  public int getPlacementsMade() {
    throw notForwarded("getPlacementsMade");
  }

  @Override
  public Collection<Territory> getTerritoriesWhereAirCantLand() {
    throw notForwarded("getTerritoriesWhereAirCantLand");
  }

  @Override
  public List<UndoablePlacement> getMovesMade() {
    throw notForwarded("getMovesMade");
  }

  @Override
  public String undoMove(final int moveIndex) {
    throw notForwarded("undoMove");
  }
}
