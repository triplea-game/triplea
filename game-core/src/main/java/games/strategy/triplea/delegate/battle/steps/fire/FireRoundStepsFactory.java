package games.strategy.triplea.delegate.battle.steps.fire;

import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.MustFightBattle;
import games.strategy.triplea.delegate.battle.steps.BattleStep;
import games.strategy.triplea.delegate.data.CasualtyDetails;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.NonNull;

/** Build the steps for the fire round (roll dice, select casualties, and mark casualties) */
@Builder
public class FireRoundStepsFactory {

  @NonNull final BattleState battleState;
  @NonNull final BattleActions battleActions;
  @NonNull final Function<BattleState, Collection<FiringGroup>> firingGroupSplitter;
  @NonNull final BattleState.Side side;
  @NonNull final MustFightBattle.ReturnFire returnFire;
  @NonNull final BiFunction<IDelegateBridge, RollDice, DiceRoll> diceRoller;
  @NonNull final BiFunction<IDelegateBridge, SelectCasualties, CasualtyDetails> casualtySelector;

  public List<BattleStep> createSteps() {
    return firingGroupSplitter.apply(battleState).stream()
        .sorted(
            Comparator.comparing(FiringGroup::getDisplayName)
                .thenComparing(FiringGroup::isSuicideOnHit))
        .map(
            firingGroup -> {
              final FireRoundState fireRoundState = new FireRoundState();
              return List.of(
                  new RollDice(battleState, side, firingGroup, fireRoundState, diceRoller),
                  new SelectCasualties(
                      battleState, side, firingGroup, fireRoundState, casualtySelector),
                  new MarkCasualties(
                      battleState, battleActions, side, firingGroup, fireRoundState, returnFire));
            })
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }
}
