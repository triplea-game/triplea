package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;

/** Calculates how much support units can give */
@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SupportCalculator {

  Map<UnitSupportAttachment.BonusType, List<UnitSupportAttachment>> supportRules = new HashMap<>();
  Map<UnitSupportAttachment, IntegerMap<Unit>> supportRulesToUnitsGivingSupport = new HashMap<>();
  /** Tracks UnitTypes that are giving support */
  Map<UnitType, Map<GamePlayer, IntegerMap<UnitSupportAttachment>>> unitTypesGivingSupport =
      new HashMap<>();

  /** Tracks UnitTypes that are receiving support */
  Map<UnitType, Map<UnitSupportAttachment.BonusType, Set<UnitSupportAttachment>>>
      unitTypesReceivingSupport = new HashMap<>();

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
      final UnitType unitTypeGivingSupport = (UnitType) rule.getAttachedTo();
      final Predicate<Unit> canSupport =
          Matches.unitIsOfType(unitTypeGivingSupport).and(Matches.unitOwnedBy(rule.getPlayers()));
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
      supporters.forEach(
          unit -> {
            unitsForRule.put(unit, rule.getNumber());
            unitTypesGivingSupport
                .computeIfAbsent(unit.getType(), key -> new HashMap<>())
                .computeIfAbsent(unit.getOwner(), key -> new IntegerMap<>())
                .add(rule, rule.getNumber());
          });
      impArtTechUnits.forEach(
          unit -> {
            unitsForRule.add(unit, rule.getNumber());
            unitTypesGivingSupport
                .computeIfAbsent(unit.getType(), key -> new HashMap<>())
                .computeIfAbsent(unit.getOwner(), key -> new IntegerMap<>())
                .add(rule, rule.getNumber());
          });
      supportRulesToUnitsGivingSupport.put(rule, unitsForRule);
      supportRules.computeIfAbsent(rule.getBonusType(), (bonusType) -> new ArrayList<>()).add(rule);

      rule.getUnitType()
          .forEach(
              unitType -> {
                unitTypesReceivingSupport
                    .computeIfAbsent(unitType, key -> new HashMap<>())
                    .computeIfAbsent(rule.getBonusType(), key -> new HashSet<>())
                    .add(rule);
              });
    }
  }

  public int getSupport(final UnitSupportAttachment rule) {
    return supportRulesToUnitsGivingSupport.getOrDefault(rule, new IntegerMap<>()).totalValues();
  }

  public Collection<List<UnitSupportAttachment>> getUnitSupportAttachments() {
    return supportRules.values();
  }
}
