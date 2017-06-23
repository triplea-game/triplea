package games.strategy.triplea.ui;

import java.util.concurrent.CountDownLatch;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;

/**
 * Abstract superclass for all action panels. <br>
 */
public abstract class ActionPanel extends JPanel {
  private static final long serialVersionUID = -5954576036704958641L;
  private final GameData data;
  private PlayerID currentPlayer;
  protected final MapPanel map;
  private boolean active;
  private CountDownLatch latch;
  private final Object latchLock = new Object();

  /** Creates new ActionPanel. */
  public ActionPanel(final GameData data, final MapPanel map) {
    this.data = data;
    this.map = map;
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    setBorder(new EmptyBorder(5, 5, 0, 0));
  }

  protected final boolean isWW2V2() {
    return games.strategy.triplea.Properties.getWW2V2(data);
  }

  protected final boolean isWW2V3TechModel() {
    return games.strategy.triplea.Properties.getWW2V3TechModel(data);
  }

  protected final boolean isRestrictedPurchase() {
    return games.strategy.triplea.Properties.getPlacementRestrictedByFactory(data);
  }


  protected final boolean isSelectableTechRoll() {
    return games.strategy.triplea.Properties.getSelectableTechRoll(data);
  }

  /**
   * Waitfor another thread to call release.
   * If the thread is interupted, we will return silently.
   *
   * <p>
   * A memory barrier will be crossed both on entering and before exiting this method.
   * </p>
   *
   * <p>
   * This method will return in the event of the game shutting down.
   * </p>
   */
  protected void waitForRelease() {
    if (Thread.currentThread().isInterrupted()) {
      release();
      return;
    }
    synchronized (latchLock) {
      if (latch != null) {
        throw new IllegalStateException("Latch not null");
      }
      latch = new CountDownLatch(1);
      map.getUIContext().addShutdownLatch(latch);
    }
    try {
      latch.await();
    } catch (final InterruptedException e) {
      release();
    }
    // cross a memory barrier
    synchronized (latchLock) {
    }
  }

  /**
   * Release the latch acquired by waitOnNewLatch()
   *
   * <p>
   * This method will crossed on entering this method.
   * </p>
   */
  protected void release() {
    synchronized (latchLock) {
      // not set up yet
      // this is ok as we set up in one thread
      // and wait in another
      // if the release happens too early
      // the user will be able to press done again
      if (latch == null) {
        return;
      }
      map.getUIContext().removeShutdownLatch(latch);
      latch.countDown();
      latch = null;
    }
  }

  protected GameData getData() {
    return data;
  }

  public void display(final PlayerID player) {
    currentPlayer = player;
    setActive(true);
  }

  protected PlayerID getCurrentPlayer() {
    return currentPlayer;
  }

  protected MapPanel getMap() {
    return map;
  }

  /**
   * Called when the history panel shows used to disable the panel
   * temporarily.
   */
  public void setActive(final boolean aBool) {
    active = aBool;
  }

  public boolean getActive() {
    return active;
  }

  /**
   * Refreshes the action panel. Should be run within the swing event queue.
   */
  protected final Runnable REFRESH = () -> {
    revalidate();
    repaint();
  };
}
