package org.triplea.client.launch;

import java.awt.Dimension;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.ClientContext;
import games.strategy.engine.chat.Chat;
import games.strategy.engine.chat.ChatPanel;
import games.strategy.engine.config.client.LobbyServerPropertiesFetcher;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.data.properties.IEditableProperty;
import games.strategy.engine.framework.GameDataManager;
import games.strategy.engine.framework.GameObjectStreamFactory;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.ProcessRunnerUtil;
import games.strategy.engine.framework.map.download.DownloadMapsWindow;
import games.strategy.engine.framework.startup.login.ClientLoginValidator;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.framework.ui.NewGameChooser;
import games.strategy.engine.framework.ui.NewGameChooserEntry;
import games.strategy.engine.lobby.client.LobbyClient;
import games.strategy.engine.lobby.client.login.LobbyLogin;
import games.strategy.engine.lobby.client.login.LobbyServerProperties;
import games.strategy.engine.lobby.client.ui.LobbyFrame;
import games.strategy.engine.message.ChannelMessenger;
import games.strategy.engine.message.RemoteMessenger;
import games.strategy.engine.message.unifiedmessenger.UnifiedMessenger;
import games.strategy.net.ServerMessenger;
import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.ui.SwingComponents;
import swinglib.GridBagHelper;
import swinglib.JButtonBuilder;
import swinglib.JCheckBoxBuilder;
import swinglib.JLabelBuilder;
import swinglib.JPanelBuilder;
import swinglib.JSplitPaneBuilder;
import swinglib.JTabbedPaneBuilder;
import swinglib.JTextFieldBuilder;
import tools.map.making.MapCreator;

public enum LaunchScreen {
  INSTANCE;

  private JFrame frame;

  public static void show() {
    synchronized (INSTANCE) {
      if (INSTANCE.frame == null) {
        INSTANCE.frame = new JFrame("TripleA - " + ClientContext.engineVersion().getExactVersion());
        INSTANCE.frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        INSTANCE.frame.setIconImage(GameRunner.getGameIcon(INSTANCE.frame));
      }
      INSTANCE.showInitialScreen();
      INSTANCE.frame.setSize(900, 700);
      INSTANCE.frame.setMinimumSize(new Dimension(400, 500));

      INSTANCE.frame.setLocationRelativeTo(null);
      INSTANCE.frame.setVisible(true);
      INSTANCE.frame.toFront();
    }
  }

