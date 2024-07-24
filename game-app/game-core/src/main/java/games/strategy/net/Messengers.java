package games.strategy.net;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.chat.ChatController;
import games.strategy.engine.chat.IChatChannel;
import games.strategy.engine.chat.IChatController;
import games.strategy.engine.message.ChannelMessenger;
import games.strategy.engine.message.IChannelMessenger;
import games.strategy.engine.message.IChannelSubscriber;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.engine.message.RemoteMessenger;
import games.strategy.engine.message.RemoteName;
import games.strategy.engine.message.unifiedmessenger.UnifiedMessenger;
import java.io.Serializable;
import lombok.ToString;

/** Convenience grouping of a messenger, remote messenger and channel messenger. */
@ToString
public class Messengers implements IMessenger, IRemoteMessenger, IChannelMessenger {
  private final IMessenger messenger;
  private final IRemoteMessenger remoteMessenger;
  private final IChannelMessenger channelMessenger;

  public Messengers(final IMessenger messenger) {
    this.messenger = messenger;
    final UnifiedMessenger unifiedMessenger = new UnifiedMessenger(messenger);
    channelMessenger = new ChannelMessenger(unifiedMessenger);
    remoteMessenger = new RemoteMessenger(unifiedMessenger);
  }

  @VisibleForTesting
  public Messengers(
      final IMessenger messenger,
      final IRemoteMessenger remoteMessenger,
      final IChannelMessenger channelMessenger) {
    this.messenger = messenger;
    this.remoteMessenger = remoteMessenger;
    this.channelMessenger = channelMessenger;
  }

  // TODO: API could be improved, perhaps return an optional, and/or store exact instance types from
  // constructor.
  public IServerMessenger getServerMessenger() {
    return (IServerMessenger) messenger;
  }

  public IChatController getRemoteChatController(final String chatName) {
    return (IChatController)
        remoteMessenger.getRemote(ChatController.getChatControllerRemoteName(chatName));
  }

  public void addChatChannelSubscriber(
      final IChatChannel chatChannelSubscriber, final String chatChannelName) {
    channelMessenger.registerChannelSubscriber(
        chatChannelSubscriber, new RemoteName(chatChannelName, IChatChannel.class));
  }

  @Override
  public IChannelSubscriber getChannelBroadcaster(final RemoteName channelName) {
    return channelMessenger.getChannelBroadcaster(channelName);
  }

  @Override
  public void registerChannelSubscriber(final Object implementor, final RemoteName channelName) {
    channelMessenger.registerChannelSubscriber(implementor, channelName);
  }

  @Override
  public void unregisterChannelSubscriber(final Object implementor, final RemoteName channelName) {
    channelMessenger.unregisterChannelSubscriber(implementor, channelName);
  }

  @Override
  public IRemote getRemote(final RemoteName name) {
    return remoteMessenger.getRemote(name);
  }

  @Override
  public IRemote getRemote(final RemoteName name, final boolean ignoreResults) {
    return remoteMessenger.getRemote(name, ignoreResults);
  }

  @Override
  public void registerRemote(final Object implementor, final RemoteName name) {
    remoteMessenger.registerRemote(implementor, name);
  }

  @Override
  public void unregisterRemote(final String name) {
    remoteMessenger.unregisterRemote(name);
  }

  @Override
  public void unregisterRemote(final RemoteName name) {
    remoteMessenger.unregisterRemote(name);
  }

  @Override
  public boolean hasLocalImplementor(final RemoteName name) {
    return remoteMessenger.hasLocalImplementor(name);
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
