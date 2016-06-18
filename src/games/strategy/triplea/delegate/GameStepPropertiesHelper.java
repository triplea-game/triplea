package games.strategy.triplea.delegate;

import java.util.HashSet;
import java.util.Set;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.data.PlayerID;

/**
 * A helper class for determining Game Step Properties.
 * These are things such as whether a move phase is combat move or noncombat move,
 * or whether we are going to post to a forum during this end turn phase.
 */
public class GameStepPropertiesHelper {
  /**
   * Do we skip posting the game summary and save to a forum or email?
   */
  public static boolean isSkipPosting(final GameData data) {
    final boolean skipPosting;
    data.acquireReadLock();
    try {
      skipPosting = Boolean.parseBoolean(
          data.getSequence().getStep().getProperties().getProperty(GameStep.PROPERTY_skipPosting, "false"));
    } finally {
      data.releaseReadLock();
    }
    return skipPosting;
  }

  /**
   * What players is this turn summary for? If more than 1 player, whose phases are touching or intermeshed, then we
   * will summarize for all
   * those phases.
   *
   * @return colon separated list of player names. could be empty. can be null if not set.
   */
  public static Set<PlayerID> getTurnSummaryPlayers(final GameData data) {
    final Set<PlayerID> allowedIDs;
    data.acquireReadLock();
    try {
      final String allowedPlayers =
          data.getSequence().getStep().getProperties().getProperty(GameStep.PROPERTY_turnSummaryPlayers);
      if (allowedPlayers != null) {
        allowedIDs = new HashSet<>();
        for (final String p : allowedPlayers.split(":")) {
          final PlayerID id = data.getPlayerList().getPlayerID(p);
          if (id == null) {
            System.err.println("gamePlay sequence step: " + data.getSequence().getStep().getName() + " stepProperty: "
                + GameStep.PROPERTY_turnSummaryPlayers + " player: " + p + " DOES NOT EXIST");
          } else {
            allowedIDs.add(id);
          }
        }
      } else {
        allowedIDs = null;
      }
    } finally {
      data.releaseReadLock();
    }
    return allowedIDs;
  }

  /**
   * For various things related to movement validation.
   */
  public static boolean isAirborneMove(final GameData data) {
    final boolean isAirborneMove;
    data.acquireReadLock();
    try {
      final String prop = data.getSequence().getStep().getProperties().getProperty(GameStep.PROPERTY_airborneMove);
      if (prop != null) {
        isAirborneMove = Boolean.parseBoolean(prop);
      } else {
        isAirborneMove = isAirborneDelegate(data);
      }
    } finally {
      data.releaseReadLock();
    }
    return isAirborneMove;
  }

  /**
   * For various things related to movement validation.
   */
  public static boolean isCombatMove(final GameData data, final boolean doNotThrowErrorIfNotMoveDelegate) {
    return checkMoveType(GameStep.PROPERTY_combatMove, data, doNotThrowErrorIfNotMoveDelegate);
  }

  private static boolean checkMoveType(String gameStep, final GameData data, final boolean doNotThrowErrorIfNotMoveDelegate) {
    final boolean isCombatMove;
    data.acquireReadLock();
    try {
      final String prop = data.getSequence().getStep().getProperties().getProperty(gameStep);
      if (prop != null) {
        isCombatMove = Boolean.parseBoolean(prop);
      } else if (isCombatDelegate(data)) {
        isCombatMove = true;
      } else if (isNonCombatDelegate(data)) {
        isCombatMove = false;
      } else if (doNotThrowErrorIfNotMoveDelegate) {
        isCombatMove = false;
      } else {
        throw new IllegalStateException("Cannot determine combat or not: " + data.getSequence().getStep().getName());
      }
    } finally {
      data.releaseReadLock();
    }
    return isCombatMove;
  }

  /**
   * For various things related to movement validation.
   */
  public static boolean isNonCombatMove(final GameData data, final boolean doNotThrowErrorIfNotMoveDelegate) {
    return checkMoveType(GameStep.PROPERTY_nonCombatMove, data, doNotThrowErrorIfNotMoveDelegate);
  }

