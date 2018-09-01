package games.strategy.engine.framework.startup.ui.panels.main;

import java.awt.BorderLayout;
import java.awt.Component;
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

import games.strategy.engine.chat.IChatPanel;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.startup.ui.ISetupPanel;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameSelectorPanel;
import games.strategy.ui.SwingAction;
import swinglib.GridBagHelper;
import swinglib.JButtonBuilder;
import swinglib.JPanelBuilder;

/**
 * When the game launches, the MainFrame is loaded which will contain
 * the MainPanel. The contents of the MainPanel are swapped out
 * until a new game has been started (TODO: check if the lobby
 * uses mainpanel at all)
 */
public class MainPanel extends JPanel implements Observer, ScreenChangeListener {
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
  private ISetupPanel gameSetupPanel;
  private boolean isChatShowing;
  private final Supplier<Optional<IChatPanel>> chatPanelSupplier;

  /**
   * MainPanel is the full contents of the 'mainFrame'. This panel represents the
   * welcome screen and subsequent screens..
   */
  MainPanel(
      final GameSelectorPanel gameSelectorPanel,
      final Consumer<MainPanel> launchAction,
      final Supplier<Optional<IChatPanel>> chatPanelSupplier,
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
    final IChatPanel chat = chatPanelSupplier.get().orElse(null);
    if (chat != null && !chat.isHeadless()) {
      chatPanelHolder.add((Component) chat, BorderLayout.CENTER);
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
  public void screenChangeEvent(final ISetupPanel panel) {
    gameSetupPanel = panel;
    gameSetupPanelHolder.removeAll();
    gameSetupPanelHolder.add(panel.getDrawable(), BorderLayout.CENTER);
    panel.addObserver(this);
    setWidgetActivation();
    // add the cancel button if we are not choosing the type.
    if (panel.showCancelButton()) {
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

  private static void createUserActionMenu(final ISetupPanel gameSetupPanel, final JPanel cancelPanel) {
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
