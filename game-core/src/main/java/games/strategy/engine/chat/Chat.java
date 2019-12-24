package games.strategy.engine.chat;

import com.google.common.collect.EvictingQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import org.triplea.domain.data.PlayerName;
import org.triplea.http.client.lobby.chat.ChatParticipant;
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

  @Getter
  private final Collection<ChatMessage> chatHistory =
      Collections.synchronizedCollection(EvictingQueue.create(1000));

  private final ChatIgnoreList ignoreList = new ChatIgnoreList();
  @Getter private final PlayerName localPlayerName;
  private final Collection<Consumer<StatusUpdate>> statusUpdateListeners = new ArrayList<>();

  public Chat(final ChatTransmitter chatTransmitter) {
    this.localPlayerName = chatTransmitter.getLocalPlayerName();
    this.chatTransmitter = chatTransmitter;
    chatTransmitter.setChatClient(this);
    sentMessagesHistory = new SentMessagesHistory();
    chatters.addAll(chatTransmitter.connect());
    updateConnections();
  }

  private void updateConnections() {
    final List<ChatParticipant> playerNames =
        chatters.stream()
            .sorted(Comparator.comparing(c -> c.getPlayerName().getValue()))
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
        listener -> listener.playerJoined(chatParticipant.getPlayerName() + " has joined"));
  }

  @Override
  public void participantRemoved(final PlayerName playerName) {
    chatters.stream()
        .filter(chatter -> chatter.getPlayerName().equals(playerName))
        .findAny()
        .ifPresent(
            node -> {
              chatters.remove(node);
              updateConnections();
              chatMessageListeners.forEach(
                  listener -> listener.playerLeft(node.getPlayerName() + " has left"));
            });
  }

  @Override
  public void slappedBy(final PlayerName from) {
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
        .filter(chatter -> chatter.getPlayerName().equals(statusUpdate.getPlayerName()))
        .findAny()
        .ifPresent(
            node -> {
              chatters.stream()
                  .filter(chatter -> chatter.getPlayerName().equals(statusUpdate.getPlayerName()))
                  .findAny()
                  .ifPresent(chatter -> chatter.setStatus(statusUpdate.getStatus()));
              statusUpdateListeners.forEach(l -> l.accept(statusUpdate));
            });
  }

  void updateStatus(final String status) {
    chatTransmitter.updateStatus(status);
  }

  String getStatus(final PlayerName playerName) {
    return chatters.stream()
        .filter(chatter -> chatter.getPlayerName().equals(playerName))
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

  void sendSlap(final PlayerName playerName) {
    chatTransmitter.slap(playerName);
  }

  public void sendMessage(final String message) {
    chatTransmitter.sendMessage(message);
    sentMessagesHistory.append(message);
  }

  void setIgnored(final PlayerName playerName, final boolean isIgnored) {
    if (isIgnored) {
      ignoreList.add(playerName);
    } else {
      ignoreList.remove(playerName);
    }
  }

  boolean isIgnored(final PlayerName playerName) {
    return ignoreList.shouldIgnore(playerName);
  }

  Collection<PlayerName> getOnlinePlayers() {
    return chatters.stream().map(ChatParticipant::getPlayerName).collect(Collectors.toSet());
  }
}