  /**
   * Fire rockets after phase is over. Normally would occur after combat move for WW2v2 and WW2v3, and after noncombat
   * move for WW2v1.
   */
  public static boolean isFireRockets(final GameData data) {
    final boolean isFireRockets;
    data.acquireReadLock();
    try {
      final String prop = data.getSequence().getStep().getProperties().getProperty(GameStep.PROPERTY_fireRockets);
      if (prop != null) {
        Boolean.parseBoolean(prop);
      } else if (games.strategy.triplea.Properties.getWW2V2(data) || games.strategy.triplea.Properties.getWW2V3(data)) {
        isFireRockets = isCombatDelegate(data);
      } else {
        isFireRockets = isNonCombatDelegate(data);
      }
    } finally {
      data.releaseReadLock();
    }
    return isFireRockets;
  }

  /**
   * Repairs damaged units. Normally would occur at either start of combat move or end of turn, depending.
   */
  public static boolean isRepairUnits(final GameData data) {
    data.acquireReadLock();
    try {
      final boolean repairAtStartAndOnlyOwn =
          games.strategy.triplea.Properties.getBattleshipsRepairAtBeginningOfRound(data);
      final boolean repairAtEndAndAll = games.strategy.triplea.Properties.getBattleshipsRepairAtEndOfRound(data);
      // if both are off, we do no repairing, no matter what
      if (!repairAtStartAndOnlyOwn && !repairAtEndAndAll) {
        return false;
      } else {
        final String prop = data.getSequence().getStep().getProperties().getProperty(GameStep.PROPERTY_repairUnits);
        return (prop != null && Boolean.parseBoolean(prop)) ||
            (isCombatDelegate(data) && repairAtStartAndOnlyOwn) ||
            (data.getSequence().getStep().getName().endsWith("EndTurn") && repairAtEndAndAll);
      }
    } finally {
      data.releaseReadLock();
    }
  }

  /**
   * Resets then gives bonus movement. Normally would occur at the start of combat movement phase.
   */
  public static boolean isGiveBonusMovement(final GameData data) {
    data.acquireReadLock();
    try {
      final String prop = data.getSequence().getStep().getProperties().getProperty(GameStep.PROPERTY_giveBonusMovement);
      return (prop != null && Boolean.parseBoolean(prop)) ||isCombatDelegate(data);
    } finally {
      data.releaseReadLock();
    }
  }

  /**
   * Kills all air that cannot land. Normally would occur both at the end of noncombat movement and also at end of
   * placement phase.
   */
  public static boolean isRemoveAirThatCanNotLand(final GameData data) {
    final boolean isRemoveAir;
    data.acquireReadLock();
    try {
      final String prop =
          data.getSequence().getStep().getProperties().getProperty(GameStep.PROPERTY_removeAirThatCanNotLand);
      if (prop != null) {
        isRemoveAir = Boolean.parseBoolean(prop);
      } else if (data.getSequence().getStep().getDelegate() != null
          && NoAirCheckPlaceDelegate.class.equals(data.getSequence().getStep().getDelegate().getClass())) {
        isRemoveAir = false;
      } else if (isNonCombatDelegate(data)) {
        isRemoveAir = true;
      } else {
        isRemoveAir = data.getSequence().getStep().getName().endsWith("Place");
      }
    } finally {
      data.releaseReadLock();
    }
    return isRemoveAir;
  }

