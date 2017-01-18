package games.strategy.engine.framework.startup.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowEvent;
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
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;

import games.strategy.engine.chat.IChatPanel;
import games.strategy.engine.framework.startup.launcher.ILauncher;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.framework.startup.mc.SetupPanelModel;

/**
 * When the game launches, the MainFrame is loaded which will contain
 * the MainPanel. The contents of the MainPanel are swapped out
 * until a new game has been started (TODO: check if the lobby
 * uses mainpanel at all)
 */
public class MainPanel extends JPanel implements Observer {
  private static final long serialVersionUID = -5548760379892913464L;
  private static final Dimension initialSize = new Dimension(800, 620);

  private JButton playButton;
  private JButton cancelButton;
  private ISetupPanel gameSetupPanel;
  private JPanel gameSetupPanelHolder;
  private JPanel chatPanelHolder;
  private final SetupPanelModel gameTypePanelModel;
  private final JPanel mainPanel = new JPanel();
  private JSplitPane chatSplit;

  private boolean isChatShowing;

  public MainPanel(final SetupPanelModel typePanelModel) {
    gameTypePanelModel = typePanelModel;
    GameSelectorModel gameSelectorModel = typePanelModel.getGameSelectorModel();

    playButton = new JButton("Play");
    playButton.setToolTipText(
        "<html>Start your game! <br>If not enabled, then you must select a way to play your game first: <br>Play Online, or Local Game, or PBEM, or Host Networked.</html>");
    JButton quitButton = new JButton("Quit");
    quitButton.setToolTipText("Close TripleA.");
    cancelButton = new JButton("Cancel");
    cancelButton.setToolTipText("Go back to main screen.");
    GameSelectorPanel gameSelectorPanel = new GameSelectorPanel(gameSelectorModel);
    gameSelectorPanel.setBorder(new EtchedBorder());
    gameSetupPanelHolder = new JPanel();
    gameSetupPanelHolder.setLayout(new BorderLayout());
    JScrollPane gameSetupPanelScroll = new JScrollPane(gameSetupPanelHolder);
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
    playButton.addActionListener(e -> play());
    quitButton.addActionListener(e -> {
      try {
        gameSetupPanel.shutDown();
      } finally {
        MainFrame.getInstance().dispatchEvent(new WindowEvent(MainFrame.getInstance(), WindowEvent.WINDOW_CLOSING));
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
    if (chat != null && !chat.isHeadless()) {
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
      createUserActionMenu(cancelPanel);
      cancelPanel.add(cancelButton);
      gameSetupPanelHolder.add(cancelPanel, BorderLayout.SOUTH);
    }
    final boolean panelHasChat = (gameTypePanelModel.getPanel().getChatPanel() != null);
    if (panelHasChat != isChatShowing) {
      addChat();
    }
    revalidate();
  }

  private void createUserActionMenu(final JPanel cancelPanel) {
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


  private void play() {
    gameSetupPanel.preStartGame();
    final ILauncher launcher = gameTypePanelModel.getPanel().getLauncher();
    if (launcher != null) {
      launcher.launch(this);
    }
    gameSetupPanel.postStartGame();
  }

  private void setWidgetActivation() {
    if (!SwingUtilities.isEventDispatchThread()) {
      SwingUtilities.invokeLater(() -> setWidgetActivation());
      return;
    }
    gameTypePanelModel.setWidgetActivation();
    if (gameSetupPanel != null) {
      playButton.setEnabled(gameSetupPanel.canGameStart());
    } else {
      playButton.setEnabled(false);
    }
  }

  @Override
  public void update(final Observable o, final Object arg) {
    setWidgetActivation();
  }
}
