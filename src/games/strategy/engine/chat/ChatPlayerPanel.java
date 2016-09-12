package games.strategy.engine.chat;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Image;
import java.awt.image.BufferedImage;
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
import javax.swing.AbstractAction;
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
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import games.strategy.net.INode;
import games.strategy.ui.SwingAction;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Popup;
import javafx.util.Callback;

public class ChatPlayerPanel extends BorderPane implements IChatListener {
  private static final Icon ignoreIcon;
  static {
    final URL ignore = ChatPlayerPanel.class.getResource("ignore.png");
    if (ignore == null) {
      throw new IllegalStateException("Could not find ignore icon");
    }
    BufferedImage img;
    try {
      img = ImageIO.read(ignore);
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
    ignoreIcon = new ImageIcon(img);
  }

  private ListView<INode> players;
  private Chat chat;
  private final Set<String> hiddenPlayers = new HashSet<>();
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
    setChat(chat);
  }

  public void addHiddenPlayerName(final String name) {
    hiddenPlayers.add(name);
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
    if (this.chat != null) {
      this.chat.removeChatListener(this);
    }
    this.chat = chat;
    if (chat != null) {
      chat.addChatListener(this);
    } else {
      // empty our player list
      updatePlayerList(Collections.emptyList());
    }
  }

  /**
   * set minimum size based on players (number and max name length) and distribution to playerIDs
   */
  private void setDynamicPreferredSize() {
    final List<INode> onlinePlayers = chat.getOnlinePlayers();
    int maxNameLength = 0;
    for (final INode iNode : onlinePlayers) {
      maxNameLength = (int) Math.max(maxNameLength, new Text(iNode.getName()).getLayoutBounds().getWidth());
    }
    int iconCounter = 0;
    if (setCellRenderer instanceof PlayerChatRenderer) {
      iconCounter = ((PlayerChatRenderer) setCellRenderer).getMaxIconCounter();
    }
    setPrefWidth(maxNameLength + 40 + iconCounter * 14);
    setPrefHeight(80);
  }

  private void createComponents() {
    players = new ListView<>();
    // TODO with CSS
    players.setCellFactory(new Callback<ListView<INode>, ListCell<INode>>() {

      @Override
      public ListCell<INode> call(ListView<INode> listView) {
        return new ListCell<INode>() {

          @Override
          protected void updateItem(INode node, boolean b) {
            super.updateItem(node, b);
            if (node != null) {
              setText(getDisplayString(node));
              if (chat.isIgnored(node)) {
                node.setIcon(ignoreIcon);
              }
            }
          }

        };
      }
    });
  }

  private void layoutComponents() {
    setCenter(new ScrollPane(players));
  }

  private void setupListeners() {
    players.addEventHandler(MouseEvent.MOUSE_CLICKED, this::mouseOnPlayersList);
    players.addEventHandler(MouseEvent.MOUSE_PRESSED, this::mouseOnPlayersList);
    players.addEventHandler(MouseEvent.MOUSE_RELEASED, this::mouseOnPlayersList);
    actionFactories.add(clickedOn -> {
      // you can't slap or ignore yourself
      if (clickedOn.equals(chat.getLocalNode())) {
        return Collections.emptyList();
      }
      final boolean isIgnored = chat.isIgnored(clickedOn);
      final Action ignore = SwingAction.of(isIgnored ? "Stop Ignoring" : "Ignore", e -> {
        chat.setIgnored(clickedOn, !isIgnored);
        // repaint();
      });
      final Action slap = new AbstractAction("Slap " + clickedOn.getName()) {
        private static final long serialVersionUID = -5514772068903406263L;

        @Override
        public void actionPerformed(final ActionEvent event) {
          chat.sendSlap(clickedOn.getName());
        }
      };
      return Arrays.asList(slap, ignore);
    });
  }

  private void setWidgetActivation() {}

  /**
   * The renderer will be passed in a string
   */
  public void setPlayerRenderer(final ListCellRenderer<Object> renderer) {
    setCellRenderer = renderer;
    setDynamicPreferredSize();
  }

  private void mouseOnPlayersList(final MouseEvent e) {
    if (!e.isPopupTrigger()) {
      return;
    }
    final INode player = players.getSelectionModel().getSelectedItem();
    final Popup menu = new Popup();
    boolean hasActions = false;
    for (final IPlayerActionFactory factory : actionFactories) {
      final List<Action> actions = factory.mouseOnPlayer(player);
      if (actions != null && !actions.isEmpty()) {
        if (hasActions) {
          menu.getContent().add(new Separator());
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

  /**
   * @param players
   *        - a collection of Strings representing player names.
   */
  @Override
  public synchronized void updatePlayerList(final Collection<INode> players) {
    Platform.runLater(() -> this.players.setItems(FXCollections.observableArrayList(players)));
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
    if (notes != null && notes.length() > 0) {
      extra = extra + notes;
    }
    String status = chat.getStatusManager().getStatus(node);
    final StringBuilder statusSB = new StringBuilder("");
    if (status != null && status.length() > 0) {
      if (status.length() > 25) {
        status = status.substring(0, 25);
      }
      for (int i = 0; i < status.length(); i++) {
        final char c = status.charAt(i);
        // skip combining characters
        if (c >= '\u0300' && c <= '\u036F') {
          continue;
        }
        statusSB.append(c);
      }
      extra = extra + " (" + statusSB.toString() + ")";
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
