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
import games.strategy.engine.config.GameEngineProperty;
import games.strategy.engine.config.PropertyReader;
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
  private JButton m_startLocal;
  private JButton m_startPBEM;
  private JButton m_hostGame;
  private JButton m_connectToHostedGame;
  private JButton m_connectToLobby;
  private JButton m_enginePreferences;
  private JButton m_ruleBook;
  private JButton m_helpButton;

  private final SetupPanelModel m_model;

  public MetaSetupPanel(final SetupPanelModel model) {
    this.m_model = model;

    createComponents();
    layoutComponents();
    setupListeners();
    setWidgetActivation();
  }

  private void createComponents() {
    m_connectToLobby = new JButton("Play Online");
    final Font bigButtonFont = new Font(m_connectToLobby.getFont().getName(), m_connectToLobby.getFont().getStyle(),
        m_connectToLobby.getFont().getSize() + 3);
    m_connectToLobby.setFont(bigButtonFont);
    m_connectToLobby.setToolTipText(
        "<html>Find Games Online on the Lobby Server. <br>TripleA is MEANT to be played Online against other humans. <br>Any other way is not as fun!</html>");
    m_startLocal = new JButton("Start Local Game");
    m_startLocal.setToolTipText(
        "<html>Start a game on this computer. <br>You can play against a friend sitting besides you (hotseat mode), <br>or against one of the AIs.</html>");
    m_startPBEM = new JButton("Start PBEM (Play-By-Email/Forum) Game");
    m_startPBEM.setToolTipText(
        "<html>Starts a game which will be emailed back and forth between all players, <br>or be posted to an online forum or message board.</html>");
    m_hostGame = new JButton("Host Networked Game");
    m_hostGame.setToolTipText(
        "<html>Hosts a network game, which people can connect to. <br>Anyone on a LAN will be able to connect. <br>Anyone from the internet can connect as well, but only if the host has configured port forwarding correctly.</html>");
    m_connectToHostedGame = new JButton("Connect to Networked Game");
    m_connectToHostedGame
        .setToolTipText("<html>Connects to someone's hosted game, <br>so long as you know their IP address.</html>");
    m_enginePreferences = new JButton("Engine Preferences");
    m_enginePreferences.setToolTipText("<html>Configure certain options related to the engine.");
    m_ruleBook = new JButton("Rule Book");
    m_helpButton = new JButton("Help");
    m_ruleBook.setToolTipText(
        "<html>Download a manual of how to play <br>(it is also included in the directory TripleA was installed to).</html>");
  }

  private void layoutComponents() {
    setLayout(new GridBagLayout());
    // top space
    add(new JPanel(), new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
        new Insets(00, 0, 0, 0), 0, 0));
    add(m_connectToLobby, new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
        new Insets(10, 0, 0, 0), 0, 0));
    add(m_startLocal, new GridBagConstraints(0, 2, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
        new Insets(10, 0, 0, 0), 0, 0));
    add(m_startPBEM, new GridBagConstraints(0, 3, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
        new Insets(10, 0, 0, 0), 0, 0));
    add(m_hostGame, new GridBagConstraints(0, 4, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
        new Insets(10, 0, 0, 0), 0, 0));
    add(m_connectToHostedGame, new GridBagConstraints(0, 5, 1, 1, 0, 0, GridBagConstraints.CENTER,
        GridBagConstraints.NONE, new Insets(10, 0, 0, 0), 0, 0));
    add(m_enginePreferences, new GridBagConstraints(0, 6, 1, 1, 0, 0, GridBagConstraints.CENTER,
        GridBagConstraints.NONE, new Insets(10, 0, 0, 0), 0, 0));
    add(m_ruleBook, new GridBagConstraints(0, 8, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
        new Insets(10, 0, 0, 0), 0, 0));
    add(m_helpButton, new GridBagConstraints(0, 9, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
        new Insets(10, 0, 0, 0), 0, 0));
    // top space
    add(new JPanel(), new GridBagConstraints(0, 100, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
        new Insets(00, 0, 0, 0), 0, 0));
  }

  private void setupListeners() {
    m_startLocal.addActionListener(e -> m_model.showLocal());
    m_startPBEM.addActionListener(e -> m_model.showPBEM());
    m_hostGame.addActionListener(e -> m_model.showServer(MetaSetupPanel.this));
    m_connectToHostedGame.addActionListener(e -> m_model.showClient(MetaSetupPanel.this));
    m_connectToLobby.addActionListener(e -> connectToLobby());
    m_enginePreferences.addActionListener(e -> enginePreferences());
    m_ruleBook.addActionListener(e -> ruleBook());
    m_helpButton.addActionListener(e -> helpPage());
  }

  private static void ruleBook() {
    SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.RULE_BOOK);
  }

  private static void helpPage() {
    SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.WEBSITE_HELP);
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
    final PropertyReader propReader = ClientContext.propertyReader();
    final String urlProp = propReader.readProperty(GameEngineProperty.LOBBY_PROPS_URL);

    final File propFile = ClientFileSystemHelper.createTempFile();
    try {
      DownloadUtils.downloadFile(urlProp, propFile);
    } catch (final IOException e) {
      ClientLogger.logQuietly(
          "Failed to download lobby server props file: " + urlProp + ", using the backup local property file instead.",
          e);
    }
    Optional<List<Map<String, Object>>> yamlDataObj = loadYaml(propFile);
    if (!yamlDataObj.isPresent()) {
      // try reading properties from the local file as a backup
      final String localFileProp = propReader.readProperty(GameEngineProperty.LOBBY_PROPS_BACKUP_FILE);
      final File localFile = new File(ClientFileSystemHelper.getRootFolder(), localFileProp);
      yamlDataObj = loadYaml(localFile);
      if (!yamlDataObj.isPresent()) {
        throw new IllegalStateException(
            "Failed to read lobby properties from both: " + urlProp + ", and: " + localFile.getAbsolutePath());
      }
    }

    final Map<String, Object> yamlProps = matchCurrentVersion(yamlDataObj.get());

    final LobbyServerProperties lobbyProps = new LobbyServerProperties(yamlProps);
    return lobbyProps;
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
    if (m_model == null || m_model.getGameSelectorModel() == null
        || m_model.getGameSelectorModel().getGameData() == null) {
      m_startLocal.setEnabled(false);
      m_startPBEM.setEnabled(false);
      m_hostGame.setEnabled(false);
    } else {
      m_startLocal.setEnabled(true);
      m_startPBEM.setEnabled(true);
      m_hostGame.setEnabled(true);
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
