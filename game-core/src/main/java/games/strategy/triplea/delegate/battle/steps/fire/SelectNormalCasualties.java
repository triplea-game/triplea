package games.strategy.triplea.delegate.battle.steps.fire;

import static games.strategy.triplea.delegate.battle.BattleState.Side.DEFENSE;
import static games.strategy.triplea.delegate.battle.BattleState.UnitBattleFilter.ACTIVE;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.BaseEditDelegate;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.casualty.CasualtySelector;
import games.strategy.triplea.delegate.data.CasualtyDetails;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import lombok.NoArgsConstructor;
import org.triplea.java.collections.CollectionUtils;

/** Selects casualties for normal (basically, anything that isn't AA) hits */
@NoArgsConstructor
public class SelectNormalCasualties
    implements BiFunction<IDelegateBridge, SelectCasualties, CasualtyDetails> {

  private Select selectFunction = new Select();

  @VisibleForTesting
  SelectNormalCasualties(final Select selectFunction) {
    this.selectFunction = selectFunction;
  }

  @Override
  public CasualtyDetails apply(final IDelegateBridge bridge, final SelectCasualties step) {

    if (BaseEditDelegate.getEditMode(step.getBattleState().getGameData())) {
      final CasualtyDetails message =
          selectFunction.apply(bridge, step, step.getFiringGroup().getTargetUnits(), 0);
      return new CasualtyDetails(message, true);
    }

    final List<Unit> combatUnits;
    final Collection<Unit> restrictedTransports;
    if (Properties.getTransportCasualtiesRestricted(step.getBattleState().getGameData())) {
      combatUnits =
          CollectionUtils.getMatches(
              step.getFiringGroup().getTargetUnits(),
              Matches.unitIsNotTransportButCouldBeCombatTransport().or(Matches.unitIsNotSea()));
      restrictedTransports =
          CollectionUtils.getMatches(
              step.getFiringGroup().getTargetUnits(),
              Matches.unitIsTransportButNotCombatTransport().and(Matches.unitIsSea()));
    } else {
      combatUnits = new ArrayList<>(step.getFiringGroup().getTargetUnits());
      restrictedTransports = List.of();
    }

    final int totalHitPointsAvailable = getMaxHits(combatUnits);
    final int hitCount = step.getFireRoundState().getDice().getHits();
    final int hitsLeftForRestrictedTransports = hitCount - totalHitPointsAvailable;

    if (totalHitPointsAvailable > hitCount) {
      // not all units were hit so the player needs to pick which ones are killed
      return selectFunction.apply(
          bridge, step, combatUnits, step.getFireRoundState().getDice().getHits());

    } else if (totalHitPointsAvailable == hitCount || restrictedTransports.isEmpty()) {
      // all of the combat units were hit so kill them without asking the player
      return new CasualtyDetails(combatUnits, List.of(), true);

    } else if (hitsLeftForRestrictedTransports >= restrictedTransports.size()) {
      // in addition to the combat units, all of the restricted transports were hit
      // so kill them all without asking the player
      combatUnits.addAll(restrictedTransports);
      return new CasualtyDetails(combatUnits, List.of(), true);

    } else {
      // not all restricted transports were hit so the player needs to pick which ones are killed
      final CasualtyDetails message =
          selectFunction.apply(
              bridge,
              step,
              limitTransportsToSelect(restrictedTransports, hitsLeftForRestrictedTransports),
              hitsLeftForRestrictedTransports);
      combatUnits.addAll(message.getKilled());
      return new CasualtyDetails(combatUnits, List.of(), true);
    }
  }

  /**
   * The maximum number of hits that this collection of units can sustain, taking into account units
   * with two hits, and accounting for existing damage.
   */
  private static int getMaxHits(final Collection<Unit> units) {
    int count = 0;
    for (final Unit unit : units) {
      count += UnitAttachment.get(unit.getType()).getHitPoints();
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

  static class Select {
    public CasualtyDetails apply(
        final IDelegateBridge bridge,
        final SelectCasualties step,
        final Collection<Unit> targetsToPickFrom,
        final int diceHitOverride) {

      return CasualtySelector.selectCasualties(
          step.getBattleState().getPlayer(step.getSide().getOpposite()),
          targetsToPickFrom,
          step.getBattleState().filterUnits(ACTIVE, step.getSide().getOpposite()),
          step.getBattleState().filterUnits(ACTIVE, step.getSide()),
          step.getBattleState().getBattleSite(),
          step.getBattleState().getTerritoryEffects(),
          bridge,
          "Hits from " + step.getFiringGroup().getDisplayName() + ", ",
          step.getFireRoundState().getDice(),
          step.getSide().getOpposite() == DEFENSE,
          step.getBattleState().getBattleId(),
          step.getBattleState().getStatus().isHeadless(),
          diceHitOverride,
          true);
    }
  }
}
