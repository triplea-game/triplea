package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import games.strategy.triplea.delegate.Matches;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
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
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class AvailableSupportCalculator {

  public static final AvailableSupportCalculator EMPTY_RESULT =
      AvailableSupportCalculator.builder()
          .supportRules(new HashMap<>())
          .supportUnits(new HashMap<>())
          .build();

  final Map<UnitSupportAttachment.BonusType, List<UnitSupportAttachment>> supportRules;
  final Map<UnitSupportAttachment, IntegerMap<Unit>> supportUnits;

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
        .forEach((bonusType, unitSupportAttachment) -> unitSupportAttachment.sort(supportRuleSort));
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
      final Collection<UnitSupportAttachment> rules,
      final boolean defence,
      final boolean allies) {
    if (unitsGivingTheSupport == null || unitsGivingTheSupport.isEmpty()) {
      return EMPTY_RESULT;
    }
    final Map<UnitSupportAttachment.BonusType, List<UnitSupportAttachment>> supportRules =
        new HashMap<>();
    final Map<UnitSupportAttachment, IntegerMap<Unit>> supportUnits = new HashMap<>();

    for (final UnitSupportAttachment rule : rules) {
      final Set<UnitType> types = rule.getUnitType();
      if (rule.getPlayers().isEmpty() || types == null || types.isEmpty()) {
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
      if (supporters.isEmpty()) {
        continue;
      }
      final List<Unit> impArtTechUnits =
          rule.getImpArtTech()
              ? CollectionUtils.getMatches(
                  supporters, Matches.unitOwnerHasImprovedArtillerySupportTech())
              : List.of();

      final IntegerMap<Unit> unitsForRule = new IntegerMap<>();
      supporters.forEach(unit -> unitsForRule.put(unit, rule.getNumber()));
      impArtTechUnits.forEach(unit -> unitsForRule.add(unit, rule.getNumber()));
      supportUnits.put(rule, unitsForRule);
      supportRules.computeIfAbsent(rule.getBonusType(), (bonusType) -> new ArrayList<>()).add(rule);
    }

    return builder().supportRules(supportRules).supportUnits(supportUnits).build();
  }

  /** Constructs a filtered version of this */
  AvailableSupportCalculator filter(final Predicate<UnitSupportAttachment> ruleFilter) {

    final Map<UnitSupportAttachment.BonusType, List<UnitSupportAttachment>> supportRules =
        new HashMap<>();
    for (final Map.Entry<UnitSupportAttachment.BonusType, List<UnitSupportAttachment>> entry :
        this.supportRules.entrySet()) {
      final List<UnitSupportAttachment> filteredSupportRules =
          entry.getValue().stream().filter(ruleFilter).collect(Collectors.toList());
      if (!filteredSupportRules.isEmpty()) {
        supportRules.put(entry.getKey(), filteredSupportRules);
      }
    }

    final Map<UnitSupportAttachment, IntegerMap<Unit>> supportUnits = new HashMap<>();
    for (final UnitSupportAttachment usa : this.supportUnits.keySet()) {
      if (ruleFilter.test(usa)) {
        supportUnits.put(usa, new IntegerMap<>(this.supportUnits.get(usa)));
      }
    }

    return builder().supportRules(supportRules).supportUnits(supportUnits).build();
  }

  public int getSupportLeft(final UnitSupportAttachment support) {
    return supportUnits.getOrDefault(support, new IntegerMap<>()).totalValues();
  }

  /**
   * Gives the unit as much of the available support that is possible depending on the support
   * rules.
   *
   * <p>Each time this is called, the amount of available support will decrease equal to the amount
   * returned.
   */
  IntegerMap<Unit> giveSupportToUnit(final Unit unit) {
    final IntegerMap<Unit> supportUsed = new IntegerMap<>();
    for (final List<UnitSupportAttachment> rulesByBonusType : supportRules.values()) {

      int maxPerBonusType = rulesByBonusType.get(0).getBonusType().getCount();
      for (final UnitSupportAttachment rule : rulesByBonusType) {
        if (!rule.getUnitType().contains(unit.getType())) {
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
        Math.min(
            support.getBonusType().getCount(),
            supportUnits.getOrDefault(support, new IntegerMap<>()).totalValues()));
  }

  /**
   * Get next unit that can give support.
   *
   * <p>This may return the same unit multiple times in a row depending on how much support that
   * unit can give.
   */
  private Unit getNextAvailableSupporter(final UnitSupportAttachment support) {
    final Set<Unit> supporters = supportUnits.get(support).keySet();
    final Unit u = supporters.iterator().next();
    supportUnits.get(support).add(u, -1);
    if (supportUnits.get(support).getInt(u) <= 0) {
      supportUnits.get(support).removeKey(u);
    }
    return u;
  }
}
