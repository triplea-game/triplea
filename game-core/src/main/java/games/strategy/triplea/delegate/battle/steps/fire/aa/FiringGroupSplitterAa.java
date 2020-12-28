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
import java.io.Serializable;
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
 * <p>For each unique typeAa, there will be at least one firing group.
 *
 * <p>If there are multiple isSuicideOnHit unit types in the same typeAa, then there will be one
 * firing group for each of the isSuicideOnHit unit types and one firing group for all the other
 * unit types.
 *
 * <p>See {@link FiringGroup} for why isSuicideOnHit needs to be separated by unit type.
 */
@Value(staticConstructor = "of")
public class FiringGroupSplitterAa
    implements Function<BattleState, Collection<FiringGroup>>, Serializable {
  private static final long serialVersionUID = -12634991698142952L;

  BattleState.Side side;

  @Override
  public List<FiringGroup> apply(final BattleState battleState) {
    // only defense can fire at special airborne units (old "paratroopers")
    final Map<String, Set<UnitType>> airborneTechTargetsAllowed =
        side == DEFENSE
            ? TechAbilityAttachment.getAirborneTargettedByAa(
                battleState.getPlayer(side.getOpposite()),
                battleState.getGameData().getTechnologyFrontier())
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
                battleState.getGameData().getRelationshipTracker()));

    final List<String> typeAas = UnitAttachment.getAllOfTypeAas(aaUnits);

    final Collection<Unit> validTargetUnits =
        CollectionUtils.getMatches(
            battleState.filterUnits(ALIVE, side.getOpposite()),
            Matches.unitIsBeingTransported().negate());

    final List<FiringGroup> firingGroups = new ArrayList<>();
    // go through each of the typeAas in the game and find any units in validTargetUnits
    // that are defined in targetsAa of the typeAa.
    for (final String typeAa : typeAas) {
      final Collection<Unit> firingUnits =
          CollectionUtils.getMatches(aaUnits, Matches.unitIsAaOfTypeAa(typeAa));

      // grab the unit types that this typeAa can fire at
      final Set<UnitType> validTargetTypes =
          UnitAttachment.get(firingUnits.iterator().next().getType())
              .getTargetsAa(battleState.getGameData().getUnitTypeList());

      final Collection<Unit> targetUnits =
          CollectionUtils.getMatches(
              validTargetUnits,
              Matches.unitIsOfTypes(validTargetTypes)
                  .or(
                      // originally, aa units could only fire at airborne targets. This
                      // generally meant air units but certain old technologies allowed
                      // "paratroopers" that turned a land unit into an air unit for combat
                      // movement.
                      // If the user has this technology and the unit is one of these "paratroopers"
                      // then it should be available as a target for older aa units.
                      // These "paratroopers" are not the same thing as units being carried
                      // by "air transports".
                      Matches.unitIsAirborne()
                          .and(Matches.unitIsOfTypes(airborneTechTargetsAllowed.get(typeAa)))));

      firingGroups.addAll(FiringGroup.groupBySuicideOnHit(typeAa, firingUnits, targetUnits));
    }
    return firingGroups;
  }
}
