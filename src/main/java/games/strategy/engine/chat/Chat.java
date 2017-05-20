package games.strategy.engine.chat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import games.strategy.engine.chat.IChatController.Tag;
import games.strategy.engine.message.IChannelMessenger;
import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.engine.message.MessageContext;
import games.strategy.engine.message.RemoteName;
import games.strategy.net.IMessenger;
import games.strategy.net.INode;
import games.strategy.net.Messengers;
import games.strategy.sound.ClipPlayer;
import games.strategy.sound.SoundPath;
import games.strategy.util.Tuple;

/**
 * chat logic.
 *
 * <p>
 * A chat can be bound to multiple chat panels.
 * </p>
 */
public class Chat {
  private final List<IChatListener> listeners = new CopyOnWriteArrayList<>();
  private final Messengers messengers;
  private final String chatChannelName;
  private final String chatName;
  private final SentMessagesHistory sentMessages;
  private volatile long chatInitVersion = -1;
  // mutex used for access synchronization to nodes
  // TODO: check if this mutex is used for something else as well
  private final Object mutexNodes = new Object();
  private List<INode> nodes;
  // this queue is filled ONLY in init phase when chatInitVersion is default (-1) and nodes should not be changed
  // until end of
  // initialization
  // synchronizes access to queue
  private final Object mutexQueue = new Object();
  private List<Runnable> queuedInitMessages = new ArrayList<>();
  private final List<ChatMessage> chatHistory = new ArrayList<>();
  private final StatusManager statusManager;
  private final ChatIgnoreList ignoreList = new ChatIgnoreList();
  private final HashMap<INode, LinkedHashSet<String>> notesMap = new HashMap<>();
  private static final String TAG_MODERATOR = "[Mod]";
  private final CHAT_SOUND_PROFILE chatSoundProfile;

  public enum CHAT_SOUND_PROFILE {
    LOBBY_CHATROOM, GAME_CHATROOM, NO_SOUND
  }

  private void addToNotesMap(final INode node, final Tag tag) {
    if (tag == Tag.NONE) {
      return;
    }
    final LinkedHashSet<String> current = getTagText(tag);
    notesMap.put(node, current);
  }

  private static LinkedHashSet<String> getTagText(final Tag tag) {
    final LinkedHashSet<String> rVal = new LinkedHashSet<>();
    if (tag == Tag.NONE) {
      return null;
    }
    if (tag == Tag.MODERATOR) {
      rVal.add(TAG_MODERATOR);
    }
    // add more here....
    return rVal;
  }

  String getNotesForNode(final INode node) {
    final LinkedHashSet<String> notes = notesMap.get(node);
    if (notes == null) {
      return null;
    }
    final StringBuilder sb = new StringBuilder("");
    for (final String note : notes) {
      sb.append(" ");
      sb.append(note);
    }
    return sb.toString();
  }

  /** Creates a new instance of Chat. */
  public Chat(final String chatName, final Messengers messengers, final CHAT_SOUND_PROFILE chatSoundProfile) {
    this.chatSoundProfile = chatSoundProfile;
    this.messengers = messengers;
    statusManager = new StatusManager(messengers);
    chatChannelName = ChatController.getChatChannelName(chatName);
    this.chatName = chatName;
    sentMessages = new SentMessagesHistory();
    init();
  }

  public Chat(final IMessenger messenger, final String chatName, final IChannelMessenger channelMessenger,
      final IRemoteMessenger remoteMessenger, final CHAT_SOUND_PROFILE chatSoundProfile) {
    this(chatName, new Messengers(messenger, remoteMessenger, channelMessenger), chatSoundProfile);
  }

  public SentMessagesHistory getSentMessagesHistory() {
    return sentMessages;
  }

  public void addChatListener(final IChatListener listener) {
    listeners.add(listener);
    updateConnections();
  }

  public StatusManager getStatusManager() {
    return statusManager;
  }

  public void removeChatListener(final IChatListener listener) {
    listeners.remove(listener);
  }

  public Object getMutex() {
    return mutexNodes;
  }

