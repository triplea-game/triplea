package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.BattleState;
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
import org.triplea.java.collections.IntegerMap;

/** Calculates how much support units can give */
@Value
@Getter(value = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SupportCalculator {

  Map<UnitSupportAttachment.BonusType, List<UnitSupportAttachment>> supportRules;
  Map<UnitSupportAttachment, IntegerMap<Unit>> supportUnits;
  BattleState.Side side;
  boolean allies;

  /**
   * @param side are the receiving units defending?
   * @param allies are the receiving units allied to the giving units?
   */
  public SupportCalculator(
      final Collection<Unit> unitsGivingTheSupport,
      final Collection<UnitSupportAttachment> rules,
      final BattleState.Side side,
      final boolean allies) {
    this.side = side;
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
      if (!((side == BattleState.Side.DEFENSE && rule.getDefence())
          || (side == BattleState.Side.OFFENSE && rule.getOffence()))) {
        continue;
      }
      if (!((allies && rule.getAllied()) || (!allies && rule.getEnemy()))) {
        continue;
      }
      final Predicate<Unit> canSupport =
          Matches.unitIsOfType((UnitType) rule.getAttachedTo())
              .and(Matches.unitIsOwnedByAnyOf(rule.getPlayers()));
      final Predicate<Unit> impArtTech =
          rule.getImpArtTech() ? Matches.unitOwnerHasImprovedArtillerySupportTech() : u -> false;
      final IntegerMap<Unit> unitsForRule = new IntegerMap<>();
      for (Unit unit : unitsGivingTheSupport) {
        if (!canSupport.test(unit)) {
          continue;
        }

        unitsForRule.put(unit, rule.getNumber());
        if (impArtTech.test(unit)) {
          unitsForRule.add(unit, rule.getNumber());
        }
      }
      if (!unitsForRule.isEmpty()) {
        supportUnits.put(rule, unitsForRule);
        supportRules.computeIfAbsent(rule.getBonusType(), (bt) -> new ArrayList<>()).add(rule);
      }
    }
  }

  public int getSupport(final UnitSupportAttachment rule) {
    return supportUnits.getOrDefault(rule, IntegerMap.of()).totalValues();
  }

  public Collection<List<UnitSupportAttachment>> getUnitSupportAttachments() {
    return supportRules.values();
  }

  public static Map<Unit, IntegerMap<Unit>> getCombinedSupportGiven(
      AvailableSupports supportFromFriends, AvailableSupports supportFromEnemies) {
    Map<Unit, IntegerMap<Unit>> support = new HashMap<>();
    for (var entry : supportFromFriends.getUnitsGivingSupport().entrySet()) {
      support.computeIfAbsent(entry.getKey(), u -> new IntegerMap<>()).add(entry.getValue());
    }
    for (var entry : supportFromEnemies.getUnitsGivingSupport().entrySet()) {
      support.computeIfAbsent(entry.getKey(), u -> new IntegerMap<>()).add(entry.getValue());
    }
    return support;
  }
}
