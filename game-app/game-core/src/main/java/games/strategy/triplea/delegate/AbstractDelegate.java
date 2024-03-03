package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.net.websocket.ClientNetworkBridge;
import games.strategy.triplea.delegate.battle.casualty.CasualtySelector;
import java.io.Serializable;
import javax.annotation.Nullable;

/**
 * Base class designed to make writing custom delegates simpler. Code common to all delegates is
 * implemented here.
 */
public abstract class AbstractDelegate implements IDelegate {
  protected String name;
  protected String displayName;
  protected GamePlayer player;
  protected IDelegateBridge bridge;
  @Nullable protected ClientNetworkBridge clientNetworkBridge;

  @Override
  public void initialize(final String name, final String displayName) {
    this.name = name;
    this.displayName = displayName;
  }

  @Override
  public void setDelegateBridgeAndPlayer(
      final IDelegateBridge delegateBridge, final ClientNetworkBridge clientNetworkBridge) {
    bridge = delegateBridge;
    player = delegateBridge.getGamePlayer();
    this.clientNetworkBridge = clientNetworkBridge;
  }

  @Override
  public void setDelegateBridgeAndPlayer(final IDelegateBridge delegateBridge) {
    bridge = delegateBridge;
    player = delegateBridge.getGamePlayer();
  }

  /**
   * Called before the delegate will run. All classes should call super.start if they override this.
   */
  @Override
  public void start() {
    CasualtySelector.clearOolCache();
  }

  /**
   * Called before the delegate will stop running. All classes should call super.end if they
   * override this.
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
   * Returns the state of the Delegate. All classes should super.saveState if they override this.
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
  public IDelegateBridge getBridge() {
    return bridge;
  }

  protected GameData getData() {
    return bridge.getData();
  }

  protected GameProperties getProperties() {
    return getData().getProperties();
  }
}