  private void init() {
    // the order of events is significant.
    // 1 register our channel listener
    // once the channel is registered, we are guaranteed that
    // when we receive the response from our init(...) message, our channel
    // subscriber has been added, and will see any messages broadcasted by the server
    // 2 call the init message on the server remote. Any add or join messages sent from the server
    // will queue until we receive the init return value (they queue since they see the init version is -1)
    // 3 when we receive the init message response, acquire the lock, and initialize our state
    // and run any queued messages. Queued messages may be ignored if the
    // server version is incorrect.
    // this all seems a lot more involved than it needs to be.
    final IChatController controller = (IChatController) messengers.getRemoteMessenger()
        .getRemote(ChatController.getChatControlerRemoteName(chatName));
    messengers.getChannelMessenger().registerChannelSubscriber(m_chatChannelSubscribor,
        new RemoteName(chatChannelName, IChatChannel.class));
    final Tuple<Map<INode, Tag>, Long> init = controller.joinChat();
    final Map<INode, Tag> chatters = init.getFirst();
    synchronized (mutexNodes) {
      nodes = new ArrayList<>(chatters.keySet());
    }
    chatInitVersion = init.getSecond().longValue();
    synchronized (mutexQueue) {
      queuedInitMessages.add(0, () -> assignNodeTags(chatters));
      for (final Runnable job : queuedInitMessages) {
        job.run();
      }
      queuedInitMessages = null;
    }
    updateConnections();
  }

  /**
   * Call only when mutex for node is locked.
   *
   * @param chatters
   *        map from node to tag
   */
  private void assignNodeTags(final Map<INode, Tag> chatters) {
    for (final INode node : chatters.keySet()) {
      final Tag tag = chatters.get(node);
      addToNotesMap(node, tag);
    }
  }

  /**
   * Stop receiving events from the messenger.
   */
  public void shutdown() {
    messengers.getChannelMessenger().unregisterChannelSubscriber(m_chatChannelSubscribor,
        new RemoteName(chatChannelName, IChatChannel.class));
    if (messengers.getMessenger().isConnected()) {
      final RemoteName chatControllerName = ChatController.getChatControlerRemoteName(chatName);
      final IChatController controller =
          (IChatController) messengers.getRemoteMessenger().getRemote(chatControllerName);
      controller.leaveChat();
    }
  }

  void sendSlap(final String playerName) {
    final IChatChannel remote = (IChatChannel) messengers.getChannelMessenger()
        .getChannelBroadcastor(new RemoteName(chatChannelName, IChatChannel.class));
    remote.slapOccured(playerName);
  }

  public void sendMessage(final String message, final boolean meMessage) {
    final IChatChannel remote = (IChatChannel) messengers.getChannelMessenger()
        .getChannelBroadcastor(new RemoteName(chatChannelName, IChatChannel.class));
    if (meMessage) {
      remote.meMessageOccured(message);
    } else {
      remote.chatOccured(message);
    }
    sentMessages.append(message);
  }

  private void updateConnections() {
    synchronized (mutexNodes) {
      if (nodes == null) {
        return;
      }
      final List<INode> playerNames = new ArrayList<>(nodes);
      Collections.sort(playerNames);
      for (final IChatListener listener : listeners) {
        listener.updatePlayerList(playerNames);
      }
    }
  }

  void setIgnored(final INode node, final boolean isIgnored) {
    if (isIgnored) {
      ignoreList.add(node.getName());
    } else {
      ignoreList.remove(node.getName());
    }
  }

  public boolean isIgnored(final INode node) {
    return ignoreList.shouldIgnore(node.getName());
  }

  public INode getLocalNode() {
    return messengers.getMessenger().getLocalNode();
  }

  public INode getServerNode() {
    return messengers.getMessenger().getServerNode();
  }

  private final List<INode> m_playersThatLeft_Last10 = new ArrayList<>();

  public List<INode> getPlayersThatLeft_Last10() {
    return new ArrayList<>(m_playersThatLeft_Last10);
  }

  public List<INode> getOnlinePlayers() {
    return new ArrayList<>(nodes);
  }

