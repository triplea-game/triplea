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
  private JTextPane text;
  private JScrollPane scrollPane;
  private JTextField nextMessage;
  private JButton save;
  private final GameData data;
  private final TripleAFrame frame;
  private Map<PlayerID, Icon> iconMap;
  private final SimpleAttributeSet bold = new SimpleAttributeSet();
  private final SimpleAttributeSet italic = new SimpleAttributeSet();
  private final SimpleAttributeSet normal = new SimpleAttributeSet();

  CommentPanel(final TripleAFrame frame, final GameData data) {
    this.frame = frame;
    this.data = data;
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
    scrollPane = new JScrollPane(text);
    content.add(scrollPane, BorderLayout.CENTER);
    content.add(scrollPane, BorderLayout.CENTER);
    final JPanel savePanel = new JPanel();
    savePanel.setLayout(new BorderLayout());
    savePanel.add(nextMessage, BorderLayout.CENTER);
    savePanel.add(save, BorderLayout.WEST);
    content.add(savePanel, BorderLayout.SOUTH);
  }

  private void createComponents() {
    text = new JTextPane();
    text.setEditable(false);
    text.setFocusable(false);
    nextMessage = new JTextField(10);
    // when enter is pressed, send the message
    final Insets inset = new Insets(3, 3, 3, 3);
    save = new JButton(saveAction);
    save.setMargin(inset);
    save.setFocusable(false);
    // create icon map
    iconMap = new HashMap<>();
    for (final PlayerID playerId : data.getPlayerList().getPlayers()) {
      iconMap.put(playerId, new ImageIcon(frame.getUIContext().getFlagImageFactory().getSmallFlag(playerId)));
    }
  }

  private void setupListeners() {
    data.getHistory().addTreeModelListener(new TreeModelListener() {
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
      data.acquireReadLock();
      try {
        final Document doc = text.getDocument();
        final HistoryNode node = (HistoryNode) (tme.getTreePath().getLastPathComponent());
        final TreeNode child = node == null ? null : (node.getChildCount() > 0 ? node.getLastChild() : null);
        final String title =
            child != null ? (child instanceof Event ? ((Event) child).getDescription() : child.toString())
                : (node != null ? node.getTitle() : "");
        final Pattern p = Pattern.compile("^COMMENT: (.*)");
        final Matcher m = p.matcher(title);
        if (m.matches()) {
          final PlayerID playerId = data.getSequence().getStep().getPlayerID();
          final int round = data.getSequence().getRound();
          final String player = playerId.getName();
          final Icon icon = iconMap.get(playerId);
          try {
            // insert into ui document
            final String prefix = " " + player + "(" + round + ") : ";
            text.insertIcon(icon);
            doc.insertString(doc.getLength(), prefix, bold);
            doc.insertString(doc.getLength(), m.group(1) + "\n", normal);
          } catch (final BadLocationException e1) {
            ClientLogger.logQuietly(e1);
          }
        }
      } finally {
        data.releaseReadLock();
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
    final InputMap nextMessageKeymap = nextMessage.getInputMap();
    nextMessageKeymap.put(KeyStroke.getKeyStroke('\n'), saveAction);
  }

  private void loadHistory() {
    final Document doc = text.getDocument();
    final HistoryNode rootNode = (HistoryNode) data.getHistory().getRoot();
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
          icon = iconMap.get(playerId);
        }
      } else {
        final String title = node.getTitle();
        final Matcher m = p.matcher(title);
        if (m.matches()) {
          try {
            // insert into ui document
            final String prefix = " " + player + "(" + round + ") : ";
            text.insertIcon(icon);
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
        final Document doc = text.getDocument();
        // save history entry
        final IEditDelegate delegate = frame.getEditDelegate();
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
      final BoundedRangeModel scrollModel = scrollPane.getVerticalScrollBar().getModel();
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

  private final Action saveAction = SwingAction.of("Add Comment", e -> {
    if (nextMessage.getText().trim().length() == 0) {
      return;
    }
    addMessage(nextMessage.getText());
    nextMessage.setText("");
  });
}
