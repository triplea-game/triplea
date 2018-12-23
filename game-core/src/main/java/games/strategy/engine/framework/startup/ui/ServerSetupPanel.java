package games.strategy.engine.framework.startup.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.swing.Action;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import games.strategy.engine.chat.IChatPanel;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.framework.network.ui.BanPlayerAction;
import games.strategy.engine.framework.network.ui.BootPlayerAction;
import games.strategy.engine.framework.network.ui.MutePlayerAction;
import games.strategy.engine.framework.network.ui.SetPasswordAction;
import games.strategy.engine.framework.startup.launcher.ILauncher;
import games.strategy.engine.framework.startup.login.ClientLoginValidator;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.framework.startup.mc.IRemoteModelListener;
import games.strategy.engine.framework.startup.mc.ServerModel;
import games.strategy.engine.lobby.client.ui.action.EditGameCommentAction;
import games.strategy.engine.lobby.client.ui.action.RemoveGameFromLobbyAction;
import games.strategy.engine.pbem.IEmailSender;
import games.strategy.engine.pbem.IForumPoster;
import games.strategy.engine.pbem.PbemMessagePoster;
import games.strategy.engine.random.IRemoteDiceServer;
import games.strategy.net.IServerMessenger;
import games.strategy.net.OpenFileUtility;
import games.strategy.triplea.UrlConstants;
import games.strategy.util.ExitStatus;

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
    internalPlayerListChanged();
  }

  private void createLobbyWatcher() {
    final InGameLobbyWatcher.LobbyWatcherHandler handler = new InGameLobbyWatcher.LobbyWatcherHandler() {
      @Override
      public void reportError(final String message) {
        SwingUtilities.invokeLater(() -> {
          final Frame parentComponent = JOptionPane.getFrameForComponent(ServerSetupPanel.this);
          if (JOptionPane.showConfirmDialog(parentComponent,
              message + "\nDo you want to view the tutorial on how to host? This will open in your internet browser.",
              "View Help Website?", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            OpenFileUtility.openUrl(UrlConstants.HOSTING_GUIDE.toString());
          }
          ExitStatus.FAILURE.exit();
        });
      }

      @Override
      public String getSupportEmail() {
        return "";
      }

      @Override
      public boolean isPlayer() {
        return true;
      }
    };
    lobbyWatcher.setInGameLobbyWatcher(InGameLobbyWatcher.newInGameLobbyWatcher(model.getMessenger(), handler,
        lobbyWatcher.getInGameLobbyWatcher()));
    lobbyWatcher.setGameSelectorModel(gameSelectorModel);
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
    allianceConstraints.gridx = gridx;
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
    if (playerRows.isEmpty()) {
      final JLabel noPlayers = new JLabel("Load a game file first");
      layout.setConstraints(noPlayers, nameConstraints);
      players.add(noPlayers);
    }
    for (final PlayerRow row : playerRows) {
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

  @Override
  public void cancel() {
    model.setRemoteModelListener(IRemoteModelListener.NULL_LISTENER);
    model.cancel();
    lobbyWatcher.shutDown();
  }

  @Override
  public void postStartGame() {
    ISetupPanel.clearPbfPbemInformation(gameSelectorModel.getGameData().getProperties());
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
    SwingUtilities.invokeLater(this::internalPlayerListChanged);
  }

  @Override
  public void playersTakenChanged() {
    SwingUtilities.invokeLater(this::internalPlayersTakenChanged);
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
    final Map<String, Collection<String>> playerNamesAndAlliancesInTurnOrder =
        model.getPlayerNamesAndAlliancesInTurnOrderLinkedHashMap();
    final Map<String, String> reloadSelections = PlayerId.currentPlayers(gameSelectorModel.getGameData());
    final Set<String> playerNames = playerNamesAndAlliancesInTurnOrder.keySet();
    for (final String name : playerNames) {
      final PlayerRow newPlayerRow =
          new PlayerRow(name, reloadSelections, playerNamesAndAlliancesInTurnOrder.get(name));
      playerRows.add(newPlayerRow);
      newPlayerRow.update(players, playersEnabled);
    }
    layoutPlayers();
    internalPlayersTakenChanged();
  }

  class PlayerRow {
    private final JLabel nameLabel;
    private final JLabel playerLabel;
    private final JCheckBox localCheckBox;
    private final JCheckBox enabledCheckBox;
    private final JComboBox<String> type;
    private final JLabel alliance;

    PlayerRow(final String playerName, final Map<String, String> reloadSelections,
        final Collection<String> playerAlliances) {
      nameLabel = new JLabel(playerName);
      playerLabel = new JLabel(model.getMessenger().getLocalNode().getName());
      localCheckBox = new JCheckBox();
      localCheckBox.addActionListener(localPlayerActionListener);
      localCheckBox.setSelected(true);
      enabledCheckBox = new JCheckBox();
      enabledCheckBox.addActionListener(disablePlayerActionListener);
      // this gets updated later
      enabledCheckBox.setSelected(true);
      final String[] playerTypes = PlayerType.playerTypes();
      type = new JComboBox<>(playerTypes);
      String previousSelection = reloadSelections.get(playerName);
      if (previousSelection.equalsIgnoreCase("Client")) {
        previousSelection = playerTypes[0];
      }
      if (!previousSelection.equals("no_one") && Arrays.asList(playerTypes).contains(previousSelection)) {
        type.setSelectedItem(previousSelection);
        model.setLocalPlayerType(nameLabel.getText(), PlayerType.fromLabel((String) type.getSelectedItem()));
      } else if (playerName.startsWith("Neutral") || playerName.startsWith("AI")) {
        // the 4th in the list should be Pro AI (Hard AI)
        type.setSelectedItem(PlayerType.PRO_AI.getLabel());
        model.setLocalPlayerType(nameLabel.getText(), PlayerType.PRO_AI);
      }
      if (playerAlliances.contains(playerName)) {
        alliance = new JLabel();
      } else {
        alliance = new JLabel(playerAlliances.toString());
      }
      type.addActionListener(
          e -> model.setLocalPlayerType(nameLabel.getText(),
              PlayerType.fromLabel((String) type.getSelectedItem())));
    }

    public JComboBox<String> getType() {
      return type;
    }

    public JLabel getName() {
      return nameLabel;
    }

    public JLabel getAlliance() {
      return alliance;
    }

    public JLabel getPlayer() {
      return playerLabel;
    }

    public JCheckBox getLocal() {
      return localCheckBox;
    }

    public JCheckBox getEnabledPlayer() {
      return enabledCheckBox;
    }

    public void update(final Map<String, String> playersToNodes, final Map<String, Boolean> playersEnabled) {
      String text = playersToNodes.get(nameLabel.getText());
      if (text == null) {
        text = "-";
      }
      playerLabel.setText(text);
      localCheckBox.setSelected(text.equals(model.getMessenger().getLocalNode().getName()));
      enabledCheckBox.setSelected(playersEnabled.get(nameLabel.getText()));
      setWidgetActivation();
    }

    private void setWidgetActivation() {
      type.setEnabled(localCheckBox.isSelected());
      nameLabel.setEnabled(enabledCheckBox.isSelected());
      playerLabel.setEnabled(enabledCheckBox.isSelected());
      localCheckBox.setEnabled(enabledCheckBox.isSelected());
      alliance.setEnabled(enabledCheckBox.isSelected());
      enabledCheckBox.setEnabled(model.getPlayersAllowedToBeDisabled().contains(nameLabel.getText()));
    }

    private final ActionListener localPlayerActionListener = new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        if (localCheckBox.isSelected()) {
          model.takePlayer(nameLabel.getText());
        } else {
          model.releasePlayer(nameLabel.getText());
        }
        setWidgetActivation();
      }
    };
    private final ActionListener disablePlayerActionListener = new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        if (enabledCheckBox.isSelected()) {
          model.enablePlayer(nameLabel.getText());
          type.setSelectedItem(PlayerType.HUMAN_PLAYER);
        } else {
          model.disablePlayer(nameLabel.getText());
          type.setSelectedItem(PlayerType.WEAK_AI.name());
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
  public synchronized Optional<ILauncher> getLauncher() {
    return model.getLauncher()
        .map(launcher -> {
          launcher.setInGameLobbyWatcher(lobbyWatcher);
          return launcher;
        });
  }

  @Override
  public JComponent getDrawable() {
    return this;
  }

  @Override
  public List<Action> getUserActions() {
    final List<Action> actions = new ArrayList<>();
    actions.add(new BootPlayerAction(this, model.getMessenger()));
    actions.add(new BanPlayerAction(this, model.getMessenger()));
    actions.add(new MutePlayerAction(this, model.getMessenger()));
    actions.add(
        new SetPasswordAction(this, lobbyWatcher, (ClientLoginValidator) model.getMessenger().getLoginValidator()));
    if (lobbyWatcher.isActive()) {
      actions.add(new EditGameCommentAction(lobbyWatcher, ServerSetupPanel.this));
      actions.add(new RemoveGameFromLobbyAction(lobbyWatcher));
    }
    return actions;
  }

  @Override
  public boolean showCancelButton() {
    return true;
  }

}
