package games.strategy.engine.framework.startup.ui;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.triplea.client.launch.screens.LaunchScreenWindow;

import games.strategy.engine.config.client.LobbyServerPropertiesFetcher;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.ProcessRunnerUtil;
import games.strategy.engine.framework.startup.mc.SetupPanelModel;
import games.strategy.engine.lobby.client.LobbyClient;
import games.strategy.engine.lobby.client.login.LobbyLogin;
import games.strategy.engine.lobby.client.login.LobbyServerProperties;
import games.strategy.engine.lobby.client.ui.LobbyFrame;
import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.ui.SwingComponents;
import swinglib.JButtonBuilder;
import tools.map.making.MapCreator;

/**
 * This is the main welcome panel with 'play online' button.
 * This panel is just the upper right of the main screen, it does not include the map information
 * nor the 'play' and 'quit' buttons.
 */
public class MetaSetupPanel extends SetupPanel {

  private static final long serialVersionUID = 3926503672972937677L;
  private JButton startLocal;
  private JButton startPbem;
  private JButton hostGame;
  private JButton connectToHostedGame;
  private JButton connectToLobby;
  private JButton enginePreferences;
  private JButton ruleBook;
  private JButton helpButton;

  private final SetupPanelModel model;

  public MetaSetupPanel(final SetupPanelModel model) {
    this.model = model;

    createComponents();
    layoutComponents();
    setupListeners();
    setWidgetActivation();
  }

  private void createComponents() {
    connectToLobby = new JButton("Play Online");
    final Font bigButtonFont = new Font(connectToLobby.getFont().getName(), connectToLobby.getFont().getStyle(),
        connectToLobby.getFont().getSize() + 3);
    connectToLobby.setFont(bigButtonFont);
    connectToLobby.setToolTipText("<html>Find Games Online on the Lobby Server. <br>"
        + "TripleA is MEANT to be played Online against other humans. <br>"
        + "Any other way is not as fun!</html>");
    startLocal = new JButton("Start Local Game");
    startLocal.setToolTipText("<html>Start a game on this computer. <br>"
        + "You can play against a friend sitting besides you (hotseat mode), <br>"
        + "or against one of the AIs.</html>");
    startPbem = new JButton("Start PBEM (Play-By-Email/Forum) Game");
    startPbem.setToolTipText("<html>Starts a game which will be emailed back and forth between all players, <br>"
        + "or be posted to an online forum or message board.</html>");
    hostGame = new JButton("Host Networked Game");
    hostGame.setToolTipText("<html>Hosts a network game, which people can connect to. <br>"
        + "Anyone on a LAN will be able to connect. <br>"
        + "Anyone from the internet can connect as well, but only if the host has configured port forwarding "
        + "correctly.</html>");
    connectToHostedGame = new JButton("Connect to Networked Game");
    connectToHostedGame
        .setToolTipText("<html>Connects to someone's hosted game, <br>so long as you know their IP address.</html>");
    enginePreferences = new JButton("Engine Preferences");
    enginePreferences.setToolTipText("<html>Configure certain options related to the engine.");
    ruleBook = new JButton("Rule Book");
    helpButton = new JButton("Help");
    ruleBook.setToolTipText("<html>Download a manual of how to play <br>"
        + "(it is also included in the directory TripleA was installed to).</html>");
  }

  private void layoutComponents() {
    setLayout(new GridBagLayout());
    // top space
    add(new JPanel(), new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
        new Insets(00, 0, 0, 0), 0, 0));
    add(connectToLobby, new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
        new Insets(10, 0, 0, 0), 0, 0));
    add(startLocal, new GridBagConstraints(0, 2, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
        new Insets(10, 0, 0, 0), 0, 0));
    add(startPbem, new GridBagConstraints(0, 3, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
        new Insets(10, 0, 0, 0), 0, 0));
    add(hostGame, new GridBagConstraints(0, 4, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
        new Insets(10, 0, 0, 0), 0, 0));
    add(connectToHostedGame, new GridBagConstraints(0, 5, 1, 1, 0, 0, GridBagConstraints.CENTER,
        GridBagConstraints.NONE, new Insets(10, 0, 0, 0), 0, 0));
    add(enginePreferences, new GridBagConstraints(0, 6, 1, 1, 0, 0, GridBagConstraints.CENTER,
        GridBagConstraints.NONE, new Insets(10, 0, 0, 0), 0, 0));
    add(ruleBook, new GridBagConstraints(0, 8, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
        new Insets(10, 0, 0, 0), 0, 0));

    final JButton mapCreator = JButtonBuilder.builder()
        .title("Run the Map Creator")
        .actionListener(() -> ProcessRunnerUtil.runClass(MapCreator.class))
        .build();

    add(mapCreator, new GridBagConstraints(0, 9, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
        new Insets(10, 0, 0, 0), 0, 0));

    add(helpButton, new GridBagConstraints(0, 10, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
        new Insets(10, 0, 0, 0), 0, 0));

    // top space
    add(new JPanel(), new GridBagConstraints(0, 100, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
        new Insets(00, 0, 0, 0), 0, 0));

    if (ClientSetting.SHOW_BETA_FEATURES.booleanValue()) {
      final JButton newUiButton = JButtonBuilder.builder()
          .title("Launch new UI")
          .actionListener(LaunchScreenWindow::show)
          .build();
      add(newUiButton, new GridBagConstraints(0, 13, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
          new Insets(10, 0, 0, 0), 0, 0));
    }
  }

  private void setupListeners() {
    startLocal.addActionListener(e -> model.showLocal());
    startPbem.addActionListener(e -> model.showPbem());
    hostGame.addActionListener(e -> model.showServer(MetaSetupPanel.this));
    connectToHostedGame.addActionListener(e -> model.showClient(MetaSetupPanel.this));
    connectToLobby.addActionListener(e -> connectToLobby());
    enginePreferences.addActionListener(e -> ClientSetting.showSettingsWindow());
    ruleBook.addActionListener(e -> ruleBook());
    helpButton.addActionListener(e -> helpPage());
  }

  private static void ruleBook() {
    SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.RULE_BOOK);
  }

  private static void helpPage() {
    SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.GITHUB_HELP);
  }


  private void connectToLobby() {
    final LobbyServerProperties lobbyServerProperties = new LobbyServerPropertiesFetcher().fetchLobbyServerProperties();
    final LobbyLogin login = new LobbyLogin(
        JOptionPane.getFrameForComponent(this),
        lobbyServerProperties);
    final LobbyClient client = login.login();
    if (client == null) {
      return;
    }
    final LobbyFrame lobbyFrame = new LobbyFrame(client, lobbyServerProperties);
    GameRunner.hideMainFrame();
    lobbyFrame.setVisible(true);
  }

  @Override
  public void setWidgetActivation() {
    if ((model == null) || (model.getGameSelectorModel() == null)
        || (model.getGameSelectorModel().getGameData() == null)) {
      startLocal.setEnabled(false);
      startPbem.setEnabled(false);
      hostGame.setEnabled(false);
    } else {
      startLocal.setEnabled(true);
      startPbem.setEnabled(true);
      hostGame.setEnabled(true);
    }
  }

  @Override
  public boolean canGameStart() {
    // we cannot start
    return false;
  }

  @Override
  public void shutDown() {}

  @Override
  public boolean isMetaSetupPanelInstance() {
    return true;
  }

  @Override
  public void cancel() {
    // nothing to do
  }
}
