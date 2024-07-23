package games.strategy.engine.chat;

import com.google.common.base.Strings;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.message.MessageContext;
import games.strategy.engine.message.RemoteName;
import games.strategy.net.IConnectionChangeListener;
import games.strategy.net.INode;
import games.strategy.net.Messengers;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.triplea.domain.data.ChatParticipant;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.domain.data.UserName;

/** Default implementation of {@link IChatController}. */
@Slf4j
public class ChatController implements IChatController {
  private static final String CHAT_REMOTE = "_ChatRemote_";
  private static final String CHAT_CHANNEL = "_ChatControl_";
  private final Messengers messengers;

  private final String chatName;
  private final Map<INode, Tag> chatters = new HashMap<>();

  private final Predicate<INode> isModerator =
      node -> {
        if (chatters.isEmpty() && !GameRunner.headless()) {
          return true;
        } else if (GameRunner.headless() && chatters.size() == 1) {
          return true;
        } else {
          return false;
        }
      };

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

  public ChatController(final String name, final Messengers messengers) {
    chatName = name;
    this.messengers = messengers;
    chatChannel = getChatChannelName(name);
    messengers.registerRemote(this, getChatControllerRemoteName(name));
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
            getChatBroadcaster().ping();
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
      final IChatChannel chatter = getChatBroadcaster();
      for (final INode node : chatters.keySet()) {
        chatter.speakerRemoved(node.getPlayerName());
      }
      messengers.unregisterRemote(getChatControllerRemoteName(chatName));
    }
    messengers.removeConnectionChangeListener(connectionChangeListener);
  }

  private IChatChannel getChatBroadcaster() {
    return (IChatChannel)
        messengers.getChannelBroadcaster(new RemoteName(chatChannel, IChatChannel.class));
  }

  // a player has joined
  @Override
  public Collection<ChatParticipant> joinChat() {
    final INode node = MessageContext.getSender();
    log.info("Chatter:" + node + " is joining chat:" + chatName);
    final Tag tag = isModerator.test(node) ? Tag.MODERATOR : Tag.NONE;
    synchronized (mutex) {
      final PlayerChatId id = PlayerChatId.newId();
      chatterIds.put(node, id);
      chatters.put(node, tag);
      getChatBroadcaster()
          .speakerAdded(
              ChatParticipant.builder()
                  .userName(node.getPlayerName().getValue())
                  .playerChatId(id.getValue())
                  .isModerator(tag == Tag.MODERATOR)
                  .build());

      return chatters.entrySet().stream()
          .map(
              entry ->
                  ChatParticipant.builder()
                      .isModerator(entry.getValue() == Tag.MODERATOR)
                      .userName(entry.getKey().getPlayerName().getValue())
                      .playerChatId(chatterIds.get(entry.getKey()).getValue())
                      .status(chatterStatus.get(entry.getKey().getPlayerName()))
                      .build())
          .collect(Collectors.toSet());
    }
  }

  @Override
  public void setStatus(final String status) {
    final INode node = MessageContext.getSender();
    if (Strings.isNullOrEmpty(status)) {
      chatterStatus.remove(node.getPlayerName());
    } else {
      chatterStatus.put(node.getPlayerName(), status);
    }
    getChatBroadcaster().statusChanged(node.getPlayerName(), status);
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
    getChatBroadcaster().speakerRemoved(node.getPlayerName());
    log.info("Chatter:" + node + " has left chat:" + chatName);
  }
}
