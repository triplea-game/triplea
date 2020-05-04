package games.strategy.triplea.delegate.battle.casualty.power.model;

import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.battle.casualty.CasualtyOrderOfLosses;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;

/**
 * Contains a set of unit power buffs. We for example provide operations where if we remove a unit,
 * we'll decrement the needed unit power buff and shift any support buffs to other units.
 */
@AllArgsConstructor
public class UnitGroupSet {
  private final Map<UnitTypeByPlayer, UnitGroup> unitGroups = new HashMap<>();
  private final Map<UnitType, Integer> unitTypeCounts = new HashMap<>();

  // TODO: populate friendly and enemy support buffs
  // map of units providing support to the buff they provide
  private final Map<UnitType, SupportBuff> friendlySupportBuffs;
  // TODO: populate friendly and enemy support buffs
  // map of units providing support to the buff they provide
  private final Map<UnitType, SupportBuff> enemySupportBuffs = new HashMap<>();

  public UnitGroupSet(final CasualtyOrderOfLosses.Parameters parameters) {
    for (final Unit unit : parameters.getTargetsToPickFrom()) {
      final var unitTypeByPlayer = new UnitTypeByPlayer(unit.getType(), unit.getOwner());

      if (!unitTypeCounts.containsKey(unit.getType())) {
        unitTypeCounts.put(unit.getType(), 0);
      }
      unitTypeCounts.put(unit.getType(), unitTypeCounts.get(unit.getType()) + 1);

      if (!unitGroups.containsKey(unitTypeByPlayer)) {
        unitGroups.put(
            unitTypeByPlayer,
            UnitGroup.builder()
                .unitTypeByPlayer(unitTypeByPlayer)
                .diceRolls(unitTypeByPlayer.getDiceRolls(parameters.getCombatModifiers()))
                .strength(unitTypeByPlayer.getStrength(parameters.getCombatModifiers()))
                .unitCount(0)
                .build());
      }
      unitGroups.get(unitTypeByPlayer).incrementCount();
    }

    friendlySupportBuffs = parameters.getData().getUnitTypeList().getSupportRules().stream()
        // do we have any units providing this support?
        .filter(
            s ->
                s.getOffence() == !parameters.getCombatModifiers().isDefending()
                    || s.getDefence() == parameters.getCombatModifiers().isDefending())
        // do we have any units that can receive support?
        .filter(
            s ->
                s.getUnitType().stream()
                    .anyMatch(
                        supportable ->
                            unitGroups.keySet().stream()
                                .map(g -> g.getUnitType())
                                .anyMatch(t -> supportable.equals(t))))
        .map(SupportBuff::new)
        .collect(Collectors.toMap(b -> b.getSupportingUnitType(), Function.identity()));
  }

  public void removeUnit(final UnitTypeByPlayer unitTypeByPlayer) {
    final UnitGroup unitGroup =
        Optional.ofNullable(unitGroups.get(unitTypeByPlayer))
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Could not find: " + unitTypeByPlayer + ", in groups: " + unitGroups));
    unitGroup.decrementCount();
    if (unitGroup.isEmpty()) {
      unitGroups.remove(unitTypeByPlayer);
    }

    // if the count of support buffs is greater than the number of units providing
    // the support, then decrement support buff as we just lost a unit that was providing support.

    if (friendlySupportBuffs.containsKey(unitTypeByPlayer.getUnitType())) {
      final int numberOfUnitsProvidingSupport = unitTypeCounts.get(unitTypeByPlayer.getUnitType());

      if (friendlySupportBuffs.get(unitTypeByPlayer.getUnitType()).getCount()
          > numberOfUnitsProvidingSupport) {
        friendlySupportBuffs.get(unitTypeByPlayer.getUnitType()).decrement();
        if (friendlySupportBuffs.get(unitTypeByPlayer.getUnitType()).isEmpty()) {
          friendlySupportBuffs.remove(unitTypeByPlayer.getUnitType());
        }
      }
    }
  }

  public Collection<UnitTypeByPlayer> getWeakestUnit() {
    final Set<UnitTypeByPlayer> weakestUnits = new HashSet<>();
    int weakestStrength = Integer.MAX_VALUE;

    // TODO: switch to key iteration?
    for (final Map.Entry<UnitTypeByPlayer, UnitGroup> unitTypeEntry : unitGroups.entrySet()) {
      final int weakestInGroup = getWeakestUnit(unitTypeEntry.getValue());
      if (weakestStrength >= weakestInGroup) {
        if (weakestStrength > weakestInGroup) {
          // we have found a new minimum
          weakestStrength = weakestInGroup;
          weakestUnits.clear();
        }
        weakestUnits.add(unitTypeEntry.getKey());
      }
    }
    return weakestUnits;
  }

  private int getWeakestUnit(final UnitGroup unitGroup) {
    final Set<SupportBuff> availableBuffs =
        friendlySupportBuffs.values().stream()
            .filter(s -> s.canReceiveSupport(unitGroup.getUnitTypeByPlayer().getUnitType()))
            .collect(Collectors.toSet());

    final int bestStrengthBuff =
        availableBuffs.stream()
            .filter(buff -> isUnitGroupSupportedBySupportBuff(unitGroup, buff, unitGroups.values()))
            .mapToInt(SupportBuff::getStrengthModifier)
            .max()
            .orElse(0);

    final int bestRollsBuff =
        availableBuffs.stream()
            .filter(buff -> isUnitGroupSupportedBySupportBuff(unitGroup, buff, unitGroups.values()))
            .mapToInt(SupportBuff::getRollModifier)
            .max()
            .orElse(0);

    final int supportProvided =
        Optional.ofNullable(friendlySupportBuffs.get(unitGroup.getUnitTypeByPlayer().getUnitType()))
            .map(buff -> buff.computeBonusProvided(unitGroup, unitGroups))
            .orElse(0);

    final int enemyStrengthBuff;
    final int enemyRollbuff;

    return (unitGroup.getStrength() + bestStrengthBuff) // - enemyStrengthBuff)
            * (unitGroup.getDiceRolls() + bestRollsBuff) // - enemyRollbuff)
        + supportProvided;
    //        + (strengthSupportBeingProvided * rollSupportBeingProvided);
  }

  // TODO: add test case
  // 3 artillery (A) @ 2, providing +2
  // 2 marines (M) @ 3
  // OOL:  [ A, A, M, A, M ]

  private boolean isUnitGroupSupportedBySupportBuff(
      final UnitGroup unitGroup, final SupportBuff supportBuff, Collection<UnitGroup> units) {
    // check if we have enough support for all units of the current type.
    // if we do not, then there are units that are not supported. In this case
    // when looking for the weakest unit, we will choose the units that are not supported
    final int supportsAvailable =
        supportBuff.getCount()
            * units.stream()
                .filter(g -> supportBuff.canProvideSupport(g.getUnitTypeByPlayer().getUnitType()))
                .mapToInt(g -> g.getUnitCount())
                .sum();

    final int unitsPresent =
        units.stream()
            .filter(
                g ->
                    g.getUnitTypeByPlayer()
                        .getUnitType()
                        .equals(unitGroup.getUnitTypeByPlayer().getUnitType()))
            .mapToInt(g -> g.getUnitCount())
            .sum();

    return supportsAvailable >= unitsPresent;
  }

  public Collection<UnitTypeByPlayer> unitTypes() {
    return unitGroups.keySet();
  }

  public boolean isEmpty() {
    return unitGroups.isEmpty();
  }
}
