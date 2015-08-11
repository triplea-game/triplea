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
 *
 * Mutiple chat panels can be connected to the same Chat.
 * <p>
 *
 * We can change the chat we are connected to using the setChat(...) method.
 *
 */
public class ChatPanel extends JPanel implements IChatPanel {
  private static final long serialVersionUID = -6177517517279779486L;

  static int s_divider_size = 5;

  private ChatPlayerPanel m_chatPlayerPanel;
  private ChatMessagePanel m_chatMessagePanel;

  /** Creates a new instance of ChatFrame */
  public ChatPanel(final IMessenger messenger, final IChannelMessenger channelMessenger, final IRemoteMessenger remoteMessenger,
      final String chatName, final CHAT_SOUND_PROFILE chatSoundProfile) {
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
  public String getAllText() {
    return m_chatMessagePanel.getAllText();
  }

  @Override
  public void shutDown() {
    final Chat chat = getChat(); // get first, before below turns it null
    m_chatMessagePanel.shutDown();
    m_chatPlayerPanel.shutDown();
    if (chat != null) {
      chat.shutdown(); // now shut down
    }
  }

  @Override
  public void setChat(final Chat chat) {
    m_chatMessagePanel.setChat(chat);
    m_chatPlayerPanel.setChat(chat);
  }

  @Override
  public Chat getChat() {
    return m_chatMessagePanel.getChat();
  }

  private void layoutComponents() {
    final Container content = this;
    content.setLayout(new BorderLayout());
    final JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    split.setLeftComponent(m_chatMessagePanel);
    split.setRightComponent(m_chatPlayerPanel);
    split.setOneTouchExpandable(false);
    split.setDividerSize(s_divider_size);
    split.setResizeWeight(1);
    content.add(split, BorderLayout.CENTER);
  }

  private void createComponents() {
    m_chatPlayerPanel = new ChatPlayerPanel(null);
    m_chatMessagePanel = new ChatMessagePanel(null);
  }

  @Override
  public void setPlayerRenderer(final DefaultListCellRenderer renderer) {
    m_chatPlayerPanel.setPlayerRenderer(renderer);
    m_chatMessagePanel.setPreferredSize(new Dimension(30, m_chatMessagePanel.getPreferredSize().height)); // gets remaining width from
                                                                                                          // parent component, so setting
                                                                                                          // the width is not really
                                                                                                          // necessary
  }

  @Override
  public void setShowChatTime(final boolean showTime) {
    m_chatMessagePanel.setShowTime(showTime);
  }


  public ChatPlayerPanel getChatPlayerPanel() {
    return m_chatPlayerPanel;
  }

  public ChatMessagePanel getChatMessagePanel() {
    return m_chatMessagePanel;
  }
}
