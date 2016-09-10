package games.strategy.engine.chat;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Insets;
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

import games.strategy.debug.ClientLogger;
import games.strategy.net.INode;
import games.strategy.net.ServerMessenger;
import games.strategy.sound.ClipPlayer;
import games.strategy.sound.SoundPath;
import games.strategy.ui.SwingAction;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;

/**
 * A Chat window.
 * Mutiple chat panels can be connected to the same Chat.
 * <p>
 * We can change the chat we are connected to using the setChat(...) method.
 */
public class ChatMessagePanel extends BorderPane implements IChatListener {
  private static final long serialVersionUID = 118727200083595226L;
  private final ChatFloodControl floodControl = new ChatFloodControl();
  private static final int MAX_LINES = 5000;
  private TextArea text;
  private ScrollPane scrollPane;
  private TextField nextMessage;
  private Button send;
  private Button setStatus;
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
    setPrefSize(300, 200);
  }

  public String getAllText() {
    return text.getText();
  }

  public void shutDown() {
    if (chat != null) {
      chat.removeChatListener(this);
    }
    chat = null;
    this.setVisible(false);
    this.getChildren().clear();
  }

  public void setChat(final Chat chat) {
    if (!SwingUtilities.isEventDispatchThread()) {
      SwingAction.invokeAndWait(() -> setChat(chat));
      return;
    }
    if (chat != null) {
      chat.removeChatListener(this);
    }
    this.chat = chat;
    if (chat != null) {
      setupKeyMap();
      chat.addChatListener(this);
      send.setDisable(false);
      text.setDisable(false);
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
      send.setDisable(true);
      text.setDisable(true);
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
    scrollPane = new ScrollPane(text);
    setCenter(scrollPane);
    final BorderPane sendPanel = new BorderPane();
    sendPanel.setCenter(nextMessage);
    sendPanel.setLeft(send);
    sendPanel.setRight(setStatus);
    setBottom(sendPanel);
  }

  @Override
  public void requestFocus() {
    nextMessage.requestFocus();
  }

  private void createComponents() {
    text = new TextArea();
    text.setEditable(false);
    text.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
      final String markedText = text.getSelectedText();
      if (markedText == null || markedText.length() == 0) {
        nextMessage.requestFocus();
      }
    });
    nextMessage = new TextField();
    // when enter is pressed, send the message
    setStatus = new Button();
    setStatus.setOnAction(e -> setStatusAction.run());
    final Insets inset = new Insets(3, 3, 3, 3);
    send = new Button();
    send.setOnAction(e -> sendAction.run());
  }

  private void setupKeyMap() {
    addEventHandler(KeyEvent.KEY_PRESSED, e -> {
      if(e.getCode() == KeyCode.UP){
        upAction.run();
      } else if(e.getCode() == KeyCode.DOWN){
        downAction.run();
      }  else if(e.getCode() == KeyCode.ENTER){
        sendAction.run();
      }
    });
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
        scrollPane.setVvalue(1);
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
      ClientLogger.logError("There was an Error whilst trying to add the Chat Message \"" + message + "\" sent by "
          + from + " at " + time, e);
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

  private final Runnable setStatusAction = () -> {
    String status = JOptionPane.showInputDialog(JOptionPane.getFrameForComponent(ChatMessagePanel.this),
        "Enter Status Text (leave blank for no status)", "");
    if (status != null) {
      if (status.trim().length() == 0) {
        status = null;
      }
      chat.getStatusManager().setStatus(status);
    }
  };
  private final Runnable sendAction = () -> {
    if (nextMessage.getText().trim().length() == 0) {
      return;
    }
    if (isThirdPerson(nextMessage.getText())) {
      chat.sendMessage(nextMessage.getText().substring(ME.length()), true);
    } else {
      chat.sendMessage(nextMessage.getText(), false);
    }
    nextMessage.setText("");
  };
  private final Runnable downAction = () -> {
    if (chat == null) {
      return;
    }
    chat.getSentMessagesHistory().next();
    nextMessage.setText(chat.getSentMessagesHistory().current());
  };
  private final Runnable upAction = () -> {
    if (chat == null) {
      return;
    }
    chat.getSentMessagesHistory().prev();
    nextMessage.setText(chat.getSentMessagesHistory().current());
  };

  @Override
  public void updatePlayerList(final Collection<INode> players) {}
}
