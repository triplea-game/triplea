package org.triplea.sound;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.MustFightBattle.RetreatType;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/** Contains methods to play various sound clips. */
public final class SoundUtils {

  private SoundUtils() {}

  /** Plays appropriate sound clip for type of battle (land, air, sea, subs). */
  public static void playBattleType(
      final GamePlayer attacker,
      final List<Unit> attackingUnits,
      final List<Unit> defendingUnits,
      final IDelegateBridge bridge) {
    if (attackingUnits.stream().anyMatch(Matches.unitIsSea())
        || defendingUnits.stream().anyMatch(Matches.unitIsSea())) {
      if ((!attackingUnits.isEmpty() && attackingUnits.stream().allMatch(Matches.unitCanEvade()))
          || (attackingUnits.stream().anyMatch(Matches.unitCanEvade())
              && defendingUnits.stream().anyMatch(Matches.unitCanEvade()))) {
        bridge
            .getSoundChannelBroadcaster()
            .playSoundForAll(SoundPath.CLIP_BATTLE_SEA_SUBS, attacker);
      } else {
        bridge
            .getSoundChannelBroadcaster()
            .playSoundForAll(SoundPath.CLIP_BATTLE_SEA_NORMAL, attacker);
      }
    } else if (!attackingUnits.isEmpty()
        && attackingUnits.stream().allMatch(Matches.unitIsAir())
        && !defendingUnits.isEmpty()
        && defendingUnits.stream().allMatch(Matches.unitIsAir())) {
      bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BATTLE_AIR, attacker);
    } else {
      bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BATTLE_LAND, attacker);
    }
  }

  /**
   * Plays appropriate sound clip for firing battle AA based on type and whether it got any hits.
   */
  public static void playFireBattleAa(
      final GamePlayer firingPlayer,
      final String aaType,
      final boolean isHit,
      final IDelegateBridge bridge) {
    if (aaType.equals("AA")) {
      if (isHit) {
        bridge
            .getSoundChannelBroadcaster()
            .playSoundForAll(SoundPath.CLIP_BATTLE_AA_HIT, firingPlayer);
      } else {
        bridge
            .getSoundChannelBroadcaster()
            .playSoundForAll(SoundPath.CLIP_BATTLE_AA_MISS, firingPlayer);
      }
    } else {
      if (isHit) {
        bridge
            .getSoundChannelBroadcaster()
            .playSoundForAll(
                SoundPath.CLIP_BATTLE_X_PREFIX
                    + aaType.toLowerCase(Locale.ROOT)
                    + SoundPath.CLIP_BATTLE_X_HIT,
                firingPlayer);
      } else {
        bridge
            .getSoundChannelBroadcaster()
            .playSoundForAll(
                SoundPath.CLIP_BATTLE_X_PREFIX
                    + aaType.toLowerCase(Locale.ROOT)
                    + SoundPath.CLIP_BATTLE_X_MISS,
                firingPlayer);
      }
    }
  }

  /** Plays appropriate sound clip for type of retreat (land, air, sea, subs). */
  public static void playRetreatType(
      final GamePlayer attacker,
      final Collection<Unit> units,
      final RetreatType retreatType,
      final IDelegateBridge bridge) {
    switch (retreatType) {
      case SUBS:
        bridge
            .getSoundChannelBroadcaster()
            .playSoundForAll(SoundPath.CLIP_BATTLE_RETREAT_SUBMERGE, attacker);
        break;
      case PLANES:
        bridge
            .getSoundChannelBroadcaster()
            .playSoundForAll(SoundPath.CLIP_BATTLE_RETREAT_AIR, attacker);
        break;
      default:
        if (units.stream().anyMatch(Matches.unitIsSea())) {
          bridge
              .getSoundChannelBroadcaster()
              .playSoundForAll(SoundPath.CLIP_BATTLE_RETREAT_SEA, attacker);
        } else if (units.stream().anyMatch(Matches.unitIsLand())) {
          bridge
              .getSoundChannelBroadcaster()
              .playSoundForAll(SoundPath.CLIP_BATTLE_RETREAT_LAND, attacker);
        } else {
          bridge
              .getSoundChannelBroadcaster()
              .playSoundForAll(SoundPath.CLIP_BATTLE_RETREAT_AIR, attacker);
        }
    }
  }

  /**
   * Plays appropriate sound clip for successful air or sea battle. No sound clips for a successful
   * land battle because a territory will be captured which has its own sound clips.
   */
  public static void playAttackerWinsAirOrSea(
      final GamePlayer attacker,
      final List<Unit> attackingUnits,
      final boolean isWater,
      final IDelegateBridge bridge) {
    if (isWater) {
      if (!attackingUnits.isEmpty() && attackingUnits.stream().allMatch(Matches.unitIsAir())) {
        bridge
            .getSoundChannelBroadcaster()
            .playSoundForAll(SoundPath.CLIP_BATTLE_AIR_SUCCESSFUL, attacker);
      } else {
        bridge
            .getSoundChannelBroadcaster()
            .playSoundForAll(SoundPath.CLIP_BATTLE_SEA_SUCCESSFUL, attacker);
      }
    } else {
      if (!attackingUnits.isEmpty() && attackingUnits.stream().allMatch(Matches.unitIsAir())) {
        bridge
            .getSoundChannelBroadcaster()
            .playSoundForAll(SoundPath.CLIP_BATTLE_AIR_SUCCESSFUL, attacker);
      }
    }
  }
}
