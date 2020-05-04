package games.strategy.triplea.delegate.battle.casualty.power.model;

import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import org.triplea.java.Postconditions;
import org.triplea.java.collections.CollectionUtils;

class SupportBuff {
  /** The unit providing support. */
  @Getter private final UnitType supportingUnitType;

  /** The full set of all units that could be supported by this buff. */
  private final Set<UnitType> applicableUnitTypes;

  /** Strength adjustment, can be negative. A +1 means unit rolls at +1. */
  @Getter private final Integer strengthModifier;

  /** Roll adjustment, can be negative, allows unit to roll more or less dice. */
  @Getter private final Integer rollModifier;

  /**
   * The number of supports of this type available. EG: if we have a count of 2, and 3 units, then 2
   * of those 3 units are supported.
   */
  @Getter private Integer count;

  public SupportBuff(final UnitSupportAttachment unitSupportAttachment) {
    supportingUnitType = (UnitType) unitSupportAttachment.getAttachedTo();
    applicableUnitTypes = unitSupportAttachment.getUnitType();
    strengthModifier = unitSupportAttachment.getStrength() ? unitSupportAttachment.getBonus() : 0;
    rollModifier = unitSupportAttachment.getRoll() ? unitSupportAttachment.getBonus() : 0;
    count = unitSupportAttachment.getBonusType().getCount();
  }

  void decrement() {
    count--;
    Postconditions.assertState(count >= 0);
  }

  boolean isEmpty() {
    return count == 0;
  }

  boolean canProvideSupport(final UnitType unitType) {
    return supportingUnitType.equals(unitType);
  }

  boolean canReceiveSupport(final UnitType unitType) {
    return applicableUnitTypes.contains(unitType);
  }

  // counts how much support is being received from this support buff.
  // If we have too many of the current unit, we are not receiving support
  // (EG: 3 infantry and 2 artillery)
  // Ignore any units with weaker strength
  int strengthWithBonus(
      final UnitGroup unitGroup, final Map<UnitTypeByPlayer, UnitGroup> unitGroups) {
    final int supportsAvailable =
        unitGroups.entrySet().stream()
            .filter(entry -> entry.getKey().getUnitType().equals(supportingUnitType))
            .mapToInt(entry -> entry.getValue().getUnitCount() * count)
            .sum();

    final int supportsTaken =
        unitGroups.entrySet().stream()
            .filter(entry -> canReceiveSupport(entry.getKey().getUnitType()))
            .filter(entry -> entry.getValue().getStrength() >= unitGroup.getStrength())
            .mapToInt(entry -> entry.getValue().getUnitCount())
            .sum();

    if (supportsTaken > supportsAvailable) {
      return (unitGroup.getStrength() * unitGroup.getDiceRolls());
    } else {
      return (unitGroup.getStrength() + this.strengthModifier)
          * (unitGroup.getDiceRolls() + this.rollModifier);
    }
  }

  int computeBonusProvided(
      final UnitGroup unitGroup, final Map<UnitTypeByPlayer, UnitGroup> unitGroups) {
    if (!this.supportingUnitType.equals(unitGroup.getUnitTypeByPlayer().getUnitType())) {
      return 0;
    }

    final int supportsAvailable = this.count * unitGroup.getUnitCount();

    final Collection<UnitGroup> unitsBeingSupported =
        unitGroups.entrySet().stream()
            .filter(entry -> canReceiveSupport(entry.getKey().getUnitType()))
            .map(Map.Entry::getValue)
            .collect(Collectors.toSet());

    if (unitsBeingSupported.isEmpty()) {
      return 0;
    }

    final int supportsTaken =
        unitsBeingSupported.stream()
            .filter(entry -> canReceiveSupport(entry.getUnitTypeByPlayer().getUnitType()))
            .mapToInt(UnitGroup::getUnitCount)
            .sum();

    if (supportsAvailable > supportsTaken) {
      return 0;
    }

    // TODO: more work here as we need to find the nth weakest unit ->
    //    if we are supporting 3 of 5 units, then we need the strength of that 3rd weakest unit.
    // we are certainly providing support
    // find the weakest unit that we are supporting
    final UnitGroup weakestUnitGroup =
        CollectionUtils.findMin(
                unitsBeingSupported, unit -> (unit.getStrength() * unit.getDiceRolls()))
            .iterator()
            .next();

    return (weakestUnitGroup.getStrength() + strengthModifier)
            * (weakestUnitGroup.getDiceRolls() + this.rollModifier)
        - (weakestUnitGroup.getStrength() * weakestUnitGroup.getDiceRolls());
  }
}
