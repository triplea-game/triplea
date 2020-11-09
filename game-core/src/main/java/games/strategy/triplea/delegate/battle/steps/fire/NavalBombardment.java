package games.strategy.triplea.delegate.battle.steps.fire;

import static games.strategy.triplea.delegate.battle.BattleState.Side.OFFENSE;
import static games.strategy.triplea.delegate.battle.BattleState.UnitBattleFilter.ALIVE;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.MustFightBattle;
import games.strategy.triplea.delegate.battle.casualty.CasualtySelector;
import games.strategy.triplea.delegate.battle.steps.BattleStep;
import games.strategy.triplea.delegate.data.CasualtyDetails;
import games.strategy.triplea.delegate.power.calculator.CombatValueBuilder;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.triplea.sound.SoundPath;

@AllArgsConstructor
public class NavalBombardment implements BattleStep {

  private static final long serialVersionUID = 3338296388191048761L;

  private static final BattleState.Side side = OFFENSE;

  protected final BattleState battleState;

  protected final BattleActions battleActions;

  @Override
  public List<String> getNames() {
    return !valid()
        ? List.of()
        : getSteps().stream()
            .flatMap(step -> step.getNames().stream())
            .collect(Collectors.toList());
  }

  @Override
  public Order getOrder() {
    return Order.NAVAL_BOMBARDMENT;
  }

  @Override
  public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
    if (!valid()) {
      return;
    }
    final Collection<Unit> bombardingUnits = battleState.getBombardingUnits();

    if (!bombardingUnits.isEmpty()) {
      // bombarding units can't move after bombarding even if there are no units to bombard
      final Change change = ChangeFactory.markNoMovementChange(bombardingUnits);
      bridge.addChange(change);
    }

    final List<BattleStep> steps = getSteps();

    if (!steps.isEmpty()) {
      bridge
          .getSoundChannelBroadcaster()
          .playSoundForAll(SoundPath.CLIP_BATTLE_BOMBARD, battleState.getPlayer(side));

      // steps go in reverse order on the stack
      Collections.reverse(steps);
      steps.forEach(stack::push);
    }
  }

  private List<BattleStep> getSteps() {
    return FireRoundStepsFactory.builder()
        .battleState(battleState)
        .battleActions(battleActions)
        .firingGroupSplitter(FiringGroupSplitterBombard.of())
        .side(side)
        .returnFire(
            Properties.getNavalBombardCasualtiesReturnFire(
                    battleState.getGameData().getProperties())
                ? MustFightBattle.ReturnFire.ALL
                : MustFightBattle.ReturnFire.NONE)
        .diceRoller(new BombardmentDiceRoller())
        .casualtySelector(new BombardmentCasualtySelector())
        .build()
        .createSteps();
  }

  private boolean valid() {
    return battleState.getStatus().isFirstRound()
        && !battleState.getBombardingUnits().isEmpty()
        && !battleState.getBattleSite().isWater();
  }

  public static class BombardmentDiceRoller
      implements BiFunction<IDelegateBridge, RollDiceStep, DiceRoll> {

    @Override
    public DiceRoll apply(final IDelegateBridge bridge, final RollDiceStep step) {
      return DiceRoll.rollDice(
          step.getFiringGroup().getFiringUnits(),
          step.getBattleState().getPlayer(step.getSide()),
          bridge,
          DiceRoll.getAnnotation(
              step.getFiringGroup().getFiringUnits(),
              step.getBattleState().getPlayer(step.getSide()),
              step.getBattleState().getBattleSite(),
              step.getBattleState().getStatus().getRound()),
          CombatValueBuilder.navalBombardmentCombatValue()
              .enemyUnits(step.getBattleState().filterUnits(ALIVE, step.getSide().getOpposite()))
              .friendlyUnits(step.getBattleState().filterUnits(ALIVE, step.getSide()))
              .supportAttachments(
                  step.getBattleState().getGameData().getUnitTypeList().getSupportRules())
              .lhtrHeavyBombers(
                  Properties.getLhtrHeavyBombers(
                      step.getBattleState().getGameData().getProperties()))
              .gameDiceSides(step.getBattleState().getGameData().getDiceSides())
              .territoryEffects(step.getBattleState().getTerritoryEffects())
              .build());
    }
  }

  public static class BombardmentCasualtySelector
      implements BiFunction<IDelegateBridge, SelectCasualties, CasualtyDetails> {

    @Override
    public CasualtyDetails apply(final IDelegateBridge bridge, final SelectCasualties step) {
      return CasualtySelector.selectCasualties(
          step.getBattleState().getPlayer(step.getSide().getOpposite()),
          step.getFiringGroup().getTargetUnits(),
          CombatValueBuilder.navalBombardmentCombatValue()
              .enemyUnits(step.getBattleState().filterUnits(ALIVE, step.getSide()))
              .friendlyUnits(step.getBattleState().filterUnits(ALIVE, step.getSide().getOpposite()))
              .supportAttachments(
                  step.getBattleState().getGameData().getUnitTypeList().getSupportRules())
              .lhtrHeavyBombers(
                  Properties.getLhtrHeavyBombers(
                      step.getBattleState().getGameData().getProperties()))
              .gameDiceSides(step.getBattleState().getGameData().getDiceSides())
              .territoryEffects(step.getBattleState().getTerritoryEffects())
              .build(),
          step.getBattleState().getBattleSite(),
          bridge,
          "Hits from " + step.getFiringGroup().getDisplayName() + ", ",
          step.getFireRoundState().getDice(),
          step.getBattleState().getBattleId(),
          step.getBattleState().getStatus().isHeadless(),
          step.getFireRoundState().getDice().getHits(),
          true);
    }
  }
}
