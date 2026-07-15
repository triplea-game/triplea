package games.strategy.triplea.delegate;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.UnitAttachment;
import lombok.experimental.UtilityClass;

/** Resolves a unit's maximum movement for the current movement phase. */
@UtilityClass
public final class MovementAllowanceResolver {
  public enum MovementPhase {
    COMBAT,
    REDEPLOYMENT,
    OTHER
  }

  public static int resolveMaximumMovement(final Unit unit) {
    return resolveMaximumMovement(unit, resolveCurrentPhase(unit.getData()));
  }

  @VisibleForTesting
  public static int resolveMaximumMovement(final Unit unit, final MovementPhase phase) {
    final UnitAttachment attachment = unit.getUnitAttachment();
    final int attachmentMovement =
        switch (phase) {
          case COMBAT -> attachment.getCombatMovement(unit.getOwner());
          case REDEPLOYMENT -> attachment.getRedeploymentMovement(unit.getOwner());
          case OTHER -> attachment.getMovement(unit.getOwner());
        };
    return Math.max(0, attachmentMovement + unit.getBonusMovement());
  }

  private static MovementPhase resolveCurrentPhase(final GameData data) {
    if (data.getSequence().size() == 0) {
      return MovementPhase.OTHER;
    }
    if (GameStepPropertiesHelper.isCombatMove(data, true)) {
      return MovementPhase.COMBAT;
    }
    if (GameStepPropertiesHelper.isNonCombatMove(data, true)) {
      return MovementPhase.REDEPLOYMENT;
    }
    return MovementPhase.OTHER;
  }
}
