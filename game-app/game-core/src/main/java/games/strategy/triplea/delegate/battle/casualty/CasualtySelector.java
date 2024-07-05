package games.strategy.triplea.delegate.battle.casualty;

import com.google.common.base.Preconditions;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.player.Player;
import games.strategy.triplea.Properties;
import games.strategy.triplea.ai.weak.WeakAi;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.EditDelegate;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.data.CasualtyDetails;
import games.strategy.triplea.delegate.data.CasualtyList;
import games.strategy.triplea.delegate.power.calculator.CombatValue;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeparator;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;
import org.triplea.util.Tuple;

/**
 * Utility class for determining casualties and selecting casualties. The code was being duplicated
 * all over the place.
 */
@Slf4j
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
      final CombatValue combatValue,
      final Territory battleSite,
      final IDelegateBridge bridge,
      final String text,
      final DiceRoll dice,
      final UUID battleId,
      final boolean headLess,
      final int extraHits,
      final boolean allowMultipleHitsPerUnit) {
    if (targetsToPickFrom.isEmpty()) {
      return new CasualtyDetails();
    }
    final GameData data = bridge.getData();

    final Player tripleaPlayer =
        player.isNull() ? new WeakAi(player.getName()) : bridge.getRemotePlayer(player);
    final Map<Unit, Collection<Unit>> dependents =
        headLess ? Map.of() : CasualtyUtil.getDependents(targetsToPickFrom);

    final int hitsRemaining =
        Properties.getTransportCasualtiesRestricted(data.getProperties())
            ? extraHits
            : dice.getHits();

    if (EditDelegate.getEditMode(data.getProperties())) {
      return tripleaPlayer.selectCasualties(
          targetsToPickFrom,
          dependents,
          hitsRemaining,
          text,
          dice,
          player,
          combatValue.getFriendUnits(),
          combatValue.getEnemyUnits(),
          false,
          List.of(),
          new CasualtyDetails(),
          battleId,
          battleSite,
          allowMultipleHitsPerUnit);
    }

    if (dice.getHits() == 0) {
      return new CasualtyDetails();
    }

    // Create production cost map, Maybe should do this elsewhere, but in case prices change, we do
    // it here.
    final IntegerMap<UnitType> costs = bridge.getCostsForTuv(player);
    final Tuple<CasualtyList, List<Unit>> defaultCasualtiesAndSortedTargets =
        getDefaultCasualties(
            targetsToPickFrom,
            hitsRemaining,
            player,
            combatValue,
            battleSite,
            costs,
            data,
            allowMultipleHitsPerUnit);
    final CasualtyList defaultCasualties = defaultCasualtiesAndSortedTargets.getFirst();
    final List<Unit> sortedTargetsToPickFrom = defaultCasualtiesAndSortedTargets.getSecond();
    if (sortedTargetsToPickFrom.size() != targetsToPickFrom.size()) {
      throw new IllegalStateException(
          "sortedTargetsToPickFrom must have the same size as targetsToPickFrom list");
    }
    final int totalHitpoints =
        (allowMultipleHitsPerUnit
            ? CasualtyUtil.getTotalHitpointsLeft(sortedTargetsToPickFrom)
            : sortedTargetsToPickFrom.size());

    final boolean autoChooseCasualties =
        hitsRemaining >= totalHitpoints
            || sortedTargetsToPickFrom.size() == 1
            || allTargetsOneTypeOneHitPoint(
                sortedTargetsToPickFrom, dependents, data.getProperties());
    final CasualtyDetails casualtyDetails;
    if (autoChooseCasualties) {
      casualtyDetails = new CasualtyDetails(defaultCasualties, true);
    } else {
      Preconditions.checkState(
          defaultCasualties.size() == hitsRemaining,
          String.format(
              "Select Casualties showing different numbers for number of hits to take (%s) vs "
                  + "total size of default casualty selections (%s) in %s (player = %s)",
              hitsRemaining, defaultCasualties.size(), battleSite, player.getName()));
      casualtyDetails =
          tripleaPlayer.selectCasualties(
              sortedTargetsToPickFrom,
              dependents,
              hitsRemaining,
              text,
              dice,
              player,
              combatValue.getFriendUnits(),
              combatValue.getEnemyUnits(),
              false,
              List.of(),
              defaultCasualties,
              battleId,
              battleSite,
              allowMultipleHitsPerUnit);
    }

    if (!Properties.getPartialAmphibiousRetreat(data.getProperties())) {
      final boolean unitsWithMarineBonusAndWasAmphibiousKilled =
          casualtyDetails.getKilled().stream()
              .anyMatch(u -> u.getUnitAttachment().getIsMarine() != 0 && u.getWasAmphibious());
      if (unitsWithMarineBonusAndWasAmphibiousKilled) {
        casualtyDetails.ensureUnitsWithPositiveMarineBonusAreKilledLast(sortedTargetsToPickFrom);
      }
    }

    // Prefer units with less movement left to be killed first.
    casualtyDetails.ensureUnitsAreKilledFirst(
        sortedTargetsToPickFrom, Matches.unitIsAir(), Comparator.comparing(Unit::getMovementLeft));

    // Prefer units with most movement left for damage (to have a better chance to get to safety).
    casualtyDetails.ensureUnitsAreDamagedFirst(
        sortedTargetsToPickFrom,
        Matches.unitIsAir(),
        Comparator.comparing(Unit::getMovementLeft).reversed());

    final List<Unit> damaged = casualtyDetails.getDamaged();
    final List<Unit> killed = casualtyDetails.getKilled();
    int numhits = killed.size();
    if (!allowMultipleHitsPerUnit) {
      damaged.clear();
    } else {
      for (final Unit unit : killed) {
        final UnitAttachment ua = unit.getUnitAttachment();
        final int damageToUnit = Collections.frequency(damaged, unit);
        // allowed damage
        numhits += Math.max(0, Math.min(damageToUnit, (ua.getHitPoints() - (1 + unit.getHits()))));
        // remove from damaged list, since they will die
        damaged.removeIf(unit::equals);
      }
    }

    // check right number
    if (numhits + damaged.size() != Math.min(hitsRemaining, totalHitpoints)) {
      tripleaPlayer.reportError("Wrong number of casualties selected");
      if (headLess) {
        log.error(
            "Possible Infinite Loop: Wrong number of casualties selected: number of hits on units "
                + (numhits + damaged.size())
                + " != number of hits to take "
                + Math.min(hitsRemaining, totalHitpoints)
                + ", for "
                + casualtyDetails);
      }
      return selectCasualties(
          player,
          sortedTargetsToPickFrom,
          combatValue,
          battleSite,
          bridge,
          text,
          dice,
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
        log.error(
            "Possible Infinite Loop: Cannot remove enough units of those types: targets "
                + MyFormatter.unitsToTextNoOwner(sortedTargetsToPickFrom)
                + ", for "
                + casualtyDetails);
      }
      return selectCasualties(
          player,
          sortedTargetsToPickFrom,
          combatValue,
          battleSite,
          bridge,
          text,
          dice,
          battleId,
          headLess,
          extraHits,
          allowMultipleHitsPerUnit);
    }
    return casualtyDetails;
  }

  /**
   * A unit with two hitpoints will be listed twice if they will die. The first time they are listed
   * it is as damaged. The second time they are listed, it is dead.
   */
  private static Tuple<CasualtyList, List<Unit>> getDefaultCasualties(
      final Collection<Unit> targetsToPickFrom,
      final int hits,
      final GamePlayer player,
      final CombatValue combatValue,
      final Territory battlesite,
      final IntegerMap<UnitType> costs,
      final GameData data,
      final boolean allowMultipleHitsPerUnit) {
    final CasualtyList defaultCasualtySelection = new CasualtyList();
    // Sort units by power and cost in ascending order
    final List<Unit> sorted =
        getCasualtyOrderOfLoss(targetsToPickFrom, player, combatValue, battlesite, costs, data);
    // Remove two hit bb's selecting them first for default casualties
    if (allowMultipleHitsPerUnit) {
      for (final Unit unit : sorted) {
        // Stop if we have already selected as many hits as there are targets
        final int numSelectedCasualties = defaultCasualtySelection.size();
        if (defaultCasualtySelection.size() >= hits) {
          return Tuple.of(defaultCasualtySelection, sorted);
        }
        final UnitAttachment ua = unit.getUnitAttachment();
        final int extraHitPoints =
            Math.min((hits - numSelectedCasualties), (ua.getHitPoints() - (1 + unit.getHits())));
        for (int i = 0; i < extraHitPoints; i++) {
          defaultCasualtySelection.addToDamaged(unit);
        }
      }
    }
    // Select units
    for (final Unit unit : sorted) {
      // Stop if we have already selected as many hits as there are targets
      if (defaultCasualtySelection.size() >= hits) {
        return Tuple.of(defaultCasualtySelection, sorted);
      }
      defaultCasualtySelection.addToKilled(unit);
    }
    return Tuple.of(defaultCasualtySelection, sorted);
  }

  public static List<Unit> getCasualtyOrderOfLoss(
      final Collection<Unit> targetsToPickFrom,
      final GamePlayer player,
      final CombatValue combatValue,
      final Territory battlesite,
      final IntegerMap<UnitType> costs,
      final GameData data) {
    return CasualtyOrderOfLosses.sortUnitsForCasualtiesWithSupport(
        CasualtyOrderOfLosses.Parameters.builder()
            .targetsToPickFrom(targetsToPickFrom)
            .player(player)
            .combatValue(combatValue)
            .battlesite(battlesite)
            .costs(costs)
            .data(data)
            .build());
  }

  /**
   * Checks if the given targets are all of one category as defined by <code>
   * UnitSeparator.categorize</code> and if they are not multiple hit units.
   *
   * @param targets a collection of target units
   * @param dependents map of dependent units for target units
   * @param properties game properties
   */
  private static boolean allTargetsOneTypeOneHitPoint(
      final Collection<Unit> targets,
      final Map<Unit, Collection<Unit>> dependents,
      final GameProperties properties) {
    final boolean separateByRetreatPossibility = Properties.getPartialAmphibiousRetreat(properties);
    final Set<UnitCategory> categorized =
        UnitSeparator.categorize(
            targets,
            UnitSeparator.SeparatorCategories.builder()
                .retreatPossibility(separateByRetreatPossibility)
                .dependents(dependents)
                .build());
    if (categorized.size() == 1) {
      final UnitCategory unitCategory = CollectionUtils.getAny(categorized);
      return unitCategory.getHitPoints() - unitCategory.getDamaged() <= 1;
    }
    return false;
  }
}
