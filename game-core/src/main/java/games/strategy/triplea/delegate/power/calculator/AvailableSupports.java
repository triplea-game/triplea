package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.UnitSupportAttachment;
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
import lombok.Getter;
import org.triplea.java.collections.IntegerMap;

/**
 * Tracks the available support that a collection of units can give to other units.
 *
 * <p>Once a support is used, it will no longer be available for other units to use.
 */
@Data
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor
class AvailableSupports {

  static final AvailableSupports EMPTY_RESULT =
      AvailableSupports.builder()
          .supportRules(new HashMap<>())
          .supportUnits(new HashMap<>())
          .build();

  final Map<UnitSupportAttachment.BonusType, List<UnitSupportAttachment>> supportRules;
  final Map<UnitSupportAttachment, IntegerMap<Unit>> supportUnits;

  /**
   * Keeps track of the units that have provided support in {@link
   * AvailableSupports#giveSupportToUnit} and whom they are providing it to
   */
  @Getter private final Map<Unit, IntegerMap<Unit>> unitsGivingSupport = new HashMap<>();

  /** Sorts 'supportsAvailable' lists based on unit support attachment rules. */
  static AvailableSupports getSortedSupport(final SupportCalculator supportCalculator) {
    final AvailableSupports supportCalculationResult = getSupport(supportCalculator);

    final SupportRuleSort supportRuleSort =
        SupportRuleSort.builder()
            .side(supportCalculator.getSide())
            .friendly(supportCalculator.isAllies())
            .roll(UnitSupportAttachment::getRoll)
            .strength(UnitSupportAttachment::getStrength)
            .build();
    supportCalculationResult
        .getSupportRules()
        .forEach((bonusType, unitSupportAttachment) -> unitSupportAttachment.sort(supportRuleSort));
    return supportCalculationResult;
  }

  static AvailableSupports getSupport(final SupportCalculator supportCalculator) {
    return builder()
        .supportRules(supportCalculator.getSupportRules())
        .supportUnits(supportCalculator.getSupportUnits())
        .build();
  }

  /** Constructs a copied version of this */
  AvailableSupports copy() {
    return filter(support -> true);
  }

  /** Constructs a filtered version of this */
  AvailableSupports filter(final Predicate<UnitSupportAttachment> ruleFilter) {

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

  int getSupportLeft(final UnitSupportAttachment support) {
    return supportUnits.getOrDefault(support, new IntegerMap<>()).totalValues();
  }

  /**
   * Gives the unit as much of the available support that is possible depending on the support
   * rules.
   *
   * <p>Each time this is called, the amount of available support will decrease equal to the amount
   * returned.
   */
  int giveSupportToUnit(final Unit unit) {
    int amountOfSupportGiven = 0;
    for (final List<UnitSupportAttachment> rulesByBonusType : supportRules.values()) {

      int maxPerBonusType = rulesByBonusType.get(0).getBonusType().getCount();
      for (final UnitSupportAttachment rule : rulesByBonusType) {
        if (!rule.getUnitType().contains(unit.getType())) {
          continue;
        }

        final int numSupportAvailableToApply = getSupportAvailable(rule);
        for (int i = 0; i < numSupportAvailableToApply; i++) {
          final Unit supporter = getNextAvailableSupporter(rule);
          amountOfSupportGiven += rule.getBonus();
          unitsGivingSupport
              .computeIfAbsent(supporter, (newSupport) -> new IntegerMap<>())
              .add(unit, rule.getBonus());
        }

        maxPerBonusType -= numSupportAvailableToApply;
        if (maxPerBonusType <= 0) {
          break;
        }
      }
    }
    return amountOfSupportGiven;
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
