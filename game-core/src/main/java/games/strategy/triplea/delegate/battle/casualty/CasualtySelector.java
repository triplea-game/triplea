package games.strategy.triplea.delegate.battle.casualty;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.player.Player;
import games.strategy.triplea.Properties;
import games.strategy.triplea.ai.weak.WeakAi;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.BaseEditDelegate;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.UnitBattleComparator.CombatModifiers;
import games.strategy.triplea.delegate.data.CasualtyDetails;
import games.strategy.triplea.delegate.data.CasualtyList;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.util.TuvUtils;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeparator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;
import org.triplea.util.Tuple;

/**
 * Utility class for determining casualties and selecting casualties. The code was being duplicated
 * all over the place.
 */
@Log
@UtilityClass
public class CasualtySelector {

  public static void clearOolCache() {
    CasualtyOrderOfLosses.clearOolCache();
  }

  /**
   * Selects casualties for the specified battle.
   *
   * @param battleId may be null if we are not in a battle (eg, if this is an aa fire due to
   *     moving).
   */
  public static CasualtyDetails selectCasualties(
      final GamePlayer player,
      final Collection<Unit> targetsToPickFrom,
      final Collection<Unit> friendlyUnits,
      final Collection<Unit> enemyUnits,
      final boolean amphibious,
      final Collection<Unit> amphibiousLandAttackers,
      final Territory battlesite,
      final Collection<TerritoryEffect> territoryEffects,
      final IDelegateBridge bridge,
      final String text,
      final DiceRoll dice,
      final boolean defending,
      final UUID battleId,
      final boolean headLess,
      final int extraHits,
      final boolean allowMultipleHitsPerUnit) {
    if (targetsToPickFrom.isEmpty()) {
      return new CasualtyDetails();
    }
    if (!friendlyUnits.containsAll(targetsToPickFrom)) {
      throw new IllegalStateException(
          "friendlyUnits should but does not contain all units from targetsToPickFrom"
              + ", battlesite: "
              + battlesite
              + ", friendlyUnits: "
              + friendlyUnits
              + ", targetsToPickFrom: "
              + targetsToPickFrom);
    }
    final GameData data = bridge.getData();
    final boolean isEditMode = BaseEditDelegate.getEditMode(data);
    final Player tripleaPlayer =
        player.isNull() ? new WeakAi(player.getName()) : bridge.getRemotePlayer(player);
    final Map<Unit, Collection<Unit>> dependents =
        headLess ? Map.of() : CasualtyUtil.getDependents(targetsToPickFrom);
    if (isEditMode && !headLess) {
      final CasualtyDetails editSelection =
          tripleaPlayer.selectCasualties(
              targetsToPickFrom,
              dependents,
              0,
              text,
              dice,
              player,
              friendlyUnits,
              enemyUnits,
              amphibious,
              amphibiousLandAttackers,
              new CasualtyList(),
              battleId,
              battlesite,
              allowMultipleHitsPerUnit);
      final List<Unit> killed = editSelection.getKilled();
      // if partial retreat is possible, kill amphibious units first
      if (Properties.getPartialAmphibiousRetreat(data)) {
        killAmphibiousFirst(killed, targetsToPickFrom);
      }
      return editSelection;
    }
    if (dice.getHits() == 0) {
      return new CasualtyDetails(List.of(), List.of(), true);
    }
    int hitsRemaining = dice.getHits();
    if (Properties.getTransportCasualtiesRestricted(data)) {
      hitsRemaining = extraHits;
    }
    if (!isEditMode && allTargetsOneTypeOneHitPoint(targetsToPickFrom, dependents)) {
      final List<Unit> killed = new ArrayList<>();
      final Iterator<Unit> iter = targetsToPickFrom.iterator();
      for (int i = 0; i < hitsRemaining; i++) {
        if (i >= targetsToPickFrom.size()) {
          break;
        }
        killed.add(iter.next());
      }
      return new CasualtyDetails(killed, List.of(), true);
    }
    // Create production cost map, Maybe should do this elsewhere, but in case prices change, we do
    // it here.
    final IntegerMap<UnitType> costs = TuvUtils.getCostsForTuv(player, data);
    final Tuple<CasualtyList, List<Unit>> defaultCasualtiesAndSortedTargets =
        getDefaultCasualties(
            targetsToPickFrom,
            hitsRemaining,
            defending,
            player,
            enemyUnits,
            amphibious,
            amphibiousLandAttackers,
            battlesite,
            costs,
            territoryEffects,
            data,
            allowMultipleHitsPerUnit);
    final CasualtyList defaultCasualties = defaultCasualtiesAndSortedTargets.getFirst();
    final List<Unit> sortedTargetsToPickFrom = defaultCasualtiesAndSortedTargets.getSecond();
//    if (sortedTargetsToPickFrom.size() != targetsToPickFrom.size()
//        || !targetsToPickFrom.containsAll(sortedTargetsToPickFrom)
//        || !sortedTargetsToPickFrom.containsAll(targetsToPickFrom)) {
//      throw new IllegalStateException(
//          "sortedTargetsToPickFrom must contain the same units as targetsToPickFrom list");
//    }
    final int totalHitpoints =
        (allowMultipleHitsPerUnit
            ? CasualtyUtil.getTotalHitpointsLeft(sortedTargetsToPickFrom)
            : sortedTargetsToPickFrom.size());
    final CasualtyDetails casualtySelection;
    if (hitsRemaining >= totalHitpoints) {
      casualtySelection = new CasualtyDetails(defaultCasualties, true);
    } else {
      casualtySelection =
          tripleaPlayer.selectCasualties(
              sortedTargetsToPickFrom,
              dependents,
              hitsRemaining,
              text,
              dice,
              player,
              friendlyUnits,
              enemyUnits,
              amphibious,
              amphibiousLandAttackers,
              defaultCasualties,
              battleId,
              battlesite,
              allowMultipleHitsPerUnit);
    }
    final List<Unit> killed = casualtySelection.getKilled();
    // if partial retreat is possible, kill amphibious units first
    if (Properties.getPartialAmphibiousRetreat(data)) {
      killAmphibiousFirst(killed, sortedTargetsToPickFrom);
    }
    final List<Unit> damaged = casualtySelection.getDamaged();
    int numhits = killed.size();
    if (!allowMultipleHitsPerUnit) {
      damaged.clear();
    } else {
      for (final Unit unit : killed) {
        final UnitAttachment ua = UnitAttachment.get(unit.getType());
        final int damageToUnit = Collections.frequency(damaged, unit);
        // allowed damage
        numhits += Math.max(0, Math.min(damageToUnit, (ua.getHitPoints() - (1 + unit.getHits()))));
        // remove from damaged list, since they will die
        damaged.removeIf(unit::equals);
      }
    }
    // check right number
    if (!isEditMode && numhits + damaged.size() != Math.min(hitsRemaining, totalHitpoints)) {
      tripleaPlayer.reportError("Wrong number of casualties selected");
      if (headLess) {
        log.severe(
            "Possible Infinite Loop: Wrong number of casualties selected: number of hits on units "
                + (numhits + damaged.size())
                + " != number of hits to take "
                + Math.min(hitsRemaining, totalHitpoints)
                + ", for "
                + casualtySelection.toString());
      }
      return selectCasualties(
          player,
          sortedTargetsToPickFrom,
          friendlyUnits,
          enemyUnits,
          amphibious,
          amphibiousLandAttackers,
          battlesite,
          territoryEffects,
          bridge,
          text,
          dice,
          defending,
          battleId,
          headLess,
          extraHits,
          allowMultipleHitsPerUnit);
    }
    // check we have enough of each type
    if (!sortedTargetsToPickFrom.containsAll(killed)
        || !sortedTargetsToPickFrom.containsAll(damaged)) {
      tripleaPlayer.reportError("Cannot remove enough units of those types");
      if (headLess) {
        log.severe(
            "Possible Infinite Loop: Cannot remove enough units of those types: targets "
                + MyFormatter.unitsToTextNoOwner(sortedTargetsToPickFrom)
                + ", for "
                + casualtySelection.toString());
      }
      return selectCasualties(
          player,
          sortedTargetsToPickFrom,
          friendlyUnits,
          enemyUnits,
          amphibious,
          amphibiousLandAttackers,
          battlesite,
          territoryEffects,
          bridge,
          text,
          dice,
          defending,
          battleId,
          headLess,
          extraHits,
          allowMultipleHitsPerUnit);
    }
    return casualtySelection;
  }

