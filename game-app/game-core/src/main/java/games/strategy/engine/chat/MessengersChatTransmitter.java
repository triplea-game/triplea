package games.strategy.engine.chat;

import games.strategy.engine.message.MessageContext;
import games.strategy.engine.message.RemoteName;
import games.strategy.net.Messengers;
import games.strategy.net.websocket.ClientNetworkBridge;
import games.strategy.triplea.settings.ClientSetting;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.lobby.web.socket.messages.envelopes.chat.ChatParticipant;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;
import org.triplea.java.concurrency.AsyncRunner;

/** Chat transmitter that sends and receives messages over Java NIO sockets. */
@Slf4j
public class MessengersChatTransmitter implements ChatTransmitter {
  private final UserName userName;
  @Getter private final Messengers messengers;

  private IChatChannel chatChannelSubscriber;

  private final String chatName;
  private final String chatChannelName;
  private final ClientNetworkBridge clientNetworkBridge;
  private final List<Runnable> listenerRemovals = new ArrayList<>();

  public MessengersChatTransmitter(
      final String chatName,
      final Messengers messengers,
      final ClientNetworkBridge clientNetworkBridge) {
    this.userName = messengers.getLocalNode().getPlayerName();
    this.messengers = messengers;
    this.chatName = chatName;
    this.chatChannelName = ChatController.getChatChannelName(chatName);
    this.clientNetworkBridge = clientNetworkBridge;
  }

  @Override
  public void setChatClient(final ChatClient chatClient) {
    chatChannelSubscriber = chatChannelSubscriber(chatClient);
    addTrackedListener(
        IChatChannel.ChatMessage.TYPE, message -> message.invokeCallback(chatChannelSubscriber));
    addTrackedListener(
        IChatChannel.PingMessage.TYPE, message -> message.invokeCallback(chatChannelSubscriber));
    addTrackedListener(
        IChatChannel.StatusChangedMessage.TYPE,
        message -> message.invokeCallback(chatChannelSubscriber));
    addTrackedListener(
        IChatChannel.SlapMessage.TYPE, message -> message.invokeCallback(chatChannelSubscriber));
    addTrackedListener(
        IChatChannel.SpeakAddedMessage.TYPE,
        message -> message.invokeCallback(chatChannelSubscriber));
    addTrackedListener(
        IChatChannel.SpeakerRemovedMessage.TYPE,
        message -> message.invokeCallback(chatChannelSubscriber));
  }

  private <T extends WebSocketMessage> void addTrackedListener(
      final MessageType<T> messageType, final Consumer<T> listener) {
    clientNetworkBridge.addListener(messageType, listener);
    listenerRemovals.add(() -> clientNetworkBridge.removeListener(messageType, listener));
  }

  private IChatChannel chatChannelSubscriber(final ChatClient chatClient) {
    return new IChatChannel() {
      @Override
      public void chatOccurred(final String message) {
        chatClient.messageReceived(MessageContext.getSender().getPlayerName(), message);
      }

      @Override
      public void slapOccurred(final UserName slappedPlayer) {
        final UserName slapper = MessageContext.getSender().getPlayerName();
        if (slappedPlayer.equals(userName)) {
          chatClient.slappedBy(slapper);
        } else {
          chatClient.eventReceived(slappedPlayer + " was slapped by " + slapper);
        }
      }

      @Override
      public void speakerAdded(final ChatParticipant chatParticipant) {
        chatClient.participantAdded(chatParticipant);
      }

      @Override
      public void speakerRemoved(final UserName userName) {
        chatClient.participantRemoved(userName);
      }

      @Override
      public void ping() {}

      @Override
      public void statusChanged(final UserName userName, final String status) {
        chatClient.statusUpdated(userName, status);
      }
    };
  }

  @Override
  public Collection<ChatParticipant> connect() {
    final String chatChannelName = ChatController.getChatChannelName(chatName);
    final IChatController controller = messengers.getRemoteChatController(chatName);
    addTrackedListener(
        IChatController.SetChatStatusMessage.TYPE, message -> message.invokeCallback(controller));
    messengers.addChatChannelSubscriber(chatChannelSubscriber, chatChannelName);
    return controller.joinChat();
  }

  @Override
  public void disconnect() {
    if (messengers.isConnected()) {
      if (ClientSetting.useWebsocketNetwork.getValue().orElse(false)) {
        clientNetworkBridge.disconnect();
      } else {
        messengers.getRemoteChatController(chatName).leaveChat();
      }
    }
    messengers.unregisterChannelSubscriber(
        chatChannelSubscriber, new RemoteName(chatChannelName, IChatChannel.class));
    listenerRemovals.forEach(Runnable::run);
    listenerRemovals.clear();
  }

  @Override
  public void slap(final UserName userName) {
    if (ClientSetting.useWebsocketNetwork.getValue().orElse(false)) {
      clientNetworkBridge.sendMessage(new IChatChannel.SlapMessage(userName));
    } else {
      final IChatChannel remote =
          (IChatChannel)
              messengers.getChannelBroadcaster(new RemoteName(chatChannelName, IChatChannel.class));
      remote.slapOccurred(userName);
    }
  }

  @Override
  public void updateStatus(final String status) {
    if (ClientSetting.useWebsocketNetwork.getValue().orElse(false)) {
      clientNetworkBridge.sendMessage(new IChatController.SetChatStatusMessage(status));
    } else {
      final RemoteName chatControllerName = ChatController.getChatControllerRemoteName(chatName);
      final IChatController controller = (IChatController) messengers.getRemote(chatControllerName);
      AsyncRunner.runAsync(() -> controller.setStatus(status))
          .exceptionally(throwable -> log.warn("Error updating status", throwable));
    }
  }

  @Override
  public UserName getLocalUserName() {
    return messengers.getLocalNode().getPlayerName();
  }

  @Override
  public void sendMessage(final String message) {
    if (ClientSetting.useWebsocketNetwork.getValue().orElse(false)) {
      clientNetworkBridge.sendMessage(new IChatChannel.ChatMessage(message));
    } else {
      final IChatChannel remote =
          (IChatChannel)
              messengers.getChannelBroadcaster(new RemoteName(chatChannelName, IChatChannel.class));
      remote.chatOccurred(message);
    }
  }
}
