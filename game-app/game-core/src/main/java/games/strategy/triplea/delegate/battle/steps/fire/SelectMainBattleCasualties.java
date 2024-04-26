package games.strategy.triplea.delegate.battle.steps.fire;

import static games.strategy.triplea.delegate.battle.BattleState.UnitBattleFilter.ALIVE;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.EditDelegate;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.casualty.CasualtySelector;
import games.strategy.triplea.delegate.data.CasualtyDetails;
import games.strategy.triplea.delegate.power.calculator.CombatValueBuilder;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import lombok.NoArgsConstructor;
import lombok.Value;
import org.triplea.java.collections.CollectionUtils;

/** Selects casualties for normal (basically, anything that isn't AA) hits */
@NoArgsConstructor
public class SelectMainBattleCasualties
    implements BiFunction<IDelegateBridge, SelectCasualties, CasualtyDetails>, Serializable {
  private static final long serialVersionUID = -7908927357325784680L;

  private Select selectFunction = new Select();

  @VisibleForTesting
  SelectMainBattleCasualties(final Select selectFunction) {
    this.selectFunction = selectFunction;
  }

  @Override
  public CasualtyDetails apply(final IDelegateBridge bridge, final SelectCasualties step) {

    final TargetUnits targetUnits = getTargetUnits(step);
    final int totalHitPointsAvailable = getMaxHits(targetUnits.combatUnits);
    final int hitCount = step.getFireRoundState().getDice().getHits();
    final int hitsLeftForRestrictedTransports = hitCount - totalHitPointsAvailable;

    final CasualtyDetails casualtyDetails;
    if (EditDelegate.getEditMode(step.getBattleState().getGameData().getProperties())) {
      final CasualtyDetails message =
          selectFunction.apply(bridge, step, step.getFiringGroup().getTargetUnits(), 0);
      casualtyDetails = new CasualtyDetails(message, true);

    } else if (totalHitPointsAvailable > hitCount) {
      // not all units were hit so the player needs to pick which ones are killed
      casualtyDetails =
          selectFunction.apply(
              bridge, step, targetUnits.combatUnits, step.getFireRoundState().getDice().getHits());

    } else if (totalHitPointsAvailable == hitCount || targetUnits.restrictedTransports.isEmpty()) {
      // all of the combat units were hit so kill them without asking the player
      casualtyDetails = new CasualtyDetails(targetUnits.combatUnits, List.of(), true);

    } else if (hitsLeftForRestrictedTransports >= targetUnits.restrictedTransports.size()) {
      // in addition to the combat units, all of the restricted transports were hit
      // so kill them all without asking the player
      targetUnits.combatUnits.addAll(targetUnits.restrictedTransports);
      casualtyDetails = new CasualtyDetails(targetUnits.combatUnits, List.of(), true);

    } else {
      // not all restricted transports were hit so the player needs to pick which ones are killed
      final CasualtyDetails message =
          selectFunction.apply(
              bridge,
              step,
              limitTransportsToSelect(
                  targetUnits.restrictedTransports, hitsLeftForRestrictedTransports),
              hitsLeftForRestrictedTransports);
      targetUnits.combatUnits.addAll(message.getKilled());
      casualtyDetails = new CasualtyDetails(targetUnits.combatUnits, List.of(), true);
    }
    return casualtyDetails;
  }

  @Value(staticConstructor = "of")
  private static class TargetUnits {
    List<Unit> combatUnits;
    List<Unit> restrictedTransports;
  }

  private TargetUnits getTargetUnits(final SelectCasualties step) {
    final TargetUnits targetUnits;
    if (Properties.getTransportCasualtiesRestricted(
        step.getBattleState().getGameData().getProperties())) {
      targetUnits =
          TargetUnits.of(
              CollectionUtils.getMatches(
                  step.getFiringGroup().getTargetUnits(),
                  Matches.unitIsNotSeaTransportButCouldBeCombatSeaTransport()
                      .or(Matches.unitIsNotSea())),
              CollectionUtils.getMatches(
                  step.getFiringGroup().getTargetUnits(),
                  Matches.unitIsSeaTransportButNotCombatSeaTransport().and(Matches.unitIsSea())));
    } else {
      targetUnits =
          TargetUnits.of(new ArrayList<>(step.getFiringGroup().getTargetUnits()), List.of());
    }
    return targetUnits;
  }

  /**
   * The maximum number of hits that this collection of units can sustain, taking into account units
   * with two hits, and accounting for existing damage.
   */
  private static int getMaxHits(final Collection<Unit> units) {
    int count = 0;
    for (final Unit unit : units) {
      count += unit.getUnitAttachment().getHitPoints();
      count -= unit.getHits();
    }
    return count;
  }

  /**
   * Limit the number of transports to hitsLeftForTransports per ally
   *
   * @param restrictedTransports All the transports available to be selected
   * @param hitsLeftForTransports The number of transports per ally to return
   * @return List of transports that is equal to a maximum of hitsLeftForTransports * ally count
   */
  private List<Unit> limitTransportsToSelect(
      final Collection<Unit> restrictedTransports, final int hitsLeftForTransports) {
    final Map<GamePlayer, Collection<Unit>> alliedHitPlayer = new HashMap<>();
    for (final Unit unit : restrictedTransports) {
      alliedHitPlayer.computeIfAbsent(unit.getOwner(), (owner) -> new ArrayList<>()).add(unit);
    }
    final List<Unit> transportsToSelect = new ArrayList<>();
    for (final Map.Entry<GamePlayer, Collection<Unit>> playerTransports :
        alliedHitPlayer.entrySet()) {
      transportsToSelect.addAll(
          playerTransports.getValue().stream()
              .limit(hitsLeftForTransports)
              .collect(Collectors.toList()));
    }
    return transportsToSelect;
  }

  static class Select implements Serializable {
    private static final long serialVersionUID = 7023198402140098480L;

    public CasualtyDetails apply(
        final IDelegateBridge bridge,
        final SelectCasualties step,
        final Collection<Unit> targetsToPickFrom,
        final int diceHitOverride) {

      return CasualtySelector.selectCasualties(
          step.getBattleState().getPlayer(step.getSide().getOpposite()),
          targetsToPickFrom,
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
          step.getBattleState().getBattleSite(),
          bridge,
          "Hits from " + step.getFiringGroup().getDisplayName() + ", ",
          step.getFireRoundState().getDice(),
          step.getBattleState().getBattleId(),
          step.getBattleState().getStatus().isHeadless(),
          diceHitOverride,
          true);
    }
  }
}
