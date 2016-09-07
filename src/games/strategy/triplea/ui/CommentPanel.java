package games.strategy.triplea.ui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Action;
import javax.swing.BoundedRangeModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.tree.TreeNode;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.history.Event;
import games.strategy.engine.history.HistoryNode;
import games.strategy.engine.history.Round;
import games.strategy.engine.history.Step;
import games.strategy.triplea.delegate.remote.IEditDelegate;
import games.strategy.ui.SwingAction;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class CommentPanel extends BorderPane {
  private static final long serialVersionUID = -9122162393288045888L;
  private TextFlow m_text;
  private ScrollPane m_scrollPane;
  private TextField m_nextMessage;
  private Button m_save;
  private final GameData m_data;
  private final TripleAFrame m_frame;
  private Map<PlayerID, Icon> m_iconMap;
  private final SimpleAttributeSet bold = new SimpleAttributeSet();
  private final SimpleAttributeSet italic = new SimpleAttributeSet();
  private final SimpleAttributeSet normal = new SimpleAttributeSet();

  public CommentPanel(final TripleAFrame frame, final GameData data) {
    m_frame = frame;
    m_data = data;
    init();
  }

  private void init() {
    createComponents();
    layoutComponents();
    setupKeyMap();
    StyleConstants.setBold(bold, true);
    StyleConstants.setItalic(italic, true);
    setWidth(300);
    setHeight(200);
    loadHistory();
    setupListeners();
  }

  private void layoutComponents() {
    m_scrollPane = new ScrollPane(m_text);
    getChildren().add(m_scrollPane);
    final BorderPane savePanel = new BorderPane();
    savePanel.getChildren().add(m_nextMessage);
    savePanel.getChildren().add(m_save);
    getChildren().add(savePanel);
  }

  private void createComponents() {
    m_text = new TextFlow();
    m_nextMessage = new TextField();
    // when enter is pressed, send the message
    final Insets inset = new Insets(3, 3, 3, 3);
    m_save = new Button();
    m_save.setOnAction(e -> m_saveAction.run());
    // create icon map
    m_iconMap = new HashMap<>();
    for (final PlayerID playerId : m_data.getPlayerList().getPlayers()) {
      m_iconMap.put(playerId, new ImageIcon(m_frame.getUIContext().getFlagImageFactory().getSmallFlag(playerId)));
    }
  }

  private void setupListeners() {
    m_data.getHistory().addTreeModelListener(new TreeModelListener() {
      @Override
      public void treeNodesChanged(final TreeModelEvent e) {}

      @Override
      public void treeNodesInserted(final TreeModelEvent e) {
        readHistoryTreeEvent(e);
      }

      @Override
      public void treeNodesRemoved(final TreeModelEvent e) {}

      @Override
      public void treeStructureChanged(final TreeModelEvent e) {
        readHistoryTreeEvent(e);
      }
    });
  }

  private void readHistoryTreeEvent(final TreeModelEvent e) {
    final TreeModelEvent tme = e;
    final Runnable runner = () -> {
      m_data.acquireReadLock();
      try {
        final List<Node> doc = m_text.getChildren();
        final HistoryNode node = (HistoryNode) (tme.getTreePath().getLastPathComponent());
        final TreeNode child = node == null ? null : (node.getChildCount() > 0 ? node.getLastChild() : null);
        final String title =
            child != null ? (child instanceof Event ? ((Event) child).getDescription() : child.toString())
                : (node != null ? node.getTitle() : "");
        final Pattern p = Pattern.compile("^COMMENT: (.*)");
        final Matcher m = p.matcher(title);
        if (m.matches()) {
          final PlayerID playerId = m_data.getSequence().getStep().getPlayerID();
          final int round = m_data.getSequence().getRound();
          final String player = playerId.getName();
          final Icon icon = m_iconMap.get(playerId);
          try {
            // insert into ui document
            final String prefix = " " + player + "(" + round + ") : ";
            doc.add(new ImageView(SwingFXUtils.toFXImage((BufferedImage)((ImageIcon)icon).getImage(), null)));
            Text prefixText = new Text(prefix);
            prefixText.setFont(Font.font(prefixText.getFont().getFamily(), FontWeight.BOLD, prefixText.getFont().getSize()));
            doc.add(prefixText);
            doc.add(new Text(m.group(1) + "\n"));
          } catch (final BadLocationException e1) {
            ClientLogger.logQuietly(e1);
          }
        }
      } finally {
        m_data.releaseReadLock();
      }
    };
    // invoke in the swing event thread
    if (SwingUtilities.isEventDispatchThread()) {
      runner.run();
    } else {
      SwingUtilities.invokeLater(runner);
    }
  }

  private void setupKeyMap() {
    m_nextMessage.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
      if(e.getCharacter().equalsIgnoreCase("\n")){
        m_saveAction.run();
      }
    });
  }

  private void loadHistory() {
    final Document doc = m_text.getDocument();
    final HistoryNode rootNode = (HistoryNode) m_data.getHistory().getRoot();
    @SuppressWarnings("unchecked")
    final Enumeration<HistoryNode> nodeEnum = rootNode.preorderEnumeration();
    final Pattern p = Pattern.compile("^COMMENT: (.*)");
    String player = "";
    int round = 0;
    Icon icon = null;
    while (nodeEnum.hasMoreElements()) {
      final HistoryNode node = nodeEnum.nextElement();
      if (node instanceof Round) {
        round++;
      } else if (node instanceof Step) {
        final PlayerID playerId = ((Step) node).getPlayerID();
        if (playerId != null) {
          player = playerId.getName();
          icon = m_iconMap.get(playerId);
        }
      } else {
        final String title = node.getTitle();
        final Matcher m = p.matcher(title);
        if (m.matches()) {
          try {
            // insert into ui document
            final String prefix = " " + player + "(" + round + ") : ";
            m_text.insertIcon(icon);
            doc.insertString(doc.getLength(), prefix, bold);
            doc.insertString(doc.getLength(), m.group(1) + "\n", normal);
          } catch (final BadLocationException e) {
            ClientLogger.logQuietly(e);
          }
        }
      }
    }
  }

  /** thread safe */
  public void addMessage(final String message) {
    final Runnable runner = () -> {
      try {
        final Document doc = m_text.getDocument();
        // save history entry
        final IEditDelegate delegate = m_frame.getEditDelegate();
        String error;
        if (delegate == null) {
          error = "You can only add comments during your turn";
        } else {
          error = delegate.addComment(message);
        }
        if (error != null) {
          doc.insertString(doc.getLength(), error + "\n", italic);
        }
      } catch (final BadLocationException e) {
        ClientLogger.logQuietly(e);
      }
      final BoundedRangeModel scrollModel = m_scrollPane.getVerticalScrollBar().getModel();
      scrollModel.setValue(scrollModel.getMaximum());
    };
    // invoke in the swing event thread
    if (SwingUtilities.isEventDispatchThread()) {
      runner.run();
    } else {
      SwingUtilities.invokeLater(runner);
    }
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
      ClientLogger.logQuietly(e);
    }
  }

  private final Runnable m_saveAction = () -> {
    if (m_nextMessage.getText().trim().length() == 0) {
      return;
    }
    addMessage(m_nextMessage.getText());
    m_nextMessage.setText("");
  };
}
