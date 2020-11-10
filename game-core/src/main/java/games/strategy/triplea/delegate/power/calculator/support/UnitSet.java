package games.strategy.triplea.delegate.power.calculator.support;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;

/**
 * Set of units for a unit type and player
 *
 * <p>Knows which support rules that this unit type provides and has links to UnitSets that give
 * support to this unit type.
 */
@Data
@Setter(AccessLevel.NONE)
@Getter(AccessLevel.NONE)
public class UnitSet implements Comparable<UnitSet> {

  private final GamePlayer player;

  private final UnitType unitType;

  private final int power;

  private final List<Unit> units = new ArrayList<>();
  private final Map<UnitSupportAttachment.BonusType, List<GivingUnitSet>> unitSetsGivingSupport =
      new HashMap<>();
  private final Map<UnitSupportAttachment.BonusType, List<GivingUnitSet>> unitSetsReceivingSupport =
      new HashMap<>();

  private Integer greatestFullyReceivedSupport = null;
  private Integer greatestFullyGivenSupport = null;

  void addUnit(final Unit unit) {
    units.add(unit);
  }

  void attachSupportGivingUnitSet(
      final UnitSet givingUnitSet,
      final UnitSupportAttachment.BonusType bonusType,
      final Set<UnitSupportAttachment> supportRules) {

    unitSetsGivingSupport
        .computeIfAbsent(bonusType, key -> new ArrayList<>())
        .add(new GivingUnitSet(givingUnitSet, supportRules));
    givingUnitSet.attachSupportReceivingUnitSet(this, bonusType, supportRules);
  }

  @Value
  private static class GivingUnitSet {
    UnitSet unitSet;
    Set<UnitSupportAttachment> supportRules;
  }

  private void attachSupportReceivingUnitSet(
      final UnitSet receivingUnitSet,
      final UnitSupportAttachment.BonusType bonusType,
      final Set<UnitSupportAttachment> supportRules) {
    unitSetsReceivingSupport
        .computeIfAbsent(bonusType, key -> new ArrayList<>())
        .add(new GivingUnitSet(receivingUnitSet, supportRules));
  }

  Unit getUnit() {
    final Unit unit = this.units.remove(this.units.size() - 1);

    return unit;
  }

  void reset() {
    this.greatestFullyGivenSupport = null;
    this.greatestFullyReceivedSupport = null;
  }

  @Override
  public int compareTo(final UnitSet other) {
    final int totalUsefulCombatAbility =
        this.power
            + this.calculateGreatestFullyGivenSupport()
            + this.calculateGreatestFullyReceivedSupport();

    final int otherTotalUsefulCombatAbility =
        other.power
            + other.calculateGreatestFullyGivenSupport()
            + other.calculateGreatestFullyReceivedSupport();

    if (totalUsefulCombatAbility == otherTotalUsefulCombatAbility) {
      // both unit sets have the same usefulness so see if one is receiving more support and
      // sort it lower so that it can potentially free up the unitset giving it support.
      if (this.calculateGreatestFullyReceivedSupport()
          != other.calculateGreatestFullyReceivedSupport()) {
        return Integer.compare(
            other.calculateGreatestFullyReceivedSupport(),
            this.calculateGreatestFullyReceivedSupport());
      }
    }

    return Integer.compare(totalUsefulCombatAbility, otherTotalUsefulCombatAbility);
  }

  int getSize() {
    return units.size();
  }

  /**
   * Greatest amount of full support that this unitset can receive
   *
   * <p>Example: This UnitSet is A. B and C both support A. There is 2 A, 1 B, and 3 C. B can't
   * support both As so B's support isn't included. C can support both As so C's support is
   * included.
   */
  private int calculateGreatestFullyReceivedSupport() {
    if (greatestFullyReceivedSupport == null) {
      greatestFullyReceivedSupport =
          unitSetsGivingSupport.entrySet().stream()
              .mapToInt(
                  (entry) -> {
                    // TODO: bonus stacking
                    // TODO: bonus with more than one rule
                    final int supportAvailable =
                        entry.getValue().stream()
                            .mapToInt(givingUnitSet -> givingUnitSet.unitSet.getSize())
                            .sum();
                    if (supportAvailable >= getSize()) {
                      // enough support to cover all of the units
                      if (entry.getValue().size() == 1) {
                        return entry.getValue().get(0).supportRules.stream()
                            .mapToInt(UnitSupportAttachment::getBonus)
                            .sum();
                      }
                    }
                    return 0;
                  })
              .sum();
    }
    return greatestFullyReceivedSupport;
  }

  /**
   * Greatest amount of full support that this unitset can give
   *
   * <p>Example: This UnitSet is A. A supports both B and C. There is 2 A, 1 B, and 3 C. All the Bs
   * can be fully supported so it is included. But not all the Cs so that is not included.
   */
  private int calculateGreatestFullyGivenSupport() {
    if (greatestFullyGivenSupport == null) {
      greatestFullyGivenSupport =
          unitSetsReceivingSupport.entrySet().stream()
              .mapToInt(
                  (entry) -> {
                    // TODO: bonus stacking
                    // TODO: bonus with more than one rule
                    final int supportNeeded =
                        entry.getValue().stream()
                            .mapToInt(receivingUnitSet -> receivingUnitSet.unitSet.getSize())
                            .sum();
                    if (supportNeeded >= getSize()) {
                      // all the support from this UnitSet will be used up
                      if (entry.getValue().size() == 1) {
                        return entry.getValue().get(0).supportRules.stream()
                            .mapToInt(UnitSupportAttachment::getBonus)
                            .sum();
                      }
                    }
                    return 0;
                  })
              .sum();
    }
    return greatestFullyGivenSupport;
  }

  @Override
  public String toString() {
    return "UnitSet["
        + "UnitType: "
        + unitType.getName()
        + ",Size: "
        + units.size()
        + ",Power: "
        + power
        + ",FullyGivenSupport: "
        + calculateGreatestFullyGivenSupport()
        + ",FullyReceivedSupport: "
        + calculateGreatestFullyReceivedSupport()
        + "]";
  }
}
