package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import games.strategy.triplea.delegate.Matches;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;

/**
 * Tracks the available support that a collection of units can give to other units.
 *
 * <p>Once a support is used, it will no longer be available for other units to use.
 */
@Data
@Builder(access = AccessLevel.PACKAGE)
@AllArgsConstructor
public class AvailableSupportCalculator {

  public static final AvailableSupportCalculator EMPTY_RESULT =
      AvailableSupportCalculator.builder()
          .supportRules(new HashSet<>())
          .supportUnits(new HashMap<>())
          .supportLeft(new IntegerMap<>())
          .build();

  final Set<List<UnitSupportAttachment>> supportRules;
  final IntegerMap<UnitSupportAttachment> supportLeft;
  final Map<UnitSupportAttachment, IntegerMap<Unit>> supportUnits;

  AvailableSupportCalculator(final AvailableSupportCalculator availableSupportCalculator) {

    supportRules = availableSupportCalculator.supportRules;
    supportLeft = new IntegerMap<>(availableSupportCalculator.supportLeft);

    final Map<UnitSupportAttachment, IntegerMap<Unit>> supportUnitsCopy = new HashMap<>();
    for (final UnitSupportAttachment usa : availableSupportCalculator.supportUnits.keySet()) {
      supportUnitsCopy.put(usa, new IntegerMap<>(availableSupportCalculator.supportUnits.get(usa)));
    }
    supportUnits = supportUnitsCopy;
  }

  /** Sorts 'supportsAvailable' lists based on unit support attachment rules. */
  static AvailableSupportCalculator getSortedSupport(
      final Collection<Unit> unitsGivingTheSupport,
      final Set<UnitSupportAttachment> rules,
      final boolean defence,
      final boolean allies) {
    final AvailableSupportCalculator supportCalculationResult =
        getSupport(unitsGivingTheSupport, rules, defence, allies);

    final SupportRuleSort supportRuleSort =
        SupportRuleSort.builder()
            .defense(defence)
            .friendly(allies)
            .roll(UnitSupportAttachment::getRoll)
            .strength(UnitSupportAttachment::getStrength)
            .build();
    supportCalculationResult
        .getSupportRules()
        .forEach((unitSupportAttachment) -> unitSupportAttachment.sort(supportRuleSort));
    return supportCalculationResult;
  }

  /**
   * Constructs an AvailableSupportTracker to track the possible support given by the units
   *
   * @param defence are the receiving units defending?
   * @param allies are the receiving units allied to the giving units?
   */
  public static AvailableSupportCalculator getSupport(
      final Collection<Unit> unitsGivingTheSupport,
      final Set<UnitSupportAttachment> rules,
      final boolean defence,
      final boolean allies) {
    if (unitsGivingTheSupport == null || unitsGivingTheSupport.isEmpty()) {
      return EMPTY_RESULT;
    }
    final Set<List<UnitSupportAttachment>> supportsAvailable = new HashSet<>();
    final IntegerMap<UnitSupportAttachment> supportLeft = new IntegerMap<>();
    final Map<UnitSupportAttachment, IntegerMap<Unit>> supportUnitsLeft = new HashMap<>();

    for (final UnitSupportAttachment rule : rules) {
      if (rule.getPlayers().isEmpty()) {
        continue;
      }
      if (!((defence && rule.getDefence()) || (!defence && rule.getOffence()))) {
        continue;
      }
      if (!((allies && rule.getAllied()) || (!allies && rule.getEnemy()))) {
        continue;
      }
      final Predicate<Unit> canSupport =
          Matches.unitIsOfType((UnitType) rule.getAttachedTo())
              .and(Matches.unitOwnedBy(rule.getPlayers()));
      final List<Unit> supporters = CollectionUtils.getMatches(unitsGivingTheSupport, canSupport);
      int numSupport = supporters.size();
      if (numSupport <= 0) {
        continue;
      }
      final List<Unit> impArtTechUnits = new ArrayList<>();
      if (rule.getImpArtTech()) {
        impArtTechUnits.addAll(
            CollectionUtils.getMatches(
                supporters, Matches.unitOwnerHasImprovedArtillerySupportTech()));
      }
      numSupport += impArtTechUnits.size();
      supportLeft.put(rule, numSupport * rule.getNumber());
      final IntegerMap<Unit> unitsForRule = new IntegerMap<>();
      supporters.forEach(unit -> unitsForRule.put(unit, rule.getNumber()));
      impArtTechUnits.forEach(unit -> unitsForRule.add(unit, rule.getNumber()));
      supportUnitsLeft.put(rule, unitsForRule);
      final Iterator<List<UnitSupportAttachment>> iter2 = supportsAvailable.iterator();
      List<UnitSupportAttachment> ruleType = null;
      boolean found = false;
      final String bonusType = rule.getBonusType().getName();
      while (iter2.hasNext()) {
        ruleType = iter2.next();
        if (ruleType.get(0).getBonusType().getName().equals(bonusType)) {
          found = true;
          break;
        }
      }
      if (!found) {
        ruleType = new ArrayList<>();
        supportsAvailable.add(ruleType);
      }
      ruleType.add(rule);
    }

    return builder()
        .supportLeft(supportLeft)
        .supportRules(supportsAvailable)
        .supportUnits(supportUnitsLeft)
        .build();
  }

  /**
   * Gives the unit as much of the available support that is possible depending on the support
   * rules.
   *
   * <p>Each time this is called, the amount of available support will decrease equal to the amount
   * returned.
   */
  IntegerMap<Unit> giveSupportToUnit(
      final Unit unit, final Predicate<UnitSupportAttachment> ruleFilter) {
    final IntegerMap<Unit> supportUsed = new IntegerMap<>();
    for (final List<UnitSupportAttachment> rulesByBonusType : supportRules) {

      int maxPerBonusType = rulesByBonusType.get(0).getBonusType().getCount();
      for (final UnitSupportAttachment rule : rulesByBonusType) {
        final Set<UnitType> types = rule.getUnitType();
        if (!ruleFilter.test(rule) || types == null || !types.contains(unit.getType())) {
          continue;
        }

        final int numSupportAvailableToApply = getSupportAvailable(rule);
        for (int i = 0; i < numSupportAvailableToApply; i++) {
          final Unit supporter = getNextAvailableSupporter(rule);
          supportUsed.add(supporter, rule.getBonus());
        }

        maxPerBonusType -= numSupportAvailableToApply;
        if (maxPerBonusType <= 0) {
          break;
        }
      }
    }
    return supportUsed;
  }

  private int getSupportAvailable(final UnitSupportAttachment support) {
    return Math.max(
        0,
        Collections.min(
            Arrays.asList(
                support.getBonusType().getCount(),
                supportLeft.getInt(support),
                supportUnits.get(support).size())));
  }

  /**
   * Get next unit that can give support.
   *
   * <p>This may return the same unit multiple times in a row depending on how much support that
   * unit can give.
   */
  private Unit getNextAvailableSupporter(final UnitSupportAttachment support) {
    supportLeft.add(support, -1);
    final Set<Unit> supporters = supportUnits.get(support).keySet();
    final Unit u = supporters.iterator().next();
    supportUnits.get(support).add(u, -1);
    if (supportUnits.get(support).getInt(u) <= 0) {
      supportUnits.get(support).removeKey(u);
    }
    return u;
  }
}
