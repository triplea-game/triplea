package games.strategy.engine.framework.startup.ui;

import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import org.apache.commons.io.FileUtils;
import org.yaml.snakeyaml.Yaml;

import com.google.common.base.Throwables;

import games.strategy.common.swing.SwingComponents;
import games.strategy.debug.ClientLogger;
import games.strategy.engine.ClientContext;
import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.config.GameEngineProperty;
import games.strategy.engine.config.PropertyReader;
import games.strategy.engine.framework.mapDownload.DownloadUtils;
import games.strategy.engine.framework.mapDownload.MapDownloadController;
import games.strategy.engine.framework.startup.mc.SetupPanelModel;
import games.strategy.engine.framework.ui.NewGameChooser;
import games.strategy.engine.framework.ui.background.BackgroundTaskRunner;
import games.strategy.engine.lobby.client.LobbyClient;
import games.strategy.engine.lobby.client.login.LobbyLogin;
import games.strategy.engine.lobby.client.login.LobbyServerProperties;
import games.strategy.engine.lobby.client.ui.LobbyFrame;
import games.strategy.net.DesktopUtilityBrowserLauncher;
import games.strategy.triplea.UrlConstants;
import games.strategy.util.Version;

public class MetaSetupPanel extends SetupPanel {

  private static final long serialVersionUID = 3926503672972937677L;
  private JButton m_startLocal;
  private JButton m_startPBEM;
  private JButton m_hostGame;
  private JButton m_connectToHostedGame;
  private JButton m_connectToLobby;
  private JButton m_enginePreferences;
  private JButton m_downloadMaps;
  private JButton m_ruleBook;
  private JButton m_donate;
  private JButton m_about;

  private final SetupPanelModel m_model;
  private final MapDownloadController mapDownloadController;

  public MetaSetupPanel(final SetupPanelModel model) {
    this.m_model = model;
    this.mapDownloadController = ClientContext.mapDownloadController();

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
    m_downloadMaps = new JButton("Download Maps");
    m_downloadMaps.setToolTipText(
        "<html>Download new maps. Everyone should use this, <br>the best maps are online and have to be downloaded!</html>");
    m_ruleBook = new JButton("Rule Book");
    m_ruleBook.setToolTipText(
        "<html>Download a manual of how to play <br>(it is also included in the directory TripleA was installed to).</html>");
    m_donate = new JButton("Donate");
    m_donate.setToolTipText("Help Support TripleA's development.");
    m_about = new JButton("About");
    m_about.setToolTipText(
        "<html>See info about version number, developers, <br>official website, and a quick instruction on how to play.</html>");
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
    add(m_downloadMaps, new GridBagConstraints(0, 7, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
        new Insets(10, 0, 0, 0), 0, 0));
    add(m_ruleBook, new GridBagConstraints(0, 8, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
        new Insets(10, 0, 0, 0), 0, 0));
    add(m_donate, new GridBagConstraints(0, 9, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
        new Insets(10, 0, 0, 0), 0, 0));
    add(m_about, new GridBagConstraints(0, 10, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
        new Insets(10, 0, 0, 0), 0, 0));
    // top space
    add(new JPanel(), new GridBagConstraints(0, 100, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
        new Insets(00, 0, 0, 0), 0, 0));
  }