  private static void killAmphibiousFirst(final List<Unit> killed, final Collection<Unit> targets) {
    // Get a list of all selected killed units that are NOT amphibious
    final Predicate<Unit> match = Matches.unitIsLand().and(Matches.unitWasNotAmphibious());
    final Collection<Unit> killedNonAmphibUnits =
        new ArrayList<>(CollectionUtils.getMatches(killed, match));
    // If all killed units are amphibious, just return them
    if (killedNonAmphibUnits.isEmpty()) {
      return;
    }
    // Get a list of all units that are amphibious and remove those that are killed
    final Collection<Unit> allAmphibUnits =
        new ArrayList<>(CollectionUtils.getMatches(targets, Matches.unitWasAmphibious()));
    allAmphibUnits.removeAll(CollectionUtils.getMatches(killed, Matches.unitWasAmphibious()));
    // Get a collection of the unit types of the amphib units
    final Collection<UnitType> amphibTypes = new ArrayList<>();
    for (final Unit unit : allAmphibUnits) {
      final UnitType ut = unit.getType();
      if (!amphibTypes.contains(ut)) {
        amphibTypes.add(ut);
      }
    }
    // For each killed unit- see if there is an amphib unit that can be killed instead
    for (final Unit unit : killedNonAmphibUnits) {
      if (amphibTypes.contains(unit.getType())) { // add a unit from the collection
        final List<Unit> oneAmphibUnit =
            CollectionUtils.getNMatches(allAmphibUnits, 1, Matches.unitIsOfType(unit.getType()));
        if (!oneAmphibUnit.isEmpty()) {
          final Unit amphibUnit = oneAmphibUnit.iterator().next();
          killed.remove(unit);
          killed.add(amphibUnit);
          allAmphibUnits.remove(amphibUnit);
        } else { // If there are no more units of that type, remove the type from the collection
          amphibTypes.remove(unit.getType());
        }
      }
    }
  }