  private final IChatChannel m_chatChannelSubscribor = new IChatChannel() {
    private void assertMessageFromServer() {
      final INode senderNode = MessageContext.getSender();
      final INode serverNode = messengers.getMessenger().getServerNode();
      // this will happen if the message is queued
      // but to queue a message, we must first test where it came from
      // so it is safe in this case to return ok
      if (senderNode == null) {
        return;
      }
      if (!senderNode.equals(serverNode)) {
        throw new IllegalStateException("The node:" + senderNode + " sent a message as the server!");
      }
    }

    @Override
    public void chatOccured(final String message) {
      final INode from = MessageContext.getSender();
      if (isIgnored(from)) {
        return;
      }
      synchronized (mutexNodes) {
        chatHistory.add(new ChatMessage(message, from.getName(), false));
        for (final IChatListener listener : listeners) {
          listener.addMessage(message, from.getName(), false);
        }
        // limit the number of messages in our history.
        while (chatHistory.size() > 1000) {
          chatHistory.remove(0);
        }
      }
    }

    @Override
    public void meMessageOccured(final String message) {
      final INode from = MessageContext.getSender();
      if (isIgnored(from)) {
        return;
      }
      synchronized (mutexNodes) {
        chatHistory.add(new ChatMessage(message, from.getName(), true));
        for (final IChatListener listener : listeners) {
          listener.addMessage(message, from.getName(), true);
        }
      }
    }

    @Override
    public void speakerAdded(final INode node, final Tag tag, final long version) {
      assertMessageFromServer();
      if (chatInitVersion == -1) {
        synchronized (mutexQueue) {
          if (queuedInitMessages == null) {
            speakerAdded(node, tag, version);
          } else {
            queuedInitMessages.add(() -> speakerAdded(node, tag, version));
          }
        }
        return;
      }
      if (version > chatInitVersion) {
        synchronized (mutexNodes) {
          nodes.add(node);
          addToNotesMap(node, tag);
          updateConnections();
        }
        for (final IChatListener listener : listeners) {
          listener.addStatusMessage(node.getName() + " has joined");
          if (chatSoundProfile == CHAT_SOUND_PROFILE.GAME_CHATROOM) {
            ClipPlayer.play(SoundPath.CLIP_CHAT_JOIN_GAME);
          }
        }
      }
    }

    @Override
    public void speakerRemoved(final INode node, final long version) {
      assertMessageFromServer();
      if (chatInitVersion == -1) {
        synchronized (mutexQueue) {
          if (queuedInitMessages == null) {
            speakerRemoved(node, version);
          } else {
            queuedInitMessages.add(() -> speakerRemoved(node, version));
          }
        }
        return;
      }
      if (version > chatInitVersion) {
        synchronized (mutexNodes) {
          nodes.remove(node);
          notesMap.remove(node);
          updateConnections();
        }
        for (final IChatListener listener : listeners) {
          listener.addStatusMessage(node.getName() + " has left");
        }
        m_playersThatLeft_Last10.add(node);
        if (m_playersThatLeft_Last10.size() > 10) {
          m_playersThatLeft_Last10.remove(0);
        }
      }
    }

    @Override
    public void speakerTagUpdated(final INode node, final Tag tag) {
      synchronized (mutexNodes) {
        notesMap.remove(node);
        addToNotesMap(node, tag);
        updateConnections();
      }
    }

    @Override
    public void slapOccured(final String to) {
      final INode from = MessageContext.getSender();
      if (isIgnored(from)) {
        return;
      }
      synchronized (mutexNodes) {
        if (to.equals(messengers.getChannelMessenger().getLocalNode().getName())) {
          handleSlap("You were slapped by " + from.getName(), from);
        } else if (from.equals(messengers.getChannelMessenger().getLocalNode())) {
          handleSlap("You just slapped " + to, from);
        }
      }
    }

    private void handleSlap(String message, INode from) {
      for (final IChatListener listener : listeners) {
        chatHistory.add(new ChatMessage(message, from.getName(), false));
        listener.addMessageWithSound(message, from.getName(), false, SoundPath.CLIP_CHAT_SLAP);
      }
    }

    @Override
    public void ping() {
      // System.out.println("Pinged");
    }
  };

  /**
   * While using this, you should synchronize on getMutex().
   *
   * @return the messages that have occured so far.
   */
  public List<ChatMessage> getChatHistory() {
    return chatHistory;
  }
}


