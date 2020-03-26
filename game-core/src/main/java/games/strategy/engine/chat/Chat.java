package games.strategy.engine.chat;

import com.google.common.collect.EvictingQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import org.triplea.domain.data.ChatParticipant;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.lobby.chat.messages.server.ChatMessage;
import org.triplea.http.client.lobby.chat.messages.server.ChatterList;
import org.triplea.http.client.lobby.chat.messages.server.StatusUpdate;

/**
 * chat logic.
 *
 * <p>A chat can be bound to multiple chat panels.
 */
public class Chat implements ChatClient {

  private final ChatTransmitter chatTransmitter;

  private final List<ChatMessageListener> chatMessageListeners = new CopyOnWriteArrayList<>();
  private final List<ChatPlayerListener> chatPlayerListeners = new CopyOnWriteArrayList<>();

  @Getter(AccessLevel.PACKAGE)
  private final SentMessagesHistory sentMessagesHistory;

  private final Collection<ChatParticipant> chatters = new HashSet<>();

  /**
   * ChatHistory is used to copy chat contents from a game staging screen to an actual game once it
   * has started. For examples, players will chat during the staging screen, click play, then
   * in-game there is a new chat and we'll copy the history messages to that new chat.
   */
  @Getter
  private final Collection<ChatMessage> chatHistory =
      Collections.synchronizedCollection(EvictingQueue.create(50));

  private final ChatIgnoreList ignoreList = new ChatIgnoreList();
  @Getter private final UserName localUserName;
  private final Collection<Consumer<StatusUpdate>> statusUpdateListeners = new ArrayList<>();

  public Chat(final ChatTransmitter chatTransmitter) {
    this.localUserName = chatTransmitter.getLocalUserName();
    this.chatTransmitter = chatTransmitter;
    chatTransmitter.setChatClient(this);
    sentMessagesHistory = new SentMessagesHistory();
    chatters.addAll(chatTransmitter.connect());
    updateConnections();
  }

  private void updateConnections() {
    final List<ChatParticipant> playerNames =
        chatters.stream()
            .sorted(Comparator.comparing(c -> c.getUserName().getValue()))
            .collect(Collectors.toList());

    chatPlayerListeners.forEach(listener -> listener.updatePlayerList(playerNames));
  }

  @Override
  public void connected(final ChatterList chatters) {
    this.chatters.addAll(chatters.getChatters());
    updateConnections();
  }

  @Override
  public void messageReceived(final ChatMessage chatMessage) {
    if (isIgnored(chatMessage.getFrom())) {
      return;
    }
    chatHistory.add(chatMessage);
    chatMessageListeners.forEach(listener -> listener.messageReceived(chatMessage));
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
    final String message = "You were slapped by " + from;
    chatHistory.add(new ChatMessage(from, message));
    chatMessageListeners.forEach(listener -> listener.slapped(message, from));
  }

  @Override
  public void playerSlapped(final String eventMessage) {
    chatMessageListeners.forEach(listener -> listener.slap(eventMessage));
  }

  @Override
  public void statusUpdated(final StatusUpdate statusUpdate) {
    chatters.stream()
        .filter(chatter -> chatter.getUserName().equals(statusUpdate.getUserName()))
        .findAny()
        .ifPresent(
            node -> {
              chatters.stream()
                  .filter(chatter -> chatter.getUserName().equals(statusUpdate.getUserName()))
                  .findAny()
                  .ifPresent(chatter -> chatter.setStatus(statusUpdate.getStatus()));
              statusUpdateListeners.forEach(l -> l.accept(statusUpdate));
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

  void addChatListener(final ChatMessageListener listener) {
    chatMessageListeners.add(listener);
  }

  void addStatusUpdateListener(final Consumer<StatusUpdate> statusUpdateListener) {
    statusUpdateListeners.add(statusUpdateListener);
  }

  void removeChatListener(final ChatMessageListener listener) {
    chatMessageListeners.remove(listener);
  }

  void removeChatListener(final ChatPlayerListener listener) {
    chatPlayerListeners.remove(listener);
  }

  void removeStatusUpdateListener(final Consumer<StatusUpdate> statusUpdateListener) {
    statusUpdateListeners.remove(statusUpdateListener);
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
    return ignoreList.shouldIgnore(userName);
  }

  Collection<UserName> getOnlinePlayers() {
    return chatters.stream().map(ChatParticipant::getUserName).collect(Collectors.toSet());
  }
}
