package games.strategy.triplea.delegate.remote.typed;

import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.player.PlayerBridge;
import games.strategy.net.websocket.ClientNetworkBridge;
import java.io.Serializable;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

/**
 * Base for the typed-message-backed delegate adapters handed to the AI and the action UI in place
 * of the reflective current-delegate proxy. Business methods on the concrete subclass forward to
 * {@link #invokeCurrent} which addresses the current step's delegate over the messenger latch,
 * mirroring {@link PlayerBridge#getRemoteDelegate()}'s lookup. The lifecycle and state methods of
 * {@link IDelegate} are never reached through a remote proxy (they run server-side on the real
 * delegate), so they stay unsupported here.
 */
public abstract class AbstractTypedCurrentDelegate implements IDelegate, IRemote {
  private final PlayerBridge playerBridge;

  protected AbstractTypedCurrentDelegate(final PlayerBridge playerBridge) {
    this.playerBridge = playerBridge;
  }

  protected <R extends WebSocketMessage> R invokeCurrent(
      final WebSocketMessage request, final MessageType<R> responseType) {
    return playerBridge.invokeCurrentDelegate(request, responseType);
  }

  protected UnsupportedOperationException notForwarded(final String method) {
    return new UnsupportedOperationException(
        method
            + " is not forwarded by the typed delegate adapter; it runs server-side on the real"
            + " delegate, never through the remote proxy");
  }

  @Override
  public void initialize(final String name, final String displayName) {
    throw notForwarded("initialize");
  }

  @Override
  public void setDelegateBridgeAndPlayer(
      final IDelegateBridge delegateBridge, final ClientNetworkBridge clientNetworkBridge) {
    throw notForwarded("setDelegateBridgeAndPlayer");
  }

  @Override
  public void setDelegateBridgeAndPlayer(final IDelegateBridge delegateBridge) {
    throw notForwarded("setDelegateBridgeAndPlayer");
  }

  @Override
  public void start() {
    throw notForwarded("start");
  }

  @Override
  public void end() {
    throw notForwarded("end");
  }

  @Override
  public String getName() {
    throw notForwarded("getName");
  }

  @Override
  public String getDisplayName() {
    throw notForwarded("getDisplayName");
  }

  @Override
  public IDelegateBridge getBridge() {
    throw notForwarded("getBridge");
  }

  @Override
  public Serializable saveState() {
    throw notForwarded("saveState");
  }

  @Override
  public void loadState(final Serializable state) {
    throw notForwarded("loadState");
  }

  @Override
  public Class<? extends IRemote> getRemoteType() {
    throw notForwarded("getRemoteType");
  }

  @Override
  public boolean delegateCurrentlyRequiresUserInput() {
    throw notForwarded("delegateCurrentlyRequiresUserInput");
  }
}