  private void setupListeners() {
    m_startLocal.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        m_model.showLocal();
      }
    });
    m_startPBEM.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        m_model.showPBEM();
      }
    });
    m_hostGame.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        m_model.showServer(MetaSetupPanel.this);
      }
    });
    m_connectToHostedGame.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        m_model.showClient(MetaSetupPanel.this);
      }
    });
    m_connectToLobby.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        connectToLobby();
      }
    });
    m_enginePreferences.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        enginePreferences();
      }
    });
    m_downloadMaps.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        downloadMaps();
      }
    });
    m_ruleBook.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        ruleBook();
      }
    });
    m_donate.addActionListener(e -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.PAYPAL_DONATE));
    m_about.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        about();
      }
    });
  }

  private void downloadMaps() {
    JComponent parentWindow = this;
    mapDownloadController.openDownloadMapScreen(parentWindow);
  }

  private static void ruleBook() {
    // We open both the actual rule book, and the web page for all guides.
    // This way we can add other guides and rulebooks and tutorials later, as well as being able to update them after
    // the stable is out.
    SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.SF_RULE_BOOK_PDF);
    SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.SF_WIKI_GUIDES);
  }

  private void enginePreferences() {
    EnginePreferences.showEnginePreferences(this);
  }

  private void about() {
    final String text =
        "<h2>TripleA</h2>" + "<p><b>Engine Version:</b> " + ClientContext.engineVersion()
            + "<br><b>Authors:</b> Sean Bridges, and many others. Current Developers: Veqryn (Chris Duncan)."
            + "<br>TripleA is an open-source game engine, allowing people to play many different games and maps."
            + "<br>For more information please visit:<br>"
            + "<b>Site:</b> <a hlink='http://triplea.sourceforge.net/'>http://triplea.sourceforge.net/</a><br>"
            + "<b>Forum:</b> <a hlink='http://triplea.sourceforge.net/mywiki/Forum'>http://triplea.sourceforge.net/mywiki/Forum</a><br>"
            + "<b>Ladder:</b> <a hlink='http://www.tripleawarclub.org/'>http://www.tripleawarclub.org/</a></p>"
            + "<p><b>Very Basic How to Play:</b>"
            + "<br>Though some games have special rules enforced, most games follow most of these basic guidelines.<br><ol>"
            + "<li>Players start their turn by choosing what they will produce.  They spend the money they gathered during their "
            + "<br>last turn on new units or even technology.  Units are displayed on the purchase screen as having x Cost, and "
            + "<br>their attack/defense/movement values.  These units will be put on the board at the end of the player's turn.</li>"
            + "<li>That Player then does a <em>Combat Move</em>, which means moving units to all the places they wish to attack this "
            + "<br>turn.  Simply click on a unit, then move your mouse to the territory you wish to attack, and then click again "
            + "<br>to drop it there.  You can deselect a unit by right-clicking.  You can select a path for a unit to take by holding "
            + "<br>down 'ctrl' and clicking on all the territories on the way to the final territory.  Pressing shift or ctrl while "
            + "<br>selecting a unit will select all units in that territory.</li>"
            + "<li>Then everyone resolves all the combat battles.  This involves rolling dice for the attacking units and the "
            + "<br>defending units too.  For example, a <em>Tank</em> might attack at a <em>3</em> meaning that when you roll the dice you need "
            + "<br>a 3 or less for him to <em>hit</em> the enemy.  If the tank hits the enemy, then the other player chooses one of his "
            + "<br>units to die, and the battle continues.  After each round of dice, the attacker chooses to retreat or press on "
            + "<br>until he has defeated all enemy units in that territory.  The game rolls the dice for you automatically.</li>"
            + "<li>After this, the Player may move any units that have not yet moved as a <em>Non-Combat</em> move, and any air units "
            + "<br>return to friendly territories to land.</li>"
            + "<li>When the player has completed all of this, then he or she may place the units that they have purchased at the "
            + "<br>beginning of their turn.  Then the game engine counts out the value of the territories they control and gives "
            + "<br>them that much money.  The next nation then begins their turn.  Games last until one side surrenders.</li></ol>"
            + "To see specific rules for each game, click <em>Game Notes</em> from inside that game, "
            + "<br> accessible from the <em>Help</em> menu button at the top of the screen inside a game.</p>";
    final JEditorPane editorPane = new JEditorPane();
    editorPane.setBorder(null);
    editorPane.setBackground(getBackground());
    editorPane.setEditable(false);
    editorPane.setContentType("text/html");
    editorPane.setText(text);
    final JScrollPane scroll = new JScrollPane(editorPane, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    scroll.setBorder(null);
    JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(getParent()), editorPane, "About...",
        JOptionPane.PLAIN_MESSAGE);
  }

  private void connectToLobby() {
    final LobbyServerProperties props = getLobbyServerProperties();

    final LobbyLogin login = new LobbyLogin(JOptionPane.getFrameForComponent(this), props);
    final LobbyClient client = login.login();
    if (client == null) {
      return;
    }
    final LobbyFrame lobbyFrame = new LobbyFrame(client, props);
    NewGameChooser.clearNewGameChooserModel();
    MainFrame.getInstance().setVisible(false);
    MainFrame.getInstance().dispose();
    lobbyFrame.setVisible(true);
  }


  private static Optional<List<Map<String, Object>>> loadYaml(File yamlFile) {
    String yamlContent;
    try {
      yamlContent = new String(Files.readAllBytes(yamlFile.toPath()));
    } catch (IOException e) {
      ClientLogger.logQuietly(e);
      return Optional.empty();
    }
    Yaml yaml = new Yaml();
    List<Map<String, Object>> yamlDataObj = (List<Map<String, Object>>) yaml.load(yamlContent);
    if( yamlDataObj == null ) {
      return Optional.empty();
    } else {
      return Optional.of(yamlDataObj);
    }
  }

  private static LobbyServerProperties getLobbyServerProperties() {
    PropertyReader propReader = ClientContext.propertyReader();
    String urlProp = propReader.readProperty(GameEngineProperty.LOBBY_PROPS_URL);

    File propFile = ClientFileSystemHelper.createTempFile();
    try {
      DownloadUtils.downloadFile(urlProp, propFile);
    } catch (IOException e) {
      ClientLogger.logQuietly("Failed to download lobby server props file: " + urlProp + ", using the backup local property file instead.", e);
    }
    Optional<List<Map<String, Object>>> yamlDataObj = loadYaml(propFile);
    if(!yamlDataObj.isPresent()) {
      // try reading properties from the local file as a backup
      String localFileProp = propReader.readProperty(GameEngineProperty.LOBBY_PROPS_BACKUP_FILE);
      File localFile = new File(ClientFileSystemHelper.getRootFolder(), localFileProp);
      yamlDataObj = loadYaml(propFile);
      if( !yamlDataObj.isPresent()) {
        throw new IllegalStateException("Failed to read lobby properties from both: " + urlProp + ", and: " + localFile.getAbsolutePath());
      }
    }

    Map<String, Object> yamlProps = matchCurrentVersion(yamlDataObj.get());

    LobbyServerProperties lobbyProps = new LobbyServerProperties(yamlProps);
    return lobbyProps;
  }

  private static Map<String, Object> matchCurrentVersion(List<Map<String, Object>> lobbyProps) {
    checkNotNull(lobbyProps);
    Version currentVersion = ClientContext.engineVersion().getCompatabilityVersion();
    for (Map<String, Object> props : lobbyProps) {
      if (props.containsKey("version")) {
        Version otherVersion = new Version((String) props.get("version"));
        if (otherVersion.equals(currentVersion)) {
          return props;
        }
      }
    }

    return lobbyProps.get(0);
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
  public void cancel() {
    // nothing to do
  }
}
