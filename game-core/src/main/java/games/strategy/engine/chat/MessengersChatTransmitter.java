package games.strategy.engine.chat;

import games.strategy.engine.message.MessageContext;
import games.strategy.engine.message.RemoteName;
import games.strategy.net.Messengers;
import java.util.Collection;
import org.triplea.domain.data.PlayerName;
import org.triplea.http.client.lobby.chat.ChatParticipant;
import org.triplea.http.client.lobby.chat.messages.server.ChatMessage;
import org.triplea.http.client.lobby.chat.messages.server.StatusUpdate;

/** Chat transmitter that sends and receives messages over Java NIO sockets. */
public class MessengersChatTransmitter implements ChatTransmitter {
  private final PlayerName playerName;
  private final Messengers messengers;

  private IChatChannel chatChannelSubscriber;

  private final String chatName;
  private final String chatChannelName;

  public MessengersChatTransmitter(final String chatName, final Messengers messengers) {
    this.playerName = messengers.getLocalNode().getPlayerName();
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
      public void slapOccurred(final PlayerName slappedPlayer) {
        final PlayerName slapper = MessageContext.getSender().getPlayerName();
        if (slappedPlayer.equals(playerName)) {
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
      public void speakerRemoved(final PlayerName playerName) {
        chatClient.participantRemoved(playerName);
      }

      @Override
      public void ping() {}

      @Override
      public void statusChanged(final PlayerName playerName, final String status) {
        chatClient.statusUpdated(new StatusUpdate(playerName, status));
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
  public void slap(final PlayerName playerName) {
    final IChatChannel remote =
        (IChatChannel)
            messengers.getChannelBroadcaster(new RemoteName(chatChannelName, IChatChannel.class));
    remote.slapOccurred(playerName);
  }

  @Override
  public void updateStatus(final String status) {
    final RemoteName chatControllerName = ChatController.getChatControllerRemoteName(chatName);
    final IChatController controller = (IChatController) messengers.getRemote(chatControllerName);
    controller.setStatus(status);
  }

  @Override
  public PlayerName getLocalPlayerName() {
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
