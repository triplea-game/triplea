package games.strategy.engine.framework.startup.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

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
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.framework.startup.mc.SetupPanelModel;
import games.strategy.ui.SwingAction;

/**
 * When the game launches, the MainFrame is loaded which will contain
 * the MainPanel. The contents of the MainPanel are swapped out
 * until a new game has been started (TODO: check if the lobby
 * uses mainpanel at all)
 */
public class MainPanel extends JPanel implements Observer {
  private static final long serialVersionUID = -5548760379892913464L;
  private static final Dimension initialSize = new Dimension(800, 620);

  private final JButton playButton;
  private final JButton cancelButton;
  private ISetupPanel gameSetupPanel;
  private final JPanel gameSetupPanelHolder;
  private JPanel chatPanelHolder;
  private final SetupPanelModel gameTypePanelModel;
  private final JPanel mainPanel = new JPanel();
  private final JSplitPane chatSplit;

  private boolean isChatShowing;

  /**
   * MainPanel is the full contents of the 'mainFrame'. This panel represents the
   * welcome screen and subsequent screens..
   */
  public MainPanel(final SetupPanelModel typePanelModel) {
    gameTypePanelModel = typePanelModel;
    final GameSelectorModel gameSelectorModel = typePanelModel.getGameSelectorModel();

    playButton = new JButton("Play");
    playButton.setToolTipText("<html>Start your game! <br>"
        + "If not enabled, then you must select a way to play your game first: <br>"
        + "Play Online, or Local Game, or PBEM, or Host Networked.</html>");
    final JButton quitButton = new JButton("Quit");
    quitButton.setToolTipText("Close TripleA.");
    cancelButton = new JButton("Cancel");
    cancelButton.setToolTipText("Go back to main screen.");
    final GameSelectorPanel gameSelectorPanel = new GameSelectorPanel(gameSelectorModel);
    gameSelectorPanel.setBorder(new EtchedBorder());
    gameSetupPanelHolder = new JPanel();
    gameSetupPanelHolder.setLayout(new BorderLayout());
    final JScrollPane gameSetupPanelScroll = new JScrollPane(gameSetupPanelHolder);
    gameSetupPanelScroll.setBorder(BorderFactory.createEmptyBorder());
    chatPanelHolder = new JPanel();
    chatPanelHolder.setLayout(new BorderLayout());
    chatSplit = new JSplitPane();
    chatSplit.setOrientation(JSplitPane.VERTICAL_SPLIT);
    chatSplit.setResizeWeight(0.8);
    chatSplit.setOneTouchExpandable(false);
    chatSplit.setDividerSize(5);

    final JPanel buttonsPanel = new JPanel();
    buttonsPanel.setBorder(new EtchedBorder());
    buttonsPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
    buttonsPanel.add(playButton);
    buttonsPanel.add(quitButton);
    setLayout(new BorderLayout());
    mainPanel.setLayout(new GridBagLayout());
    mainPanel.setBorder(BorderFactory.createEmptyBorder());
    gameSetupPanelHolder.setLayout(new BorderLayout());
    mainPanel.add(gameSelectorPanel, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.VERTICAL, new Insets(00, 0, 0, 0), 0, 0));
    mainPanel.add(gameSetupPanelScroll, new GridBagConstraints(1, 0, 1, 1, 1, 1, GridBagConstraints.CENTER,
        GridBagConstraints.BOTH, new Insets(00, 0, 0, 0), 0, 0));
    addChat();
    add(buttonsPanel, BorderLayout.SOUTH);
    setPreferredSize(initialSize);

    gameTypePanelModel.addObserver((o, arg) -> setGameSetupPanel(gameTypePanelModel.getPanel()));
    playButton.addActionListener(e -> play(gameSetupPanel, gameTypePanelModel, this));
    quitButton.addActionListener(e -> {
      try {
        gameSetupPanel.shutDown();
      } finally {
        GameRunner.quitGame();
      }
    });
    cancelButton.addActionListener(e -> gameTypePanelModel.showSelectType());
    gameSelectorModel.addObserver(this);


    setWidgetActivation();
    if (typePanelModel.getPanel() != null) {
      setGameSetupPanel(typePanelModel.getPanel());
    }
  }

  private void addChat() {
    remove(mainPanel);
    remove(chatSplit);
    chatPanelHolder.removeAll();
    final IChatPanel chat = gameTypePanelModel.getPanel().getChatPanel();
    if ((chat != null) && !chat.isHeadless()) {
      chatPanelHolder = new JPanel();
      chatPanelHolder.setLayout(new BorderLayout());
      chatPanelHolder.setPreferredSize(new Dimension(chatPanelHolder.getPreferredSize().width, 62));

      chatPanelHolder.add((Component) chat, BorderLayout.CENTER);

      chatSplit.setTopComponent(mainPanel);
      chatSplit.setBottomComponent(chatPanelHolder);

      add(chatSplit, BorderLayout.CENTER);
    } else {
      add(mainPanel, BorderLayout.CENTER);
    }
    isChatShowing = chat != null;
  }

  private void setGameSetupPanel(final ISetupPanel panel) {
    SetupPanel setupPanel = null;
    if (SetupPanel.class.isAssignableFrom(panel.getClass())) {
      setupPanel = (SetupPanel) panel;
    }
    if (gameSetupPanel != null) {
      gameSetupPanel.removeObserver(this);
      if (setupPanel != null) {
        gameSetupPanelHolder.remove(setupPanel);
      }
    }
    gameSetupPanel = panel;
    gameSetupPanelHolder.removeAll();
    if (setupPanel != null) {
      gameSetupPanelHolder.add(setupPanel, BorderLayout.CENTER);
    }
    panel.addObserver(this);
    setWidgetActivation();
    // add the cancel button if we are not choosing the type.
    if (!(panel.isMetaSetupPanelInstance())) {
      final JPanel cancelPanel = new JPanel();
      cancelPanel.setBorder(new EmptyBorder(10, 0, 10, 10));
      cancelPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
      createUserActionMenu(gameSetupPanel, cancelPanel);
      cancelPanel.add(cancelButton);
      gameSetupPanelHolder.add(cancelPanel, BorderLayout.SOUTH);
    }
    final boolean panelHasChat = (gameTypePanelModel.getPanel().getChatPanel() != null);
    if (panelHasChat != isChatShowing) {
      addChat();
    }
    revalidate();
  }

  private static void createUserActionMenu(final ISetupPanel gameSetupPanel, final JPanel cancelPanel) {
    if (gameSetupPanel.getUserActions().isEmpty()) {
      return;
    }
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


  private static void play(final ISetupPanel gameSetupPanel, final SetupPanelModel gameTypePanelModel,
      final MainPanel mainPanel) {
    gameSetupPanel.preStartGame();

    gameTypePanelModel.getPanel().getLauncher()
        .ifPresent(launcher -> launcher.launch(mainPanel));

    gameSetupPanel.postStartGame();
  }

  private void setWidgetActivation() {
    SwingAction.invokeNowOrLater(() -> {
      gameTypePanelModel.setWidgetActivation();
      playButton.setEnabled((gameSetupPanel != null) && gameSetupPanel.canGameStart());
    });
  }

  @Override
  public void update(final Observable o, final Object arg) {
    setWidgetActivation();
  }
}
