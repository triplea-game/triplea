package games.strategy.engine.chat;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import games.strategy.engine.chat.Chat.CHAT_SOUND_PROFILE;
import games.strategy.engine.message.IChannelMessenger;
import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.net.IMessenger;

/**
 * A Chat window.
 * Mutiple chat panels can be connected to the same Chat.
 * <p>
 * We can change the chat we are connected to using the setChat(...) method.
 */
public class ChatPanel extends JPanel implements IChatPanel {
  private static final long serialVersionUID = -6177517517279779486L;
  static int s_divider_size = 5;
  private ChatPlayerPanel chatPlayerPanel;
  private ChatMessagePanel chatMessagePanel;

  /** Creates a new instance of ChatFrame */
  public ChatPanel(final IMessenger messenger, final IChannelMessenger channelMessenger,
      final IRemoteMessenger remoteMessenger, final String chatName, final CHAT_SOUND_PROFILE chatSoundProfile) {
    init();
    final Chat chat = new Chat(messenger, chatName, channelMessenger, remoteMessenger, chatSoundProfile);
    setChat(chat);
  }

  public ChatPanel(final Chat chat) {
    init();
    setChat(chat);
  }

  private void init() {
    createComponents();
    layoutComponents();
    setSize(300, 200);
  }

  @Override
  public boolean isHeadless() {
    return false;
  }

  @Override
  public String getAllText() {
    return chatMessagePanel.getAllText();
  }

  @Override
  public void shutDown() {
    // get first, before below turns it null
    final Chat chat = getChat();
    chatMessagePanel.shutDown();
    chatPlayerPanel.shutDown();
    if (chat != null) {
      // now shut down
      chat.shutdown();
    }
  }

  @Override
  public void setChat(final Chat chat) {
    chatMessagePanel.setChat(chat);
    chatPlayerPanel.setChat(chat);
  }

  @Override
  public Chat getChat() {
    return chatMessagePanel.getChat();
  }

  private void layoutComponents() {
    final Container content = this;
    content.setLayout(new BorderLayout());
    final JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    split.setLeftComponent(chatMessagePanel);
    split.setRightComponent(chatPlayerPanel);
    split.setOneTouchExpandable(false);
    split.setDividerSize(s_divider_size);
    split.setResizeWeight(1);
    content.add(split, BorderLayout.CENTER);
  }

  private void createComponents() {
    chatPlayerPanel = new ChatPlayerPanel(null);
    chatMessagePanel = new ChatMessagePanel(null);
  }

  @Override
  public void setPlayerRenderer(final DefaultListCellRenderer renderer) {
    chatPlayerPanel.setPlayerRenderer(renderer);
    // gets remaining width from parent component, so setting
    // the width is not really necessary
    chatMessagePanel.setPreferredSize(new Dimension(30, chatMessagePanel.getPreferredSize().height));
  }

  @Override
  public void setShowChatTime(final boolean showTime) {
    chatMessagePanel.setShowTime(showTime);
  }

  public ChatPlayerPanel getChatPlayerPanel() {
    return chatPlayerPanel;
  }

  public ChatMessagePanel getChatMessagePanel() {
    return chatMessagePanel;
  }
}
