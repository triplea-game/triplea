package games.strategy.engine.chat;

import com.google.common.base.Ascii;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
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
import org.triplea.domain.data.ChatParticipant;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.lobby.chat.messages.server.StatusUpdate;
import org.triplea.swing.SwingAction;

/** A UI component that displays the players participating in a chat. */
public class ChatPlayerPanel extends JPanel implements ChatPlayerListener {
  private static final String TAG_MODERATOR = "[Mod]";
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

  private JList<ChatParticipant> players;
  private DefaultListModel<ChatParticipant> listModel;
  private Chat chat;
  // if our renderer is overridden we do not set this directly on the JList,
  // instead we feed it the node name and status as a string
  private ListCellRenderer<Object> setCellRenderer = new DefaultListCellRenderer();
  private final List<IPlayerActionFactory> actionFactories = new ArrayList<>();
  private final Consumer<StatusUpdate> statusUpdateListener = status -> repaint();

  public ChatPlayerPanel(final Chat chat) {
    createComponents();
    layoutComponents();
    setupListeners();
    setChat(chat);
  }

  /** Sets the chat whose players will be displayed in this panel. */
  public void setChat(final Chat chat) {
    if (this.chat != null) {
      this.chat.removeChatListener(this);
      this.chat.removeStatusUpdateListener(statusUpdateListener);
    }
    this.chat = chat;
    if (chat != null) {
      chat.addChatListener(this);
      chat.addStatusUpdateListener(statusUpdateListener);
    } else {
      // empty our player list
      updatePlayerList(List.of());
    }
    repaint();
  }

  /**
   * set minimum size based on players (number and max name length) and distribution to playerIDs.
   */
  private void setDynamicPreferredSize() {
    int maxNameLength = 0;
    final FontMetrics fontMetrics = this.getFontMetrics(UIManager.getFont("TextField.font"));
    for (final UserName onlinePlayer : chat.getOnlinePlayers()) {
      maxNameLength = Math.max(maxNameLength, fontMetrics.stringWidth(onlinePlayer.getValue()));
    }
    int iconCounter = 0;
    if (setCellRenderer instanceof PlayerChatRenderer) {
      iconCounter = ((PlayerChatRenderer) setCellRenderer).getMaxIconCounter();
    }
    setPreferredSize(new Dimension(maxNameLength + 40 + iconCounter * 14, 80));
  }

  private void createComponents() {
    listModel = new DefaultListModel<>();
    players = new JList<>(listModel);
    players.setFocusable(false);
    players.setCellRenderer(
        (list, node, index, isSelected, cellHasFocus) -> {
          if (setCellRenderer == null) {
            return new JLabel();
          }
          final DefaultListCellRenderer renderer;
          if (setCellRenderer instanceof PlayerChatRenderer) {
            renderer =
                (DefaultListCellRenderer)
                    setCellRenderer.getListCellRendererComponent(
                        list, node, index, isSelected, cellHasFocus);
          } else {
            renderer =
                (DefaultListCellRenderer)
                    setCellRenderer.getListCellRendererComponent(
                        list, getDisplayString(node), index, isSelected, cellHasFocus);
          }
          if (chat.isIgnored(node.getUserName())) {
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
    players.addMouseListener(
        new MouseAdapter() {
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
    actionFactories.add(
        clickedOn -> {
          // you can't slap or ignore yourself
          if (clickedOn.getUserName().equals(chat.getLocalUserName())) {
            return List.of();
          }
          final boolean isIgnored = chat.isIgnored(clickedOn.getUserName());
          final Action ignore =
              SwingAction.of(
                  isIgnored ? "Stop Ignoring" : "Ignore",
                  e -> {
                    chat.setIgnored(clickedOn.getUserName(), !isIgnored);
                    repaint();
                  });
          final Action slap =
              SwingAction.of(
                  "Slap " + clickedOn.getUserName(), e -> chat.sendSlap(clickedOn.getUserName()));
          return List.of(slap, ignore);
        });
  }

  /** The renderer will be passed in a string. */
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
    final ChatParticipant player = listModel.get(index);
    final JPopupMenu menu = new JPopupMenu();
    boolean hasActions = false;
    for (final IPlayerActionFactory factory : actionFactories) {
      final List<Action> actions = factory.mouseOnPlayer(player);
      if (actions != null && !actions.isEmpty()) {
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
  public synchronized void updatePlayerList(final Collection<ChatParticipant> updatedPlayers) {
    SwingAction.invokeNowOrLater(
        () -> {
          listModel.clear();
          updatedPlayers.forEach(listModel::addElement);
        });
  }

  private String getDisplayString(final ChatParticipant chatParticipant) {
    if (chat == null) {
      return "";
    }

    final String extra = chatParticipant.isModerator() ? " " + TAG_MODERATOR : "";
    final String status = Ascii.truncate(chat.getStatus(chatParticipant.getUserName()), 25, "");
    final String suffix = status.isEmpty() ? "" : " (" + status + ")";

    return chatParticipant.getUserName() + extra + suffix;
  }

  /**
   * Add an action factory that will be used to populate the pop up many when right clicking on a
   * player in the chat panel.
   */
  public void addActionFactory(final IPlayerActionFactory actionFactory) {
    actionFactories.add(actionFactory);
  }
}
