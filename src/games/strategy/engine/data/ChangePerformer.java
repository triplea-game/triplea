package games.strategy.engine.data;

import javax.swing.SwingUtilities;

/**
 * Allows changes to be performed outside of the data package.
 * Should not be created by non engine code.
 * Made this since I didn't want to unprotect the Change.perform method,
 * but didn't want to put everything that needed to
 * perform a change in the data package.
 */
public class ChangePerformer {
  private final GameData m_data;

  /**
   * Creates a new instance of ChangePerformer
   *
   * @param data
   *        game data
   */
  public ChangePerformer(final GameData data) {
    if (data == null) {
      throw new IllegalArgumentException("Data cannot be null");
    }
    m_data = data;
  }

  public void perform(final Change aChange) {
    if (m_data.areChangesOnlyInSwingEventThread() && !SwingUtilities.isEventDispatchThread()) {
      throw new IllegalStateException("Wrong thread");
    }
    try {
      m_data.acquireWriteLock();
      aChange.perform(m_data);
    } finally {
      m_data.releaseWriteLock();
    }
    m_data.notifyGameDataChanged(aChange);
  }

  public static void perform(final Change aChange, final GameData gameData) {
    (new ChangePerformer(gameData)).perform(aChange);
  }
}
