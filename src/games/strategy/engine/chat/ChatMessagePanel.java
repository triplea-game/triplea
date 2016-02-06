package games.strategy.engine.chat;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import javax.swing.AbstractAction;
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

import games.strategy.common.swing.SwingAction;
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
  private JTextPane m_text;
  private JScrollPane m_scrollPane;
  private JTextField m_nextMessage;
  private JButton m_send;
  private JButton m_setStatus;
  private Chat m_chat;
  private boolean m_showTime = false;
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
    return m_text.getText();
  }

  public void shutDown() {
    if (m_chat != null) {
      m_chat.removeChatListener(this);
      cleanupKeyMap();
    }
    m_chat = null;
    this.setVisible(false);
    this.removeAll();
  }

  public void setChat(final Chat chat) {
    if (!SwingUtilities.isEventDispatchThread()) {
      try {
        SwingUtilities.invokeAndWait(new Runnable() {
          @Override
          public void run() {
            setChat(chat);
          }
        });
      } catch (final InterruptedException e) {
        e.printStackTrace();
      } catch (final InvocationTargetException e) {
        e.printStackTrace();
      }
      return;
    }
    if (m_chat != null) {
      m_chat.removeChatListener(this);
      cleanupKeyMap();
    }
    m_chat = chat;
    if (m_chat != null) {
      setupKeyMap();
      m_chat.addChatListener(this);
      m_send.setEnabled(true);
      m_text.setEnabled(true);
      synchronized (m_chat.getMutex()) {
        m_text.setText("");
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
          addChatMessage(message.getMessage(), message.getFrom(), message.isMeMessage());
        }
      }
    } else {
      m_send.setEnabled(false);
      m_text.setEnabled(false);
      updatePlayerList(Collections.<INode>emptyList());
    }
  }

  public Chat getChat() {
    return m_chat;
  }

  public void setShowTime(final boolean showTime) {
    m_showTime = showTime;
  }

  private void layoutComponents() {
    final Container content = this;
    content.setLayout(new BorderLayout());
    m_scrollPane = new JScrollPane(m_text);
    content.add(m_scrollPane, BorderLayout.CENTER);
    final JPanel sendPanel = new JPanel();
    sendPanel.setLayout(new BorderLayout());
    sendPanel.add(m_nextMessage, BorderLayout.CENTER);
    sendPanel.add(m_send, BorderLayout.WEST);
    sendPanel.add(m_setStatus, BorderLayout.EAST);
    content.add(sendPanel, BorderLayout.SOUTH);
  }

  @Override
  public boolean requestFocusInWindow() {
    return m_nextMessage.requestFocusInWindow();
  }

  private void createComponents() {
    m_text = new JTextPane();
    m_text.setEditable(false);
    m_text.addMouseListener(new MouseListener() {
      @Override
      public void mouseReleased(final MouseEvent e) {
        final String markedText = m_text.getSelectedText();
        if (markedText == null || markedText.length() == 0) {
          m_nextMessage.requestFocusInWindow();
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
    m_nextMessage = new JTextField(10);
    // when enter is pressed, send the message
    m_setStatus = new JButton(m_setStatusAction);
    m_setStatus.setFocusable(false);
    final Insets inset = new Insets(3, 3, 3, 3);
    m_send = new JButton(m_sendAction);
    m_send.setMargin(inset);
    m_send.setFocusable(false);
  }

  private void setupKeyMap() {
    final InputMap nextMessageKeymap = m_nextMessage.getInputMap();
    nextMessageKeymap.put(KeyStroke.getKeyStroke('\n'), m_sendAction);
    nextMessageKeymap.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_UP, 0, false), m_UpAction);
    nextMessageKeymap.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DOWN, 0, false), m_DownAction);
  }

  private void cleanupKeyMap() {
    final InputMap nextMessageKeymap = m_nextMessage.getInputMap();
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
    final Runnable runner = new Runnable() {
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
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            final BoundedRangeModel scrollModel = m_scrollPane.getVerticalScrollBar().getModel();
            scrollModel.setValue(scrollModel.getMaximum());
          }
        });
        ClipPlayer.play(sound);
      }
    };
    if (SwingUtilities.isEventDispatchThread()) {
      runner.run();
    } else {
      SwingUtilities.invokeLater(runner);
    }
  }

  private void addChatMessage(final String originalMessage, final String from, final boolean thirdperson) {
    final String message = trimMessage(originalMessage);
    try {
      final String time = simpleDateFormat.format(new Date());
      final Document doc = m_text.getDocument();
      if (thirdperson) {
        doc.insertString(doc.getLength(), (m_showTime ? "* " + time + " " + from : "* " + from), bold);
      } else {
        doc.insertString(doc.getLength(), (m_showTime ? time + " " + from + ": " : from + ": "), bold);
      }
      doc.insertString(doc.getLength(), " " + message + "\n", normal);
      // don't let the chat get too big
      trimLines(doc, MAX_LINES);
    } catch (final BadLocationException ble) {
      ble.printStackTrace();
    }
  }

  public void addServerMessage(final String message) {
    try {
      final Document doc = m_text.getDocument();
      doc.insertString(doc.getLength(), message + "\n", normal);
    } catch (final BadLocationException ble) {
      ble.printStackTrace();
    }
  }

  @Override
  public void addStatusMessage(final String message) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        try {
          final Document doc = m_text.getDocument();
          doc.insertString(doc.getLength(), message + "\n", italic);
          // don't let the chat get too big
          trimLines(doc, MAX_LINES);
        } catch (final BadLocationException ble) {
          ble.printStackTrace();
        }
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
      e.printStackTrace();
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

  private final Action m_setStatusAction = SwingAction.of("Status...", e -> {
    String status = JOptionPane.showInputDialog(JOptionPane.getFrameForComponent(ChatMessagePanel.this),
        "Enter Status Text (leave blank for no status)", "");
    if (status != null) {
      if (status.trim().length() == 0) {
        status = null;
      }
      m_chat.getStatusManager().setStatus(status);
    }
  });
  private final Action m_sendAction = SwingAction.of("Send", e -> {
    if (m_nextMessage.getText().trim().length() == 0) {
      return;
    }
    if (isThirdPerson(m_nextMessage.getText())) {
      m_chat.sendMessage(m_nextMessage.getText().substring(ME.length()), true);
    } else {
      m_chat.sendMessage(m_nextMessage.getText(), false);
    }
    m_nextMessage.setText("");
  });
  private final Action m_DownAction = new AbstractAction() {
    private static final long serialVersionUID = -1945655511272482449L;

    @Override
    public void actionPerformed(final ActionEvent e) {
      if (m_chat == null) {
        return;
      }
      m_chat.getSentMessagesHistory().next();
      m_nextMessage.setText(m_chat.getSentMessagesHistory().current());
    }
  };
  private final Action m_UpAction = SwingAction.of(e -> {
    if (m_chat == null) {
      return;
    }
    m_chat.getSentMessagesHistory().prev();
    m_nextMessage.setText(m_chat.getSentMessagesHistory().current());
  });

  @Override
  public void updatePlayerList(final Collection<INode> players) {}
}
