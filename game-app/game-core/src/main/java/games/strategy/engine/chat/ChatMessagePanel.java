package games.strategy.engine.chat;

import games.strategy.triplea.settings.ClientSetting;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.Action;
import javax.swing.BoundedRangeModel;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.triplea.domain.data.UserName;
import org.triplea.java.DateTimeUtil;
import org.triplea.java.Interruptibles;
import org.triplea.java.StringUtils;
import org.triplea.sound.ClipPlayer;
import org.triplea.sound.SoundPath;
import org.triplea.swing.SwingAction;
import org.triplea.swing.key.binding.KeyCode;
import org.triplea.swing.key.binding.SwingKeyBinding;

/**
 * A Chat window.
 *
 * <p>Multiple chat panels can be connected to the same Chat.
 *
 * <p>We can change the chat we are connected to using the setChat(...) method.
 */
@Slf4j
public class ChatMessagePanel extends JPanel implements ChatMessageListener {
  private static final long serialVersionUID = 118727200083595226L;
  private static final int MAX_LINES = 5000;

  private final ChatFloodControl floodControl = new ChatFloodControl();
  private final ClipPlayer clipPlayer;
  private JTextPane text;
  private JScrollPane scrollPane;
  private JTextField nextMessage;
  private JButton send;
  private JButton setStatus;
  @Getter private Chat chat;
  private final SimpleAttributeSet bold = new SimpleAttributeSet();
  private final SimpleAttributeSet italic = new SimpleAttributeSet();
  private final SimpleAttributeSet normal = new SimpleAttributeSet();
  private final Action setStatusAction =
      SwingAction.of(
          "Status...",
          e -> {
            final String status =
                JOptionPane.showInputDialog(
                    JOptionPane.getFrameForComponent(ChatMessagePanel.this),
                    "Enter Status Text (leave blank for no status)",
                    "");
            if (status != null) {
              chat.updateStatus(status.trim());
            }
          });
  private final ChatSoundProfile chatSoundProfile;

  /** A profile defines the sounds to use for various chat events. */
  public enum ChatSoundProfile {
    LOBBY,
    GAME
  }

  public ChatMessagePanel(
      final Chat chat, final ChatSoundProfile chatSoundProfile, final ClipPlayer clipPlayer) {
    this.chatSoundProfile = chatSoundProfile;
    this.clipPlayer = clipPlayer;
    init();
    setChat(chat);
  }

  private void init() {
    createComponents();
    layoutComponents();
    StyleConstants.setBold(bold, true);
    StyleConstants.setItalic(italic, true);
    setSize(300, 200);
  }

  void setChat(final Chat chat) {
    Interruptibles.await(
        () ->
            SwingAction.invokeAndWait(
                () -> {
                  if (chat != null) {
                    cleanupKeyMap();
                  }
                  this.chat = chat;
                  if (chat != null) {
                    setupKeyMap();
                    chat.addChatListener(this);
                    send.setEnabled(true);
                    text.setEnabled(true);
                    text.setText("");
                    chat.getChatHistory()
                        .forEach(msg -> addChatMessage(msg.getMessage(), msg.getFrom()));
                  } else {
                    send.setEnabled(false);
                    text.setEnabled(false);
                  }
                }));
  }

  private void layoutComponents() {
    final Container content = this;
    content.setLayout(new BorderLayout());
    scrollPane = new JScrollPane(text);
    content.add(scrollPane, BorderLayout.CENTER);
    final JPanel sendPanel = new JPanel();
    sendPanel.setLayout(new BorderLayout());
    sendPanel.add(nextMessage, BorderLayout.CENTER);
    sendPanel.add(send, BorderLayout.WEST);
    sendPanel.add(setStatus, BorderLayout.EAST);
    content.add(sendPanel, BorderLayout.SOUTH);
  }

  @Override
  public boolean requestFocusInWindow() {
    return nextMessage.requestFocusInWindow();
  }

  private void createComponents() {
    text = new JTextPane();
    text.setEditable(false);
    text.addMouseListener(
        new MouseListener() {
          @Override
          public void mouseReleased(final MouseEvent e) {
            final String markedText = text.getSelectedText();
            if (markedText == null || markedText.length() == 0) {
              nextMessage.requestFocusInWindow();
            }
          }

          @Override
          public void mousePressed(final MouseEvent e) {}

          @Override
          public void mouseExited(final MouseEvent e) {}

          @Override
          public void mouseEntered(final MouseEvent e) {}

          @Override
          public void mouseClicked(final MouseEvent e) {}
        });
    nextMessage = new JTextField(10);
    // when enter is pressed, send the message
    setStatus = new JButton(setStatusAction);
    setStatus.setFocusable(false);
    final Insets inset = new Insets(3, 3, 3, 3);
    send = new JButton(SwingAction.of("Send", e -> sendMessage()));
    send.setMargin(inset);
    send.setFocusable(false);
  }

