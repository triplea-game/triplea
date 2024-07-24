package games.strategy.engine.chat;

import games.strategy.engine.framework.startup.mc.messages.ModeratorMessage;
import games.strategy.engine.framework.startup.mc.messages.ModeratorPromoted;
import games.strategy.net.IMessageListener;
import games.strategy.net.INode;
import games.strategy.triplea.EngineImageLoader;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
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
import org.triplea.java.StringUtils;
import org.triplea.swing.SwingAction;

/** A UI component that displays the players participating in a chat. */
public class ChatPlayerPanel extends JPanel implements ChatPlayerListener {
  private static final String TAG_MODERATOR = "[Mod]";
  private static final long serialVersionUID = -3153022965393962945L;
  private static final Icon ignoreIcon;

  static {
    ignoreIcon = new ImageIcon(EngineImageLoader.loadImage("images", "ignore.png"));
  }

  private JList<ChatParticipant> players;
  private DefaultListModel<ChatParticipant> listModel;
  private Chat chat;
  // if our renderer is overridden we do not set this directly on the JList,
  // instead we feed it the node name and status as a string
  private ListCellRenderer<Object> setCellRenderer = new DefaultListCellRenderer();
  private final List<IPlayerActionFactory> actionFactories = new ArrayList<>();

  private final BiConsumer<UserName, String> statusUpdateListener = (username, status) -> repaint();

  public ChatPlayerPanel(final Chat chat) {
    createComponents();
    layoutComponents();
    setupListeners();
    setChat(chat);
    chat.addMessengersListener(
        new IMessageListener() {
          @Override
          public void messageReceived(Serializable msg, INode from) {
            if (msg instanceof ModeratorPromoted) {
              String newModerator = ((ModeratorPromoted) msg).getPlayerName();
              for (int i = 0; i < listModel.getSize(); i++) {
                if (listModel.get(i).getUserName().toString().equals(newModerator)) {
                  listModel.get(i).setModerator(true);
                  players.repaint();
                  break;
                }
              }
              repaint();
            }
          }
        });
  }

  /** Sets the chat whose players will be displayed in this panel. */
  public void setChat(final Chat chat) {
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
    if (chat == null) {
      return;
    }
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

          // TODO: add check for if we are moderator
          final Action disconnect =
              SwingAction.of(
                  "Disconnect " + clickedOn.getUserName(),
                  e ->
                      chat.getMessengers()
                          .sendToServer(
                              ModeratorMessage.newDisconnect(clickedOn.getUserName().getValue())));
          // TODO: add check for if we are moderator

          final Action ban =
              SwingAction.of(
                  "Ban " + clickedOn.getUserName(),
                  e ->
                      chat.getMessengers()
                          .sendToServer(
                              ModeratorMessage.newBan(clickedOn.getUserName().getValue())));

          return List.of(slap, ignore, disconnect, ban);
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
          updatedPlayers.stream()
              .sorted(
                  Comparator.comparing(
                      chatParticipant -> chatParticipant.getUserName().getValue().toUpperCase()))
              .forEach(listModel::addElement);
        });
  }

  private String getDisplayString(final ChatParticipant chatParticipant) {
    if (chat == null) {
      return "";
    }

    final String extra = chatParticipant.isModerator() ? " " + TAG_MODERATOR : "";
    final String status = StringUtils.truncate(chat.getStatus(chatParticipant.getUserName()), 25);
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
