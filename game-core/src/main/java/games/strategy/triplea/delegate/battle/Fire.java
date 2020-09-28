package games.strategy.triplea.delegate.battle;

import static games.strategy.triplea.delegate.battle.BattleState.Side.DEFENSE;
import static games.strategy.triplea.delegate.battle.BattleState.Side.OFFENSE;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.BOMBARD;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.UNITS;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.IExecutable;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.steps.fire.FireRoundState;
import games.strategy.triplea.delegate.battle.steps.fire.FiringGroup;
import games.strategy.triplea.delegate.battle.steps.fire.MarkCasualties;
import games.strategy.triplea.delegate.battle.steps.fire.RollDice;
import games.strategy.triplea.delegate.battle.steps.fire.RollNormal;
import games.strategy.triplea.delegate.battle.steps.fire.SelectCasualties;
import games.strategy.triplea.delegate.battle.steps.fire.SelectNormalCasualties;
import games.strategy.triplea.delegate.data.CasualtyDetails;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.triplea.java.RemoveOnNextMajorRelease;

/** Maintains the state of a group of units firing during a {@link MustFightBattle}. */
@RemoveOnNextMajorRelease("This is no longer used")
@Deprecated
@SuppressWarnings("unused")
public class Fire implements IExecutable {

  private static final long serialVersionUID = -3687054738070722403L;

  private final String stepName;
  private final Collection<Unit> firingUnits;
  private final Collection<Unit> attackableUnits;
  private final MustFightBattle.ReturnFire canReturnFire;
  private final String text;
  private final MustFightBattle battle;
  private final GamePlayer firingPlayer;
  private final GamePlayer hitPlayer;
  private final boolean defending;
  private final Map<Unit, Collection<Unit>> dependentUnits;
  private final UUID battleId;
  private final boolean headless;
  private final Territory battleSite;
  private final Collection<TerritoryEffect> territoryEffects;
  private final Collection<Unit> allEnemyUnitsAliveOrWaitingToDie;
  private final Collection<Unit> allFriendlyUnitsAliveOrWaitingToDie;
  private final Collection<Unit> allFriendlyUnitsNotIncludingWaitingToDie;
  private final Collection<Unit> allEnemyUnitsNotIncludingWaitingToDie;
  private final boolean isAmphibious = false;
  private final Collection<Unit> amphibiousLandAttackers = List.of();

  // These variables change state during execution
  private DiceRoll dice;
  private Collection<Unit> killed;
  private Collection<Unit> damaged;
  private boolean confirmOwnCasualties = true;

  Fire(
      final Collection<Unit> attackableUnits,
      final MustFightBattle.ReturnFire canReturnFire,
      final GamePlayer firingPlayer,
      final GamePlayer hitPlayer,
      final Collection<Unit> firingUnits,
      final String stepName,
      final String text,
      final MustFightBattle battle,
      final boolean defending,
      final Map<Unit, Collection<Unit>> dependentUnits,
      final boolean headless,
      final Territory battleSite,
      final Collection<TerritoryEffect> territoryEffects,
      final Collection<Unit> allEnemyUnitsAliveOrWaitingToDie,
      final Collection<Unit> allFriendlyUnitsAliveOrWaitingToDie) {
    this.attackableUnits = attackableUnits;
    this.canReturnFire = canReturnFire;
    this.firingUnits = firingUnits;
    this.stepName = stepName;
    this.text = text;
    this.battle = battle;
    this.hitPlayer = hitPlayer;
    this.firingPlayer = firingPlayer;
    this.defending = defending;
    this.dependentUnits = dependentUnits;
    this.headless = headless;
    battleId = battle.getBattleId();
    this.battleSite = battleSite;
    this.territoryEffects = territoryEffects;
    this.allEnemyUnitsAliveOrWaitingToDie = allEnemyUnitsAliveOrWaitingToDie;
    this.allFriendlyUnitsAliveOrWaitingToDie = allFriendlyUnitsAliveOrWaitingToDie;
    allFriendlyUnitsNotIncludingWaitingToDie =
        this.defending ? this.battle.getDefendingUnits() : this.battle.getAttackingUnits();
    allEnemyUnitsNotIncludingWaitingToDie =
        !this.defending ? this.battle.getDefendingUnits() : this.battle.getAttackingUnits();
  }

  /** We must execute in atomic steps, push these steps onto the stack, and let them execute. */
  @Override
  public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
    // add to the stack so we will execute, we want to roll dice, select casualties, then notify in
    // that order, so push onto the stack in reverse order
    final IExecutable rollDice =
        new IExecutable() {
          private static final long serialVersionUID = 7578210876028725797L;

          @Override
          public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
            final FireRoundState fireRoundState = new FireRoundState();
            final FiringGroup firingGroup =
                new FiringGroup(
                    text.equals("Bombard") ? BOMBARD : UNITS,
                    "",
                    firingUnits,
                    attackableUnits,
                    firingUnits.stream().anyMatch(Matches.unitIsSuicideOnHit()));
            new RollDice(
                    battle,
                    defending ? DEFENSE : OFFENSE,
                    firingGroup,
                    fireRoundState,
                    new RollNormal())
                .execute(stack, bridge);
            dice = fireRoundState.getDice();
          }
        };
    final IExecutable selectCasualties =
        new IExecutable() {
          private static final long serialVersionUID = -7687053541570519623L;

          @Override
          public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
            final FireRoundState fireRoundState = new FireRoundState();
            fireRoundState.setDice(dice);
            final FiringGroup firingGroup =
                new FiringGroup(
                    text.equals("Bombard") ? BOMBARD : UNITS,
                    "",
                    firingUnits,
                    attackableUnits,
                    firingUnits.stream().anyMatch(Matches.unitIsSuicideOnHit()));
            new SelectCasualties(
                    battle,
                    defending ? DEFENSE : OFFENSE,
                    firingGroup,
                    fireRoundState,
                    new SelectNormalCasualties())
                .execute(stack, bridge);
            confirmOwnCasualties = fireRoundState.getCasualties().getAutoCalculated();
            killed = fireRoundState.getCasualties().getKilled();
            damaged = fireRoundState.getCasualties().getKilled();
          }
        };
    final IExecutable notifyCasualties =
        new IExecutable() {
          private static final long serialVersionUID = -9173385989239225660L;

          @Override
          public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
            final FireRoundState fireRoundState = new FireRoundState();
            fireRoundState.setDice(dice);
            fireRoundState.setCasualties(
                new CasualtyDetails(
                    new ArrayList<>(killed), new ArrayList<>(damaged), confirmOwnCasualties));
            final FiringGroup firingGroup =
                new FiringGroup(
                    text.equals("Bombard") ? BOMBARD : UNITS,
                    "",
                    firingUnits,
                    attackableUnits,
                    firingUnits.stream().anyMatch(Matches.unitIsSuicideOnHit()));
            new MarkCasualties(
                    battle,
                    battle,
                    defending ? DEFENSE : OFFENSE,
                    firingGroup,
                    fireRoundState,
                    canReturnFire)
                .execute(stack, bridge);
          }
        };
    stack.push(notifyCasualties);
    stack.push(selectCasualties);
    stack.push(rollDice);
  }
}
