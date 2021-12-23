package games.strategy.engine.chat;

import games.strategy.engine.chat.ChatMessagePanel.ChatSoundProfile;
import games.strategy.net.Messengers;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import org.triplea.game.chat.ChatModel;
import org.triplea.java.Interruptibles;
import org.triplea.swing.SwingAction;

/**
 * A Chat window.
 *
 * <p>Multiple chat panels can be connected to the same Chat.
 *
 * <p>We can change the chat we are connected to using the setChat(...) method.
 */
public class ChatPanel extends JPanel implements ChatModel {
  private static final long serialVersionUID = -6177517517279779486L;
  private static final int DIVIDER_SIZE = 5;
  private final ChatPlayerPanel chatPlayerPanel;
  private final ChatMessagePanel chatMessagePanel;

  public ChatPanel(final Chat chat, final ChatSoundProfile chatSoundProfile) {
    setSize(300, 200);
    chatPlayerPanel = new ChatPlayerPanel(chat);
    chatMessagePanel = new ChatMessagePanel(chat, chatSoundProfile);
    setLayout(new BorderLayout());
    final JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    split.setLeftComponent(chatMessagePanel);
    split.setRightComponent(chatPlayerPanel);
    split.setOneTouchExpandable(false);
    split.setDividerSize(DIVIDER_SIZE);
    split.setResizeWeight(1);
    add(split, BorderLayout.CENTER);
  }

  /**
   * Creates a Chat object instance on the current thread based on the provided arguments and calls
   * the ChatPanel constructor with it on the EDT. This is to allow for easy off-EDT initialisation
   * of this ChatPanel. Note that if this method is being called on the EDT It will still work, but
   * the UI might freeze for a long time.
   */
  public static ChatPanel newChatPanel(
      final Messengers messengers, final String chatName, final ChatSoundProfile chatSoundProfile) {
    final Chat chat = new Chat(new MessengersChatTransmitter(chatName, messengers));
    return Interruptibles.awaitResult(
            () -> SwingAction.invokeAndWaitResult(() -> new ChatPanel(chat, chatSoundProfile)))
        .result
        .orElseThrow(() -> new IllegalStateException("Error during Chat Panel creation"));
  }

  public void deleteChat() {
    chatMessagePanel.setChat(null);
    chatPlayerPanel.setChat(null);
  }

  @Override
  public Chat getChat() {
    return chatMessagePanel.getChat();
  }

  public void setPlayerRenderer(final DefaultListCellRenderer renderer) {
    chatPlayerPanel.setPlayerRenderer(renderer);
    // gets remaining width from parent component, so setting the width is not really necessary
    chatMessagePanel.setPreferredSize(
        new Dimension(30, chatMessagePanel.getPreferredSize().height));
  }

  public ChatMessagePanel getChatMessagePanel() {
    return chatMessagePanel;
  }
}
