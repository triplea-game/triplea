package games.strategy.engine.chat;

import com.google.common.collect.EvictingQueue;
import games.strategy.engine.lobby.PlayerName;
import games.strategy.engine.message.MessageContext;
import games.strategy.net.Messengers;
import games.strategy.sound.ClipPlayer;
import games.strategy.sound.SoundPath;
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

  private final List<IChatListener> listeners = new CopyOnWriteArrayList<>();

  @Getter(AccessLevel.PACKAGE)
  private final SentMessagesHistory sentMessagesHistory;

  private final Map<ChatParticipant, String> chatters;

  @Getter
  private final Collection<ChatMessage> chatHistory =
      Collections.synchronizedCollection(EvictingQueue.create(1000));

  private final ChatIgnoreList ignoreList = new ChatIgnoreList();
  private final ChatSoundProfile chatSoundProfile;
  @Getter private final PlayerName localPlayerName;
  private final Collection<BiConsumer<PlayerName, String>> statusUpdateListeners =
      new ArrayList<>();

  /** A profile defines the sounds to use for various chat events. */
  public enum ChatSoundProfile {
    LOBBY_CHATROOM,
    GAME_CHATROOM,
    NO_SOUND
  }

  public Chat(
      final Messengers messengers, final String chatName, final ChatSoundProfile chatSoundProfile) {
    this.localPlayerName = messengers.getLocalNode().getPlayerName();
    chatTransmitter = new JavaSocketChatTransmitter(this, chatName, messengers);
    this.chatSoundProfile = chatSoundProfile;
    sentMessagesHistory = new SentMessagesHistory();
    chatters = Optional.ofNullable(chatTransmitter.connect()).orElseGet(HashMap::new);
    updateConnections();
  }

  private void updateConnections() {
    final List<ChatParticipant> playerNames =
        chatters.keySet().stream()
            .sorted(Comparator.comparing(c -> c.getPlayerName().getValue()))
            .collect(Collectors.toList());

    for (final IChatListener listener : listeners) {
      listener.updatePlayerList(playerNames);
    }
  }

  @Override
  public void messageReceived(final String message) {
    final PlayerName from = MessageContext.getSender().getPlayerName();
    if (isIgnored(from)) {
      return;
    }
    chatHistory.add(new ChatMessage(message, from));
    for (final IChatListener listener : listeners) {
      listener.addMessage(message, from);
    }
  }

  @Override
  public void participantAdded(final ChatParticipant chatParticipant) {
    if (chatters == null) {
      return;
    }
    chatters.put(chatParticipant, "");
    updateConnections();
    for (final IChatListener listener : listeners) {
      listener.addStatusMessage(chatParticipant.getPlayerName() + " has joined");
      if (chatSoundProfile == ChatSoundProfile.GAME_CHATROOM) {
        ClipPlayer.play(SoundPath.CLIP_CHAT_JOIN_GAME);
      }
    }
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
              for (final IChatListener listener : listeners) {
                listener.addStatusMessage(node.getPlayerName() + " has left");
              }
            });
  }

  @Override
  public void slappedBy(final PlayerName from) {
    final String message = "You were slapped by " + from;
    for (final IChatListener listener : listeners) {
      chatHistory.add(new ChatMessage(message, from));
      listener.addMessageWithSound(message, from, SoundPath.CLIP_CHAT_SLAP);
    }
  }

  @Override
  public void eventMessage(final String eventMessage) {
    for (final IChatListener listener : listeners) {
      listener.addStatusMessage(eventMessage);
    }
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

  void addChatListener(final IChatListener listener) {
    listeners.add(listener);
    updateConnections();
  }

  void addStatusUpdateListener(final BiConsumer<PlayerName, String> statusUpdateListener) {
    statusUpdateListeners.add(statusUpdateListener);
  }

  void removeChatListener(final IChatListener listener) {
    listeners.remove(listener);
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
