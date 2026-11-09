package games.strategy.engine.framework.startup.ui.panels.main;

import games.strategy.engine.chat.ChatPanel;
import games.strategy.engine.framework.I18nEngineFramework;
import games.strategy.engine.framework.startup.ui.LaunchScreenStyle;
import games.strategy.engine.framework.startup.ui.MenuPanel;
import games.strategy.engine.framework.startup.ui.SetupPanel;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameSelectorPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import org.triplea.game.chat.ChatModel;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.SectionHeader;
import org.triplea.swing.SwingAction;
import org.triplea.swing.jpanel.GridBagConstraintsAnchor;
import org.triplea.swing.jpanel.GridBagConstraintsBuilder;
import org.triplea.swing.jpanel.JPanelBuilder;

/**
 * When the game launches, the MainFrame is loaded which will contain the MainPanel. The contents of
 * the MainPanel are swapped out until a new game has been started (TODO: check if the lobby uses
 * MainPanel at all).
 */
public class MainPanel extends JPanel {
  private static final long serialVersionUID = -5548760379892913464L;
  private static final Dimension initialSize = new Dimension(900, 780);
  private static final float NAVIGATION_FONT_INCREASE = 4;

  private final JButton playButton =
      new JButtonBuilder()
          .title(I18nEngineFramework.get().getText("startup.MainPanel.btn.Play.Lbl"))
          .toolTip(I18nEngineFramework.get().getText("startup.MainPanel.btn.Play.Tltp"))
          .build();
  private final JButton backButton =
      new JButtonBuilder()
          .title(I18nEngineFramework.get().getText("startup.MainPanel.btn.Back.Lbl"))
          .build();
  private final Supplier<Optional<Runnable>> backAction;
  private final JLabel startBlockedReason = new JLabel();
  private final Component startBlockedReasonBalance = Box.createRigidArea(new Dimension(0, 0));
  private final String playButtonToolTip;

  private final JPanel gameSetupPanelHolder = new JPanelBuilder().borderLayout().build();
  private final JPanel gameOptionsHolder = new JPanelBuilder().borderLayout().build();
  private final JPanel mainPanel;
  private final JSplitPane chatSplit;
  private final JPanel chatPanelHolder = new JPanelBuilder().height(62).borderLayout().build();
  private SetupPanel gameSetupPanel;
  private final GameSelectorPanel gameSelectorPanel;

  /**
   * MainPanel is the full contents of the 'mainFrame'. This panel represents the welcome screen and
   * subsequent screens.
   */
  MainPanel(
      final Runnable quitAction,
      final GameSelectorPanel gameSelectorPanel,
      final Consumer<MainPanel> launchAction,
      @Nullable final ChatModel chatModel,
      final Supplier<Optional<Runnable>> backAction) {
    this.gameSelectorPanel = gameSelectorPanel;
    this.backAction = backAction;
    playButton.addActionListener(e -> launchAction.accept(this));
    playButtonToolTip = playButton.getToolTipText();
    backButton.addActionListener(e -> backAction.get().ifPresent(Runnable::run));

    final JScrollPane gameSetupPanelScroll = new JScrollPane(gameSetupPanelHolder);
    gameSetupPanelScroll.setBorder(BorderFactory.createEmptyBorder());
    chatSplit = new JSplitPane();
    chatSplit.setOrientation(JSplitPane.VERTICAL_SPLIT);
    chatSplit.setResizeWeight(0.8);
    chatSplit.setOneTouchExpandable(false);
    chatSplit.setDividerSize(5);

    mainPanel =
        new JPanelBuilder()
            .border(0)
            .borderLayout()
            .addNorth(gameSelectorPanel)
            .addWest(gameOptionsHolder)
            .addCenter(gameSetupPanelScroll)
            .build();
    gameOptionsHolder.add(gameSelectorPanel.getGameOptionsPanel(), BorderLayout.CENTER);
    setLayout(new BorderLayout());

    if (chatModel instanceof ChatPanel) {
      addChat((ChatPanel) chatModel);
    } else {
      add(mainPanel, BorderLayout.CENTER);
    }

    final JButton quitButton =
        new JButtonBuilder()
            .title(I18nEngineFramework.get().getText("startup.MainPanel.btn.Quit.Lbl"))
            .toolTip(I18nEngineFramework.get().getText("startup.MainPanel.btn.Quit.Tltp"))
            .actionListener(quitAction)
            .build();
    for (final JButton button : List.of(playButton, backButton, quitButton)) {
      button.setText(button.getText().toUpperCase(Locale.ROOT));
    }
    LaunchScreenStyle.styleAsPrimary(playButton, NAVIGATION_FONT_INCREASE);
    LaunchScreenStyle.styleAsMenuItem(backButton, NAVIGATION_FONT_INCREASE);
    LaunchScreenStyle.styleAsMenuItem(quitButton, NAVIGATION_FONT_INCREASE);
    startBlockedReason.setForeground(UIManager.getColor("Label.disabledForeground"));
    final JPanel navigation =
        new JPanelBuilder().add(playButton).add(backButton).add(quitButton).build();
    // the reason sits left of Play; an equally wide strut on the right keeps the buttons centred
    final JPanel buttonsPanel = new JPanel(new GridBagLayout());
    buttonsPanel.add(
        startBlockedReason,
        new GridBagConstraintsBuilder(0, 0)
            .weightX(1)
            .anchor(GridBagConstraintsAnchor.EAST)
            .build());
    buttonsPanel.add(navigation, new GridBagConstraintsBuilder(1, 0).build());
    buttonsPanel.add(
        startBlockedReasonBalance, new GridBagConstraintsBuilder(2, 0).weightX(1).build());
    buttonsPanel.setBorder(
        BorderFactory.createCompoundBorder(
            SectionHeader.ruleAbove(), BorderFactory.createEmptyBorder(6, 0, 6, 0)));
    add(buttonsPanel, BorderLayout.SOUTH);
    setPreferredSize(initialSize);
    updatePlayButtonState();
    LaunchScreenStyle.install(this);
  }

