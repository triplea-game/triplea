package games.strategy.triplea.delegate;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Splitter;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.GameStep;
import games.strategy.triplea.Properties;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * A helper class for determining Game Step Properties. These are things such as whether a move
 * phase is combat move or noncombat move, or whether we are going to post to a forum during this
 * end turn phase.
 */
@Slf4j
@UtilityClass
public final class GameStepPropertiesHelper {
  /**
   * Indicates we skip posting the game summary and save to a forum or email. Defaults to true for
   * purchase phase and false for all others.
   */
  public static boolean isSkipPosting(final GameData data) {
    try (GameData.Unlocker ignored = data.acquireReadLock()) {
      final String defaultSkipPosting = isPurchaseDelegate(data) ? "true" : "false";
      return Boolean.parseBoolean(
          data.getSequence()
              .getStep()
              .getProperties()
              .getProperty(GameStep.PropertyKeys.SKIP_POSTING, defaultSkipPosting));
    }
  }

  /**
   * What players is this turn summary for? If more than 1 player, whose phases are touching or
   * intermeshed, then we will summarize for all those phases.
   *
   * @return The set of players; may be empty if not set.
   */
  public static Set<GamePlayer> getTurnSummaryPlayers(final GameData data) {
    checkNotNull(data);

    return getPlayersFromProperty(data, GameStep.PropertyKeys.TURN_SUMMARY_PLAYERS, null);
  }

  private static Set<GamePlayer> getPlayersFromProperty(
      final GameData gameData, final String propertyKey, final @Nullable GamePlayer defaultPlayer) {
    final Set<GamePlayer> players = new HashSet<>();
    if (defaultPlayer != null) {
      players.add(defaultPlayer);
    }

    try (GameData.Unlocker ignored = gameData.acquireReadLock()) {
      final @Nullable String encodedPlayerNames =
          gameData.getSequence().getStep().getProperties().getProperty(propertyKey);
      if (encodedPlayerNames != null) {
        for (final String playerName : Splitter.on(':').split(encodedPlayerNames)) {
          final @Nullable GamePlayer player = gameData.getPlayerList().getPlayerId(playerName);
          if (player != null) {
            players.add(player);
          } else {
            log.warn(
                "gameplay sequence step: {} stepProperty: {} player: {} DOES NOT EXIST",
                gameData.getSequence().getStep().getName(),
                propertyKey,
                playerName);
          }
        }
      }
    }

    return players;
  }

  /** For various things related to movement validation. */
  public static boolean isAirborneMove(final GameData data) {
    try (GameData.Unlocker ignored = data.acquireReadLock()) {
      final String prop =
          data.getSequence()
              .getStep()
              .getProperties()
              .getProperty(GameStep.PropertyKeys.AIRBORNE_MOVE);
      return prop != null ? Boolean.parseBoolean(prop) : isAirborneDelegate(data);
    }
  }

  public static boolean isCombatMove(final GameData data) {
    return isCombatMove(data, false);
  }

  /** For various things related to movement validation. */
  public static boolean isCombatMove(
      final GameData data, final boolean doNotThrowErrorIfNotMoveDelegate) {
    try (GameData.Unlocker ignored = data.acquireReadLock()) {
      final String prop =
          data.getSequence()
              .getStep()
              .getProperties()
              .getProperty(GameStep.PropertyKeys.COMBAT_MOVE);
      if (prop != null) {
        return Boolean.parseBoolean(prop);
      } else if (isCombatDelegate(data)) {
        return true;
      } else if (isNonCombatDelegate(data) || doNotThrowErrorIfNotMoveDelegate) {
        return false;
      } else {
        throw new IllegalStateException(
            "Cannot determine combat or not: " + data.getSequence().getStep().getName());
      }
    }
  }

  /** For various things related to movement validation. */
  public static boolean isNonCombatMove(
      final GameData data, final boolean doNotThrowErrorIfNotMoveDelegate) {
    try (GameData.Unlocker ignored = data.acquireReadLock()) {
      if (data.getSequence().getStep().isNonCombat()) {
        return true;
      } else if (isCombatDelegate(data) || doNotThrowErrorIfNotMoveDelegate) {
        return false;
      } else {
        throw new IllegalStateException(
            "Cannot determine combat or not: " + data.getSequence().getStep().getName());
      }
    }
  }

  /**
   * Repairs damaged units. Normally would occur at either start of combat move or end of turn,
   * depending.
   */
  static boolean isRepairUnits(final GameData data) {
    try (GameData.Unlocker ignored = data.acquireReadLock()) {
      final boolean repairAtStartAndOnlyOwn =
          Properties.getBattleshipsRepairAtBeginningOfRound(data.getProperties());
      final boolean repairAtEndAndAll =
          Properties.getBattleshipsRepairAtEndOfRound(data.getProperties());
      // if both are off, we do no repairing, no matter what
      if (!repairAtStartAndOnlyOwn && !repairAtEndAndAll) {
        return false;
      }

      final String prop =
          data.getSequence()
              .getStep()
              .getProperties()
              .getProperty(GameStep.PropertyKeys.REPAIR_UNITS);
      if (prop != null) {
        return Boolean.parseBoolean(prop);
      }

      return (isCombatDelegate(data) && repairAtStartAndOnlyOwn)
          || (data.getSequence().getStep().getName().endsWith("EndTurn") && repairAtEndAndAll);
    }
  }

