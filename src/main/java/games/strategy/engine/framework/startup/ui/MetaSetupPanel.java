package games.strategy.engine.framework.startup.ui;

import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.yaml.snakeyaml.Yaml;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.ClientContext;
import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.config.GameEnginePropertyReader;
import games.strategy.engine.framework.map.download.DownloadUtils;
import games.strategy.engine.framework.startup.mc.SetupPanelModel;
import games.strategy.engine.lobby.client.LobbyClient;
import games.strategy.engine.lobby.client.login.LobbyLogin;
import games.strategy.engine.lobby.client.login.LobbyServerProperties;
import games.strategy.engine.lobby.client.ui.LobbyFrame;
import games.strategy.triplea.UrlConstants;
import games.strategy.ui.SwingComponents;
import games.strategy.util.Version;

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
    add(helpButton, new GridBagConstraints(0, 9, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
        new Insets(10, 0, 0, 0), 0, 0));
    // top space
    add(new JPanel(), new GridBagConstraints(0, 100, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
        new Insets(00, 0, 0, 0), 0, 0));
  }

  private void setupListeners() {
    startLocal.addActionListener(e -> model.showLocal());
    startPbem.addActionListener(e -> model.showPBEM());
    hostGame.addActionListener(e -> model.showServer(MetaSetupPanel.this));
    connectToHostedGame.addActionListener(e -> model.showClient(MetaSetupPanel.this));
    connectToLobby.addActionListener(e -> connectToLobby());
    enginePreferences.addActionListener(e -> enginePreferences());
    ruleBook.addActionListener(e -> ruleBook());
    helpButton.addActionListener(e -> helpPage());
  }

  private static void ruleBook() {
    SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.RULE_BOOK);
  }

  private static void helpPage() {
    SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.GITHUB_HELP);
  }



  private void enginePreferences() {
    EnginePreferences.showEnginePreferences(this);
  }


  private void connectToLobby() {
    final LobbyServerProperties props = getLobbyServerProperties();

    final LobbyLogin login = new LobbyLogin(JOptionPane.getFrameForComponent(this), props);
    final LobbyClient client = login.login();
    if (client == null) {
      return;
    }
    final LobbyFrame lobbyFrame = new LobbyFrame(client, props);
    MainFrame.getInstance().setVisible(false);
    MainFrame.getInstance().dispose();
    lobbyFrame.setVisible(true);
  }


  private static Optional<List<Map<String, Object>>> loadYaml(final File yamlFile) {
    String yamlContent;
    try {
      yamlContent = new String(Files.readAllBytes(yamlFile.toPath()));
    } catch (final IOException e) {
      ClientLogger.logQuietly("Failed to read from: " + yamlFile.getAbsolutePath(), e);
      return Optional.empty();
    }
    final Yaml yaml = new Yaml();
    @SuppressWarnings("unchecked")
    final List<Map<String, Object>> yamlDataObj = (List<Map<String, Object>>) yaml.load(yamlContent);
    if (yamlDataObj == null) {
      return Optional.empty();
    } else {
      return Optional.of(yamlDataObj);
    }
  }

  private static LobbyServerProperties getLobbyServerProperties() {
    final GameEnginePropertyReader propReader = ClientContext.gameEnginePropertyReader();
    final String urlProp = ClientContext.gameEnginePropertyReader().readLobbyPropertiesUrl();
    Optional<List<Map<String, Object>>> yamlDataObj = loadRemoteLobbyServerProperties(urlProp);
    if (!yamlDataObj.isPresent()) {
      // try reading properties from the local file as a backup
      final String localFileProp = propReader.readLobbyPropertiesBackupFile();
      final File localFile = new File(ClientFileSystemHelper.getRootFolder(), localFileProp);
      yamlDataObj = loadYaml(localFile);
      if (!yamlDataObj.isPresent()) {
        throw new IllegalStateException(
            "Failed to read lobby properties from both: " + urlProp + ", and: " + localFile.getAbsolutePath());
      }
    }

    final Map<String, Object> yamlProps = matchCurrentVersion(yamlDataObj.get());

    return new LobbyServerProperties(yamlProps);
  }

  private static Optional<List<Map<String, Object>>> loadRemoteLobbyServerProperties(final String lobbyPropsUrl) {
    final File file = ClientFileSystemHelper.createTempFile();
    try {
      try {
        DownloadUtils.downloadToFile(lobbyPropsUrl, file);
      } catch (final IOException e) {
        ClientLogger.logQuietly(
            String.format(
                "Failed to download lobby server props file (%s); using the backup local property file instead.",
                lobbyPropsUrl),
            e);
      }
      return loadYaml(file);
    } finally {
      file.delete();
    }
  }

  private static Map<String, Object> matchCurrentVersion(final List<Map<String, Object>> lobbyProps) {
    checkNotNull(lobbyProps);
    final Version currentVersion = ClientContext.engineVersion().getVersion();

    final Optional<Map<String, Object>> matchingVersionProps = lobbyProps.stream()
        .filter(props -> currentVersion.equals(props.get("version")))
        .findFirst();
    return matchingVersionProps.orElse(lobbyProps.get(0));
  }


  @Override
  public void setWidgetActivation() {
    if (model == null || model.getGameSelectorModel() == null
        || model.getGameSelectorModel().getGameData() == null) {
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
