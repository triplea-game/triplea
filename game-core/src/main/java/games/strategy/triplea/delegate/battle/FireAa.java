package games.strategy.triplea.delegate.battle;

import static games.strategy.triplea.delegate.battle.BattleState.Side.DEFENSE;
import static games.strategy.triplea.delegate.battle.BattleState.Side.OFFENSE;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.attachments.TechAbilityAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.IExecutable;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.MustFightBattle.ReturnFire;
import games.strategy.triplea.delegate.battle.steps.fire.FireRoundState;
import games.strategy.triplea.delegate.battle.steps.fire.FiringGroup;
import games.strategy.triplea.delegate.battle.steps.fire.MarkCasualties;
import games.strategy.triplea.delegate.battle.steps.fire.RollDiceStep;
import games.strategy.triplea.delegate.battle.steps.fire.SelectCasualties;
import games.strategy.triplea.delegate.battle.steps.fire.aa.AaFireAndCasualtyStep;
import games.strategy.triplea.delegate.data.CasualtyDetails;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.triplea.java.RemoveOnNextMajorRelease;
import org.triplea.java.collections.CollectionUtils;

/**
 * Maintains the state of a group of AA units firing during a {@link
 * games.strategy.triplea.delegate.battle.MustFightBattle}.
 */
@RemoveOnNextMajorRelease
@Deprecated
@SuppressWarnings("unused")
public class FireAa implements IExecutable {
  private static final long serialVersionUID = -6406659798754841382L;

  private final Collection<Unit> firingUnits;
  private final Collection<Unit> attackableUnits;
  private final MustFightBattle battle;
  private final GamePlayer firingPlayer;
  private final GamePlayer hitPlayer;
  private final boolean defending;
  private final Map<Unit, Collection<Unit>> dependentUnits;
  private final UUID battleId;
  private final boolean headless;
  private final Territory battleSite;
  private final Collection<TerritoryEffect> territoryEffects;
  private final List<Unit> allFriendlyUnitsAliveOrWaitingToDie;
  private final List<Unit> allEnemyUnitsAliveOrWaitingToDie;
  private final boolean isAmphibious = false;
  private final Collection<Unit> amphibiousLandAttackers = List.of();

  private final List<String> aaTypes;

  // These variables change state during execution
  private DiceRoll dice;
  private CasualtyDetails casualties;
  private final Collection<Unit> casualtiesSoFar = new ArrayList<>();

  FireAa(
      final Collection<Unit> attackableUnits,
      final GamePlayer firingPlayer,
      final GamePlayer hitPlayer,
      final Collection<Unit> firingUnits,
      final MustFightBattle battle,
      final boolean defending,
      final Map<Unit, Collection<Unit>> dependentUnits,
      final boolean headless,
      final Territory battleSite,
      final Collection<TerritoryEffect> territoryEffects,
      final List<Unit> allFriendlyUnitsAliveOrWaitingToDie,
      final List<Unit> allEnemyUnitsAliveOrWaitingToDie,
      final List<String> aaTypes) {
    this.attackableUnits =
        CollectionUtils.getMatches(attackableUnits, Matches.unitIsBeingTransported().negate());
    this.firingUnits = firingUnits;
    this.battle = battle;
    this.hitPlayer = hitPlayer;
    this.firingPlayer = firingPlayer;
    this.defending = defending;
    this.dependentUnits = dependentUnits;
    this.headless = headless;
    battleId = battle.getBattleId();
    this.battleSite = battleSite;
    this.territoryEffects = territoryEffects;
    this.allFriendlyUnitsAliveOrWaitingToDie = allFriendlyUnitsAliveOrWaitingToDie;
    this.allEnemyUnitsAliveOrWaitingToDie = allEnemyUnitsAliveOrWaitingToDie;
    this.aaTypes = aaTypes;
  }

