package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import games.strategy.triplea.delegate.Matches;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;

@UtilityClass
public class AvailableSupportCalculator {
  public static SupportCalculationResult getSortedAaSupport(
      final Collection<Unit> unitsGivingTheSupport,
      final GameData data,
      final boolean defence,
      final boolean allies) {
    final Set<UnitSupportAttachment> rules =
        UnitSupportAttachment.get(data).parallelStream()
            .filter(usa -> (usa.getAaRoll() || usa.getAaStrength()))
            .collect(Collectors.toSet());
    return getSortedSupport(unitsGivingTheSupport, rules, defence, allies);
  }

  /** Sorts 'supportsAvailable' lists based on unit support attachment rules. */
  public static SupportCalculationResult getSortedSupport(
      final Collection<Unit> unitsGivingTheSupport,
      final Set<UnitSupportAttachment> rules,
      final boolean defence,
      final boolean allies) {
    final SupportCalculationResult supportCalculationResult =
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
        .forEach(unitSupportAttachment -> unitSupportAttachment.sort(supportRuleSort));
    return supportCalculationResult;
  }

  /**
   * Returns a calculation with support possibly given by these units.
   *
   * @param defence are the receiving units defending?
   * @param allies are the receiving units allied to the giving units?
   */
  public static SupportCalculationResult getSupport(
      final Collection<Unit> unitsGivingTheSupport,
      final Set<UnitSupportAttachment> rules,
      final boolean defence,
      final boolean allies) {
    if (unitsGivingTheSupport == null || unitsGivingTheSupport.isEmpty()) {
      return SupportCalculationResult.EMPTY_RESULT;
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

    return SupportCalculationResult.builder()
        .supportLeft(supportLeft)
        .supportRules(supportsAvailable)
        .supportUnits(supportUnitsLeft)
        .build();
  }
}