  public void loadSaveFile(final Path file) {
    gameSelectorPanel.loadSaveFile(file);
  }

  private void addChat(final Component chatComponent) {
    remove(mainPanel);
    remove(chatSplit);
    chatPanelHolder.removeAll();

    chatPanelHolder.setBorder(SectionHeader.ruleAbove());
    chatPanelHolder.add(chatComponent, BorderLayout.CENTER);
    chatSplit.setTopComponent(mainPanel);
    chatSplit.setBottomComponent(chatPanelHolder);
    add(chatSplit, BorderLayout.CENTER);
  }

  /** This method will 'change' screens, swapping out one setup panel for another. */
  public void setSetupPanel(final SetupPanel panel) {
    if (gameSetupPanel != null) {
      gameSetupPanel.setPanelChangedListener(null);
    }
    gameSetupPanel = panel;
    // Note: The listener on the panel itself only updates widgets and doesn't
    // rebuild the UI components (this method), as that would cause focus to be lost.
    panel.setPanelChangedListener(setupPanel -> updatePlayButtonState());
    updatePlayButtonState();

    final boolean isMenu = panel instanceof MenuPanel;
    gameSelectorPanel.setVisible(!isMenu);
    gameOptionsHolder.setVisible(!isMenu);
    playButton.setVisible(!isMenu);
    backButton.setVisible(backAction.get().isPresent());

    gameSetupPanelHolder.removeAll();
    if (isMenu) {
      gameSetupPanelHolder.setBorder(null);
      gameSetupPanelHolder.add(panel, BorderLayout.CENTER);
    } else {
      // staging screens read top-down from the left edge, like the game options beside them
      gameSetupPanelHolder.setBorder(new EmptyBorder(4, 16, 12, 16));
      final JPanel stack = new JPanel(new BorderLayout());
      stack.add(panel, BorderLayout.CENTER);
      final List<Action> actions = gameSetupPanel.getUserActions();
      if (!actions.isEmpty()) {
        final JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        actionsPanel.setBorder(new EmptyBorder(12, 0, 0, 0));
        createUserActionMenu(actionsPanel, actions);
        stack.add(actionsPanel, BorderLayout.SOUTH);
      }
      gameSetupPanelHolder.add(stack, BorderLayout.NORTH);
    }

    Optional.ofNullable(panel.getChatModel())
        .filter(ChatPanel.class::isInstance)
        .map(ChatPanel.class::cast)
        .ifPresentOrElse(
            this::addChat,
            () -> {
              remove(chatSplit);
              add(mainPanel);
            });

    revalidate();
  }

  private static void createUserActionMenu(final JPanel actionsPanel, final List<Action> actions) {
    if (actions.size() == 1) {
      actionsPanel.add(new JButton(actions.get(0)));
      return;
    }
    // if we need this for something other than network, add a way to set it
    final JButton button =
        new JButton(I18nEngineFramework.get().getText("startup.MainPanel.btn.Network.Lbl"));
    button.addActionListener(
        e -> {
          final JPopupMenu menu = new JPopupMenu();
          for (final Action a : actions) {
            menu.add(a);
          }
          menu.show(button, 0, button.getHeight());
        });
    actionsPanel.add(button);
  }

  public void updatePlayButtonState() {
    SwingAction.invokeNowOrLater(
        () -> {
          final boolean canStart = gameSetupPanel != null && gameSetupPanel.canGameStart();
          playButton.setEnabled(canStart);
          final String reason =
              canStart || gameSetupPanel == null || gameSetupPanel instanceof MenuPanel
                  ? ""
                  : gameSetupPanel.getStartBlockedReason().orElse("");
          startBlockedReason.setText(reason);
          final Dimension balance =
              new Dimension(reason.isEmpty() ? 0 : startBlockedReason.getPreferredSize().width, 0);
          startBlockedReasonBalance.setPreferredSize(balance);
          startBlockedReasonBalance.setMinimumSize(balance);
          startBlockedReasonBalance.getParent().revalidate();
          playButton.setToolTipText(reason.isEmpty() ? playButtonToolTip : reason);
        });
  }
}
