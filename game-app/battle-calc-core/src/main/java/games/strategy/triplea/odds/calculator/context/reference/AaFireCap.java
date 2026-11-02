package games.strategy.triplea.odds.calculator.context.reference;

import games.strategy.triplea.odds.calculator.context.model.CombatProfile;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;

/**
 * The engine's per-round AA dice cap ({@code AaPowerStrengthAndRolls}) as a pure function of the
 * firing guns and the live air-target count: the total AA dice a group rolls in a round is bounded
 * by the number of eligible air targets. Finite guns contribute their per-gun cap up to that bound;
 * a single infinite gun ({@code maxAaAttacks == -1}) fills whatever targets the finite guns leave.
 *
 * <p>{@code GameData}-free and stateless so the reference simulator can cap AA fire without the
 * profile carrying a target-varying roll count (which would fragment the merge key). Exact for a
 * homogeneous single-gun-type group — the shape every stock map fields. Three residuals remain,
 * none reached by a stock map's single homogeneous gun:
 *
 * <ul>
 *   <li>Mixed-strength guns of different types in one group: the total dice are exact, but the
 *       engine drops basic guns weaker than the best infinite gun and fires their share at the
 *       infinite gun's higher strength, whereas this keeps each finite gun firing at its own
 *       strength — same dice count, lower hit probability, so it moves only the Monte-Carlo odds.
 *   <li>Overstack guns: the engine adds {@code getMayOverStackAa} guns uncapped on top of the
 *       target count, whereas this clamps every gun at it.
 *   <li>Per-gun {@code targetsAa} restriction: the target count counts all eligible air (the
 *       resolver's {@code eligibleTargets} returns all air), so a gun able to fire at only a subset
 *       of the air would over-count the target bound — an inherited pre-existing simplification.
 * </ul>
 */
final class AaFireCap {

  private AaFireCap() {}

  /**
   * The firing vector to actually roll: each gun profile carrying its capped share of the round's
   * dice as its roll count (one body per profile), so the ordinary {@link DiceHitRoller} rolls the
   * capped total without any AA-specific roll path. The gun's own baked {@code rolls} is unused on
   * the AA path — this capped share replaces it. Finite guns take dice strongest-first up to the
   * target cap; the strongest infinite gun fills the remainder. The synthetic profiles are
   * transient roll inputs only — they never enter the state vector, so overriding {@code rolls}
   * here does not touch the merge key.
   */
  static Map<CombatProfile, Integer> cappedFiring(
      final Map<CombatProfile, Integer> firing,
      final int targetCount,
      final ToIntFunction<CombatProfile> strength) {
    final Map<CombatProfile, Integer> rolled = new LinkedHashMap<>();
    if (targetCount <= 0) {
      return rolled;
    }
    final List<CombatProfile> strongestFirst =
        firing.keySet().stream().sorted(Comparator.comparingInt(strength).reversed()).toList();
    int remaining = targetCount;
    CombatProfile strongestInfinite = null;
    for (final CombatProfile profile : strongestFirst) {
      final int cap = profile.maxAaAttacks();
      if (cap < 0) {
        if (strongestInfinite == null) {
          strongestInfinite = profile;
        }
        continue;
      }
      if (remaining <= 0) {
        continue;
      }
      final int dice = Math.min(remaining, firing.get(profile) * cap);
      if (dice > 0) {
        rolled.put(withRolls(profile, dice), 1);
        remaining -= dice;
      }
    }
    if (remaining > 0 && strongestInfinite != null) {
      rolled.put(withRolls(strongestInfinite, remaining), 1);
    }
    return rolled;
  }

  // Collapsing a gun's whole count into one synthetic body with rolls = its capped dice assumes no
  // CHOOSE_BEST_ROLL AA gun: that roller flag scores at most one hit per body (keying on rolls vs
  // count), so the collapse would miscount under it. It is the LHTR heavy-bomber flag, never set on
  // AA (the adapter forces it off), so this is latent only.
  private static CombatProfile withRolls(final CombatProfile profile, final int rolls) {
    return new CombatProfile(
        profile.type(),
        profile.attack(),
        profile.defense(),
        rolls,
        profile.maxRoundsAa(),
        profile.maxAaAttacks(),
        profile.hitPoints(),
        profile.domain(),
        profile.damage(),
        profile.gives(),
        profile.receives(),
        profile.flags(),
        profile.next());
  }
}
