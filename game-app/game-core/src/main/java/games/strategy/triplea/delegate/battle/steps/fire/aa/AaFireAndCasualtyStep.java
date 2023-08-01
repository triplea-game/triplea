package games.strategy.triplea.delegate.battle.steps.fire.aa;

import static games.strategy.triplea.delegate.battle.BattleState.UnitBattleFilter.ALIVE;

import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.MustFightBattle;
import games.strategy.triplea.delegate.battle.casualty.AaCasualtySelector;
import games.strategy.triplea.delegate.battle.steps.BattleStep;
import games.strategy.triplea.delegate.battle.steps.fire.FireRoundStepsFactory;
import games.strategy.triplea.delegate.battle.steps.fire.RollDiceStep;
import games.strategy.triplea.delegate.battle.steps.fire.SelectCasualties;
import games.strategy.triplea.delegate.data.CasualtyDetails;
import games.strategy.triplea.delegate.dice.RollDiceFactory;
import games.strategy.triplea.delegate.power.calculator.CombatValueBuilder;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.triplea.sound.SoundUtils;

@AllArgsConstructor
public abstract class AaFireAndCasualtyStep implements BattleStep {
  private static final long serialVersionUID = -3195299749378932928L;

  protected final BattleState battleState;

  protected final BattleActions battleActions;

  @Override
  public List<StepDetails> getAllStepDetails() {
    return getSteps().stream()
        .flatMap(step -> step.getAllStepDetails().stream())
        .collect(Collectors.toList());
  }

  @Override
  public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
    final List<BattleStep> steps = getSteps();

    // steps go in reverse order on the stack
    Collections.reverse(steps);
    steps.forEach(stack::push);
  }

  private List<BattleStep> getSteps() {
    return FireRoundStepsFactory.builder()
        .battleState(battleState)
        .battleActions(battleActions)
        .firingGroupSplitter(FiringGroupSplitterAa.of(getSide()))
        .side(getSide())
        .returnFire(MustFightBattle.ReturnFire.ALL)
        .diceRoller(new AaDiceRoller())
        .casualtySelector(new SelectAaCasualties())
        .build()
        .createSteps();
  }

  abstract BattleState.Side getSide();

  public static class SelectAaCasualties
      implements BiFunction<IDelegateBridge, SelectCasualties, CasualtyDetails>, Serializable {
    private static final long serialVersionUID = 5901942018345133129L;

    @Override
    public CasualtyDetails apply(final IDelegateBridge bridge, final SelectCasualties step) {
      return AaCasualtySelector.getAaCasualties(
          step.getFiringGroup().getTargetUnits(),
          step.getFiringGroup().getFiringUnits(),
          CombatValueBuilder.mainCombatValue()
              .enemyUnits(step.getBattleState().filterUnits(ALIVE, step.getSide()))
              .friendlyUnits(step.getBattleState().filterUnits(ALIVE, step.getSide().getOpposite()))
              .side(step.getSide().getOpposite())
              .gameSequence(step.getBattleState().getGameData().getSequence())
              .supportAttachments(
                  step.getBattleState().getGameData().getUnitTypeList().getSupportRules())
              .lhtrHeavyBombers(
                  Properties.getLhtrHeavyBombers(
                      step.getBattleState().getGameData().getProperties()))
              .gameDiceSides(step.getBattleState().getGameData().getDiceSides())
              .territoryEffects(step.getBattleState().getTerritoryEffects())
              .build(),
          CombatValueBuilder.aaCombatValue()
              .enemyUnits(step.getBattleState().filterUnits(ALIVE, step.getSide().getOpposite()))
              .friendlyUnits(step.getBattleState().filterUnits(ALIVE, step.getSide()))
              .side(step.getSide())
              .supportAttachments(
                  step.getBattleState().getGameData().getUnitTypeList().getSupportAaRules())
              .build(),
          "Hits from " + step.getFiringGroup().getDisplayName() + ", ",
          step.getFireRoundState().getDice(),
          bridge,
          step.getBattleState().getPlayer(step.getSide().getOpposite()),
          step.getBattleState().getBattleId(),
          step.getBattleState().getBattleSite());
    }
  }

  public static class AaDiceRoller
      implements BiFunction<IDelegateBridge, RollDiceStep, DiceRoll>, Serializable {
    private static final long serialVersionUID = -2206313715234203245L;

    @Override
    public DiceRoll apply(final IDelegateBridge bridge, final RollDiceStep step) {
      final DiceRoll dice =
          RollDiceFactory.rollAaDice(
              step.getFiringGroup().getTargetUnits(),
              step.getFiringGroup().getFiringUnits(),
              bridge,
              step.getBattleState().getBattleSite(),
              CombatValueBuilder.aaCombatValue()
                  .enemyUnits(
                      step.getBattleState().filterUnits(ALIVE, step.getSide().getOpposite()))
                  .friendlyUnits(step.getBattleState().filterUnits(ALIVE, step.getSide()))
                  .side(step.getSide())
                  .supportAttachments(
                      step.getBattleState().getGameData().getUnitTypeList().getSupportAaRules())
                  .build());

      SoundUtils.playFireBattleAa(
          step.getBattleState().getPlayer(step.getSide()),
          step.getFiringGroup().getGroupName(),
          dice.getHits() > 0,
          bridge);
      return dice;
    }
  }
}
