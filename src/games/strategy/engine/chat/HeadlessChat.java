package games.strategy.engine.chat;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.swing.DefaultListCellRenderer;

import games.strategy.engine.chat.Chat.CHAT_SOUND_PROFILE;
import games.strategy.engine.message.IChannelMessenger;
import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.net.IMessenger;
import games.strategy.net.INode;
import games.strategy.net.ServerMessenger;
import games.strategy.sound.ClipPlayer;
import games.strategy.sound.SoundPath;

/**
 * Headless version of ChatPanel.
 */
public class HeadlessChat implements IChatListener, IChatPanel {
  // roughly 1000 chat messages
  private static final int MAX_LENGTH = 1000 * 200;
  private Chat m_chat;
  private boolean m_showTime = true;
  private StringBuffer m_allText = new StringBuffer();
  private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("'('HH:mm:ss')'");
  private final ChatFloodControl floodControl = new ChatFloodControl();
  private Set<String> m_hiddenPlayers = new HashSet<>();
  private final Set<INode> m_players = new HashSet<>();
  private PrintStream m_out = null;

  public HeadlessChat(final IMessenger messenger, final IChannelMessenger channelMessenger,
      final IRemoteMessenger remoteMessenger, final String chatName, final CHAT_SOUND_PROFILE chatSoundProfile) {
    final Chat chat = new Chat(messenger, chatName, channelMessenger, remoteMessenger, chatSoundProfile);
    setChat(chat);
  }

  @Override
  public boolean isHeadless() {
    return true;
  }

  public void setPrintStream(final PrintStream out) {
    m_out = out;
  }

  @Override
  public String toString() {
    return m_allText.toString();
  }

  @Override
  public String getAllText() {
    return m_allText.toString();
  }

  @Override
  public Chat getChat() {
    return m_chat;
  }

  @Override
  public void setShowChatTime(final boolean showTime) {
    m_showTime = showTime;
  }

  @Override
  public void setPlayerRenderer(final DefaultListCellRenderer renderer) { // nothing
  }

  @Override
  public synchronized void updatePlayerList(final Collection<INode> players) {
    m_players.clear();
    for (final INode name : players) {
      if (!m_hiddenPlayers.contains(name.getName())) {
        m_players.add(name);
      }
    }
  }

  @Override
  public void shutDown() {
    if (m_chat != null) {
      m_chat.removeChatListener(this);
      m_chat.shutdown();
    }
    m_chat = null;
  }

  @Override
  public void setChat(final Chat chat) {
    if (m_chat != null) {
      m_chat.removeChatListener(this);
    }
    m_chat = chat;
    if (m_chat != null) {
      m_chat.addChatListener(this);
      synchronized (m_chat.getMutex()) {
        m_allText = new StringBuffer();
        try {
          if (m_out != null) {
            m_out.println();
          }
        } catch (final Exception e) {
        }
        for (final ChatMessage message : m_chat.getChatHistory()) {
          if (message.getFrom().equals(m_chat.getServerNode().getName())) {
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

  /** thread safe */
  @Override
  public void addMessage(final String message, final String from, final boolean thirdperson) {
    addMessageWithSound(message, from, thirdperson, SoundPath.CLIP_CHAT_MESSAGE);
  }

  /** thread safe */
  @Override
  public void addMessageWithSound(final String message, final String from, final boolean thirdperson,
      final String sound) {
    // TODO: I don't really think we need a new thread for this...
    final Thread t = new Thread(new Runnable() {
      @Override
      public void run() {
        if (from.equals(m_chat.getServerNode().getName())) {
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
          if (from.equals(m_chat.getLocalNode().getName())) {
            addChatMessage("MESSAGE LIMIT EXCEEDED, TRY AGAIN LATER", "ADMIN_FLOOD_CONTROL", false);
          }
          return;
        }
        addChatMessage(message, from, thirdperson);
        ClipPlayer.play(sound);
      }
    });
    t.start();
  }

  private void addChatMessage(final String originalMessage, final String from, final boolean thirdperson) {
    final String message = trimMessage(originalMessage);
    final String time = simpleDateFormat.format(new Date());
    final String prefix = thirdperson ? (m_showTime ? "* " + time + " " + from : "* " + from)
        : (m_showTime ? time + " " + from + ": " : from + ": ");
    final String fullMessage = prefix + " " + message + "\n";
    final String currentAllText = m_allText.toString();
    if (currentAllText.length() > MAX_LENGTH) {
      m_allText = new StringBuffer(currentAllText.substring(MAX_LENGTH / 2, currentAllText.length()));
    }
    m_allText.append(fullMessage);
    try {
      if (m_out != null) {
        m_out.print("CHAT: " + fullMessage);
      }
    } catch (final Exception e) {
    }
  }

  @Override
  public void addStatusMessage(final String message) {
    final String fullMessage = "--- " + message + " ---\n";
    final String currentAllText = m_allText.toString();
    if (currentAllText.length() > MAX_LENGTH) {
      m_allText = new StringBuffer(currentAllText.substring(MAX_LENGTH / 2, currentAllText.length()));
    }
    m_allText.append(fullMessage);
    try {
      if (m_out != null) {
        m_out.print("CHAT: " + fullMessage);
      }
    } catch (final Exception e) {
    }
  }

  private static String trimMessage(final String originalMessage) {
    // dont allow messages that are too long
    if (originalMessage.length() > 200) {
      return originalMessage.substring(0, 199) + "...";
    } else {
      return originalMessage;
    }
  }
}
