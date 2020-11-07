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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;

/** Calculates how much support units can give */
@Value
@Getter(value = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SupportCalculator {

  Map<UnitSupportAttachment.BonusType, List<UnitSupportAttachment>> supportRules;
  Map<UnitSupportAttachment, IntegerMap<Unit>> supportUnits;
  boolean defence;
  boolean allies;

  /**
   * @param defence are the receiving units defending?
   * @param allies are the receiving units allied to the giving units?
   */
  public SupportCalculator(
      final Collection<Unit> unitsGivingTheSupport,
      final Collection<UnitSupportAttachment> rules,
      final boolean defence,
      final boolean allies) {
    this.defence = defence;
    this.allies = allies;
    supportRules = new HashMap<>();
    supportUnits = new HashMap<>();

    if (unitsGivingTheSupport == null || unitsGivingTheSupport.isEmpty()) {
      return;
    }

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
  }

  public int getSupport(final UnitSupportAttachment rule) {
    return supportUnits.getOrDefault(rule, new IntegerMap<>()).totalValues();
  }

  public Collection<List<UnitSupportAttachment>> getUnitSupportAttachments() {
    return supportRules.values();
  }
}
