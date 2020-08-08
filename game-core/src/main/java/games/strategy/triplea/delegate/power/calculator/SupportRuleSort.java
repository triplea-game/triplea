package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import java.util.Comparator;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import lombok.Builder;

@Builder
public class SupportRuleSort implements Comparator<UnitSupportAttachment> {
  @Nonnull private final Boolean defense;
  @Nonnull private final Boolean friendly;
  @Nonnull private final Predicate<UnitSupportAttachment> roll;
  @Nonnull private final Predicate<UnitSupportAttachment> strength;

  @Override
  public int compare(final UnitSupportAttachment u1, final UnitSupportAttachment u2) {
    int compareTo;

    // Make sure stronger supports are ordered first if friendly, and worst are ordered first
    // if enemy
    // TODO: it is possible that we will waste negative support if we reduce a units power to
    // less than zero.
    // We should actually apply enemy negative support in order from worst to least bad, on a
    // unit list that is
    // ordered from strongest to weakest.
    final boolean u1CanBonus = defense ? u1.getDefence() : u1.getOffence();
    final boolean u2CanBonus = defense ? u2.getDefence() : u2.getOffence();
    if (friendly) {
      // favor rolls over strength
      if (roll.test(u1) || roll.test(u2)) {
        final int u1Bonus = roll.test(u1) && u1CanBonus ? u1.getBonus() : 0;
        final int u2Bonus = roll.test(u2) && u2CanBonus ? u2.getBonus() : 0;
        compareTo = Integer.compare(u2Bonus, u1Bonus);
        if (compareTo != 0) {
          return compareTo;
        }
      }
      if (strength.test(u1) || strength.test(u2)) {
        final int u1Bonus = strength.test(u1) && u1CanBonus ? u1.getBonus() : 0;
        final int u2Bonus = strength.test(u2) && u2CanBonus ? u2.getBonus() : 0;
        compareTo = Integer.compare(u2Bonus, u1Bonus);
        if (compareTo != 0) {
          return compareTo;
        }
      }
    } else {
      if (roll.test(u1) || roll.test(u2)) {
        final int u1Bonus = roll.test(u1) && u1CanBonus ? u1.getBonus() : 0;
        final int u2Bonus = roll.test(u2) && u2CanBonus ? u2.getBonus() : 0;
        compareTo = Integer.compare(u1Bonus, u2Bonus);
        if (compareTo != 0) {
          return compareTo;
        }
      }
      if (strength.test(u1) || strength.test(u2)) {
        final int u1Bonus = strength.test(u1) && u1CanBonus ? u1.getBonus() : 0;
        final int u2Bonus = strength.test(u2) && u2CanBonus ? u2.getBonus() : 0;
        compareTo = Integer.compare(u1Bonus, u2Bonus);
        if (compareTo != 0) {
          return compareTo;
        }
      }
    }

    // If the bonuses are the same, we want to make sure any support which only supports 1
    // single unit type goes
    // first because there could be Support1 which supports both infantry and mech infantry,
    // and Support2
    // which only supports mech infantry. If the Support1 goes first, and the mech infantry is
    // first in the unit list
    // (highly probable), then Support1 will end up using all of itself up on the mech
    // infantry then when the Support2
    // comes up, all the mech infantry are used up, and it does nothing. Instead, we want
    // Support2 to come first,
    // support all mech infantry that it can, then have Support1 come in and support whatever
    // is left, that way no
    // support is wasted.
    // TODO: this breaks down completely if we have Support1 having a higher bonus than
    // Support2, because it will
    // come first. It should come first, unless we would have support wasted otherwise. This
    // ends up being a pretty
    // tricky math puzzle.
    final Set<UnitType> types1 = u1.getUnitType();
    final Set<UnitType> types2 = u2.getUnitType();
    final int s1 = types1 == null ? 0 : types1.size();
    final int s2 = types2 == null ? 0 : types2.size();
    compareTo = Integer.compare(s1, s2);
    if (compareTo != 0) {
      return compareTo;
    }

    // Now we need to sort so that the supporters who are the most powerful go before the less
    // powerful. This is not
    // necessary for the providing of support, but is necessary for our default casualty
    // selection method.
    final UnitType unitType1 = (UnitType) u1.getAttachedTo();
    final UnitType unitType2 = (UnitType) u2.getAttachedTo();
    final UnitAttachment ua1 = UnitAttachment.get(unitType1);
    final UnitAttachment ua2 = UnitAttachment.get(unitType2);
    final int unitPower1;
    final int unitPower2;
    if (u1.getDefence()) {
      unitPower1 =
          ua1.getDefenseRolls(GamePlayer.NULL_PLAYERID) * ua1.getDefense(GamePlayer.NULL_PLAYERID);
      unitPower2 =
          ua2.getDefenseRolls(GamePlayer.NULL_PLAYERID) * ua2.getDefense(GamePlayer.NULL_PLAYERID);
    } else {
      unitPower1 =
          ua1.getAttackRolls(GamePlayer.NULL_PLAYERID) * ua1.getAttack(GamePlayer.NULL_PLAYERID);
      unitPower2 =
          ua2.getAttackRolls(GamePlayer.NULL_PLAYERID) * ua2.getAttack(GamePlayer.NULL_PLAYERID);
    }

    return Integer.compare(unitPower2, unitPower1);
  }
}
