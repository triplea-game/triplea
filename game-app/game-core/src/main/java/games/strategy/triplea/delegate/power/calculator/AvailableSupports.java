package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
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
class AvailableSupports {

  static final AvailableSupports EMPTY_RESULT =
      AvailableSupports.builder()
          .supportRules(new HashMap<>())
          .supportUnits(new HashMap<>())
          .build();

  final Map<UnitSupportAttachment.BonusType, List<UnitSupportAttachment>> supportRules;

  // Wrapper over IntegerMap<Unit> that keeps track of the total, instead of recomputing it
  // repeatedly which can be very slow with lots of support units.
  static class SupportDetails {
    IntegerMap<Unit> supportUnits;
    int totalSupport;

    public SupportDetails(IntegerMap<Unit> supportUnits) {
      this.supportUnits = supportUnits;
      this.totalSupport = supportUnits.totalValues();
    }

    public SupportDetails(SupportDetails other) {
      this.supportUnits = new IntegerMap<>(other.supportUnits);
      this.totalSupport = other.totalSupport;
    }
  }

  final Map<UnitSupportAttachment, SupportDetails> supportUnits;

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
    Map<UnitSupportAttachment, SupportDetails> transformedSupportUnits =
        supportCalculator.getSupportUnits().entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> new SupportDetails(e.getValue())));
    return builder()
        .supportRules(supportCalculator.getSupportRules())
        .supportUnits(transformedSupportUnits)
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

    final Map<UnitSupportAttachment, SupportDetails> supportUnits = new HashMap<>();
    for (final UnitSupportAttachment usa : this.supportUnits.keySet()) {
      if (ruleFilter.test(usa)) {
        supportUnits.put(usa, new SupportDetails(this.supportUnits.get(usa)));
      }
    }

    return builder().supportRules(supportRules).supportUnits(supportUnits).build();
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

        final int numSupportersAvailable = getSupportersAvailable(rule);
        for (int i = 1; i <= numSupportersAvailable; i++) {
          // Re-query: each pick may have removed an exhausted supporter from the map, so the
          // requested position must be clamped to the current count.
          final int position = Math.min(i, getSupportersAvailable(rule));
          final Unit supporter = getNextAvailableSupporter(rule, position);
          amountOfSupportGiven += rule.getBonus();
          unitsGivingSupport
              .computeIfAbsent(supporter, (newSupport) -> new IntegerMap<>())
              .add(unit, rule.getBonus());
        }

        maxPerBonusType -= numSupportersAvailable;
        if (maxPerBonusType <= 0) {
          break;
        }
      }
    }
    return amountOfSupportGiven;
  }

  /**
   * Returns the number of distinct supporter units (capped by the bonus type's stack count) that
   * can give support to one target. A single supporter contributes at most one support per target;
   * stacking on the same target requires multiple distinct supporters.
   */
  private int getSupportersAvailable(final UnitSupportAttachment support) {
    final SupportDetails details = supportUnits.get(support);
    if (details == null) {
      return 0;
    }
    return Math.max(0, Math.min(support.getBonusType().getCount(), details.supportUnits.size()));
  }

  int getSupportLeft(final UnitSupportAttachment support) {
    SupportDetails details = supportUnits.get(support);
    return details != null ? details.totalSupport : 0;
  }

  /**
   * Picks the supporter at the given 1-based position from the rule's supporter map and consumes
   * one of its support points. A negative remaining value (number = -1, i.e. infinite) is left in
   * place.
   */
  private Unit getNextAvailableSupporter(final UnitSupportAttachment support, final int position) {
    final SupportDetails details = supportUnits.get(support);
    final IntegerMap<Unit> intMap = details.supportUnits;
    final Unit u = CollectionUtils.getAt(intMap.keySet(), position);
    intMap.add(u, -1);
    details.totalSupport -= 1;
    if (intMap.getInt(u) == 0) {
      intMap.removeKey(u);
    }
    return u;
  }
}