  private void setupKeyMap() {
    SwingKeyBinding.addKeyBinding(nextMessage, KeyCode.ENTER, this::sendMessage);
    SwingKeyBinding.addKeyBinding(
        nextMessage, KeyCode.UP, () -> loadMessageFromHistory(MessageOffset.PREVIOUS));
    SwingKeyBinding.addKeyBinding(
        nextMessage, KeyCode.DOWN, () -> loadMessageFromHistory(MessageOffset.NEXT));
  }

  private void loadMessageFromHistory(final MessageOffset messageOffset) {
    if (chat == null) {
      return;
    }

    final SentMessagesHistory sentMessagesHistory = chat.getSentMessagesHistory();
    switch (messageOffset) {
      case PREVIOUS:
        sentMessagesHistory.prev();
        break;
      case NEXT:
        sentMessagesHistory.next();
        break;
      default:
        throw new AssertionError("unknown message offset: " + messageOffset);
    }

    nextMessage.setText(sentMessagesHistory.current());
  }

  private enum MessageOffset {
    PREVIOUS,
    NEXT
  }

  private void cleanupKeyMap() {
    final InputMap nextMessageKeymap = nextMessage.getInputMap();
    nextMessageKeymap.remove(KeyStroke.getKeyStroke('\n'));
    nextMessageKeymap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, false));
    nextMessageKeymap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, false));
  }

  @Override
  public void messageReceived(final UserName userName, final String message) {
    addMessageWithSound(message, userName, SoundPath.CLIP_CHAT_MESSAGE);
  }

  @Override
  public void slapped(final UserName from) {
    final String message = "You were slapped by " + from;
    addMessageWithSound(message, from, SoundPath.CLIP_CHAT_SLAP);
  }

  private void addMessageWithSound(final String message, final UserName from, final String sound) {
    SwingAction.invokeNowOrLater(
        () -> {
          if (from == null || chat == null) {
            // someone likely disconnected from the game.
            return;
          }
          if (!floodControl.allow(from, System.currentTimeMillis())) {
            if (from.equals(chat.getLocalUserName())) {
              addChatMessage(
                  "MESSAGE LIMIT EXCEEDED, TRY AGAIN LATER", UserName.of("ADMIN_FLOOD_CONTROL"));
            }
            return;
          }
          addChatMessage(message, from);
          SwingUtilities.invokeLater(
              () -> {
                final BoundedRangeModel scrollModel = scrollPane.getVerticalScrollBar().getModel();
                scrollModel.setValue(scrollModel.getMaximum());
              });
          clipPlayer.play(sound);
        });
  }

  private void addChatMessage(final String originalMessage, final UserName from) {
    final String message = StringUtils.truncate(originalMessage, 200);
    final String time = "(" + DateTimeUtil.getLocalizedTime() + ")";
    final Document doc = text.getDocument();
    try {
      doc.insertString(
          doc.getLength(),
          ClientSetting.showChatTimeSettings.getSetting() ? time + " " + from + ": " : from + ": ",
          bold);
      doc.insertString(doc.getLength(), " " + message + "\n", normal);
      // don't let the chat get too big
      trimLines(doc, MAX_LINES);
    } catch (final BadLocationException e) {
      log.error(
          "There was an Error whilst trying to add the Chat Message \""
              + message
              + "\" sent by "
              + from
              + " at "
              + time,
          e);
    }
  }

  public void addServerMessage(final String message) {
    try {
      final Document doc = text.getDocument();
      doc.insertString(doc.getLength(), message + "\n", normal);
    } catch (final BadLocationException e) {
      log.error(
          "There was an Error whilst trying to add the Server Message \"" + message + "\"", e);
    }
  }

  @Override
  public void playerJoined(final String message) {
    addGenericMessage(message);
    if (chatSoundProfile == ChatSoundProfile.GAME) {
      clipPlayer.play(SoundPath.CLIP_CHAT_JOIN_GAME);
    }
  }

  @Override
  public void playerLeft(final String message) {
    addGenericMessage(message);
  }

  @Override
  public void eventReceived(final String chatEvent) {
    addGenericMessage(chatEvent);
  }

  private void addGenericMessage(final String message) {
    SwingUtilities.invokeLater(
        () -> {
          try {
            final Document doc = text.getDocument();
            doc.insertString(doc.getLength(), message + "\n", italic);
            // don't let the chat get too big
            trimLines(doc, MAX_LINES);
          } catch (final BadLocationException e) {
            log.error(
                "There was an Error whilst trying to add the Status Message \"" + message + "\"",
                e);
          }
        });
  }

  /** Show only the first n lines. */
  public static void trimLines(final Document doc, final int lineCount) {
    if (doc.getLength() < lineCount) {
      return;
    }
    try {
      final String text = doc.getText(0, doc.getLength());
      int returnsFound = 0;
      for (int i = text.length() - 1; i >= 0; i--) {
        if (text.charAt(i) == '\n') {
          returnsFound++;
        }
        if (returnsFound == lineCount) {
          doc.remove(0, i);
          return;
        }
      }
    } catch (final BadLocationException e) {
      log.error("There was an Error whilst trying trimming Chat", e);
    }
  }

  private void sendMessage() {
    if (nextMessage.getText().trim().length() == 0) {
      return;
    }
    chat.sendMessage(nextMessage.getText());
    nextMessage.setText("");
  }
}
