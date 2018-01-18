package games.strategy.triplea.delegate;

import java.io.Serializable;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.gamePlayer.IRemotePlayer;
import games.strategy.engine.message.IRemote;
import games.strategy.sound.ISound;

/**
 * Base class designed to make writing custom delegates simpler.
 * Code common to all delegates is implemented here.
 */
public abstract class AbstractDelegate implements IDelegate {
  protected String name;
  protected String displayName;
  protected PlayerID player;
  protected IDelegateBridge bridge;

  /**
   * Creates a new instance of the Delegate.
   */
  public AbstractDelegate() {}

  @Override
  public void initialize(final String name, final String displayName) {
    this.name = name;
    this.displayName = displayName;
  }

  @Override
  public void setDelegateBridgeAndPlayer(final IDelegateBridge delegateBridge) {
    bridge = delegateBridge;
    player = delegateBridge.getPlayerId();
  }

  /**
   * Called before the delegate will run.
   * All classes should call super.start if they override this.
   */
  @Override
  public void start() {
    // nothing to do here
  }

  /**
   * Called before the delegate will stop running.
   * All classes should call super.end if they override this.
   */
  @Override
  public void end() {
    // nothing to do here
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Returns the state of the Delegate.
   * All classes should super.saveState if they override this.
   */
  @Override
  public Serializable saveState() {
    return null;
  }

  @Override
  public void loadState(final Serializable state) {
    // nothing to save
  }

  @Override
  public abstract Class<? extends IRemote> getRemoteType();

  @Override
  public IDelegateBridge getBridge() {
    return bridge;
  }

  protected GameData getData() {
    return bridge.getData();
  }

  protected IDisplay getDisplay() {
    return getDisplay(bridge);
  }

  protected static IDisplay getDisplay(final IDelegateBridge bridge) {
    return bridge.getDisplayChannelBroadcaster();
  }

  protected ISound getSoundChannel() {
    return getSoundChannel(bridge);
  }

  protected static ISound getSoundChannel(final IDelegateBridge bridge) {
    return bridge.getSoundChannelBroadcaster();
  }

  protected IRemotePlayer getRemotePlayer() {
    return getRemotePlayer(bridge);
  }

  protected static IRemotePlayer getRemotePlayer(final IDelegateBridge bridge) {
    return bridge.getRemotePlayer();
  }

  /**
   * You should override this class with some variation of the following code (changing the AI to be something
   * meaningful if needed)
   * because otherwise an "isNull" (ie: the static "Neutral" player) will not have any remote:
   * <p>
   * if (player.isNull()) {
   * return new WeakAi(player.getName(), TripleA.WEAK_COMPUTER_PLAYER_TYPE);
   * }
   * return bridge.getRemotePlayer(player);
   * </p>
   */
  protected IRemotePlayer getRemotePlayer(final PlayerID player) {
    return bridge.getRemotePlayer(player);
  }
}
/*
 * All overriding classes should use the following format for saveState and loadState, in order to save and load the
 * superstate
 * class ExtendedDelegateState implements Serializable
 * {
 * Serializable superState;
 * // add other variables here:
 * }
 *
 * @Override
 * public Serializable saveState()
 * {
 * ExtendedDelegateState state = new ExtendedDelegateState();
 * state.superState = super.saveState();
 * // add other variables to state here:
 * return state;
 * }
 *
 * @Override
 * public void loadState(Serializable state)
 * {
 * ExtendedDelegateState s = (ExtendedDelegateState) state;
 * super.loadState(s.superState);
 * // load other variables from state here:
 * }
 */
