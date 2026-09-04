package games.strategy.net;

import games.strategy.engine.chat.IChatChannel;
import games.strategy.engine.message.IChannelSubscriber;
import games.strategy.engine.message.RemoteName;
import games.strategy.engine.message.RemoteNotFoundException;
import games.strategy.engine.message.TypedInvocation;
import games.strategy.engine.message.TypedInvocationResult;
import games.strategy.engine.message.unifiedmessenger.InvocationExecutionGate;
import games.strategy.engine.message.unifiedmessenger.TypedMessageHandler;
import games.strategy.engine.message.unifiedmessenger.UnifiedMessenger;
import java.io.Serializable;
import lombok.ToString;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

/**
 * Groups the transport {@link IMessenger} with the {@link UnifiedMessenger} that correlates typed
 * request/response invocations and dispatches inbound messages to their registered handlers. The
 * reflective remote/channel proxy layer this used to wrap was removed by the RMI flatten; every
 * remote call now rides the typed message API below.
 */
@ToString
public class Messengers implements IMessenger {
  private final IMessenger messenger;
  private final UnifiedMessenger unifiedMessenger;

  public Messengers(final IMessenger messenger) {
    this.messenger = messenger;
    unifiedMessenger = new UnifiedMessenger(messenger);
  }

  /**
   * Registers the handler a typed message dispatches to when it arrives at a local endpoint. A
   * converted method makes this one call, then sends its message type instead of a proxy call.
   */
  public <T extends WebSocketMessage> void registerMessageHandler(
      final MessageType<T> messageType, final TypedMessageHandler<T> handler) {
    unifiedMessenger.getTypedMessageRegistry().register(messageType, handler);
  }

  /**
   * Reports whether a handler is already registered for the given typed message. Registration must
   * not be guarded with this: {@link #registerMessageHandler} overwrites, and re-registering on a
   * later game is how a handler's captured per-game game data is refreshed.
   */
  public boolean hasTypedMessageHandler(final MessageType<?> type) {
    return unifiedMessenger.getTypedMessageRegistry().hasHandler(type);
  }

  /** Fire-and-forget broadcast of a typed message to every subscriber of the given channel. */
  public void sendChannelMessage(final RemoteName channel, final WebSocketMessage message) {
    unifiedMessenger.invoke(channel.getName(), new TypedInvocation(channel.getName(), message));
  }

  /** Sends a typed request to a remote endpoint and blocks for its typed reply. */
  public <R extends WebSocketMessage> R invokeRemoteMessage(
      final RemoteName remote, final WebSocketMessage request, final MessageType<R> responseType) {
    final TypedInvocationResult results;
    try {
      results =
          unifiedMessenger.invokeAndWait(
              remote.getName(), new TypedInvocation(remote.getName(), request));
    } catch (final RemoteNotFoundException e) {
      throw new IllegalStateException("No remote registered for " + remote, e);
    }
    if (results.getException() != null) {
      throw new RuntimeException("Exception on remote", results.getException());
    }
    return responseType.getPayloadType().cast(results.getReturnValue());
  }

  // TODO: API could be improved, perhaps return an optional, and/or store exact instance types from
  // constructor.
  public IServerMessenger getServerMessenger() {
    return (IServerMessenger) messenger;
  }

  public void addChatChannelSubscriber(
      final IChatChannel chatChannelSubscriber, final String chatChannelName) {
    registerChannelSubscriber(
        chatChannelSubscriber, new RemoteName(chatChannelName, IChatChannel.class));
  }

  /** Registers a channel subscriber (single-threaded endpoint) for the given channel name. */
  public void registerChannelSubscriber(final Object implementor, final RemoteName channelName) {
    if (!IChannelSubscriber.class.isAssignableFrom(channelName.getClazz())) {
      throw new IllegalStateException(channelName.getClazz() + " is not a channel subscriber");
    }
    unifiedMessenger.addImplementor(channelName, implementor, true, InvocationExecutionGate.NONE);
  }

  public void unregisterChannelSubscriber(final Object implementor, final RemoteName channelName) {
    unifiedMessenger.removeImplementor(channelName.getName(), implementor);
  }

  /** Registers a remote implementor (multi-threaded endpoint) for the given remote name. */
  public void registerRemote(final Object implementor, final RemoteName name) {
    unifiedMessenger.addImplementor(name, implementor, false, InvocationExecutionGate.NONE);
  }

  /**
   * Registers a remote implementor whose inbound calls must be bracketed by the given execution
   * gate (the delegate endpoints acquire the delegate-execution read lock so a save cannot run
   * while an inbound message mutates game state).
   */
  public void registerRemote(
      final Object implementor,
      final RemoteName name,
      final InvocationExecutionGate executionGate) {
    unifiedMessenger.addImplementor(name, implementor, false, executionGate);
  }

  public void unregisterRemote(final String name) {
    if (unifiedMessenger.hasSingleImplementor(name)) {
      unifiedMessenger.removeImplementor(name, unifiedMessenger.getImplementor(name));
    }
  }

  public void unregisterRemote(final RemoteName name) {
    unregisterRemote(name.getName());
  }

  public boolean hasLocalImplementor(final RemoteName name) {
    return unifiedMessenger.hasSingleImplementor(name.getName());
  }

  @Override
  public void send(final Serializable msg, final INode to) {
    messenger.send(msg, to);
  }

  public void sendToServer(final Serializable msg) {
    messenger.send(msg, messenger.getServerNode());
  }

  @Override
  public void addMessageListener(final IMessageListener listener) {
    messenger.addMessageListener(listener);
  }

  @Override
  public INode getLocalNode() {
    return messenger.getLocalNode();
  }

  @Override
  public boolean isConnected() {
    return messenger.isConnected();
  }

  @Override
  public void shutDown() {
    messenger.shutDown();
  }

  @Override
  public boolean isServer() {
    return messenger.isServer();
  }

  @Override
  public INode getServerNode() {
    return messenger.getServerNode();
  }

  @Override
  public void addConnectionChangeListener(final IConnectionChangeListener listener) {
    messenger.addConnectionChangeListener(listener);
  }

  @Override
  public void removeConnectionChangeListener(final IConnectionChangeListener listener) {
    messenger.removeConnectionChangeListener(listener);
  }
}
