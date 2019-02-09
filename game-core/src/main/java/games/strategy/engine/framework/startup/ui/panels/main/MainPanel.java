package games.strategy.engine.framework.startup.ui.panels.main;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;

import org.triplea.game.chat.ChatModel;
import org.triplea.swing.SwingAction;

import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.startup.ui.SetupPanel;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameSelectorPanel;
import swinglib.GridBagHelper;
import swinglib.JButtonBuilder;
import swinglib.JPanelBuilder;

/**
 * When the game launches, the MainFrame is loaded which will contain the MainPanel. The contents of the MainPanel are
 * swapped out until a new game has been started (TODO: check if the lobby uses MainPanel at all).
 */
public class MainPanel extends JPanel implements Observer, Consumer<SetupPanel> {
  private static final long serialVersionUID = -5548760379892913464L;
  private static final Dimension initialSize = new Dimension(800, 620);

  private final JButton playButton = JButtonBuilder.builder()
      .title("Play")
      .toolTip("<html>Start your game! <br>"
          + "If not enabled, then you must select a way to play your game first: <br>"
          + "Play Online, or Local Game, or PBEM, or Host Networked.</html>")
      .build();
  private final JButton cancelButton = JButtonBuilder.builder()
      .title("Cancel")
      .build();

  private final JPanel gameSetupPanelHolder = JPanelBuilder.builder()
      .borderLayout()
      .build();
  private final JPanel mainPanel;
  private final JSplitPane chatSplit;
  private final JPanel chatPanelHolder = JPanelBuilder.builder()
      .borderLayout()
      .preferredHeight(62)
      .build();
  private SetupPanel gameSetupPanel;
  private boolean isChatShowing;
  private final Supplier<Optional<ChatModel>> chatPanelSupplier;

  /**
   * MainPanel is the full contents of the 'mainFrame'. This panel represents the welcome screen and subsequent screens.
   */
  MainPanel(
      final GameSelectorPanel gameSelectorPanel,
      final Consumer<MainPanel> launchAction,
      final Supplier<Optional<ChatModel>> chatPanelSupplier,
      final Runnable cancelAction) {
    this.chatPanelSupplier = chatPanelSupplier;
    playButton.addActionListener(e -> launchAction.accept(this));
    cancelButton.addActionListener(e -> cancelAction.run());

    gameSelectorPanel.setBorder(new EtchedBorder());
    final JScrollPane gameSetupPanelScroll = new JScrollPane(gameSetupPanelHolder);
    gameSetupPanelScroll.setBorder(BorderFactory.createEmptyBorder());
    chatSplit = new JSplitPane();
    chatSplit.setOrientation(JSplitPane.VERTICAL_SPLIT);
    chatSplit.setResizeWeight(0.8);
    chatSplit.setOneTouchExpandable(false);
    chatSplit.setDividerSize(5);

    mainPanel = JPanelBuilder.builder()
        .borderEmpty()
        .gridBagLayout(2)
        .add(gameSelectorPanel, GridBagHelper.Anchor.WEST, GridBagHelper.Fill.VERTICAL)
        .add(gameSetupPanelScroll, GridBagHelper.Anchor.CENTER, GridBagHelper.Fill.VERTICAL_AND_HORIZONTAL)
        .build();

    setLayout(new BorderLayout());
    addChat();

    final JButton quitButton = JButtonBuilder.builder()
        .title("Quit")
        .toolTip("Close TripleA.")
        .actionListener(GameRunner::quitGame)
        .build();
    final JPanel buttonsPanel = JPanelBuilder.builder()
        .borderEtched()
        .flowLayout(JPanelBuilder.FlowLayoutJustification.CENTER)
        .add(playButton)
        .add(quitButton)
        .build();
    add(buttonsPanel, BorderLayout.SOUTH);
    setPreferredSize(initialSize);
    setWidgetActivation();
  }

  private void addChat() {
    remove(mainPanel);
    remove(chatSplit);
    chatPanelHolder.removeAll();
    final ChatModel chat = chatPanelSupplier.get().orElse(null);
    if (chat instanceof SetupPanel) {
      chatPanelHolder.add((SetupPanel) chat, BorderLayout.CENTER);
      chatSplit.setTopComponent(mainPanel);
      chatSplit.setBottomComponent(chatPanelHolder);
      add(chatSplit, BorderLayout.CENTER);
    } else {
      add(mainPanel, BorderLayout.CENTER);
    }
    isChatShowing = chat != null;
  }

  /**
   * This method will 'change' screens, swapping out one setup panel for another.
   */
  @Override
  public void accept(final SetupPanel panel) {
    gameSetupPanel = panel;
    gameSetupPanelHolder.removeAll();
    gameSetupPanelHolder.add(panel, BorderLayout.CENTER);
    panel.addObserver(this);
    setWidgetActivation();
    // add the cancel button if we are not choosing the type.
    if (panel.isCancelButtonVisible()) {
      final JPanel cancelPanel = new JPanel();
      cancelPanel.setBorder(new EmptyBorder(10, 0, 10, 10));
      cancelPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
      if (!gameSetupPanel.getUserActions().isEmpty()) {
        createUserActionMenu(gameSetupPanel, cancelPanel);
      }
      cancelPanel.add(cancelButton);
      gameSetupPanelHolder.add(cancelPanel, BorderLayout.SOUTH);
    }
    final boolean panelHasChat = chatPanelSupplier.get().isPresent();
    if (panelHasChat != isChatShowing) {
      addChat();
    }
    revalidate();
  }

  private static void createUserActionMenu(final SetupPanel gameSetupPanel, final JPanel cancelPanel) {
    // if we need this for something other than network, add a way to set it
    final JButton button = new JButton("Network...");
    button.addActionListener(e -> {
      final JPopupMenu menu = new JPopupMenu();
      final List<Action> actions = gameSetupPanel.getUserActions();
      for (final Action a : actions) {
        menu.add(a);
      }
      menu.show(button, 0, button.getHeight());
    });
    cancelPanel.add(button);
  }

  private void setWidgetActivation() {
    SwingAction.invokeNowOrLater(() -> playButton.setEnabled(gameSetupPanel != null && gameSetupPanel.canGameStart()));
  }

  @Override
  public void update(final Observable o, final Object arg) {
    setWidgetActivation();
  }
}
