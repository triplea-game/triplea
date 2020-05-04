package games.strategy.triplea.delegate.battle.casualty;

import com.google.common.base.Preconditions;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.battle.casualty.power.model.UnitGroupSet;
import games.strategy.triplea.delegate.battle.casualty.power.model.UnitTypeByPlayer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import lombok.Builder;

@Builder
class OrderOfLossesCalculatorByUnitGroup {

  @Nonnull private final CasualtyOrderOfLosses.Parameters parameters;

  List<Unit> sortUnitsForCasualtiesWithSupport() {
    final List<Unit> casualtyOrder = new ArrayList<>();
    final List<Unit> remainingUnits = new ArrayList<>(parameters.getTargetsToPickFrom());

    final UnitGroupSet masterUnitGroupSet = new UnitGroupSet(parameters);

    while (!masterUnitGroupSet.isEmpty()) {
      final Collection<UnitTypeByPlayer> weakestUnitsByPlayer = masterUnitGroupSet.getWeakestUnit();
      final UnitTypeByPlayer unitToPick = breakTie(weakestUnitsByPlayer);
      masterUnitGroupSet.removeUnit(unitToPick);
      final Unit unitToRemove = findUnitOfType(unitToPick, remainingUnits);
      casualtyOrder.add(unitToRemove);
      remainingUnits.remove(unitToRemove);
      if (casualtyOrder.size() == parameters.getHits()) {
        break;
      }
    }
    return casualtyOrder;
  }

  private static Unit findUnitOfType(
      final UnitTypeByPlayer unitTypeByPlayer, final Collection<Unit> units) {
    return units.stream()
        .filter(unit -> unit.getOwner().equals(unitTypeByPlayer.getGamePlayer()))
        .filter(unit -> unit.getType().equals(unitTypeByPlayer.getUnitType()))
        .findAny()
        .orElseThrow(
            () ->
                new RuntimeException(
                    "Error, expected to find unit type: "
                        + unitTypeByPlayer
                        + " in units: "
                        + units));
  }

  private UnitTypeByPlayer breakTie(final Collection<UnitTypeByPlayer> unitTypeByPlayer) {
    Preconditions.checkArgument(!unitTypeByPlayer.isEmpty());

    return OolTieBreaker.builder().parameters(parameters).build().apply(unitTypeByPlayer);
  }
}
