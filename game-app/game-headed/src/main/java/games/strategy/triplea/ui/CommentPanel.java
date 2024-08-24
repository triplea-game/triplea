package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.history.Event;
import games.strategy.engine.history.HistoryNode;
import games.strategy.engine.history.Round;
import games.strategy.engine.history.Step;
import games.strategy.triplea.delegate.remote.IEditDelegate;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Insets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;
import org.triplea.java.ThreadRunner;
import org.triplea.swing.SwingAction;

@Slf4j
class CommentPanel extends JPanel {
  private static final long serialVersionUID = -9122162393288045888L;

  private static final Pattern COMMENT_PATTERN = Pattern.compile("^COMMENT: (.*)");

  private JTextPane text;
  private JScrollPane scrollPane;
  private JTextField nextMessage;
  private JButton save;
  private final GameData data;
  private final TripleAFrame frame;
  private final Map<GamePlayer, Icon> iconMap = new HashMap<>();
  private final SimpleAttributeSet bold = new SimpleAttributeSet();
  private final SimpleAttributeSet italic = new SimpleAttributeSet();
  private final SimpleAttributeSet normal = new SimpleAttributeSet();
  private final Action saveAction =
      SwingAction.of(
          "Add Comment",
          e -> {
            if (nextMessage.getText().trim().length() == 0) {
              return;
            }
            addMessage(nextMessage.getText());
            nextMessage.setText("");
          });

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
    for (final GamePlayer gamePlayer : data.getPlayerList().getPlayers()) {
      Optional.ofNullable(frame.getUiContext().getFlagImageFactory().getSmallFlag(gamePlayer))
          .ifPresent(image -> iconMap.put(gamePlayer, new ImageIcon(image)));
    }
  }

  private void setupListeners() {
    data.getHistory()
        .addTreeModelListener(
            new TreeModelListener() {
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
    SwingAction.invokeNowOrLater(
        () -> {
          final Object[] children = e.getChildren();
          final Object child =
              (children != null && children.length > 0) ? children[children.length - 1] : null;
          final String title;
          if (child != null) {
            title = (child instanceof Event) ? ((Event) child).getDescription() : child.toString();
          } else {
            final HistoryNode node = (HistoryNode) e.getTreePath().getLastPathComponent();
            title = (node != null) ? node.getTitle() : "";
          }
          final Matcher m = COMMENT_PATTERN.matcher(title);
          if (m.matches()) {
            final GamePlayer gamePlayer;
            final String player;
            final int round;
            try (GameData.Unlocker ignored = data.acquireReadLock()) {
              gamePlayer = data.getSequence().getStep().getPlayerId();
              player = gamePlayer.getName();
              round = data.getSequence().getRound();
            }
            final Icon icon = iconMap.get(gamePlayer);
            try {
              // insert into ui document
              final Document doc = text.getDocument();
              @NonNls final String prefix = " " + player + "(" + round + ") : ";
              text.insertIcon(icon);
              doc.insertString(doc.getLength(), prefix, bold);
              doc.insertString(doc.getLength(), m.group(1) + "\n", normal);
            } catch (final BadLocationException e1) {
              log.error("Failed to add history node", e1);
            }
          }
        });
  }

  private void setupKeyMap() {
    final InputMap nextMessageKeymap = nextMessage.getInputMap();
    nextMessageKeymap.put(KeyStroke.getKeyStroke('\n'), saveAction);
  }

  private void loadHistory() {
    final Document doc = text.getDocument();
    ThreadRunner.runInNewThread(
        () -> {
          final HistoryNode rootNode = (HistoryNode) data.getHistory().getRoot();
          final Enumeration<TreeNode> nodeEnum = rootNode.preorderEnumeration();
          final Pattern p = Pattern.compile("^COMMENT: (.*)");
          String player = "";
          int round = 0;
          Icon icon = null;
          while (nodeEnum.hasMoreElements()) {
            final HistoryNode node = (HistoryNode) nodeEnum.nextElement();
            if (node instanceof Round) {
              round++;
            } else if (node instanceof Step) {
              final GamePlayer gamePlayer = ((Step) node).getPlayerId();
              if (gamePlayer != null) {
                player = gamePlayer.getName();
                icon = iconMap.get(gamePlayer);
              }
            } else {
              final String title = node.getTitle();
              final Matcher m = p.matcher(title);
              if (m.matches()) {
                @NonNls final String prefix = " " + player + "(" + round + ") : ";
                final Icon lastIcon = icon;
                SwingUtilities.invokeLater(
                    () -> {
                      try {
                        // insert into ui document
                        text.insertIcon(lastIcon);
                        doc.insertString(doc.getLength(), prefix, bold);
                        doc.insertString(doc.getLength(), m.group(1) + "\n", normal);
                      } catch (final BadLocationException e) {
                        log.error("Failed to add history", e);
                      }
                    });
              }
            }
          }
        });
  }

  /** thread safe. */
  void addMessage(final String message) {
    SwingAction.invokeNowOrLater(
        () -> {
          try {
            final Document doc = text.getDocument();
            // save history entry
            final IEditDelegate delegate = frame.getEditDelegate();
            final String error;
            if (delegate == null) {
              error = "You can only add comments during your turn";
            } else {
              error = delegate.addComment(message);
            }
            if (error != null) {
              doc.insertString(doc.getLength(), error + "\n", italic);
            }
          } catch (final BadLocationException e) {
            log.error("Failed to add comment", e);
          }
          final BoundedRangeModel scrollModel = scrollPane.getVerticalScrollBar().getModel();
          scrollModel.setValue(scrollModel.getMaximum());
        });
  }
}