  /**
   * A unit with two hitpoints will be listed twice if they will die. The first time they are listed
   * it is as damaged. The second time they are listed, it is dead.
   */
  private static Tuple<CasualtyList, List<Unit>> getDefaultCasualties(
      final Collection<Unit> targetsToPickFrom,
      final int hits,
      final boolean defending,
      final GamePlayer player,
      final Collection<Unit> enemyUnits,
      final boolean amphibious,
      final Collection<Unit> amphibiousLandAttackers,
      final Territory battlesite,
      final IntegerMap<UnitType> costs,
      final Collection<TerritoryEffect> territoryEffects,
      final GameData data,
      final boolean allowMultipleHitsPerUnit) {
    final CasualtyList defaultCasualtySelection = new CasualtyList();
    // Sort units by power and cost in ascending order
    final List<Unit> sorted;
    sorted =
        CasualtyOrderOfLosses.sortUnitsForCasualtiesWithSupport(
            CasualtyOrderOfLosses.Parameters.builder()
                .hits(hits)
                .targetsToPickFrom(targetsToPickFrom)
                .player(player)
                .enemyUnits(enemyUnits)
                .combatModifiers(
                    CombatModifiers.builder()
                        .territoryEffects(territoryEffects)
                        .amphibious(amphibious)
                        .defending(defending)
                        .build())
                .amphibiousLandAttackers(
                    amphibiousLandAttackers == null ? List.of() : amphibiousLandAttackers)
                .battlesite(battlesite)
                .costs(costs)
                .data(data)
                .build());
    // Remove two hit bb's selecting them first for default casualties
    int numSelectedCasualties = 0;
    if (allowMultipleHitsPerUnit) {
      for (final Unit unit : sorted) {
        // Stop if we have already selected as many hits as there are targets
        if (numSelectedCasualties >= hits) {
          return Tuple.of(defaultCasualtySelection, sorted);
        }
        final UnitAttachment ua = UnitAttachment.get(unit.getType());
        final int extraHitPoints =
            Math.min((hits - numSelectedCasualties), (ua.getHitPoints() - (1 + unit.getHits())));
        for (int i = 0; i < extraHitPoints; i++) {
          numSelectedCasualties++;
          defaultCasualtySelection.addToDamaged(unit);
        }
      }
    }
    // Select units
    for (final Unit unit : sorted) {
      // Stop if we have already selected as many hits as there are targets
      if (numSelectedCasualties >= hits) {
        return Tuple.of(defaultCasualtySelection, sorted);
      }
      defaultCasualtySelection.addToKilled(unit);
      numSelectedCasualties++;
    }
    return Tuple.of(defaultCasualtySelection, sorted);
  }

  /**
   * Checks if the given collections target are all of one category as defined by
   * UnitSeparator.categorize and they are not two hit units.
   *
   * @param targets a collection of target units
   * @param dependents map of depend units for target units
   */
  private static boolean allTargetsOneTypeOneHitPoint(
      final Collection<Unit> targets, final Map<Unit, Collection<Unit>> dependents) {
    final Set<UnitCategory> categorized =
        UnitSeparator.categorize(targets, dependents, false, false);
    if (categorized.size() == 1) {
      final UnitCategory unitCategory = categorized.iterator().next();
      return unitCategory.getHitPoints() - unitCategory.getDamaged() <= 1;
    }
    return false;
  }
}
