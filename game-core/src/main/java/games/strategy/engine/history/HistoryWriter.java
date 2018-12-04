package games.strategy.engine.history;

import java.io.Serializable;
import java.util.logging.Level;

import javax.swing.SwingUtilities;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.PlayerId;
import lombok.extern.java.Log;

/**
 * Used to write to a history object. Delegates should use a DelegateHistoryWriter.
 */
@Log
public class HistoryWriter implements Serializable {
  private static final long serialVersionUID = 4230519614567508061L;

  private final History history;
  private HistoryNode current;

  public HistoryWriter(final History history) {
    this.history = history;
  }

  private void assertCorrectThread() {
    if (history.getGameData().areChangesOnlyInSwingEventThread() && !SwingUtilities.isEventDispatchThread()) {
      throw new IllegalStateException("Wrong thread");
    }
  }

  /**
   * Can only be called if we are currently in a round or a step.
   */
  public void startNextStep(final String stepName, final String delegateName, final PlayerId player,
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
      throw new IllegalStateException("Not in a round");
    }
    final Step currentStep = new Step(stepName, delegateName, player, history.getChanges().size(), stepDisplayName);
    addToAndSetCurrent(currentStep);
  }

  /**
   * Prepares to write a new round. Any current event, step, or round will be automatically closed before beginning the
   * new round.
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
    history.getGameData().acquireWriteLock();
    try {
      // remove steps where nothing happened
      if (isCurrentStep()) {
        final HistoryNode parent = (HistoryNode) current.getParent();
        if (current.getChildCount() == 0) {
          final int index = parent.getChildCount() - 1;
          parent.remove(current);
          history.nodesWereRemoved(parent, new int[] {index}, new Object[] {current});
        }
        current = parent;
        return;
      }
      current = (HistoryNode) current.getParent();
      ((IndexedHistoryNode) old).setChangeEndIndex(history.getChanges().size());
    } finally {
      history.getGameData().releaseWriteLock();
    }
  }

  private void addToAndSetCurrent(final HistoryNode newNode) {
    addToCurrent(newNode);
    current = newNode;
  }

  private void addToCurrent(final HistoryNode newNode) {
    history.getGameData().acquireWriteLock();
    try {
      history.insertNodeInto(newNode, current, current.getChildCount());
    } finally {
      history.getGameData().releaseWriteLock();
    }
    history.goToEnd();
  }

  public void startEvent(final String eventName) {
    assertCorrectThread();
    if (isCurrentEvent()) {
      closeCurrent();
    }
    if (!isCurrentStep()) {
      throw new IllegalStateException("Cant add an event, not a step. "
          + "Must be in a step to add an event to the step. \nTrying to add event: " + eventName);
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

  /**
   * Add a child to the current event.
   */
  public void addChildToEvent(final EventChild node) {
    assertCorrectThread();
    if (!isCurrentEvent()) {
      log.log(Level.SEVERE, "Not in an event, but trying to add child:" + node + " current is:" + current);
      startEvent("???");
    }
    addToCurrent(node);
  }

  /**
   * Add a change to the current event.
   */
  public void addChange(final Change change) {
    assertCorrectThread();
    if (!isCurrentEvent() && !isCurrentStep()) {
      log.log(Level.SEVERE, "Not in an event, but trying to add change:" + change + " current is:" + current);
      startEvent("Bad Event for change: \n" + change.toString());
    }
    history.changeAdded(change);
  }

  /**
   * Sets the rendering data for the current event.
   */
  public void setRenderingData(final Object details) {
    assertCorrectThread();
    if (!isCurrentEvent()) {
      log.log(Level.SEVERE, "Not in an event, but trying to set details:" + details + " current is:" + current);
      startEvent("???");
    }
    history.getGameData().acquireWriteLock();
    try {
      ((Event) current).setRenderingData(details);
    } finally {
      history.getGameData().releaseWriteLock();
    }
    history.goToEnd();
  }
}
