package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import lombok.experimental.UtilityClass;
import org.triplea.java.collections.IntegerMap;

@UtilityClass
public class SupportBonusCalculator {
  /**
   * Returns the support for this unit type, and decrements the supportLeft counters.
   *
   * @return the bonus given to the unit
   */
  public static int getSupport(
      final Unit unit,
      final Set<List<UnitSupportAttachment>> supportsAvailable,
      final IntegerMap<UnitSupportAttachment> supportLeft,
      final Map<UnitSupportAttachment, IntegerMap<Unit>> supportUnitsLeft,
      final Map<Unit, IntegerMap<Unit>> unitSupportMap,
      final Predicate<UnitSupportAttachment> ruleFilter) {
    int givenSupport = 0;
    for (final List<UnitSupportAttachment> bonusType : supportsAvailable) {
      int maxPerBonusType = bonusType.get(0).getBonusType().getCount();
      for (final UnitSupportAttachment rule : bonusType) {
        if (!ruleFilter.test(rule)) {
          continue;
        }
        final Set<UnitType> types = rule.getUnitType();
        if (types != null && types.contains(unit.getType()) && supportLeft.getInt(rule) > 0) {
          final int numSupportToApply =
              Math.min(
                  maxPerBonusType,
                  Math.min(supportLeft.getInt(rule), supportUnitsLeft.get(rule).size()));
          for (int i = 0; i < numSupportToApply; i++) {
            givenSupport += rule.getBonus();
            supportLeft.add(rule, -1);
            final Set<Unit> supporters = supportUnitsLeft.get(rule).keySet();
            final Unit u = supporters.iterator().next();
            supportUnitsLeft.get(rule).add(u, -1);
            if (supportUnitsLeft.get(rule).getInt(u) <= 0) {
              supportUnitsLeft.get(rule).removeKey(u);
            }
            unitSupportMap.computeIfAbsent(u, j -> new IntegerMap<>()).add(unit, rule.getBonus());
          }
          maxPerBonusType -= numSupportToApply;
          if (maxPerBonusType <= 0) {
            break;
          }
        }
      }
    }
    return givenSupport;
  }

}