  /**
   * Resets then gives bonus movement. Normally would occur at the start of combat movement phase.
   */
  static boolean isGiveBonusMovement(final GameData data) {
    try (GameData.Unlocker ignored = data.acquireReadLock()) {
      final String prop =
          data.getSequence()
              .getStep()
              .getProperties()
              .getProperty(GameStep.PropertyKeys.GIVE_BONUS_MOVEMENT);
      return prop != null ? Boolean.parseBoolean(prop) : isCombatDelegate(data);
    }
  }

  /**
   * Kills all air that cannot land. Normally would occur both at the end of noncombat movement and
   * also at end of placement phase.
   */
  public static boolean isRemoveAirThatCanNotLand(final GameData data) {
    try (GameData.Unlocker ignored = data.acquireReadLock()) {
      final String prop =
          data.getSequence()
              .getStep()
              .getProperties()
              .getProperty(GameStep.PropertyKeys.REMOVE_AIR_THAT_CAN_NOT_LAND);
      if (prop != null) {
        return Boolean.parseBoolean(prop);
      }
      return (data.getSequence().getStep().getDelegate() == null
              || !NoAirCheckPlaceDelegate.class.equals(
                  data.getSequence().getStep().getDelegate().getClass()))
          && (isNonCombatDelegate(data)
              || data.getSequence().getStep().getName().endsWith("Place"));
    }
  }

  /**
   * For situations where player phases are intermeshed. Effects so far: Lets air live if the other
   * players could put a carrier under it.
   *
   * @return a set of player ids. if argument player is not null this set will definitely include
   *     that player, but if not the set could be empty. never null.
   */
  public static Set<GamePlayer> getCombinedTurns(
      final GameData data, final @Nullable GamePlayer player) {
    checkNotNull(data);

    return getPlayersFromProperty(data, GameStep.PropertyKeys.COMBINED_TURNS, player);
  }

  /**
   * Resets unit state, such as movement, submerged, transport unload/load, airborne, etc. Normally
   * does not occur.
   */
  static boolean isResetUnitStateAtStart(final GameData data) {
    try (GameData.Unlocker ignored = data.acquireReadLock()) {
      final String prop =
          data.getSequence()
              .getStep()
              .getProperties()
              .getProperty(GameStep.PropertyKeys.RESET_UNIT_STATE_AT_START);
      return Boolean.parseBoolean(prop);
    }
  }

  /**
   * Resets unit state, such as movement, submerged, transport unload/load, airborne, etc. Normally
   * occurs at end of noncombat move phase.
   */
  static boolean isResetUnitStateAtEnd(final GameData data) {
    try (GameData.Unlocker ignored = data.acquireReadLock()) {
      final String prop =
          data.getSequence()
              .getStep()
              .getProperties()
              .getProperty(GameStep.PropertyKeys.RESET_UNIT_STATE_AT_END);
      return prop != null ? Boolean.parseBoolean(prop) : isNonCombatDelegate(data);
    }
  }

  /** Indicates bid purchase or placement is enabled for the specified game. */
  public static boolean isBid(final GameData data) {
    try (GameData.Unlocker ignored = data.acquireReadLock()) {
      final String prop =
          data.getSequence().getStep().getProperties().getProperty(GameStep.PropertyKeys.BID);
      return prop != null
          ? Boolean.parseBoolean(prop)
          : (isBidPurchaseDelegate(data) || isBidPlaceDelegate(data));
    }
  }

  /**
   * Returns the collection of players whose units can be repaired by the specified player.
   *
   * @return a set of player ids. if argument player is not null this set will definitely include
   *     that player, but if not the set could be empty. never null.
   */
  public static Set<GamePlayer> getRepairPlayers(
      final GameData data, final @Nullable GamePlayer player) {
    checkNotNull(data);

    return getPlayersFromProperty(data, GameStep.PropertyKeys.REPAIR_PLAYERS, player);
  }

  /**
   * Allow a purchase phase which only occurs if infrastructure is disabled and allows repairing it.
   * This is useful for when combat move is before main purchase phase but want to allow repairing
   * infrastructure that provides movement bonus or repairs unit HP.
   */
  public static boolean isOnlyRepairIfDisabled(final GameData data) {
    try (GameData.Unlocker ignored = data.acquireReadLock()) {
      return Boolean.parseBoolean(
          data.getSequence()
              .getStep()
              .getProperties()
              .getProperty(GameStep.PropertyKeys.ONLY_REPAIR_IF_DISABLED, "false"));
    }
  }

  private static boolean isNonCombatDelegate(final GameState data) {
    return GameStep.isNonCombatMoveStepName(data.getSequence().getStep().getName());
  }

  private static boolean isCombatDelegate(final GameState data) {
    return GameStep.isCombatMoveStepName(data.getSequence().getStep().getName());
  }

  private static boolean isAirborneDelegate(final GameState data) {
    return GameStep.isAirborneCombatMoveStepName(data.getSequence().getStep().getName());
  }

  private static boolean isBidPurchaseDelegate(final GameState data) {
    return GameStep.isBidStepName(data.getSequence().getStep().getName());
  }

  private static boolean isBidPlaceDelegate(final GameState data) {
    return GameStep.isBidPlaceStepName(data.getSequence().getStep().getName());
  }

  private static boolean isPurchaseDelegate(final GameState data) {
    return GameStep.isPurchaseStepName(data.getSequence().getStep().getName());
  }
}
