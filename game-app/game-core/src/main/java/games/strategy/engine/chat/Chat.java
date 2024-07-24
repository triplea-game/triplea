package games.strategy.engine.chat;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.Sets;
import games.strategy.engine.framework.startup.mc.messages.ModeratorMessage;
import games.strategy.net.IMessageListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.triplea.domain.data.ChatParticipant;
import org.triplea.domain.data.UserName;

/**
 * chat logic.
 *
 * <p>A chat can be bound to multiple chat panels.
 */
@Slf4j
@SuppressWarnings("UnstableApiUsage")
public class Chat implements ChatClient {

  private final ChatTransmitter chatTransmitter;

  private final List<ChatMessageListener> chatMessageListeners = new CopyOnWriteArrayList<>();
  private final List<ChatPlayerListener> chatPlayerListeners = new CopyOnWriteArrayList<>();

  @Getter(AccessLevel.PACKAGE)
  private final SentMessagesHistory sentMessagesHistory;

  private final Collection<ChatParticipant> chatters = Sets.newConcurrentHashSet();

  /**
   * ChatHistory is used to copy chat contents from a game staging screen to an actual game once it
   * has started. For examples, players will chat during the staging screen, click play, then
   * in-game there is a new chat and we'll copy the history messages to that new chat.
   */
  @Getter
  private final Collection<ChatMessage> chatHistory =
      Collections.synchronizedCollection(EvictingQueue.create(50));

  private final Collection<UserName> ignoreList = new HashSet<>();
  @Getter private final UserName localUserName;
  private final Collection<BiConsumer<UserName, String>> statusUpdateListeners = new ArrayList<>();

  public Chat(final ChatTransmitter chatTransmitter) {
    this.localUserName = chatTransmitter.getLocalUserName();
    this.chatTransmitter = chatTransmitter;
    chatTransmitter.setChatClient(this);
    sentMessagesHistory = new SentMessagesHistory();
    chatters.addAll(chatTransmitter.connect());
    updateConnections();
  }

  public void addMessengersListener(IMessageListener messageListener) {
    if (chatTransmitter instanceof MessengersChatTransmitter) {
      log.info("addming messengers listener");
      ((MessengersChatTransmitter) chatTransmitter)
          .getMessengers()
          .addMessageListener(messageListener);
    }
  }

  private void updateConnections() {
    final List<ChatParticipant> playerNames =
        chatters.stream()
            .sorted(Comparator.comparing(c -> c.getUserName().getValue()))
            .collect(Collectors.toList());

    chatPlayerListeners.forEach(listener -> listener.updatePlayerList(playerNames));
  }

  @Override
  public void connected(final List<ChatParticipant> chatters) {
    this.chatters.addAll(chatters);
    updateConnections();
  }

  @Override
  public void messageReceived(final UserName sender, final String message) {
    if (isIgnored(sender)) {
      return;
    }
    chatHistory.add(new ChatMessage(sender, message));
    chatMessageListeners.forEach(listener -> listener.messageReceived(sender, message));
  }

  @Override
  public void eventReceived(final String chatEvent) {
    chatMessageListeners.forEach(listener -> listener.eventReceived(chatEvent));
  }

  @Override
  public void participantAdded(final ChatParticipant chatParticipant) {
    chatters.add(chatParticipant);
    updateConnections();
    chatMessageListeners.forEach(
        listener -> listener.playerJoined(chatParticipant.getUserName() + " has joined"));
  }

  @Override
  public void participantRemoved(final UserName userName) {
    chatters.stream()
        .filter(chatter -> chatter.getUserName().equals(userName))
        .findAny()
        .ifPresent(
            node -> {
              chatters.remove(node);
              updateConnections();
              chatMessageListeners.forEach(
                  listener -> listener.playerLeft(node.getUserName() + " has left"));
            });
  }

  @Override
  public void slappedBy(final UserName from) {
    chatMessageListeners.forEach(listener -> listener.slapped(from));
  }

  @Override
  public void statusUpdated(final UserName userName, final String status) {
    chatters.stream()
        .filter(chatter -> chatter.getUserName().equals(userName))
        .findAny()
        .ifPresent(
            node -> {
              node.setStatus(status);
              statusUpdateListeners.forEach(l -> l.accept(userName, status));
            });
  }

  void updateStatus(final String status) {
    chatTransmitter.updateStatus(status);
  }

  String getStatus(final UserName userName) {
    return chatters.stream()
        .filter(chatter -> chatter.getUserName().equals(userName))
        .findAny()
        .map(ChatParticipant::getStatus)
        .orElse("");
  }

  void addChatListener(final ChatPlayerListener listener) {
    chatPlayerListeners.add(listener);
    updateConnections();
  }

  public void addChatListener(final ChatMessageListener listener) {
    chatMessageListeners.add(listener);
  }

  void addStatusUpdateListener(final BiConsumer<UserName, String> statusUpdateListener) {
    statusUpdateListeners.add(statusUpdateListener);
  }

  /** Stop receiving events from the messenger. */
  public void shutdown() {
    chatTransmitter.disconnect();
  }

  void sendSlap(final UserName userName) {
    chatTransmitter.slap(userName);
  }

  public void sendMessage(final String message) {
    chatTransmitter.sendMessage(message);
    sentMessagesHistory.append(message);
  }

  void setIgnored(final UserName userName, final boolean isIgnored) {
    if (isIgnored) {
      ignoreList.add(userName);
    } else {
      ignoreList.remove(userName);
    }
  }

  boolean isIgnored(final UserName userName) {
    return ignoreList.contains(userName);
  }

  Collection<UserName> getOnlinePlayers() {
    return chatters.stream().map(ChatParticipant::getUserName).collect(Collectors.toSet());
  }

  public void sendDisconnect(UserName userName) {
    if (chatTransmitter instanceof MessengersChatTransmitter) {
      var messengers = ((MessengersChatTransmitter) chatTransmitter).getMessengers();
      messengers.sendToServer(ModeratorMessage.newDisconnect(userName.toString()));
    } else {
      throw new UnsupportedOperationException(
          "sendDisconnect on Chat.java is to support legacy 'messengers' communication only");
    }
  }

  public void sendBan(UserName userName) {
    if (chatTransmitter instanceof MessengersChatTransmitter) {
      var messengers = ((MessengersChatTransmitter) chatTransmitter).getMessengers();
      messengers.sendToServer(ModeratorMessage.newBan(userName.toString()));
    } else {
      throw new UnsupportedOperationException(
          "sendDisconnect on Chat.java is to support legacy 'messengers' communication only");
    }
  }
}
