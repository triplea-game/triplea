package games.strategy.triplea.delegate.battle.steps.change;

import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.steps.BattleStep;
import games.strategy.triplea.delegate.battle.steps.RetreatChecks;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class CheckGeneralBattleEnd implements BattleStep {
  private static final long serialVersionUID = 5172121497955756220L;

  private final BattleState battleState;

  private final BattleActions battleActions;

  protected BattleActions getBattleActions() {
    return battleActions;
  }

  protected BattleState getBattleState() {
    return battleState;
  }

  @Override
  public List<String> getNames() {
    return List.of();
  }

  @Override
  public Order getOrder() {
    return Order.GENERAL_BATTLE_END_CHECK;
  }

  @Override
  public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
    if (hasSideLost(BattleState.Side.OFFENSE)) {
      battleActions.endBattle(bridge);
      battleActions.defenderWins(bridge);

    } else if (hasSideLost(BattleState.Side.DEFENSE)) {
      battleActions.endBattle(bridge);
      battleActions.attackerWins(bridge);

    } else if (isStalemate() && !canAttackerRetreatInStalemate()) {
      battleActions.endBattle(bridge);
      battleActions.nobodyWins(bridge);
    }
  }

  protected boolean hasSideLost(final BattleState.Side side) {
    return battleState.getUnits(side).stream().noneMatch(Matches.unitIsNotInfrastructure());
  }

  protected boolean isStalemate() {
    return battleState.getBattleRoundState().isLastRound()
        || (getPower(BattleState.Side.OFFENSE) == 0 && getPower(BattleState.Side.DEFENSE) == 0);
  }

  private int getPower(final BattleState.Side side) {
    return DiceRoll.getTotalPowerAndRolls(
            DiceRoll.getUnitPowerAndRollsForNormalBattles(
                battleState.getUnits(side),
                battleState.getUnits(side.getOpposite()),
                battleState.getUnits(side),
                side == BattleState.Side.DEFENSE,
                battleState.getGameData(),
                battleState.getBattleSite(),
                battleState.getTerritoryEffects(),
                battleState.isAmphibious(),
                battleState.getAmphibiousLandAttackers()),
            battleState.getGameData())
        .getEffectivePower();
  }

  protected boolean canAttackerRetreatInStalemate() {
    // First check if any units have an explicit "can retreat on stalemate"
    // property. If none do (all are null), then we will use a fallback algorithm.
    // If any unit has "can retreat on stalemate" set, then we will return true
    // only if all units either have the property set to null or true, if any
    // are set to false then we will return false.

    // Otherwise, if we do not have an explicit property, then we fallback
    // to enforcing the V3 transport vs transport rule that allows retreat in
    // that situation. Ideally all maps would explicitly use the "can retreat
    // on stalemate property", but not all do so we need to account for the
    // V3 transport vs transport rule as a fallback algorithm without it.

    // First, collect all of the non-null 'can retreat on stalemate' option values.
    final Set<Boolean> canRetreatOptions =
        battleState.getUnits(BattleState.Side.OFFENSE).stream()
            .map(Unit::getType)
            .map(UnitAttachment::get)
            .map(UnitAttachment::getCanRetreatOnStalemate)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    final boolean propertyIsSetAtLeastOnce = !canRetreatOptions.isEmpty();

    // next, check if all of the non-null properties are set to true.
    final boolean allowRetreatFromProperty = canRetreatOptions.stream().allMatch(b -> b);

    return (propertyIsSetAtLeastOnce && allowRetreatFromProperty)
        || (!propertyIsSetAtLeastOnce && transportsVsTransports());
  }

  private boolean transportsVsTransports() {
    // Check if both sides have only V3 (non-combat) transports remaining.
    // See: https://github.com/triplea-game/triplea/issues/2367
    // Rule: "In a sea battle, if both sides have only transports remaining, the
    // attacker’s transports can remain in the contested sea zone or retreat.
    return RetreatChecks.onlyDefenselessTransportsLeft(
            battleState.getUnits(BattleState.Side.OFFENSE), battleState.getGameData())
        && RetreatChecks.onlyDefenselessTransportsLeft(
            battleState.getUnits(BattleState.Side.DEFENSE), battleState.getGameData());
  }
}
