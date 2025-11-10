package games.strategy.engine.data;

import games.strategy.engine.data.changefactory.ObjectPropertyChange;
import games.strategy.triplea.Constants;
import java.util.Optional;

/** Enum that represents various possible game data change events. */
public enum GameDataEvent {
  UNIT_MOVED,
  GAME_STEP_CHANGED,
  TECH_ATTACHMENT_CHANGED;

  /**
   * Converts a 'Change' object to a 'GameDataEvent' object, returns empty if the change object does
   * not correspond to any 'GameDataEvent'.
   */
  static Optional<GameDataEvent> lookupEvent(final Change change) {
    if (hasMoveChange(change)) {
      return Optional.of(UNIT_MOVED);
    }
    if (change instanceof ChangeAttachmentChange attachmentChange
        && attachmentChange.getAttachmentName().equals(Constants.TECH_ATTACHMENT_NAME)) {
      return Optional.of(TECH_ATTACHMENT_CHANGED);
    }

    return Optional.empty();
  }

  /**
   * Recursively checks if the change is or contains a 'ALREADY_MOVED' change action. This indicates
   * a unit has moved.
   */
  static boolean hasMoveChange(final Change change) {
    if (change instanceof CompositeChange compositeChange) {
      final boolean hasMoveChange =
          compositeChange.getChanges().stream().anyMatch(GameDataEvent::hasMoveChange);
      if (hasMoveChange) {
        return true;
      }
    }
    return (change instanceof ObjectPropertyChange objectPropertyChange
        && objectPropertyChange.getProperty().equals(Unit.PropertyName.ALREADY_MOVED.toString()));
  }
}
