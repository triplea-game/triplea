package games.strategy.triplea.ui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Insets;
import java.util.Enumeration;
import java.util.HashMap;
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

public class CommentPanel extends JPanel {
  private static final long serialVersionUID = -9122162393288045888L;
  private JTextPane m_text;
  private JScrollPane m_scrollPane;
  private JTextField m_nextMessage;
  private JButton m_save;
  private final GameData m_data;
  private final TripleAFrame m_frame;
  private Map<PlayerID, Icon> m_iconMap;
  private final SimpleAttributeSet bold = new SimpleAttributeSet();
  private final SimpleAttributeSet italic = new SimpleAttributeSet();
  private final SimpleAttributeSet normal = new SimpleAttributeSet();

  CommentPanel(final TripleAFrame frame, final GameData data) {
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
    setSize(300, 200);
    loadHistory();
    setupListeners();
  }

  private void layoutComponents() {
    final Container content = this;
    content.setLayout(new BorderLayout());
    m_scrollPane = new JScrollPane(m_text);
    content.add(m_scrollPane, BorderLayout.CENTER);
    content.add(m_scrollPane, BorderLayout.CENTER);
    final JPanel savePanel = new JPanel();
    savePanel.setLayout(new BorderLayout());
    savePanel.add(m_nextMessage, BorderLayout.CENTER);
    savePanel.add(m_save, BorderLayout.WEST);
    content.add(savePanel, BorderLayout.SOUTH);
  }

  private void createComponents() {
    m_text = new JTextPane();
    m_text.setEditable(false);
    m_text.setFocusable(false);
    m_nextMessage = new JTextField(10);
    // when enter is pressed, send the message
    final Insets inset = new Insets(3, 3, 3, 3);
    m_save = new JButton(m_saveAction);
    m_save.setMargin(inset);
    m_save.setFocusable(false);
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
        final Document doc = m_text.getDocument();
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
            m_text.insertIcon(icon);
            doc.insertString(doc.getLength(), prefix, bold);
            doc.insertString(doc.getLength(), m.group(1) + "\n", normal);
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
    final InputMap nextMessageKeymap = m_nextMessage.getInputMap();
    nextMessageKeymap.put(KeyStroke.getKeyStroke('\n'), m_saveAction);
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

  /** thread safe. */
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
   * Show only the first n lines.
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

  private final Action m_saveAction = SwingAction.of("Add Comment", e -> {
    if (m_nextMessage.getText().trim().length() == 0) {
      return;
    }
    addMessage(m_nextMessage.getText());
    m_nextMessage.setText("");
  });
}
