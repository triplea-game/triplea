package games.strategy.engine.history;

import javax.swing.SwingUtilities;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.PlayerID;

/**
 * Used to write to a history object. Delegates should use a
 * DelegateHistoryWriter
 */
public class HistoryWriter implements java.io.Serializable {
  private static final long serialVersionUID = 4230519614567508061L;
  private final History m_history;
  private HistoryNode m_current;

  public HistoryWriter(final History history) {
    m_history = history;
  }

  private void assertCorrectThread() {
    if (m_history.getGameData().areChangesOnlyInSwingEventThread() && !SwingUtilities.isEventDispatchThread()) {
      throw new IllegalStateException("Wrong thread");
    }
  }

  /**
   * Can only be called if we are currently in a round or a step
   */
  public void startNextStep(final String stepName, final String delegateName, final PlayerID player,
      final String stepDisplayName) {
    assertCorrectThread();
    // we are being called for the first time
    if (m_current == null) {
      int round = 0;
      m_history.getGameData().acquireReadLock();
      try {
        round = m_history.getGameData().getSequence().getRound();
      } finally {
        m_history.getGameData().releaseReadLock();
      }
      startNextRound(round);
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
    final Step currentStep = new Step(stepName, delegateName, player, m_history.getChanges().size(), stepDisplayName);
    addToAndSetCurrent(currentStep);
  }

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
    final Round currentRound = new Round(round, m_history.getChanges().size());
    m_current = (HistoryNode) m_history.getRoot();
    addToAndSetCurrent(currentRound);
  }

  private void closeCurrent() {
    assertCorrectThread();
    final HistoryNode old = m_current;
    m_history.getGameData().acquireWriteLock();
    try {
      // remove steps where nothing happened
      if (isCurrentStep()) {
        final HistoryNode parent = (HistoryNode) m_current.getParent();
        if (m_current.getChildCount() == 0) {
          final int index = parent.getChildCount() - 1;
          parent.remove(m_current);
          m_history.nodesWereRemoved(parent, new int[] {index}, new Object[] {m_current});
        }
        m_current = parent;
        return;
      }
      m_current = (HistoryNode) m_current.getParent();
      ((IndexedHistoryNode) old).setChangeEndIndex(m_history.getChanges().size());
    } finally {
      m_history.getGameData().releaseWriteLock();
    }
  }

  private void addToAndSetCurrent(final HistoryNode newNode) {
    addToCurrent(newNode);
    m_current = newNode;
  }

  private void addToCurrent(final HistoryNode newNode) {
    m_history.getGameData().acquireWriteLock();
    try {
      m_history.insertNodeInto(newNode, m_current, m_current.getChildCount());
    } finally {
      m_history.getGameData().releaseWriteLock();
    }
    m_history.goToEnd();
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
    final Event event = new Event(eventName, m_history.getChanges().size());
    addToAndSetCurrent(event);
  }

  private boolean isCurrentEvent() {
    return m_current instanceof Event;
  }

  private boolean isCurrentRound() {
    return m_current instanceof Round;
  }

  private boolean isCurrentStep() {
    return m_current instanceof Step;
  }

  /**
   * Add a child to the current event.
   */
  public void addChildToEvent(final EventChild node) {
    assertCorrectThread();
    if (!isCurrentEvent()) {
      new IllegalStateException("Not in an event, but trying to add child:" + node + " current is:" + m_current)
          .printStackTrace(System.out);
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
      new IllegalStateException("Not in an event, but trying to add change:" + change + " current is:" + m_current)
          .printStackTrace(System.out);
      startEvent("Bad Event for change: \n" + change.toString());
    }
    m_history.changeAdded(change);
  }

  public void setRenderingData(final Object details) {
    assertCorrectThread();
    if (!isCurrentEvent()) {
      new IllegalStateException("Not in an event, but trying to set details:" + details + " current is:" + m_current)
          .printStackTrace(System.out);
      startEvent("???");
    }
    m_history.getGameData().acquireWriteLock();
    try {
      ((Event) m_current).setRenderingData(details);
    } finally {
      m_history.getGameData().releaseWriteLock();
    }
    m_history.goToEnd();
  }
}
