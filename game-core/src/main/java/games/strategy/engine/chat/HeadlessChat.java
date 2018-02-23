package games.strategy.engine.chat;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.swing.DefaultListCellRenderer;

import com.google.common.base.Ascii;

import games.strategy.engine.chat.Chat.ChatSoundProfile;
import games.strategy.engine.message.IChannelMessenger;
import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.net.IMessenger;
import games.strategy.net.INode;
import games.strategy.net.ServerMessenger;
import games.strategy.sound.ClipPlayer;
import games.strategy.sound.SoundPath;
import games.strategy.util.TimeManager;

/**
 * Headless version of ChatPanel.
 */
public class HeadlessChat implements IChatListener, IChatPanel {
  // roughly 1000 chat messages
  private static final int MAX_LENGTH = 1000 * 200;
  private Chat chat;
  private boolean showTime = true;
  private StringBuilder allText = new StringBuilder();
  private final ChatFloodControl floodControl = new ChatFloodControl();
  private final Set<String> hiddenPlayers = new HashSet<>();
  private final Set<INode> players = new HashSet<>();

  public HeadlessChat(final IMessenger messenger, final IChannelMessenger channelMessenger,
      final IRemoteMessenger remoteMessenger, final String chatName, final ChatSoundProfile chatSoundProfile) {
    final Chat chat = new Chat(messenger, chatName, channelMessenger, remoteMessenger, chatSoundProfile);
    setChat(chat);
  }

  @Override
  public boolean isHeadless() {
    return true;
  }

  @Override
  public String toString() {
    return allText.toString();
  }

  @Override
  public String getAllText() {
    return allText.toString();
  }

  @Override
  public Chat getChat() {
    return chat;
  }

  @Override
  public void setShowChatTime(final boolean showTime) {
    this.showTime = showTime;
  }

  @Override
  public void setPlayerRenderer(final DefaultListCellRenderer renderer) { // nothing
  }

  @Override
  public synchronized void updatePlayerList(final Collection<INode> players) {
    this.players.clear();
    for (final INode name : players) {
      if (!hiddenPlayers.contains(name.getName())) {
        this.players.add(name);
      }
    }
  }

  @Override
  public void shutDown() {
    if (chat != null) {
      chat.removeChatListener(this);
      chat.shutdown();
    }
    chat = null;
  }

  @Override
  public void setChat(final Chat chat) {
    if (this.chat != null) {
      this.chat.removeChatListener(this);
    }
    this.chat = chat;
    if (this.chat != null) {
      this.chat.addChatListener(this);
      synchronized (this.chat.getMutex()) {
        allText = new StringBuilder();
        for (final ChatMessage message : this.chat.getChatHistory()) {
          if (message.getFrom().equals(this.chat.getServerNode().getName())) {
            if (message.getMessage().equals(ServerMessenger.YOU_HAVE_BEEN_MUTED_LOBBY)) {
              addChatMessage("YOUR LOBBY CHATTING HAS BEEN TEMPORARILY 'MUTED' BY THE ADMINS, TRY AGAIN LATER",
                  "ADMIN_CHAT_CONTROL", false);
              continue;
            } else if (message.getMessage().equals(ServerMessenger.YOU_HAVE_BEEN_MUTED_GAME)) {
              addChatMessage("YOUR CHATTING IN THIS GAME HAS BEEN 'MUTED' BY THE HOST", "HOST_CHAT_CONTROL", false);
              continue;
            }
          }
          addChatMessage(message.getMessage(), message.getFrom(), message.isMyMessage());
        }
      }
    } else {
      updatePlayerList(Collections.emptyList());
    }
  }

  /** thread safe. */
  @Override
  public void addMessage(final String message, final String from, final boolean thirdperson) {
    addMessageWithSound(message, from, thirdperson, SoundPath.CLIP_CHAT_MESSAGE);
  }

  /** thread safe. */
  @Override
  public void addMessageWithSound(final String message, final String from, final boolean thirdperson,
      final String sound) {
    // TODO: I don't really think we need a new thread for this...
    new Thread(() -> {
      if (from.equals(chat.getServerNode().getName())) {
        if (message.equals(ServerMessenger.YOU_HAVE_BEEN_MUTED_LOBBY)) {
          addChatMessage("YOUR LOBBY CHATTING HAS BEEN TEMPORARILY 'MUTED' BY THE ADMINS, TRY AGAIN LATER",
              "ADMIN_CHAT_CONTROL", false);
          return;
        } else if (message.equals(ServerMessenger.YOU_HAVE_BEEN_MUTED_GAME)) {
          addChatMessage("YOUR CHATTING IN THIS GAME HAS BEEN 'MUTED' BY THE HOST", "HOST_CHAT_CONTROL", false);
          return;
        }
      }
      if (!floodControl.allow(from, System.currentTimeMillis())) {
        if (from.equals(chat.getLocalNode().getName())) {
          addChatMessage("MESSAGE LIMIT EXCEEDED, TRY AGAIN LATER", "ADMIN_FLOOD_CONTROL", false);
        }
        return;
      }
      addChatMessage(message, from, thirdperson);
      ClipPlayer.play(sound);
    }).start();
  }

  private void addChatMessage(final String originalMessage, final String from, final boolean thirdperson) {
    final String message = Ascii.truncate(originalMessage, 200, "...");
    final String time = "(" + TimeManager.getLocalizedTime() + ")";
    final String prefix = thirdperson ? (showTime ? ("* " + time + " " + from) : ("* " + from))
        : (showTime ? (time + " " + from + ": ") : (from + ": "));
    final String fullMessage = prefix + " " + message + "\n";
    final String currentAllText = allText.toString();
    if (currentAllText.length() > MAX_LENGTH) {
      allText = new StringBuilder(currentAllText.substring(MAX_LENGTH / 2, currentAllText.length()));
    }
    allText.append(fullMessage);
  }

  @Override
  public void addStatusMessage(final String message) {
    final String fullMessage = "--- " + message + " ---\n";
    final String currentAllText = allText.toString();
    if (currentAllText.length() > MAX_LENGTH) {
      allText = new StringBuilder(currentAllText.substring(MAX_LENGTH / 2, currentAllText.length()));
    }
    allText.append(fullMessage);
  }
}
