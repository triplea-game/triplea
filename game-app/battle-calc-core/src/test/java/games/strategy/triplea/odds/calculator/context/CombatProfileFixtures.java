package games.strategy.triplea.odds.calculator.context;

import games.strategy.triplea.odds.calculator.context.model.CombatFlag;
import games.strategy.triplea.odds.calculator.context.model.CombatProfile;
import games.strategy.triplea.odds.calculator.context.model.DamageState;
import games.strategy.triplea.odds.calculator.context.model.Domain;
import games.strategy.triplea.odds.calculator.context.model.SupportCategory;
import games.strategy.triplea.odds.calculator.context.model.UnitTypeId;
import java.util.Arrays;
import java.util.EnumSet;

/**
 * The one place tests build a {@link CombatProfile}, so each call site shows only the stats that
 * drive its outcome; combat-irrelevant fields default (single roll, undamaged, no support, no
 * flags, single hit-point).
 */
public final class CombatProfileFixtures {

  private CombatProfileFixtures() {}

  public static CombatProfile land(
      final String name, final int attack, final int defense, final int hitPoints) {
    return profile(name, attack, defense, hitPoints, Domain.LAND, null);
  }

  public static CombatProfile sea(
      final String name, final int attack, final int defense, final int hitPoints) {
    return profile(name, attack, defense, hitPoints, Domain.SEA, null);
  }

  public static CombatProfile air(
      final String name, final int attack, final int defense, final int hitPoints) {
    return profile(name, attack, defense, hitPoints, Domain.AIR, null);
  }

  /**
   * A multi-hit-point unit as a {@link CombatProfile#next} chain, full down to the last hit-point
   * whose successor is null (the hit that kills it).
   */
  public static CombatProfile multiHp(
      final String name,
      final int attack,
      final int defense,
      final int hitPoints,
      final Domain domain) {
    CombatProfile chain = null;
    for (int hp = 1; hp <= hitPoints; hp++) {
      chain = profile(name, attack, defense, hp, domain, chain);
    }
    return chain;
  }

  /** A first-strike sea unit (eg a submarine's sneak attack), FIRST_STRIKE flag set. */
  public static CombatProfile firstStrikeSea(
      final String name, final int attack, final int defense, final int hitPoints) {
    return withFlags(sea(name, attack, defense, hitPoints), CombatFlag.FIRST_STRIKE);
  }

  /** An AA gun (fires only at air), IS_AA flag set. */
  public static CombatProfile aa(
      final String name, final int attack, final int defense, final int hitPoints) {
    return withFlags(land(name, attack, defense, hitPoints), CombatFlag.IS_AA);
  }

  /**
   * A classic air-immune submarine: eligible to submerge (CAN_SUBMERGE) and untargetable by
   * aircraft without a friendly destroyer (CANNOT_BE_TARGETED_BY_ALL). A Revised sub carries only
   * the former — see {@code submarineTargetableByAir}.
   */
  public static CombatProfile submarine(
      final String name, final int attack, final int defense, final int hitPoints) {
    return withFlags(
        sea(name, attack, defense, hitPoints),
        CombatFlag.CAN_SUBMERGE,
        CombatFlag.CANNOT_BE_TARGETED_BY_ALL);
  }

  /** A Revised-style submarine: evades (CAN_SUBMERGE) yet is still targetable by aircraft. */
  public static CombatProfile submarineTargetableByAir(
      final String name, final int attack, final int defense, final int hitPoints) {
    return withFlags(sea(name, attack, defense, hitPoints), CombatFlag.CAN_SUBMERGE);
  }

  /** Copies {@code profile} firing {@code rolls} dice per body instead of the default one. */
  public static CombatProfile rolls(final CombatProfile profile, final int rolls) {
    return new CombatProfile(
        profile.type(),
        profile.attack(),
        profile.defense(),
        rolls,
        profile.hitPoints(),
        profile.domain(),
        profile.damage(),
        profile.gives(),
        profile.receives(),
        profile.flags(),
        profile.next());
  }

  /** Copies {@code profile} carrying exactly {@code flags}. */
  public static CombatProfile withFlags(final CombatProfile profile, final CombatFlag... flags) {
    final EnumSet<CombatFlag> flagSet = EnumSet.noneOf(CombatFlag.class);
    flagSet.addAll(Arrays.asList(flags));
    return new CombatProfile(
        profile.type(),
        profile.attack(),
        profile.defense(),
        profile.rolls(),
        profile.hitPoints(),
        profile.domain(),
        profile.damage(),
        profile.gives(),
        profile.receives(),
        flagSet,
        profile.next());
  }

  /** Copies {@code profile} with a support bonus it emits. */
  public static CombatProfile gives(final CombatProfile profile, final SupportCategory category) {
    return new CombatProfile(
        profile.type(),
        profile.attack(),
        profile.defense(),
        profile.rolls(),
        profile.hitPoints(),
        profile.domain(),
        profile.damage(),
        category,
        profile.receives(),
        profile.flags(),
        profile.next());
  }

  /** Copies {@code profile} with a support bonus it consumes. */
  public static CombatProfile receives(
      final CombatProfile profile, final SupportCategory category) {
    return new CombatProfile(
        profile.type(),
        profile.attack(),
        profile.defense(),
        profile.rolls(),
        profile.hitPoints(),
        profile.domain(),
        profile.damage(),
        profile.gives(),
        category,
        profile.flags(),
        profile.next());
  }

  private static CombatProfile profile(
      final String name,
      final int attack,
      final int defense,
      final int hitPoints,
      final Domain domain,
      final CombatProfile next) {
    return new CombatProfile(
        new UnitTypeId(name),
        attack,
        defense,
        1,
        hitPoints,
        domain,
        new DamageState(0),
        SupportCategory.NONE,
        SupportCategory.NONE,
        EnumSet.noneOf(CombatFlag.class),
        next);
  }
}
