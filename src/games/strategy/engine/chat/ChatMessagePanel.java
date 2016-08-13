package games.strategy.engine.chat;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

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

import games.strategy.ui.SwingAction;
import games.strategy.debug.ClientLogger;
import games.strategy.net.INode;
import games.strategy.net.ServerMessenger;
import games.strategy.sound.ClipPlayer;
import games.strategy.sound.SoundPath;

/**
 * A Chat window.
 * Mutiple chat panels can be connected to the same Chat.
 * <p>
 * We can change the chat we are connected to using the setChat(...) method.
 */
public class ChatMessagePanel extends JPanel implements IChatListener {
  private static final long serialVersionUID = 118727200083595226L;
  private final ChatFloodControl floodControl = new ChatFloodControl();
  private static final int MAX_LINES = 5000;
  private JTextPane text;
  private JScrollPane scrollPane;
  private JTextField nextMessage;
  private JButton send;
  private JButton setStatus;
  private Chat chat;
  private boolean showTime = false;
  private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("'('HH:mm:ss')'");
  private final SimpleAttributeSet bold = new SimpleAttributeSet();
  private final SimpleAttributeSet italic = new SimpleAttributeSet();
  private final SimpleAttributeSet normal = new SimpleAttributeSet();
  public static final String ME = "/me ";

  private static boolean isThirdPerson(final String msg) {
    return msg.toLowerCase().startsWith(ME);
  }

  public ChatMessagePanel(final Chat chat) {
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

  public String getAllText() {
    return text.getText();
  }

  public void shutDown() {
    if (chat != null) {
      chat.removeChatListener(this);
      cleanupKeyMap();
    }
    chat = null;
    this.setVisible(false);
    this.removeAll();
  }

  public void setChat(final Chat chat) {
    if (!SwingUtilities.isEventDispatchThread()) {
      SwingAction.invokeAndWait(() -> setChat(chat));
      return;
    }
    if (chat != null) {
      chat.removeChatListener(this);
      cleanupKeyMap();
    }
    this.chat = chat;
    if (chat != null) {
      setupKeyMap();
      chat.addChatListener(this);
      send.setEnabled(true);
      text.setEnabled(true);
      synchronized (chat.getMutex()) {
        text.setText("");
        for (final ChatMessage message : chat.getChatHistory()) {
          if (message.getFrom().equals(chat.getServerNode().getName())) {
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
      send.setEnabled(false);
      text.setEnabled(false);
      updatePlayerList(Collections.emptyList());
    }
  }

  public Chat getChat() {
    return chat;
  }

  public void setShowTime(final boolean showTime) {
    this.showTime = showTime;
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
    text.addMouseListener(new MouseListener() {
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
    send = new JButton(sendAction);
    send.setMargin(inset);
    send.setFocusable(false);
  }

  private void setupKeyMap() {
    final InputMap nextMessageKeymap = nextMessage.getInputMap();
    nextMessageKeymap.put(KeyStroke.getKeyStroke('\n'), sendAction);
    nextMessageKeymap.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_UP, 0, false), upAction);
    nextMessageKeymap.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DOWN, 0, false), downAction);
  }

  private void cleanupKeyMap() {
    final InputMap nextMessageKeymap = nextMessage.getInputMap();
    nextMessageKeymap.remove(KeyStroke.getKeyStroke('\n'));
    nextMessageKeymap.remove(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_UP, 0, false));
    nextMessageKeymap.remove(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DOWN, 0, false));
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
    final Runnable runner = () -> {
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
      SwingUtilities.invokeLater(() -> {
        final BoundedRangeModel scrollModel = scrollPane.getVerticalScrollBar().getModel();
        scrollModel.setValue(scrollModel.getMaximum());
      });
      ClipPlayer.play(sound);
    };
    if (SwingUtilities.isEventDispatchThread()) {
      runner.run();
    } else {
      SwingUtilities.invokeLater(runner);
    }
  }

  private void addChatMessage(final String originalMessage, final String from, final boolean thirdperson) {
    final String message = trimMessage(originalMessage);
    final String time = simpleDateFormat.format(new Date());
    final Document doc = text.getDocument();
    try {
      if (thirdperson) {
        doc.insertString(doc.getLength(), (showTime ? "* " + time + " " + from : "* " + from), bold);
      } else {
        doc.insertString(doc.getLength(), (showTime ? time + " " + from + ": " : from + ": "), bold);
      }
      doc.insertString(doc.getLength(), " " + message + "\n", normal);
      // don't let the chat get too big
      trimLines(doc, MAX_LINES);
    } catch (final BadLocationException e) {
      ClientLogger.logError("There was an Error whilst trying to add the Chat Message \"" + message +"\" sent by " + from + " at " + time, e);
    }
  }

  public void addServerMessage(final String message) {
    try {
      final Document doc = text.getDocument();
      doc.insertString(doc.getLength(), message + "\n", normal);
    } catch (final BadLocationException e) {
      ClientLogger.logError("There was an Error whilst trying to add the Server Message \"" + message + "\"", e);
    }
  }

  @Override
  public void addStatusMessage(final String message) {
    SwingUtilities.invokeLater(() -> {
      try {
        final Document doc = text.getDocument();
        doc.insertString(doc.getLength(), message + "\n", italic);
        // don't let the chat get too big
        trimLines(doc, MAX_LINES);
      } catch (final BadLocationException e) {
        ClientLogger.logError("There was an Error whilst trying to add the Status Message \"" + message + "\"", e);
      }
    });
  }

  /**
   * Show only the first n lines
   */
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
      ClientLogger.logError("There was an Error whilst trying trimming Chat", e);
    }
  }

  private static String trimMessage(final String originalMessage) {
    // don't allow messages that are too long
    if (originalMessage.length() > 200) {
      return originalMessage.substring(0, 199) + "...";
    } else {
      return originalMessage;
    }
  }

  private final Action setStatusAction = SwingAction.of("Status...", e -> {
    String status = JOptionPane.showInputDialog(JOptionPane.getFrameForComponent(ChatMessagePanel.this),
        "Enter Status Text (leave blank for no status)", "");
    if (status != null) {
      if (status.trim().length() == 0) {
        status = null;
      }
      chat.getStatusManager().setStatus(status);
    }
  });
  private final Action sendAction = SwingAction.of("Send", e -> {
    if (nextMessage.getText().trim().length() == 0) {
      return;
    }
    if (isThirdPerson(nextMessage.getText())) {
      chat.sendMessage(nextMessage.getText().substring(ME.length()), true);
    } else {
      chat.sendMessage(nextMessage.getText(), false);
    }
    nextMessage.setText("");
  });
  private final Action downAction = SwingAction.of(e -> {
    if (chat == null) {
      return;
    }
    chat.getSentMessagesHistory().next();
    nextMessage.setText(chat.getSentMessagesHistory().current());
  });
  private final Action upAction = SwingAction.of(e -> {
    if (chat == null) {
      return;
    }
    chat.getSentMessagesHistory().prev();
    nextMessage.setText(chat.getSentMessagesHistory().current());
  });

  @Override
  public void updatePlayerList(final Collection<INode> players) {}
}
