package games.strategy.engine.chat;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.Action;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;

import games.strategy.net.INode;
import games.strategy.ui.SwingAction;

public class ChatPlayerPanel extends JPanel implements IChatListener {
  private static final long serialVersionUID = -3153022965393962945L;
  private static final Icon ignoreIcon;

  static {
    final URL ignore = ChatPlayerPanel.class.getResource("ignore.png");
    if (ignore == null) {
      throw new IllegalStateException("Could not find ignore icon");
    }
    final Image img;
    try {
      img = ImageIO.read(ignore);
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
    ignoreIcon = new ImageIcon(img);
  }

  private JList<INode> players;
  private DefaultListModel<INode> listModel;
  private Chat chat;
  private final Set<String> hiddenPlayers = new HashSet<>();
  private final IStatusListener statusListener;
  // if our renderer is overridden
  // we do not set this directly on the JList,
  // instead we feed it the node name and staus as a string
  private ListCellRenderer<Object> setCellRenderer = new DefaultListCellRenderer();
  private final List<IPlayerActionFactory> actionFactories = new ArrayList<>();

  public ChatPlayerPanel(final Chat chat) {
    createComponents();
    layoutComponents();
    setupListeners();
    setWidgetActivation();
    statusListener = (node, newStatus) -> repaint();
    setChat(chat);
  }

  public void addHiddenPlayerName(final String name) {
    hiddenPlayers.add(name);
  }

  void shutDown() {
    if (chat != null) {
      chat.removeChatListener(this);
      chat.getStatusManager().removeStatusListener(statusListener);
    }
    chat = null;
    this.setVisible(false);
    this.removeAll();
  }

  public void setChat(final Chat chat) {
    if (this.chat != null) {
      this.chat.removeChatListener(this);
      this.chat.getStatusManager().removeStatusListener(statusListener);
    }
    this.chat = chat;
    if (chat != null) {
      chat.addChatListener(this);
      this.chat.getStatusManager().addStatusListener(statusListener);
    } else {
      // empty our player list
      updatePlayerList(Collections.emptyList());
    }
    repaint();
  }

  /**
   * set minimum size based on players (number and max name length) and distribution to playerIDs.
   */
  private void setDynamicPreferredSize() {
    final List<INode> onlinePlayers = chat.getOnlinePlayers();
    int maxNameLength = 0;
    final FontMetrics fontMetrics = this.getFontMetrics(UIManager.getFont("TextField.font"));
    for (final INode onlinePlayer : onlinePlayers) {
      maxNameLength = Math.max(maxNameLength, fontMetrics.stringWidth(onlinePlayer.getName()));
    }
    int iconCounter = 0;
    if (setCellRenderer instanceof PlayerChatRenderer) {
      iconCounter = ((PlayerChatRenderer) setCellRenderer).getMaxIconCounter();
    }
    setPreferredSize(new Dimension(maxNameLength + 40 + (iconCounter * 14), 80));
  }

  private void createComponents() {
    listModel = new DefaultListModel<>();
    players = new JList<>(listModel);
    players.setFocusable(false);
    players.setCellRenderer((list, node, index, isSelected, cellHasFocus) -> {
      if (setCellRenderer == null) {
        return new JLabel();
      }
      final DefaultListCellRenderer renderer;
      if (setCellRenderer instanceof PlayerChatRenderer) {
        renderer = (DefaultListCellRenderer) setCellRenderer.getListCellRendererComponent(list, node, index,
            isSelected, cellHasFocus);
      } else {
        renderer = (DefaultListCellRenderer) setCellRenderer.getListCellRendererComponent(list,
            getDisplayString(node), index, isSelected, cellHasFocus);
      }
      if (chat.isIgnored(node)) {
        renderer.setIcon(ignoreIcon);
      }
      return renderer;
    });
  }

  private void layoutComponents() {
    setLayout(new BorderLayout());
    add(new JScrollPane(players), BorderLayout.CENTER);
  }

  private void setupListeners() {
    players.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(final MouseEvent e) {
        mouseOnPlayersList(e);
      }

      @Override
      public void mousePressed(final MouseEvent e) {
        mouseOnPlayersList(e);
      }

      @Override
      public void mouseReleased(final MouseEvent e) {
        mouseOnPlayersList(e);
      }
    });
    actionFactories.add(clickedOn -> {
      // you can't slap or ignore yourself
      if (clickedOn.equals(chat.getLocalNode())) {
        return Collections.emptyList();
      }
      final boolean isIgnored = chat.isIgnored(clickedOn);
      final Action ignore = SwingAction.of(isIgnored ? "Stop Ignoring" : "Ignore", e -> {
        chat.setIgnored(clickedOn, !isIgnored);
        repaint();
      });
      final Action slap = SwingAction.of("Slap " + clickedOn.getName(), e -> chat.sendSlap(clickedOn.getName()));
      return Arrays.asList(slap, ignore);
    });
  }

  private void setWidgetActivation() {}

  /**
   * The renderer will be passed in a string.
   */
  public void setPlayerRenderer(final ListCellRenderer<Object> renderer) {
    setCellRenderer = renderer;
    setDynamicPreferredSize();
  }

  private void mouseOnPlayersList(final MouseEvent e) {
    if (!e.isPopupTrigger()) {
      return;
    }
    final int index = players.locationToIndex(e.getPoint());
    if (index == -1) {
      return;
    }
    final INode player = listModel.get(index);
    final JPopupMenu menu = new JPopupMenu();
    boolean hasActions = false;
    for (final IPlayerActionFactory factory : actionFactories) {
      final List<Action> actions = factory.mouseOnPlayer(player);
      if ((actions != null) && !actions.isEmpty()) {
        if (hasActions) {
          menu.addSeparator();
        }
        hasActions = true;
        for (final Action a : actions) {
          menu.add(a);
        }
      }
    }
    if (hasActions) {
      menu.show(players, e.getX(), e.getY());
    }
  }

  @Override
  public synchronized void updatePlayerList(final Collection<INode> players) {
    SwingAction.invokeNowOrLater(() -> {
      listModel.clear();
      for (final INode name : players) {
        if (!hiddenPlayers.contains(name.getName())) {
          listModel.addElement(name);
        }
      }
    });
  }

  @Override
  public void addMessageWithSound(final String message, final String from, final boolean thirdperson,
      final String sound) {}

  @Override
  public void addMessage(final String message, final String from, final boolean thirdperson) {}

  private String getDisplayString(final INode node) {
    if (chat == null) {
      return "";
    }
    String extra = "";
    final String notes = chat.getNotesForNode(node);
    if ((notes != null) && (notes.length() > 0)) {
      extra = extra + notes;
    }
    String status = chat.getStatusManager().getStatus(node);
    final StringBuilder sb = new StringBuilder("");
    if ((status != null) && (status.length() > 0)) {
      if (status.length() > 25) {
        status = status.substring(0, 25);
      }
      for (int i = 0; i < status.length(); i++) {
        final char c = status.charAt(i);
        if ((c >= '\u0300') && (c <= '\u036F')) { // skip combining characters
          continue;
        }
        sb.append(c);
      }
      extra = extra + " (" + sb + ")";
    }
    if (extra.length() == 0) {
      return node.getName();
    }
    return node.getName() + extra;
  }

  @Override
  public void addStatusMessage(final String message) {}

  /**
   * Add an action factory that will be used to populate the pop up meny when
   * right clicking on a player in the chat panel.
   */
  public void addActionFactory(final IPlayerActionFactory actionFactory) {
    actionFactories.add(actionFactory);
  }

  public void remove(final IPlayerActionFactory actionFactory) {
    actionFactories.remove(actionFactory);
  }
}
