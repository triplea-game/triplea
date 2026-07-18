package games.strategy.engine.data;

import games.strategy.engine.data.changefactory.ObjectPropertyChange;
import games.strategy.triplea.Constants;
import java.util.EnumSet;
import java.util.Set;

/** Enum that represents various possible game data change events. */
public enum GameDataEvent {
  UNIT_MOVED,
  GAME_STEP_CHANGED,
  TECH_ATTACHMENT_CHANGED;

  /** Returns all game data events represented by a change, including nested composite changes. */
  static Set<GameDataEvent> lookupGameDataChangeEvents(final Change change) {
    final Set<GameDataEvent> events = EnumSet.noneOf(GameDataEvent.class);
    if (hasMoveChange(change)) {
      events.add(UNIT_MOVED);
    }
    if (hasTechAttachmentChange(change)) {
      events.add(TECH_ATTACHMENT_CHANGED);
    }
    return events;
  }

  /**
   * Recursively checks if the change is or contains a 'ALREADY_MOVED' change action. This indicates
   * a unit has moved.
   */
  private static boolean hasMoveChange(final Change change) {
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

  /** Recursively checks if the change contains an update to a technology attachment. */
  static boolean hasTechAttachmentChange(final Change change) {
    if (change instanceof CompositeChange compositeChange) {
      return compositeChange.getChanges().stream().anyMatch(GameDataEvent::hasTechAttachmentChange);
    }
    return change instanceof ChangeAttachmentChange attachmentChange
        && Constants.TECH_ATTACHMENT_NAME.equals(attachmentChange.getAttachmentName());
  }
}
