package games.strategy.engine.chat;

import com.google.common.base.Strings;
import games.strategy.engine.message.MessageContext;
import games.strategy.engine.message.RemoteName;
import games.strategy.net.IConnectionChangeListener;
import games.strategy.net.INode;
import games.strategy.net.Messengers;
import games.strategy.net.ServerMessenger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.lobby.web.socket.messages.envelopes.chat.ChatParticipant;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

/** Default implementation of {@link IChatController}. */
@Slf4j
public class ChatController implements IChatController {
  @NonNls private static final String CHAT_REMOTE = "_ChatRemote_";
  @NonNls private static final String CHAT_CHANNEL = "_ChatControl_";
  private final Messengers messengers;
  private final ServerMessenger serverMessenger;

  private final String chatName;
  private final Map<INode, Tag> chatters = new HashMap<>();

  private final Map<INode, PlayerChatId> chatterIds = new HashMap<>();
  private final Map<UserName, String> chatterStatus = new HashMap<>();

  private final Object mutex = new Object();
  private final String chatChannel;
  private final ScheduledExecutorService pingThread = Executors.newScheduledThreadPool(1);
  private final IConnectionChangeListener connectionChangeListener =
      new IConnectionChangeListener() {
        @Override
        public void connectionAdded(final INode to) {}

        @Override
        public void connectionRemoved(final INode to) {
          synchronized (mutex) {
            if (chatters.containsKey(to)) {
              leaveChatInternal(to);
            }
          }
        }
      };

  public ChatController(
      final String name, final Messengers messengers, ServerMessenger serverMessenger) {
    chatName = name;
    this.messengers = messengers;
    this.serverMessenger = serverMessenger;
    chatChannel = getChatChannelName(name);
    messengers.registerRemote(this, getChatControllerRemoteName(name));
    messengers.registerMessageHandler(
        IChatController.JoinChatRequest.TYPE,
        (request, implementor) ->
            new IChatController.JoinChatResponse(
                new ArrayList<>(((IChatController) implementor).joinChat())));
    messengers.registerMessageHandler(
        IChatController.LeaveChatRequest.TYPE,
        (request, implementor) -> {
          ((IChatController) implementor).leaveChat();
          return new IChatController.LeaveChatResponse();
        });
    messengers.registerMessageHandler(
        IChatController.SetChatStatusMessage.TYPE,
        (request, implementor) -> {
          request.invokeCallback((IChatController) implementor);
          return new IChatController.SetStatusResponse();
        });
    messengers.addConnectionChangeListener(connectionChangeListener);
    startPinger();
  }

  public static RemoteName getChatControllerRemoteName(final String chatName) {
    return new RemoteName(CHAT_REMOTE + chatName, IChatController.class);
  }

  public static String getChatChannelName(final String chatName) {
    return CHAT_CHANNEL + chatName;
  }

  private void startPinger() {
    pingThread.scheduleAtFixedRate(
        () -> {
          try {
            broadcastToChatChannel(new IChatChannel.PingMessage());
          } catch (final Exception e) {
            log.error("Error pinging", e);
          }
        },
        180,
        60,
        TimeUnit.SECONDS);
  }

  // clean up
  public void deactivate() {
    pingThread.shutdown();
    synchronized (mutex) {
      for (final INode node : chatters.keySet()) {
        broadcastToChatChannel(
            new IChatChannel.SpeakerRemovedMessage(node.getPlayerUserName().getValue()));
      }
      messengers.unregisterRemote(getChatControllerRemoteName(chatName));
    }
    messengers.removeConnectionChangeListener(connectionChangeListener);
  }

  private void broadcastToChatChannel(final WebSocketMessage message) {
    messengers.sendChannelMessage(new RemoteName(chatChannel, IChatChannel.class), message);
  }

  // a player has joined
  @Override
  public Collection<ChatParticipant> joinChat() {
    final INode node = MessageContext.getSender();
    log.info("Chatter: " + node + " is joining chat: " + chatName);
    final Tag tag = Tag.NONE;
    synchronized (mutex) {
      final PlayerChatId id = PlayerChatId.newId();
      chatterIds.put(node, id);
      chatters.put(node, tag);
      broadcastToChatChannel(
          new IChatChannel.SpeakAddedMessage(
              ChatParticipant.builder()
                  .userName(node.getPlayerUserName().getValue())
                  .playerChatId(id.getValue())
                  .isModerator(serverMessenger.isModerator(node))
                  .build()));

      return chatters.entrySet().stream()
          .map(
              entry ->
                  ChatParticipant.builder()
                      .isModerator(serverMessenger.isModerator(entry.getKey()))
                      .userName(entry.getKey().getPlayerUserName().getValue())
                      .playerChatId(chatterIds.get(entry.getKey()).getValue())
                      .status(chatterStatus.get(entry.getKey().getPlayerUserName()))
                      .build())
          .collect(Collectors.toSet());
    }
  }

  @Override
  public void setStatus(final String status) {
    final INode node = MessageContext.getSender();
    if (Strings.isNullOrEmpty(status)) {
      chatterStatus.remove(node.getPlayerUserName());
    } else {
      chatterStatus.put(node.getPlayerUserName(), status);
    }
    broadcastToChatChannel(new IChatChannel.StatusChangedMessage(node.getPlayerUserName(), status));
  }

  // a player has left
  @Override
  public void leaveChat() {
    leaveChatInternal(MessageContext.getSender());
  }

  private void leaveChatInternal(final INode node) {
    synchronized (mutex) {
      chatters.remove(node);
    }
    broadcastToChatChannel(
        new IChatChannel.SpeakerRemovedMessage(node.getPlayerUserName().getValue()));
    log.info("Chatter: " + node + " has left chat: " + chatName);
  }
}
