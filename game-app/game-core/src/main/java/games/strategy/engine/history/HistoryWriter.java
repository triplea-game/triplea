package games.strategy.engine.history;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import java.io.Serializable;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;

/** Used to write to a history object. Delegates should use a DelegateHistoryWriter. */
@Slf4j
public class HistoryWriter implements Serializable {
  private static final long serialVersionUID = 4230519614567508061L;

  private final History history;
  private HistoryNode current;

  public HistoryWriter(final History history) {
    this.history = history;
  }

  private void assertCorrectThread() {
    if (history.getGameData().areChangesOnlyInSwingEventThread()
        && !SwingUtilities.isEventDispatchThread()) {
      throw new IllegalStateException("Wrong thread");
    }
  }

  /** Can only be called if we are currently in a round or a step. */
  public void startNextStep(
      final String stepName,
      final String delegateName,
      final GamePlayer player,
      final String stepDisplayName) {
    assertCorrectThread();
    // we are being called for the first time
    if (current == null) {
      startNextRound(history.getGameData().getCurrentRound());
    }
    if (isCurrentEvent()) {
      closeCurrent();
    }
    // stop the current step
    if (isCurrentStep()) {
      closeCurrent();
    }
    if (!isCurrentRound()) {
      throw new IllegalStateException(
          "Not in a round, but trying to add step: "
              + stepName
              + ". Current history node is: "
              + current);
    }
    final Step currentStep =
        new Step(stepName, delegateName, player, history.getChanges().size(), stepDisplayName);
    addToAndSetCurrent(currentStep);
  }

  /**
   * Prepares to write a new round. Any current event, step, or round will be automatically closed
   * before beginning the new round.
   */
  public void startNextRound(final int round) {
    assertCorrectThread();
    if (isCurrentEvent()) {
      closeCurrent();
    }
    if (isCurrentStep()) {
      closeCurrent();
    }
    if (isCurrentRound()) {
      closeCurrent();
    }
    final Round currentRound = new Round(round, history.getChanges().size());
    current = (HistoryNode) history.getRoot();
    addToAndSetCurrent(currentRound);
  }

  private void closeCurrent() {
    assertCorrectThread();
    final HistoryNode old = current;
    try (GameData.Unlocker ignored = history.getGameData().acquireWriteLock()) {
      // remove steps where nothing happened
      if (isCurrentStep()) {
        final HistoryNode parent = (HistoryNode) current.getParent();
        if (current.getChildCount() == 0) {
          final int index = parent.getChildCount() - 1;
          parent.remove(current);
          history.nodesWereRemoved(parent, new int[] {index}, new Object[] {current});
        }
        ((Step) current).setChangeEndIndex(history.getChanges().size());
        current = parent;
        return;
      }
      current = (HistoryNode) current.getParent();
      ((IndexedHistoryNode) old).setChangeEndIndex(history.getChanges().size());
    }
  }

  private void addToAndSetCurrent(final HistoryNode newNode) {
    addToCurrent(newNode);
    current = newNode;
  }

  private void addToCurrent(final HistoryNode newNode) {
    try (GameData.Unlocker ignored = history.getGameData().acquireWriteLock()) {
      history.insertNodeInto(newNode, current, current.getChildCount());
    }
    history.goToEnd();
  }

  /** Fires a new event with the given event name. */
  public void startEvent(final String eventName) {
    assertCorrectThread();
    if (isCurrentEvent()) {
      closeCurrent();
    }
    final Event event = new Event(eventName, history.getChanges().size());
    addToAndSetCurrent(event);
  }

  private boolean isCurrentEvent() {
    return current instanceof Event;
  }

  private boolean isCurrentRound() {
    return current instanceof Round;
  }

  private boolean isCurrentStep() {
    return current instanceof Step;
  }

  /** Add a child to the current event. */
  public void addChildToEvent(final EventChild node) {
    assertCorrectThread();
    if (!isCurrentEvent()) {
      log.info(
          "Not in an event, but trying to add child: "
              + node
              + ". Current history node is: "
              + current);
      startEvent("Filler event for child: " + node);
    }
    addToCurrent(node);
  }

  /** Add a change to the current event. */
  public void addChange(final Change change) {
    assertCorrectThread();
    if (!isCurrentEvent() && !isCurrentStep()) {
      log.info(
          "Not in an event or step, but trying to add change: "
              + change
              + ". Current history node is: "
              + current);
      startEvent("Filler event for change: " + change);
    }
    history.changeAdded(change);
  }

  /** Sets the rendering data for the current event. */
  public void setRenderingData(final Object details) {
    assertCorrectThread();
    if (!isCurrentEvent()) {
      log.info(
          "Not in an event, but trying to set details: "
              + details
              + ". Current history node is: "
              + current);
      startEvent("Filler event for details: " + details);
    }
    try (GameData.Unlocker ignored = history.getGameData().acquireWriteLock()) {
      ((Event) current).setRenderingData(details);
    }
    history.goToEnd();
  }
}
