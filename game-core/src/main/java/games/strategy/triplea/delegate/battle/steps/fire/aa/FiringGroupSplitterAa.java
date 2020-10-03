package games.strategy.triplea.delegate.battle.steps.fire.aa;

import static games.strategy.triplea.delegate.battle.BattleState.Side.DEFENSE;
import static games.strategy.triplea.delegate.battle.BattleState.UnitBattleFilter.ACTIVE;
import static games.strategy.triplea.delegate.battle.BattleState.UnitBattleFilter.ALIVE;

import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.TechAbilityAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.steps.fire.FiringGroup;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import lombok.Value;
import org.triplea.java.collections.CollectionUtils;

/**
 * Creates AA and Targeted Hit firing groups
 *
 * <p>The firing groups are separated by typeAa and isSuicideOnHit
 */
@Value(staticConstructor = "of")
public class FiringGroupSplitterAa implements Function<BattleState, List<FiringGroup>> {

  BattleState.Side side;

  @Override
  public List<FiringGroup> apply(final BattleState battleState) {
    final Map<String, Set<UnitType>> airborneTechTargetsAllowed =
        side == DEFENSE
            ? TechAbilityAttachment.getAirborneTargettedByAa(
                battleState.getPlayer(side.getOpposite()), battleState.getGameData())
            : Map.of();

    final Collection<Unit> aaUnits =
        CollectionUtils.getMatches(
            battleState.filterUnits(ACTIVE, side),
            Matches.unitIsAaThatCanFire(
                battleState.filterUnits(ALIVE, side.getOpposite()),
                airborneTechTargetsAllowed,
                battleState.getPlayer(side.getOpposite()),
                Matches.unitIsAaForCombatOnly(),
                battleState.getStatus().getRound(),
                side == DEFENSE,
                battleState.getGameData()));

    final List<String> aaTypes = UnitAttachment.getAllOfTypeAas(aaUnits);

    final Collection<Unit> validTargetUnits =
        CollectionUtils.getMatches(
            battleState.filterUnits(ALIVE, side.getOpposite()),
            Matches.unitIsNotInfrastructure().and(Matches.unitIsBeingTransported().negate()));

    final List<FiringGroup> firingGroups = new ArrayList<>();
    for (final String aaType : aaTypes) {
      final Collection<Unit> firingUnits =
          CollectionUtils.getMatches(aaUnits, Matches.unitIsAaOfTypeAa(aaType));
      final Set<UnitType> validTargetTypes =
          UnitAttachment.get(firingUnits.iterator().next().getType())
              .getTargetsAa(battleState.getGameData());

      final Collection<Unit> targetUnits =
          CollectionUtils.getMatches(
              validTargetUnits,
              Matches.unitIsOfTypes(validTargetTypes)
                  .or(
                      Matches.unitIsAirborne()
                          .and(Matches.unitIsOfTypes(airborneTechTargetsAllowed.get(aaType)))));

      firingGroups.addAll(FiringGroup.groupBySuicideOnHit(aaType, firingUnits, targetUnits));
    }
    return firingGroups;
  }
}
