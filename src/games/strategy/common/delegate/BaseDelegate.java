package games.strategy.common.delegate;

import java.io.Serializable;

import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.gamePlayer.IRemotePlayer;

/**
 * Base class designed to make writing custom delegates simpler.
 */
public abstract class BaseDelegate extends AbstractDelegate {
  private boolean m_startBaseStepsFinished = false;
  private boolean m_endBaseStepsFinished = false;

  /**
   * Creates a new instance of the Delegate
   */
  public BaseDelegate() {
    super();
  }

  /**
   * Called before the delegate will run.
   * All classes should call super.start if they override this to enable where-used search.
   * Persistent delegates like Edit Delegate should not extend BaseDelegate, because we do not want to fire triggers in
   * the edit delegate.
   */
  @Override
  public void start() {
    super.start();
    if (!m_startBaseStepsFinished) {
      m_startBaseStepsFinished = true;
      triggerOnStart();
    }
  }

  /**
   * This methods is executed on start of the delegate. By default nothing is done.
   * All classes should call super.end if they override this to enable where-used search.
   */
  protected void triggerOnStart() {}

  /**
   * Called before the delegate will stop running.
   * All classes should call super.end if they override this to enable where-used search.
   * Persistent delegates like Edit Delegate should not extend BaseDelegate, because we do not want to fire triggers in
   * the edit delegate.
   */
  @Override
  public void end() {
    super.end();
    // we are firing triggers, for maps that include them
    if (!m_endBaseStepsFinished) {
      m_endBaseStepsFinished = true;
      triggerOnEnd();
    }
    // these should probably be somewhere else, but we are relying on the fact that reloading a save go into the start
    // step,
    // but nothing goes into the end step, and therefore there is no way to save then have the end step repeat itself
    m_startBaseStepsFinished = false;
    m_endBaseStepsFinished = false;
  }

  /**
   * This methods is executed on end of the delegate. By default nothing is done.
   * All classes should call super.end if they override this to enable where-used search.
   */
  protected void triggerOnEnd() {}

  /**
   * Returns the state of the Delegate.
   * All classes should call super.saveState if they override this to enable where-used search.
   */
  @Override
  public Serializable saveState() {
    final BaseDelegateState state = new BaseDelegateState();
    state.m_startBaseStepsFinished = m_startBaseStepsFinished;
    state.m_endBaseStepsFinished = m_endBaseStepsFinished;
    return state;
  }

  /**
   * Loads the delegates state
   * All classes should call super.loadState if they override this to enable where-used search.
   */
  @Override
  public void loadState(final Serializable state) {
    final BaseDelegateState s = (BaseDelegateState) state;
    m_startBaseStepsFinished = s.m_startBaseStepsFinished;
    m_endBaseStepsFinished = s.m_endBaseStepsFinished;
  }

  @Override
  protected IDisplay getDisplay() {
    return getDisplay(m_bridge);
  }

  protected static IDisplay getDisplay(final IDelegateBridge bridge) {
    return bridge.getDisplayChannelBroadcaster();
  }

  @Override
  protected IRemotePlayer getRemotePlayer() {
    return getRemotePlayer(m_bridge);
  }

  protected static IRemotePlayer getRemotePlayer(final IDelegateBridge bridge) {
    return bridge.getRemotePlayer();
  }

}


class BaseDelegateState implements Serializable {
  private static final long serialVersionUID = 7130686697155151908L;
  public boolean m_startBaseStepsFinished = false;
  public boolean m_endBaseStepsFinished = false;
}