  @Override
  public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {

    // Loop through each type of AA and break into firing groups based on suicideOnHit
    for (final String aaType : aaTypes) {
      final Collection<Unit> aaTypeUnits =
          CollectionUtils.getMatches(firingUnits, Matches.unitIsAaOfTypeAa(aaType));
      final List<Collection<Unit>> firingGroups = newFiringUnitGroups(aaTypeUnits);
      for (final Collection<Unit> firingGroup : firingGroups) {
        final Set<UnitType> validTargetTypes =
            UnitAttachment.get(firingGroup.iterator().next().getType())
                .getTargetsAa(bridge.getData());
        final Set<UnitType> airborneTypesTargeted =
            defending
                ? TechAbilityAttachment.getAirborneTargettedByAa(
                        hitPlayer, bridge.getData().getTechnologyFrontier())
                    .get(aaType)
                : new HashSet<>();
        final Collection<Unit> validTargets =
            CollectionUtils.getMatches(
                attackableUnits,
                Matches.unitIsOfTypes(validTargetTypes)
                    .or(
                        Matches.unitIsAirborne()
                            .and(Matches.unitIsOfTypes(airborneTypesTargeted))));
        final IExecutable rollDice =
            new IExecutable() {
              private static final long serialVersionUID = 6435935558879109347L;

              @Override
              public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
                final FireRoundState fireRoundState = new FireRoundState();
                new RollDiceStep(
                        battle,
                        defending ? DEFENSE : OFFENSE,
                        new FiringGroup(
                            aaType,
                            firingUnits,
                            attackableUnits,
                            firingUnits.stream().anyMatch(Matches.unitIsSuicideOnHit())),
                        fireRoundState,
                        new AaFireAndCasualtyStep.AaDiceRoller())
                    .execute(stack, bridge);
                dice = fireRoundState.getDice();
              }
            };
        final IExecutable selectCasualties =
            new IExecutable() {
              private static final long serialVersionUID = 7943295620796835166L;

              @Override
              public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
                final FireRoundState fireRoundState = new FireRoundState();
                fireRoundState.setDice(dice);
                new SelectCasualties(
                        battle,
                        defending ? DEFENSE : OFFENSE,
                        new FiringGroup(
                            aaType,
                            firingUnits,
                            attackableUnits,
                            firingUnits.stream().anyMatch(Matches.unitIsSuicideOnHit())),
                        fireRoundState,
                        new AaFireAndCasualtyStep.SelectAaCasualties())
                    .execute(stack, bridge);
                casualties = fireRoundState.getCasualties();
              }
            };
        final IExecutable notifyCasualties =
            new IExecutable() {
              private static final long serialVersionUID = -6759782085212899725L;

              @Override
              public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
                final FireRoundState fireRoundState = new FireRoundState();
                fireRoundState.setDice(dice);
                fireRoundState.setCasualties(casualties);
                new MarkCasualties(
                        battle,
                        battle,
                        defending ? DEFENSE : OFFENSE,
                        new FiringGroup(
                            aaType,
                            firingUnits,
                            attackableUnits,
                            firingUnits.stream().anyMatch(Matches.unitIsSuicideOnHit())),
                        fireRoundState,
                        ReturnFire.ALL)
                    .execute(stack, bridge);
              }
            };
        // push in reverse order of execution
        stack.push(notifyCasualties);
        stack.push(selectCasualties);
        stack.push(rollDice);
      }
    }
  }

  /**
   * Breaks list of units into groups of non suicide on hit units and each type of suicide on hit
   * units since each type of suicide on hit units need to roll separately to know which ones get
   * hits.
   */
  static List<Collection<Unit>> newFiringUnitGroups(final Collection<Unit> units) {

    // Sort suicide on hit units by type
    final Map<UnitType, Collection<Unit>> map = new HashMap<>();
    for (final Unit unit : CollectionUtils.getMatches(units, Matches.unitIsSuicideOnHit())) {
      final UnitType type = unit.getType();
      if (map.containsKey(type)) {
        map.get(type).add(unit);
      } else {
        final Collection<Unit> unitList = new ArrayList<>();
        unitList.add(unit);
        map.put(type, unitList);
      }
    }

    // Add all suicide on hit groups and the remaining units
    final List<Collection<Unit>> result = new ArrayList<>(map.values());
    final Collection<Unit> remainingUnits =
        CollectionUtils.getMatches(units, Matches.unitIsSuicideOnHit().negate());
    if (!remainingUnits.isEmpty()) {
      result.add(remainingUnits);
    }
    return result;
  }
}
