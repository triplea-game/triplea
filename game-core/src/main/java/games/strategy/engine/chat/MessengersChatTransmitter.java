package games.strategy.engine.chat;

import games.strategy.engine.message.MessageContext;
import games.strategy.engine.message.RemoteName;
import games.strategy.net.Messengers;
import java.util.Collection;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.lobby.chat.ChatParticipant;
import org.triplea.http.client.lobby.chat.messages.server.ChatMessage;
import org.triplea.http.client.lobby.chat.messages.server.StatusUpdate;

/** Chat transmitter that sends and receives messages over Java NIO sockets. */
public class MessengersChatTransmitter implements ChatTransmitter {
  private final UserName userName;
  private final Messengers messengers;

  private IChatChannel chatChannelSubscriber;

  private final String chatName;
  private final String chatChannelName;

  public MessengersChatTransmitter(final String chatName, final Messengers messengers) {
    this.userName = messengers.getLocalNode().getPlayerName();
    this.messengers = messengers;
    this.chatName = chatName;
    this.chatChannelName = ChatController.getChatChannelName(chatName);
  }

  @Override
  public void setChatClient(final ChatClient chatClient) {
    chatChannelSubscriber = chatChannelSubscriber(chatClient);
  }

  private IChatChannel chatChannelSubscriber(final ChatClient chatClient) {
    return new IChatChannel() {
      @Override
      public void chatOccurred(final String message) {
        chatClient.messageReceived(
            new ChatMessage(MessageContext.getSender().getPlayerName(), message));
      }

      @Override
      public void slapOccurred(final UserName slappedPlayer) {
        final UserName slapper = MessageContext.getSender().getPlayerName();
        if (slappedPlayer.equals(userName)) {
          chatClient.slappedBy(slapper);
        } else {
          chatClient.playerSlapped(slappedPlayer + " was slapped by " + slapper);
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
        chatClient.statusUpdated(new StatusUpdate(userName, status));
      }
    };
  }

  @Override
  public Collection<ChatParticipant> connect() {
    final String chatChannelName = ChatController.getChatChannelName(chatName);
    final IChatController controller = messengers.getRemoteChatController(chatName);
    messengers.addChatChannelSubscriber(chatChannelSubscriber, chatChannelName);
    return controller.joinChat();
  }

  @Override
  public void disconnect() {
    if (messengers.isConnected()) {
      messengers.getRemoteChatController(chatName).leaveChat();
    }
    messengers.unregisterChannelSubscriber(
        chatChannelSubscriber, new RemoteName(chatChannelName, IChatChannel.class));
  }

  @Override
  public void slap(final UserName userName) {
    final IChatChannel remote =
        (IChatChannel)
            messengers.getChannelBroadcaster(new RemoteName(chatChannelName, IChatChannel.class));
    remote.slapOccurred(userName);
  }

  @Override
  public void updateStatus(final String status) {
    final RemoteName chatControllerName = ChatController.getChatControllerRemoteName(chatName);
    final IChatController controller = (IChatController) messengers.getRemote(chatControllerName);
    controller.setStatus(status);
  }

  @Override
  public UserName getLocalUserName() {
    return messengers.getLocalNode().getPlayerName();
  }

  @Override
  public void sendMessage(final String message) {
    final IChatChannel remote =
        (IChatChannel)
            messengers.getChannelBroadcaster(new RemoteName(chatChannelName, IChatChannel.class));
    remote.chatOccurred(message);
  }
}