  /**
   * For situations where player phases are intermeshed.
   * Effects so far:
   * Lets air live if the other players could put a carrier under it.
   *
   * @return a set of player ids. if argument player is not null this set will definitely include that player, but if
   *         not the set could be
   *         empty. never null.
   */
  public static Set<PlayerID> getCombinedTurns(final GameData data, final PlayerID player) {
    final Set<PlayerID> allowedIDs = new HashSet<>();
    data.acquireReadLock();
    try {
      final String allowedPlayers =
          data.getSequence().getStep().getProperties().getProperty(GameStep.PROPERTY_combinedTurns);
      if (player != null) {
        allowedIDs.add(player);
      }
      if (allowedPlayers != null) {
        for (final String p : allowedPlayers.split(":")) {
          final PlayerID id = data.getPlayerList().getPlayerID(p);
          if (id == null) {
            System.err.println("gamePlay sequence step: " + data.getSequence().getStep().getName() + " stepProperty: "
                + GameStep.PROPERTY_combinedTurns + " player: " + p + " DOES NOT EXIST");
          } else {
            allowedIDs.add(id);
          }
        }
      }
    } finally {
      data.releaseReadLock();
    }
    return allowedIDs;
  }

  /**
   * Resets unit state, such as movement, submerged, transport unload/load, airborne, etc. Normally does not occur.
   */
  public static boolean isResetUnitStateAtStart(final GameData data) {
    data.acquireReadLock();
    try {
      final String prop =
          data.getSequence().getStep().getProperties().getProperty(GameStep.PROPERTY_resetUnitStateAtStart);
      return (prop != null && Boolean.parseBoolean(prop));
    } finally {
      data.releaseReadLock();
    }
  }

  /**
   * Resets unit state, such as movement, submerged, transport unload/load, airborne, etc. Normally occurs at end of
   * noncombat move phase.
   */
  public static boolean isResetUnitStateAtEnd(final GameData data) {
    data.acquireReadLock();
    try {
      final String prop =
          data.getSequence().getStep().getProperties().getProperty(GameStep.PROPERTY_resetUnitStateAtEnd);
      return (prop != null && Boolean.parseBoolean(prop)) || isNonCombatDelegate(data);
    } finally {
      data.releaseReadLock();
    }
  }

  public static boolean isBid(final GameData data) {
    data.acquireReadLock();
    try {
      final String prop = data.getSequence().getStep().getProperties().getProperty(GameStep.PROPERTY_bid);
      return (prop != null && Boolean.parseBoolean(prop)) || isBidPurchaseDelegate(data) || isBidPlaceDelegate(data);
    } finally {
      data.releaseReadLock();
    }
  }

  /**
   * @return a set of player ids. if argument player is not null this set will definitely include that player, but if
   *         not the set could be
   *         empty. never null.
   */
  public static Set<PlayerID> getRepairPlayers(final GameData data, final PlayerID player) {
    final Set<PlayerID> allowedIDs = new HashSet<>();
    data.acquireReadLock();
    try {
      final String allowedPlayers =
          data.getSequence().getStep().getProperties().getProperty(GameStep.PROPERTY_repairPlayers);
      if (player != null) {
        allowedIDs.add(player);
      }
      if (allowedPlayers != null) {
        for (final String p : allowedPlayers.split(":")) {
          final PlayerID id = data.getPlayerList().getPlayerID(p);
          if (id == null) {
            System.err.println("gamePlay sequence step: " + data.getSequence().getStep().getName() + " stepProperty: "
                + GameStep.PROPERTY_repairPlayers + " player: " + p + " DOES NOT EXIST");
          } else {
            allowedIDs.add(id);
          }
        }
      }
    } finally {
      data.releaseReadLock();
    }
    return allowedIDs;
  }

  // private static members for testing default situation based on name of delegate
  private static boolean isNonCombatDelegate(final GameData data) {
    return data.getSequence().getStep().getName().endsWith("NonCombatMove");
  }

  private static boolean isCombatDelegate(final GameData data) {
    return data.getSequence().getStep().getName().endsWith("CombatMove");
  }

  private static boolean isAirborneDelegate(final GameData data) {
    return data.getSequence().getStep().getName().endsWith("AirborneCombatMove");
  }

  private static boolean isBidPurchaseDelegate(final GameData data) {
    return data.getSequence().getStep().getName().endsWith("Bid");
  }

  private static boolean isBidPlaceDelegate(final GameData data) {
    return data.getSequence().getStep().getName().endsWith("BidPlace");
  }
}
