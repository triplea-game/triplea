package games.strategy.engine.chat;

import com.google.common.collect.EvictingQueue;
import games.strategy.engine.lobby.PlayerName;
import games.strategy.engine.message.MessageContext;
import games.strategy.net.Messengers;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;

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

  private final Map<ChatParticipant, String> chatters;

  @Getter
  private final Collection<ChatMessage> chatHistory =
      Collections.synchronizedCollection(EvictingQueue.create(1000));

  private final ChatIgnoreList ignoreList = new ChatIgnoreList();
  @Getter private final PlayerName localPlayerName;
  private final Collection<BiConsumer<PlayerName, String>> statusUpdateListeners =
      new ArrayList<>();

  public Chat(final Messengers messengers, final String chatName) {
    this.localPlayerName = messengers.getLocalNode().getPlayerName();
    chatTransmitter = new JavaSocketChatTransmitter(this, chatName, messengers);
    sentMessagesHistory = new SentMessagesHistory();
    chatters = Optional.ofNullable(chatTransmitter.connect()).orElseGet(HashMap::new);
    updateConnections();
  }

  private void updateConnections() {
    final List<ChatParticipant> playerNames =
        chatters.keySet().stream()
            .sorted(Comparator.comparing(c -> c.getPlayerName().getValue()))
            .collect(Collectors.toList());

    chatPlayerListeners.forEach(listener -> listener.updatePlayerList(playerNames));
  }

  @Override
  public void messageReceived(final String message) {
    final PlayerName from = MessageContext.getSender().getPlayerName();
    if (isIgnored(from)) {
      return;
    }
    chatHistory.add(new ChatMessage(message, from));
    chatMessageListeners.forEach(listener -> listener.addMessage(message, from));
  }

  @Override
  public void participantAdded(final ChatParticipant chatParticipant) {
    if (chatters == null) {
      return;
    }
    chatters.put(chatParticipant, "");
    updateConnections();
    chatMessageListeners.forEach(
        listener ->
            listener.addStatusMessage(chatParticipant.getPlayerName() + " has joined", true));
  }

  @Override
  public void participantRemoved(final PlayerName playerName) {
    chatters.keySet().stream()
        .filter(n -> n.getPlayerName().equals(playerName))
        .findAny()
        .ifPresent(
            node -> {
              chatters.remove(node);
              updateConnections();
              chatMessageListeners.forEach(
                  listener -> listener.addStatusMessage(node.getPlayerName() + " has left", false));
            });
  }

  @Override
  public void slappedBy(final PlayerName from) {
    final String message = "You were slapped by " + from;
    chatHistory.add(new ChatMessage(message, from));
    chatMessageListeners.forEach(listener -> listener.addSlapMessage(message, from));
  }

  @Override
  public void eventMessage(final String eventMessage) {
    chatMessageListeners.forEach(listener -> listener.addStatusMessage(eventMessage, false));
  }

  @Override
  public void statusUpdated(final PlayerName playerName, final String status) {
    chatters.keySet().stream()
        .filter(n -> n.getPlayerName().equals(playerName))
        .findAny()
        .ifPresent(
            node -> {
              chatters.put(node, status);
              statusUpdateListeners.forEach(l -> l.accept(playerName, status));
            });
  }

  void updateStatus(final String status) {
    chatTransmitter.updateStatus(status);
  }

  String getStatus(final PlayerName playerName) {
    return chatters.entrySet().stream()
        .filter(n -> n.getKey().getPlayerName().equals(playerName))
        .findAny()
        .map(Map.Entry::getValue)
        .orElse("");
  }

  void addChatListener(final ChatPlayerListener listener) {
    chatPlayerListeners.add(listener);
    updateConnections();
  }

  void addChatListener(final ChatMessageListener listener) {
    chatMessageListeners.add(listener);
  }

  void addStatusUpdateListener(final BiConsumer<PlayerName, String> statusUpdateListener) {
    statusUpdateListeners.add(statusUpdateListener);
  }

  void removeChatListener(final ChatMessageListener listener) {
    chatMessageListeners.remove(listener);
  }

  void removeChatListener(final ChatPlayerListener listener) {
    chatPlayerListeners.remove(listener);
  }

  void removeStatusUpdateListener(final BiConsumer<PlayerName, String> statusUpdateListener) {
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
    return chatters.keySet().stream()
        .map(ChatParticipant::getPlayerName)
        .collect(Collectors.toSet());
  }
}
