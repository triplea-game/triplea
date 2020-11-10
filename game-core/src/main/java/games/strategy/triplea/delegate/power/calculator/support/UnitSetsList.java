package games.strategy.triplea.delegate.power.calculator.support;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.power.calculator.CombatValue;
import games.strategy.triplea.delegate.power.calculator.PowerCalculator;
import games.strategy.triplea.delegate.power.calculator.SupportCalculator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A sorted list of units where the order is based on how useful the unit is in battle. The first
 * unit is the unit that supplies the least power to the battle, whether through its own power or
 * through support.
 *
 * <p>This takes into account the unit base power, the buffs it gives to units (both enemies and
 * allies) the debuffs it gives to units (both enemies and allies), and other pertinent factors.
 */
public class UnitSetsList {

  List<UnitSet> unitSets;
  List<Unit> units;

  public UnitSetsList(
      final Collection<Unit> units,
      final CombatValue combatValue,
      final SupportCalculator supportFromFriends) {
    final Map<UnitType, Map<GamePlayer, UnitSet>> mapOfBaseUnitSets = new HashMap<>();
    final PowerCalculator powerCalculator = combatValue.buildWithNoUnitSupports().getPower();

    this.units = new ArrayList<>(units);

    for (final Unit unit : this.units) {
      mapOfBaseUnitSets
          .computeIfAbsent(unit.getType(), key -> new HashMap<>())
          .computeIfAbsent(
              unit.getOwner(),
              key -> new UnitSet(unit.getOwner(), unit.getType(), powerCalculator.getValue(unit)))
          .addUnit(unit);
    }

    supportFromFriends
        .getUnitTypesReceivingSupport()
        .forEach(
            (unitTypeReceivingSupport, supportRuleGroups) -> {
              mapOfBaseUnitSets
                  .getOrDefault(unitTypeReceivingSupport, Map.of())
                  .values()
                  .forEach(
                      unitSetReceivingSupport -> {
                        supportRuleGroups.forEach(
                            (bonusType, supportRules) -> {
                              supportRules.forEach(
                                  supportRule -> {
                                    mapOfBaseUnitSets
                                        .getOrDefault(supportRule.getAttachedTo(), Map.of())
                                        .values()
                                        .forEach(
                                            unitTypeGivingSupport -> {
                                              unitSetReceivingSupport.attachSupportGivingUnitSet(
                                                  unitTypeGivingSupport, bonusType, supportRules);
                                            });
                                  });
                            });
                      });
            });

    this.unitSets =
        mapOfBaseUnitSets.values().stream()
            .flatMap(baseUnitSets -> baseUnitSets.values().stream())
            .sorted()
            .collect(Collectors.toList());
  }

  List<Unit> getUnits() {
    final List<Unit> sortedUnits = new ArrayList<>();
    for (int i = 0; i < this.units.size(); i++) {
      System.out.println("UnitSets: " + this.unitSets);
      final UnitSet unitSet = this.unitSets.get(0);
      sortedUnits.add(unitSet.getUnit());
      if (unitSet.getSize() == 0) {
        this.unitSets.remove(0);
      }
      this.unitSets.forEach(UnitSet::reset);
      // resort because support was given and received
      Collections.sort(this.unitSets);
    }
    return sortedUnits;
  }
}