  private void showInitialScreen() {
    replaceContentPaneContents(
        JPanelBuilder.builder()
            .addCenter(JPanelBuilder.builder()
                .flowLayout()
                .add(
                    JPanelBuilder.builder()
                        .verticalBoxLayout()
                        .add(Box.createVerticalStrut(40))
                        .add(JButtonBuilder.builder()
                            .title("Play Multi-Player")
                            .actionListener(INSTANCE::showMultiplayer)
                            .build())
                        .add(Box.createVerticalStrut(40))
                        .add(JButtonBuilder.builder()
                            .title("Play Singe Player")
                            .actionListener(INSTANCE::showGameHostingScreen)
                            .build())
                        .add(Box.createVerticalStrut(40))
                        .add(JButtonBuilder.builder()
                            .title("Game Rules & Help")
                            .actionListener(
                                () -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.GITHUB_HELP))
                            .build())
                        .add(Box.createVerticalStrut(40))
                        .add(JButtonBuilder.builder()
                            .title("More Options")
                            .actionListener(INSTANCE::showMoreOptions)
                            .build())
                        .add(Box.createVerticalGlue())
                        .horizontalAlignmentCenter()
                        .build())
                .build())
            .addSouth(buttonPanel())
            .build());
  }

  private void replaceContentPaneContents(
      final JComponent component) {
    final Dimension size = frame.getSize();
    frame.getContentPane().removeAll();
    frame.getContentPane().add(component);
    frame.setSize(size);
    frame.pack();
    frame.setSize(size);
  }


  private JPanel buttonPanel() {
    return buttonPanel(null);
  }

  private JPanel buttonPanel(final Runnable backAction) {
    return buttonPanel(backAction, null);
  }

  private JPanel buttonPanel(final Runnable backAction, final Runnable playAction) {
    return JPanelBuilder.builder()
        .borderEtched()
        .flowLayout()
        .add(JButtonBuilder.builder()
            .biggerFont()
            .title("Play")
            .actionListener(playAction == null ? () -> {
            } : playAction)
            .enabled(playAction != null)
            .build())
        .add(Box.createHorizontalStrut(50))
        .add(JButtonBuilder.builder()
            .title("Back")
            .actionListener(backAction == null ? () -> {
            } : backAction)
            .enabled(backAction != null)
            .build())
        .add(Box.createHorizontalStrut(50))
        .add(JButtonBuilder.builder()
            .title("Quit")
            .actionListener(() -> {
              frame.dispose();
            })
            .build())
        .add(Box.createVerticalGlue())
        .build();
  }



  private void showMoreOptions() {
    replaceContentPaneContents(
        JPanelBuilder.builder()
            .addCenter(
                JPanelBuilder.builder()
                    .flowLayout()
                    .add(
                        JPanelBuilder.builder()
                            .verticalBoxLayout()
                            .add(Box.createVerticalStrut(40))
                            .add(JButtonBuilder.builder()
                                .title("Settings")
                                .actionListener(ClientSetting::showSettingsWindow)
                                .build())
                            .add(Box.createVerticalStrut(40))
                            .add(JButtonBuilder.builder()
                                .title("Map Tools")
                                .actionListener(() -> ProcessRunnerUtil.runClass(MapCreator.class))
                                .build())
                            .add(Box.createVerticalGlue())
                            .build())
                    .build())
            .addSouth(buttonPanel(INSTANCE::showInitialScreen))
            .build());
  }

  private void showMultiplayer() {
    replaceContentPaneContents(
        JPanelBuilder.builder()
            .addCenter(
                JPanelBuilder.builder()
                    .flowLayout()
                    .add(
                        JPanelBuilder.builder()
                            .verticalBoxLayout()
                            .add(Box.createVerticalStrut(40))
                            .add(JButtonBuilder.builder()
                                .title("Play Online")
                                .actionListener(() -> {
                                  final LobbyServerProperties lobbyServerProperties =
                                      new LobbyServerPropertiesFetcher().fetchLobbyServerProperties();
                                  final LobbyLogin login = new LobbyLogin(
                                      JOptionPane.getFrameForComponent(null),
                                      lobbyServerProperties);
                                  final LobbyClient client = login.login();
                                  if (client == null) {
                                    return;
                                  }
                                  final LobbyFrame lobbyFrame = new LobbyFrame(client, lobbyServerProperties);
                                  GameRunner.hideMainFrame();
                                  lobbyFrame.setVisible(true);
                                  INSTANCE.frame.dispose();
                                })
                                .build())
                            .add(Box.createVerticalStrut(40))
                            .add(JButtonBuilder.builder()
                                .title("Play By Email or Forum")
                                .actionListener(() -> {
                                })
                                .build())
                            .add(Box.createVerticalStrut(40))
                            .add(JButtonBuilder.builder()
                                .title("Direct Network Game")
                                .actionListener(INSTANCE::showDirectNetwork)
                                .build())
                            .add(Box.createVerticalGlue())
                            .build())
                    .build())
            .addSouth(buttonPanel(INSTANCE::showInitialScreen))
            .build());
  }

  private void showDirectNetwork() {
    replaceContentPaneContents(
        JPanelBuilder.builder()
            .addCenter(
                JPanelBuilder.builder()
                    .flowLayout()
                    .add(
                        JPanelBuilder.builder()
                            .verticalBoxLayout()
                            .add(Box.createVerticalStrut(30))
                            .add(JButtonBuilder.builder()
                                .title("Host a Network Game")
                                .actionListener(INSTANCE::showHostingDirectNetworkSetup)
                                .build())
                            .add(Box.createVerticalStrut(30))
                            .add(JButtonBuilder.builder()
                                .title("Join a Network Game")
                                .actionListener(INSTANCE::showJoiningDirectNetworkGame)
                                .build())
                            .add(Box.createVerticalGlue())
                            .build())
                    .build())
            .addSouth(buttonPanel(INSTANCE::showInitialScreen))
            .build());
  }


  private void showHostingDirectNetworkSetup() {
    replaceContentPaneContents(
        JPanelBuilder.builder()
            .addNorth(
                JPanelBuilder.builder()
                    .verticalBoxLayout()
                    .add(Box.createVerticalStrut(30))
                    .add(
                        JPanelBuilder.builder()
                            .withGridBagHelper(2)
                            .addEach(
                                new JLabel(""),
                                new JLabel("Server Options"))
                            .addEach(
                                new JLabel("Name"),
                                JTextFieldBuilder.builder()
                                    .text(ClientSetting.PLAYER_NAME.value())
                                    .columns(12)
                                    .build())
                            .addEach(
                                new JLabel("Port"),
                                JTextFieldBuilder.builder()
                                    .text(GameRunner.PORT)
                                    .columns(5)
                                    .build())
                            .addEach(
                                new JLabel("Require Password"),
                                JCheckBoxBuilder.builder()
                                    .build())
                            .addEach(
                                new JLabel("Password"),
                                JTextFieldBuilder.builder()
                                    .columns(12)
                                    .build())
                            .build())
                    .add(Box.createVerticalStrut(10))
                    .add(JButtonBuilder.builder()
                        .title("Host")
                        .actionListener(INSTANCE::showGameHostingScreen) // TODO
                        .build())
                    .build())
            .addSouth(buttonPanel(INSTANCE::showDirectNetwork))
            .build());
  }

  private void showJoiningDirectNetworkGame() {
    replaceContentPaneContents(
        JPanelBuilder.builder()
            .addNorth(
                JPanelBuilder.builder()
                    .verticalBoxLayout()
                    .add(Box.createVerticalStrut(30))
                    .add(
                        JPanelBuilder.builder()
                            .withGridBagHelper(2)
                            .addEach(
                                new JLabel(""),
                                new JLabel("Client Options"))
                            .addEach(
                                new JLabel("Name"),
                                JTextFieldBuilder.builder()
                                    .text(ClientSetting.PLAYER_NAME.value())
                                    .columns(12)
                                    .build())
                            .addEach(
                                new JLabel("Server Address"),
                                JTextFieldBuilder.builder()
                                    .text("") // TODO: save and store last used, also add a drop down box of successfully used hosts
                                    .columns(12)
                                    .build())
                            .addEach(
                                new JLabel("Server Port"),
                                JTextFieldBuilder.builder()
                                    .text(GameRunner.PORT)
                                    .columns(5)
                                    .build())
                            .build())
                    .add(Box.createVerticalStrut(10))
                    .add(JButtonBuilder.builder()
                        .title("Connect")
                        .actionListener(INSTANCE::showGameHostingScreen) // TODO
                        .build())
                    .build())
            .addSouth(buttonPanel(INSTANCE::showDirectNetwork))
            .build());

  }


  private void showGameHostingScreen() {

    showGameHostingScreen(null);
  }
  private void showGameHostingScreen(GameData data) {
    if (data == null && ClientSetting.SELECTED_GAME_LOCATION.isSet()) {
      try {
        data = new NewGameChooserEntry(new URI(ClientSetting.SELECTED_GAME_LOCATION.value())).getGameData();
      } catch (final Exception e) {
        ClientLogger.logError("Failed to load: " + ClientSetting.SELECTED_GAME_LOCATION.value(), e);
        ClientSetting.SELECTED_GAME_LOCATION.save(ClientSetting.SELECTED_GAME_LOCATION.defaultValue);
        ClientSetting.flush();
      }
    }

    final GameData gameData = data;
    final GameSelectorModel model = new GameSelectorModel();


    final JPanel chatPanel;

    final GameObjectStreamFactory objectStreamFactory = new GameObjectStreamFactory(data);
    try {
      final ServerMessenger serverMessenger = new ServerMessenger("admin", 3303, objectStreamFactory);

      final ClientLoginValidator clientLoginValidator = new ClientLoginValidator(serverMessenger);
//      clientLoginValidator.setGamePassword(props.getPassword());
      serverMessenger.setLoginValidator(clientLoginValidator);
//      serverMessenger.addErrorListener(this);
//      serverMessenger.addConnectionChangeListener(this);
      final UnifiedMessenger unifiedMessenger = new UnifiedMessenger(serverMessenger);
      final RemoteMessenger remoteMessenger = new RemoteMessenger(unifiedMessenger);
//      remoteMessenger.registerRemote(m_serverStartupRemote,
//          new RemoteName(
//              "games.strategy.engine.framework.ui.ServerStartup.SERVER_REMOTE",
//              IServerStartupRemote.class));
      final ChannelMessenger channelMessenger = new ChannelMessenger(unifiedMessenger);


      chatPanel =
          new ChatPanel(serverMessenger, channelMessenger, remoteMessenger, LaunchScreen.class.getName(),
              Chat.CHAT_SOUND_PROFILE.GAME_CHATROOM);
    } catch(final Exception e) {
      ClientLogger.logError("Failed to host game", e);
      return;
    }

    
    replaceContentPaneContents(
        JPanelBuilder.builder()
            .addNorth(JPanelBuilder.builder()
                .flowLayout()
                .add(JButtonBuilder.builder()
                    .title("Select Map")
                    .actionListener(() -> {
                      final NewGameChooserEntry entry =
                          NewGameChooser.chooseGame(JOptionPane.getFrameForComponent(null), model.getGameName());
                      if (entry != null) {
                        ClientSetting.SELECTED_GAME_LOCATION.save(entry.getLocation());
                        try {
                          showGameHostingScreen(entry.fullyParseGameData());
                        } catch (GameParseException e) {
                          ClientLogger.logError("Failed to parse: " + entry.getLocation(), e);
                        }
                      }
                    })
                    .build())
                .add(JButtonBuilder.builder()
                    .title("Download Maps")
                    .actionListener(DownloadMapsWindow::showDownloadMapsWindow)
                    .build())
                .add(JButtonBuilder.builder()
                    .title("Open Save Game")
                    .actionListener(() -> {
                      GameRunner.showSaveGameFileChooser().ifPresent(file -> {
                        ClientSetting.SELECTED_GAME_LOCATION.save(file.getAbsolutePath());
                        try {
                          GameData newData = GameDataManager.loadGame(file);
                          showGameHostingScreen(newData);
                        } catch (IOException e) {
                          ClientLogger.logError("Failed to load: " + file.getAbsolutePath(), e);
                        }
                      });

                    })
                    .build())
                .add(Box.createVerticalGlue())
                .borderEtched()
                .build())
            .addCenterIf(gameData != null, () -> mapSetup(gameData))
            .addSouth(
                JPanelBuilder.builder()
                .addCenter(chatPanel)
                .addSouth(
                        buttonPanel(INSTANCE::showInitialScreen, () -> {
                        }))
                    .build())
            .build());
  }

  private static JPanel mapSetup(final GameData gameData) {
    final GameSelectorModel model = new GameSelectorModel();
    model.load(gameData, "");
    // backup current game properties before showing dialog


    return JPanelBuilder.builder()
        .addNorth(
            JPanelBuilder.builder()
                .flowLayout()
                .borderEtched()
                .add(
                    JPanelBuilder.builder()
                        .verticalBoxLayout()
                        .add(new JLabel("<html><h1>" + gameData.getGameName() + " </h1></html>"))
                        .add(new JLabel("Version: " + gameData.getGameVersion()))
                        .add(new JLabel("Round: " + gameData.getSequence().getRound()))
                        .build())
                .build())
        .addCenter(
            JSplitPaneBuilder.builder()
                .dividerLocation(240)
                .addLeft(SwingComponents.newJScrollPane(mapOptionsPanel(gameData)))
                .addRight(
                    JTabbedPaneBuilder.builder()
                        .addTab("Player Selection",
                            SwingComponents.newJScrollPane(
                                JPanelBuilder.builder()
                                    .verticalBoxLayout()
                                    .addEach(playerSelectionPanel(gameData))
                                    .build()))
                        .addTab("Handicap",
                            SwingComponents.newJScrollPane(
                                JPanelBuilder.builder()
                                    .verticalBoxLayout()
                                    .addEach(bidSelectionPanel(gameData))
                                    .build()))
                        .build())
                .build())
        .build();
  }


  private static JPanel mapOptionsPanel(final GameData gameData) {

    final GameProperties properties = gameData.getProperties();
    final List<IEditableProperty> props = properties.getEditableProperties();

    final JPanel panel = JPanelBuilder.builder()
        .gridBagLayout()
        .build();

    final GridBagHelper gridBagHelper = new GridBagHelper(panel, 2);

    gridBagHelper.add(new JLabel("<html><h2>Map Options</h2></html>"));
    gridBagHelper.add(new JPanel());
    props.stream()
        .filter(prop -> !prop.getName().contains("bid"))
        .forEach(prop -> {
          final String truncated =
              prop.getName().length() > 25 ? prop.getName().substring(0, 25) + "..." : prop.getName();

          gridBagHelper.add(JLabelBuilder.builder()
              .textWithMaxLength(truncated, 25)
              .tooltip(prop.getName())
              .build());
          gridBagHelper.add(prop.getEditorComponent());
        });
    return panel;
  }

  private static List<JPanel> playerSelectionPanel(final GameData gameData) {


    final List<JPanel> selectionPanels = new ArrayList<>();
    final Set<String> alliances = gameData.getAllianceTracker().getAlliances();



    final Map<String, Map<PlayerID, JComboBox<String>>> allianceMap = new HashMap<>();
    for (final String alliance : alliances) {
      final Map<PlayerID, JComboBox<String>> comboBoxMap = new HashMap<>();
      for (final PlayerID player : gameData.getAllianceTracker().getPlayersInAlliance(alliance)) {
        comboBoxMap.put(player, new JComboBox<>(gameData.getGameLoader().getServerPlayerTypes()));
      }
      allianceMap.put(alliance, comboBoxMap);
    }



    for (final String alliance : alliances) {
      final JPanel allianceSelection = JPanelBuilder.builder()
          .borderEtched()
          .build();
      final GridBagHelper gridBagHelper = new GridBagHelper(allianceSelection, 3);
      gridBagHelper.add(new JLabel("<html><h2>" + alliance + "</h2></html"));

      gridBagHelper.add(
          JButtonBuilder.builder()
              .title("Set " + alliance + " to Human")
              .actionListener(() -> {
                allianceMap.get(alliance).forEach((playerId, box) -> box.setSelectedItem("Human"));
              })
              .build());

      gridBagHelper.add(
          JButtonBuilder.builder()
              .title("Set " + alliance + " to AI")
              .actionListener(
                  () -> allianceMap.get(alliance).forEach((playerId, box) -> box.setSelectedItem("Hard (AI)")))
              .build());



      for (final PlayerID player : gameData.getAllianceTracker().getPlayersInAlliance(alliance)) {


        gridBagHelper.add(new JLabel(player.getName()));

        final JComboBox<String> comboBox = allianceMap.get(alliance).get(player);
        comboBox.setSelectedItem("Hard (AI)");
        gridBagHelper.add(comboBox);

        gridBagHelper.add(new JLabel(""));
      }
      selectionPanels.add(allianceSelection);
    }
    return selectionPanels;
  }

  private static List<JComponent> bidSelectionPanel(final GameData gameData) {

    final List<JComponent> selectionPanels = new ArrayList<>();
    final Set<String> alliances = gameData.getAllianceTracker().getAlliances();



    final Map<String, Map<PlayerID, JComboBox<String>>> allianceMap = new HashMap<>();
    for (final String alliance : alliances) {
      final Map<PlayerID, JComboBox<String>> comboBoxMap = new HashMap<>();
      for (final PlayerID player : gameData.getAllianceTracker().getPlayersInAlliance(alliance)) {
        comboBoxMap.put(player, new JComboBox<>(gameData.getGameLoader().getServerPlayerTypes()));
      }
      allianceMap.put(alliance, comboBoxMap);
    }


    for (final String alliance : alliances) {
      final JPanel allianceSelection = JPanelBuilder.builder()
          .borderEtched()
          .gridBagLayout()
          .build();
      final GridBagHelper gridBagHelper = new GridBagHelper(allianceSelection, 4);
      gridBagHelper.add(new JLabel("<html><h2>" + alliance + "</h2></html"));
      gridBagHelper.add(new JLabel("<html><b>Bid</b></html"));
      gridBagHelper.add(new JLabel("<html><b>Income<br />Bonus</b></html"));
      gridBagHelper.add(new JLabel("<html><b>Income<br />Multiplier</b></html"));


      for (final PlayerID player : gameData.getAllianceTracker().getPlayersInAlliance(alliance)) {


        gridBagHelper.add(new JLabel(player.getName()));


        final JTextField bid = new JTextField("0", 3);
        gridBagHelper.add(
            JPanelBuilder.builder()
                .flowLayout()
                .add(bid)
                .add(Box.createVerticalGlue())
                .build());

        final JTextField income = new JTextField("0", 3);
        gridBagHelper.add(
            JPanelBuilder.builder()
                .flowLayout()
                .add(income)
                .add(Box.createVerticalGlue())
                .build());

        final JTextField multiplier = new JTextField("100%", 5);
        gridBagHelper.add(
            JPanelBuilder.builder()
                .flowLayout()
                .add(multiplier)
                .add(Box.createVerticalGlue())
                .build());
      }
      selectionPanels.add(allianceSelection);
    }
    return selectionPanels;
  }



}
