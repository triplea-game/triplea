package games.strategy.engine.framework.startup.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Action;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import games.strategy.engine.chat.IChatPanel;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.headlessGameServer.HeadlessGameServer;
import games.strategy.engine.framework.networkMaintenance.BanPlayerAction;
import games.strategy.engine.framework.networkMaintenance.BootPlayerAction;
import games.strategy.engine.framework.networkMaintenance.MutePlayerAction;
import games.strategy.engine.framework.networkMaintenance.SetPasswordAction;
import games.strategy.engine.framework.startup.launcher.ILauncher;
import games.strategy.engine.framework.startup.launcher.ServerLauncher;
import games.strategy.engine.framework.startup.login.ClientLoginValidator;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.framework.startup.mc.IRemoteModelListener;
import games.strategy.engine.framework.startup.mc.ServerModel;
import games.strategy.engine.lobby.client.ui.action.EditGameCommentAction;
import games.strategy.engine.lobby.client.ui.action.RemoveGameFromLobbyAction;
import games.strategy.engine.pbem.PBEMMessagePoster;
import games.strategy.net.IServerMessenger;
import games.strategy.util.ThreadUtil;

/** Setup panel displayed for hosting a non-lobby network game (using host option from main panel). */
public class ServerSetupPanel extends SetupPanel implements IRemoteModelListener {
  private static final long serialVersionUID = -2849872641665561807L;
  private final ServerModel model;
  private JTextField portField;
  private JTextField addressField;
  private JTextField nameField;
  private List<PlayerRow> playerRows = new ArrayList<>();
  private final GameSelectorModel gameSelectorModel;
  private JPanel info;
  private JPanel networkPanel;
  private final InGameLobbyWatcherWrapper lobbyWatcher = new InGameLobbyWatcherWrapper();

  public ServerSetupPanel(final ServerModel model, final GameSelectorModel gameSelectorModel) {
    this.model = model;
    this.gameSelectorModel = gameSelectorModel;
    this.model.setRemoteModelListener(this);
    createLobbyWatcher();
    createComponents();
    layoutComponents();
    setupListeners();
    setWidgetActivation();
    internalPlayerListChanged();
  }

  void createLobbyWatcher() {
    if (lobbyWatcher != null) {
      lobbyWatcher.setInGameLobbyWatcher(InGameLobbyWatcher.newInGameLobbyWatcher(model.getMessenger(), this,
          lobbyWatcher.getInGameLobbyWatcher()));
      lobbyWatcher.setGameSelectorModel(gameSelectorModel);
    }
  }

  public synchronized void repostLobbyWatcher(final IGame iGame) {
    if (iGame != null) {
      return;
    }
    if (canGameStart()) {
      return;
    }
    System.out.println("Restarting lobby watcher");
    shutDownLobbyWatcher();
    ThreadUtil.sleep(1000);
    HeadlessGameServer.resetLobbyHostOldExtensionProperties();
    createLobbyWatcher();
  }

  void shutDownLobbyWatcher() {
    if (lobbyWatcher != null) {
      lobbyWatcher.shutDown();
    }
  }

  private void createComponents() {
    final IServerMessenger messenger = model.getMessenger();
    final Color backGround = new JTextField().getBackground();
    portField = new JTextField("" + messenger.getLocalNode().getPort());
    portField.setEnabled(true);
    portField.setEditable(false);
    portField.setBackground(backGround);
    portField.setColumns(6);
    addressField = new JTextField(messenger.getLocalNode().getAddress().getHostAddress());
    addressField.setEnabled(true);
    addressField.setEditable(false);
    addressField.setBackground(backGround);
    addressField.setColumns(20);
    nameField = new JTextField(messenger.getLocalNode().getName());
    nameField.setEnabled(true);
    nameField.setEditable(false);
    nameField.setBackground(backGround);
    nameField.setColumns(20);
    info = new JPanel();
    networkPanel = new JPanel();
  }

  private void layoutComponents() {
    setLayout(new BorderLayout());
    info.setLayout(new GridBagLayout());
    info.add(new JLabel("Name:"), new GridBagConstraints(0, 0, 1, 1, 0, 0.0, GridBagConstraints.EAST,
        GridBagConstraints.NONE, new Insets(5, 10, 0, 5), 0, 0));
    info.add(new JLabel("Address:"), new GridBagConstraints(0, 1, 1, 1, 0, 0.0, GridBagConstraints.EAST,
        GridBagConstraints.NONE, new Insets(5, 10, 0, 5), 0, 0));
    info.add(new JLabel("Port:"), new GridBagConstraints(0, 2, 1, 1, 0, 0.0, GridBagConstraints.EAST,
        GridBagConstraints.NONE, new Insets(5, 10, 0, 5), 0, 0));
    info.add(nameField, new GridBagConstraints(1, 0, 1, 1, 0.5, 1.0, GridBagConstraints.WEST,
        GridBagConstraints.BOTH, new Insets(5, 0, 0, 5), 0, 0));
    info.add(addressField, new GridBagConstraints(1, 1, 1, 1, 0.5, 1.0, GridBagConstraints.WEST,
        GridBagConstraints.BOTH, new Insets(5, 0, 0, 5), 0, 0));
    info.add(portField, new GridBagConstraints(1, 2, 1, 1, 0.5, 1.0, GridBagConstraints.WEST,
        GridBagConstraints.BOTH, new Insets(5, 0, 0, 5), 0, 0));
    add(info, BorderLayout.NORTH);
  }

