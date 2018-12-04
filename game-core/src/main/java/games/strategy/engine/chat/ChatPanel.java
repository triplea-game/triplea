package games.strategy.engine.chat;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import games.strategy.engine.chat.Chat.ChatSoundProfile;
import games.strategy.engine.message.IChannelMessenger;
import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.net.IMessenger;
import games.strategy.ui.SwingAction;
import games.strategy.util.Interruptibles;

/**
 * A Chat window.
 *
 * <p>
 * Multiple chat panels can be connected to the same Chat.
 * </p>
 *
 * <p>
 * We can change the chat we are connected to using the setChat(...) method.
 * </p>
 */
public class ChatPanel extends JPanel implements IChatPanel {
  private static final long serialVersionUID = -6177517517279779486L;
  private static final int DIVIDER_SIZE = 5;
  private ChatPlayerPanel chatPlayerPanel;
  private ChatMessagePanel chatMessagePanel;

  /**
   * Creates a Chat object instance on the current thread based on the provided arguments
   * and calls the ChatPanel constructor with it on the EDT.
   * This is to allow for easy off-EDT initialisation of this ChatPanel.
   * Note that if this method is being called on the EDT It will still work,
   * but the UI might freeze for a long time.
   */
  public static ChatPanel createChatPanel(
      final IMessenger messenger,
      final IChannelMessenger channelMessenger,
      final IRemoteMessenger remoteMessenger,
      final String chatName,
      final ChatSoundProfile chatSoundProfile) {
    final Chat chat = new Chat(messenger, chatName, channelMessenger, remoteMessenger, chatSoundProfile);
    return Interruptibles.awaitResult(() -> SwingAction.invokeAndWaitResult(() -> new ChatPanel(chat))).result
        .orElseThrow(() -> new IllegalStateException("Error during Chat Panel creation"));
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
    split.setDividerSize(DIVIDER_SIZE);
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
    // gets remaining width from parent component, so setting the width is not really necessary
    chatMessagePanel.setPreferredSize(new Dimension(30, chatMessagePanel.getPreferredSize().height));
  }

  @Override
  public void setShowChatTime(final boolean showTime) {
    chatMessagePanel.setShowTime(showTime);
  }

  public ChatMessagePanel getChatMessagePanel() {
    return chatMessagePanel;
  }
}