  private void layoutPlayers() {
    final JPanel players = new JPanel();
    final GridBagLayout layout = new GridBagLayout();
    players.setLayout(layout);
    final Insets spacing = new Insets(3, 16, 0, 0);
    final Insets lastSpacing = new Insets(3, 16, 0, 16);
    int gridx = 0;
    final boolean disableable = !model.getPlayersAllowedToBeDisabled().isEmpty()
        || model.getPlayersEnabledListing().containsValue(Boolean.FALSE);
    final GridBagConstraints enabledPlayerConstraints = new GridBagConstraints();
    if (disableable) {
      enabledPlayerConstraints.anchor = GridBagConstraints.WEST;
      enabledPlayerConstraints.gridx = gridx++;
      enabledPlayerConstraints.insets = new Insets(3, 20, 0, -10);
    }
    final GridBagConstraints nameConstraints = new GridBagConstraints();
    nameConstraints.anchor = GridBagConstraints.WEST;
    nameConstraints.gridx = gridx++;
    nameConstraints.insets = spacing;
    final GridBagConstraints playerConstraints = new GridBagConstraints();
    playerConstraints.anchor = GridBagConstraints.WEST;
    playerConstraints.gridx = gridx++;
    playerConstraints.insets = spacing;
    final GridBagConstraints localConstraints = new GridBagConstraints();
    localConstraints.anchor = GridBagConstraints.WEST;
    localConstraints.gridx = gridx++;
    localConstraints.insets = spacing;
    final GridBagConstraints typeConstraints = new GridBagConstraints();
    typeConstraints.anchor = GridBagConstraints.WEST;
    typeConstraints.gridx = gridx++;
    typeConstraints.insets = spacing;
    final GridBagConstraints allianceConstraints = new GridBagConstraints();
    allianceConstraints.anchor = GridBagConstraints.WEST;
    allianceConstraints.gridx = gridx++;
    allianceConstraints.insets = lastSpacing;
    if (disableable) {
      final JLabel enableLabel = new JLabel("Use");
      enableLabel.setForeground(Color.black);
      layout.setConstraints(enableLabel, enabledPlayerConstraints);
      players.add(enableLabel);
    }
    final JLabel nameLabel = new JLabel("Name");
    nameLabel.setForeground(Color.black);
    layout.setConstraints(nameLabel, nameConstraints);
    players.add(nameLabel);
    final JLabel playedByLabel = new JLabel("Played by");
    playedByLabel.setForeground(Color.black);
    layout.setConstraints(playedByLabel, playerConstraints);
    players.add(playedByLabel);
    final JLabel localLabel = new JLabel("Local");
    localLabel.setForeground(Color.black);
    layout.setConstraints(localLabel, localConstraints);
    players.add(localLabel);
    final JLabel typeLabel = new JLabel("Type");
    typeLabel.setForeground(Color.black);
    layout.setConstraints(typeLabel, typeConstraints);
    players.add(typeLabel);
    final JLabel allianceLabel = new JLabel("Alliance");
    allianceLabel.setForeground(Color.black);
    layout.setConstraints(allianceLabel, allianceConstraints);
    players.add(allianceLabel);
    final Iterator<PlayerRow> iter = playerRows.iterator();
    if (!iter.hasNext()) {
      final JLabel noPlayers = new JLabel("Load a game file first");
      layout.setConstraints(noPlayers, nameConstraints);
      players.add(noPlayers);
    }
    while (iter.hasNext()) {
      final PlayerRow row = iter.next();
      if (disableable) {
        layout.setConstraints(row.getEnabledPlayer(), enabledPlayerConstraints);
        players.add(row.getEnabledPlayer());
      }
      layout.setConstraints(row.getName(), nameConstraints);
      players.add(row.getName());
      layout.setConstraints(row.getPlayer(), playerConstraints);
      players.add(row.getPlayer());
      layout.setConstraints(row.getLocal(), localConstraints);
      players.add(row.getLocal());
      layout.setConstraints(row.getType(), typeConstraints);
      players.add(row.getType());
      layout.setConstraints(row.getAlliance(), allianceConstraints);
      players.add(row.getAlliance());
    }
    removeAll();
    add(info, BorderLayout.NORTH);
    final JScrollPane scroll = new JScrollPane(players, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    scroll.setBorder(null);
    scroll.setViewportBorder(null);
    add(scroll, BorderLayout.CENTER);
    add(networkPanel, BorderLayout.SOUTH);
    invalidate();
    validate();
  }

  private void setupListeners() {}

  @Override
  public void setWidgetActivation() {}

  @Override
  public void shutDown() {
    model.setRemoteModelListener(IRemoteModelListener.NULL_LISTENER);
    model.shutDown();
    if (lobbyWatcher != null) {
      lobbyWatcher.shutDown();
    }
  }

  @Override
  public void cancel() {
    model.setRemoteModelListener(IRemoteModelListener.NULL_LISTENER);
    model.cancel();
    if (lobbyWatcher != null) {
      lobbyWatcher.shutDown();
    }
  }

  @Override
  public void postStartGame() {
    final GameData data = gameSelectorModel.getGameData();
    data.getProperties().set(PBEMMessagePoster.PBEM_GAME_PROP_NAME, false);
  }

  @Override
  public boolean canGameStart() {
    if (gameSelectorModel.getGameData() == null || model == null) {
      return false;
    }
    final Map<String, String> players = model.getPlayersToNodeListing();
    if (players == null || players.isEmpty()) {
      return false;
    }
    for (final String player : players.keySet()) {
      if (players.get(player) == null) {
        return false;
      }
    }
    // make sure at least 1 player is enabled
    final Map<String, Boolean> someoneEnabled = model.getPlayersEnabledListing();
    for (final boolean bool : someoneEnabled.values()) {
      if (bool) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void playerListChanged() {
    SwingUtilities.invokeLater(() -> internalPlayerListChanged());
  }

  @Override
  public void playersTakenChanged() {
    SwingUtilities.invokeLater(() -> internalPlayersTakenChanged());
  }

  private void internalPlayersTakenChanged() {
    if (!SwingUtilities.isEventDispatchThread()) {
      throw new IllegalStateException("Wrong thread");
    }
    final Map<String, String> playersToNode = model.getPlayersToNodeListing();
    final Map<String, Boolean> playersEnabled = model.getPlayersEnabledListing();
    for (final PlayerRow row : playerRows) {
      row.update(playersToNode, playersEnabled);
    }
    super.notifyObservers();
  }

  private void internalPlayerListChanged() {
    if (!SwingUtilities.isEventDispatchThread()) {
      throw new IllegalStateException("Wrong thread");
    }
    playerRows = new ArrayList<>();
    final Map<String, String> players = model.getPlayersToNodeListing();
    final Map<String, Boolean> playersEnabled = model.getPlayersEnabledListing();
    final Map<String, Collection<String>> m_playerNamesAndAlliancesInTurnOrder =
        model.getPlayerNamesAndAlliancesInTurnOrderLinkedHashMap();
    final Map<String, String> reloadSelections = PlayerID.currentPlayers(gameSelectorModel.getGameData());
    final Set<String> playerNames = m_playerNamesAndAlliancesInTurnOrder.keySet();
    for (final String name : playerNames) {
      final PlayerRow newPlayerRow =
          new PlayerRow(name, reloadSelections, m_playerNamesAndAlliancesInTurnOrder.get(name),
              gameSelectorModel.getGameData().getGameLoader().getServerPlayerTypes());
      playerRows.add(newPlayerRow);
      newPlayerRow.update(players, playersEnabled);
    }
    layoutPlayers();
    internalPlayersTakenChanged();
  }

  class PlayerRow {
    private final JLabel m_nameLabel;
    private final JLabel m_playerLabel;
    private final JCheckBox m_localCheckBox;
    private final JCheckBox m_enabledCheckBox;
    private final JComboBox<String> m_type;
    private JLabel m_alliance;
    private final String[] m_types;

    PlayerRow(final String playerName, final Map<String, String> reloadSelections,
        final Collection<String> playerAlliances, final String[] types) {
      m_nameLabel = new JLabel(playerName);
      m_playerLabel = new JLabel(model.getMessenger().getLocalNode().getName());
      m_localCheckBox = new JCheckBox();
      m_localCheckBox.addActionListener(m_localPlayerActionListener);
      m_localCheckBox.setSelected(true);
      m_enabledCheckBox = new JCheckBox();
      m_enabledCheckBox.addActionListener(m_disablePlayerActionListener);
      // this gets updated later
      m_enabledCheckBox.setSelected(true);
      m_types = types;
      m_type = new JComboBox<>(types);
      String previousSelection = reloadSelections.get(playerName);
      if (previousSelection.equalsIgnoreCase("Client")) {
        previousSelection = types[0];
      }
      if (!(previousSelection.equals("no_one")) && Arrays.asList(types).contains(previousSelection)) {
        m_type.setSelectedItem(previousSelection);
        model.setLocalPlayerType(m_nameLabel.getText(), (String) m_type.getSelectedItem());
      } else if (playerName.startsWith("Neutral") || playerName.startsWith("AI")) {
        // the 4th in the list should be Pro AI (Hard AI)
        m_type.setSelectedItem(types[Math.max(0, Math.min(types.length - 1, 3))]);
        model.setLocalPlayerType(m_nameLabel.getText(), (String) m_type.getSelectedItem());
      }
      if (playerAlliances.contains(playerName)) {
        m_alliance = new JLabel();
      } else {
        m_alliance = new JLabel(playerAlliances.toString());
      }
      m_type.addActionListener(
          e -> model.setLocalPlayerType(m_nameLabel.getText(), (String) m_type.getSelectedItem()));
    }

    public JComboBox<String> getType() {
      return m_type;
    }

    public JLabel getName() {
      return m_nameLabel;
    }

    public JLabel getAlliance() {
      return m_alliance;
    }

    public JLabel getPlayer() {
      return m_playerLabel;
    }

    public JCheckBox getLocal() {
      return m_localCheckBox;
    }

    public JCheckBox getEnabledPlayer() {
      return m_enabledCheckBox;
    }

    public void update(final Map<String, String> playersToNodes, final Map<String, Boolean> playersEnabled) {
      String text = playersToNodes.get(m_nameLabel.getText());
      if (text == null) {
        text = "-";
      }
      m_playerLabel.setText(text);
      m_localCheckBox.setSelected(text.equals(model.getMessenger().getLocalNode().getName()));
      m_enabledCheckBox.setSelected(playersEnabled.get(m_nameLabel.getText()));
      setWidgetActivation();
    }

    private void setWidgetActivation() {
      m_type.setEnabled(m_localCheckBox.isSelected());
      m_nameLabel.setEnabled(m_enabledCheckBox.isSelected());
      m_playerLabel.setEnabled(m_enabledCheckBox.isSelected());
      m_localCheckBox.setEnabled(m_enabledCheckBox.isSelected());
      m_alliance.setEnabled(m_enabledCheckBox.isSelected());
      m_enabledCheckBox.setEnabled(model.getPlayersAllowedToBeDisabled().contains(m_nameLabel.getText()));
    }

    private final ActionListener m_localPlayerActionListener = new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        if (m_localCheckBox.isSelected()) {
          model.takePlayer(m_nameLabel.getText());
        } else {
          model.releasePlayer(m_nameLabel.getText());
        }
        setWidgetActivation();
      }
    };
    private final ActionListener m_disablePlayerActionListener = new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        if (m_enabledCheckBox.isSelected()) {
          model.enablePlayer(m_nameLabel.getText());
          // the 1st in the list should be human
          m_type.setSelectedItem(m_types[0]);
        } else {
          model.disablePlayer(m_nameLabel.getText());
          // the 2nd in the list should be Weak AI
          m_type.setSelectedItem(m_types[Math.max(0, Math.min(m_types.length - 1, 1))]);
        }
        setWidgetActivation();
      }
    };
  }

  @Override
  public IChatPanel getChatPanel() {
    return model.getChatPanel();
  }

  public ServerModel getModel() {
    return model;
  }

  @Override
  public synchronized ILauncher getLauncher() {
    final ServerLauncher launcher = (ServerLauncher) model.getLauncher();
    if (launcher == null) {
      return null;
    }
    launcher.setInGameLobbyWatcher(lobbyWatcher);
    return launcher;
  }

  @Override
  public List<Action> getUserActions() {
    final List<Action> rVal = new ArrayList<>();
    rVal.add(new BootPlayerAction(this, model.getMessenger()));
    rVal.add(new BanPlayerAction(this, model.getMessenger()));
    rVal.add(new MutePlayerAction(this, model.getMessenger()));
    rVal.add(
        new SetPasswordAction(this, lobbyWatcher, (ClientLoginValidator) model.getMessenger().getLoginValidator()));
    if (lobbyWatcher != null && lobbyWatcher.isActive()) {
      rVal.add(new EditGameCommentAction(lobbyWatcher, ServerSetupPanel.this));
      rVal.add(new RemoveGameFromLobbyAction(lobbyWatcher));
    }
    return rVal;
  }

  @Override
  public boolean isMetaSetupPanelInstance() {
    return false;
  }

}
