package games.strategy.engine.framework.startup.ui;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.framework.I18nEngineFramework;
import games.strategy.engine.framework.I18nResourceBundle;
import games.strategy.engine.framework.network.ui.BanPlayerAction;
import games.strategy.engine.framework.network.ui.BootPlayerAction;
import games.strategy.engine.framework.network.ui.SetPasswordAction;
import games.strategy.engine.framework.startup.LobbyWatcherThread;
import games.strategy.engine.framework.startup.launcher.ILauncher;
import games.strategy.engine.framework.startup.login.ClientLoginValidator;
import games.strategy.engine.framework.startup.mc.HeadedPlayerTypes;
import games.strategy.engine.framework.startup.mc.IRemoteModelListener;
import games.strategy.engine.framework.startup.mc.ServerModel;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameSelectorModel;
import games.strategy.engine.lobby.client.ui.action.EditGameCommentAction;
import games.strategy.engine.lobby.client.ui.action.RemoveGameFromLobbyAction;
import games.strategy.net.IServerMessenger;
import games.strategy.ui.Util;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import lombok.Getter;
import org.triplea.game.chat.ChatModel;
import org.triplea.game.startup.SetupModel;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.swing.SwingComponents;

/**
 * Setup panel displayed for hosting a non-lobby network game (using host option from main panel).
 */
public class ServerSetupPanel extends SetupPanel implements IRemoteModelListener {
  private static final long serialVersionUID = -2849872641665561807L;
  private final ServerModel model;
  private JTextField portField;
  private JTextField addressField;
  private JTextField nameField;
  private List<PlayerRow> playerRows = new ArrayList<>();
  private final GameSelectorModel gameSelectorModel;
  private JPanel infoPanel;
  private JPanel networkPanel;

  public ServerSetupPanel(final ServerModel model, final GameSelectorModel gameSelectorModel) {
    this.model = model;
    this.gameSelectorModel = gameSelectorModel;
    this.model.setRemoteModelListener(this);

    createComponents();
    layoutComponents();
    internalPlayerListChanged();
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
    infoPanel = new JPanel();
    networkPanel = new JPanel();
  }

  private void layoutComponents() {
    final I18nResourceBundle bundle = I18nEngineFramework.get();
    setLayout(new BorderLayout());
    infoPanel.setLayout(new GridBagLayout());
    infoPanel.add(
        new JLabel(bundle.getText("startup.ServerSetupPanel.infoPanel.Name.Lbl")),
        new GridBagConstraints(
            0,
            0,
            1,
            1,
            0,
            0.0,
            GridBagConstraints.EAST,
            GridBagConstraints.NONE,
            new Insets(5, 10, 0, 5),
            0,
            0));
    infoPanel.add(
        new JLabel(bundle.getText("startup.ServerSetupPanel.infoPanel.Address.Lbl")),
        new GridBagConstraints(
            0,
            1,
            1,
            1,
            0,
            0.0,
            GridBagConstraints.EAST,
            GridBagConstraints.NONE,
            new Insets(5, 10, 0, 5),
            0,
            0));
    infoPanel.add(
        new JLabel(bundle.getText("startup.ServerSetupPanel.infoPanel.Port.Lbl")),
        new GridBagConstraints(
            0,
            2,
            1,
            1,
            0,
            0.0,
            GridBagConstraints.EAST,
            GridBagConstraints.NONE,
            new Insets(5, 10, 0, 5),
            0,
            0));
    infoPanel.add(
        nameField,
        new GridBagConstraints(
            1,
            0,
            1,
            1,
            0.5,
            1.0,
            GridBagConstraints.WEST,
            GridBagConstraints.BOTH,
            new Insets(5, 0, 0, 5),
            0,
            0));
    infoPanel.add(
        addressField,
        new GridBagConstraints(
            1,
            1,
            1,
            1,
            0.5,
            1.0,
            GridBagConstraints.WEST,
            GridBagConstraints.BOTH,
            new Insets(5, 0, 0, 5),
            0,
            0));
    infoPanel.add(
        portField,
        new GridBagConstraints(
            1,
            2,
            1,
            1,
            0.5,
            1.0,
            GridBagConstraints.WEST,
            GridBagConstraints.BOTH,
            new Insets(5, 0, 0, 5),
            0,
            0));
    add(infoPanel, BorderLayout.NORTH);
  }

  private void layoutPlayers() {
    final JPanel players = new JPanel();
    final GridBagLayout layout = new GridBagLayout();
    players.setLayout(layout);
    final Insets spacing = new Insets(3, 16, 0, 0);
    final Insets lastSpacing = new Insets(3, 16, 0, 16);
    int gridx = 0;
    final boolean playersAllowedToBeDisabled =
        !model.getPlayersAllowedToBeDisabled().isEmpty()
            || model.getPlayersEnabledListing().containsValue(Boolean.FALSE);
    final GridBagConstraints enabledPlayerConstraints = new GridBagConstraints();
    if (playersAllowedToBeDisabled) {
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
    I18nResourceBundle bundle = I18nEngineFramework.get();
    if (playersAllowedToBeDisabled) {
      final JLabel enableLabel = new JLabel(bundle.getText("startup.SetupPanel.enable.Lbl"));
      enableLabel.setForeground(Color.black);
      layout.setConstraints(enableLabel, enabledPlayerConstraints);
      players.add(enableLabel);
    }
    final JLabel nameLabel = new JLabel(bundle.getText("startup.SetupPanel.name.Lbl"));
    nameLabel.setForeground(Color.black);
    layout.setConstraints(nameLabel, nameConstraints);
    players.add(nameLabel);
    final JLabel playedByLabel = new JLabel(bundle.getText("startup.SetupPanel.player.Lbl"));
    playedByLabel.setForeground(Color.black);
    layout.setConstraints(playedByLabel, playerConstraints);
    players.add(playedByLabel);
    final JLabel localLabel = new JLabel(bundle.getText("startup.SetupPanel.local.Lbl"));
    localLabel.setForeground(Color.black);
    layout.setConstraints(localLabel, localConstraints);
    players.add(localLabel);
    final JLabel typeLabel = new JLabel(bundle.getText("startup.SetupPanel.type.Lbl"));
    typeLabel.setForeground(Color.black);
    layout.setConstraints(typeLabel, typeConstraints);
    players.add(typeLabel);
    final JLabel allianceLabel = new JLabel(bundle.getText("startup.SetupPanel.alliance.Lbl"));
    allianceLabel.setForeground(Color.black);
    layout.setConstraints(allianceLabel, allianceConstraints);
    players.add(allianceLabel);
    if (playerRows.isEmpty()) {
      final JLabel noPlayers = new JLabel(bundle.getText("startup.SetupPanel.noPlayers.Lbl"));
      layout.setConstraints(noPlayers, nameConstraints);
      players.add(noPlayers);
    }
    for (final PlayerRow row : playerRows) {
      if (playersAllowedToBeDisabled) {
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
      row.getAlliance().addActionListener(e -> allianceRowButtonFired(row));
    }
    removeAll();
    add(infoPanel, BorderLayout.NORTH);
    final JScrollPane scroll =
        new JScrollPane(
            players,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    scroll.setBorder(null);
    scroll.setViewportBorder(null);
    add(scroll, BorderLayout.CENTER);
    add(networkPanel, BorderLayout.SOUTH);
    invalidate();
    validate();
  }

  private void allianceRowButtonFired(final PlayerRow playerRow) {
    final boolean playAlliance = !playerRow.isSelectedByHostPlayer();

    playerRows.stream()
        .filter(row -> row.getAlliance().getText().equals(playerRow.getAlliance().getText()))
        .forEach(
            row -> {
              if (playAlliance) {
                row.takePlayerAction();
              } else {
                row.releasePlayerAction();
              }
            });
  }

  @Override
  public void cancel() {
    model.setRemoteModelListener(IRemoteModelListener.NULL_LISTENER);
    model.cancel();
  }

  @Override
  public void postStartGame() {
    SetupModel.clearPbfPbemInformation(gameSelectorModel.getGameData().getProperties());
  }

  @Override
  public boolean canGameStart() {
    return SetupModel.staticCanGameStart(gameSelectorModel, model);
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
    Util.ensureOnEventDispatchThread();
    final Map<String, String> playersToNode = model.getPlayersToNodeListing();
    final Map<String, Boolean> playersEnabled = model.getPlayersEnabledListing();
    for (final PlayerRow row : playerRows) {
      row.update(playersToNode, playersEnabled);
    }
    super.fireListener();
  }

  private void internalPlayerListChanged() {
    Util.ensureOnEventDispatchThread();
    playerRows = new ArrayList<>();
    final Map<String, String> players = model.getPlayersToNodeListing();
    final Map<String, Boolean> playersEnabled = model.getPlayersEnabledListing();
    final Map<String, String> reloadSelections =
        GamePlayer.currentPlayers(gameSelectorModel.getGameData());
    final List<PlayerTypes.Type> visiblePlayerTypes =
        CollectionUtils.getMatches(HeadedPlayerTypes.getPlayerTypes(), PlayerTypes.Type::isVisible);
    for (final Map.Entry<String, Collection<String>> entry :
        model.getPlayerNamesAndAlliancesInTurnOrder().entrySet()) {
      final String playerName = entry.getKey();
      GamePlayer player = null;
      if (gameSelectorModel.getGameData() != null) {
        player = gameSelectorModel.getGameData().getPlayerList().getPlayerId(playerName);
      }
      PlayerTypes.Type playerType =
          determinePlayerType(playerName, player, reloadSelections, visiblePlayerTypes);
      PlayerRow row = new PlayerRow(playerName, playerType, visiblePlayerTypes, entry.getValue());
      row.update(players, playersEnabled);
      playerRows.add(row);
    }
    layoutPlayers();
    internalPlayersTakenChanged();
  }

  private PlayerTypes.Type determinePlayerType(
      String playerName,
      @Nullable GamePlayer player,
      Map<String, String> reloadSelections,
      List<PlayerTypes.Type> visiblePlayerTypes) {
    final String previousSelection = reloadSelections.get(playerName);
    if (previousSelection != null
        && previousSelection.equalsIgnoreCase(PlayerTypes.PLAYER_TYPE_DEFAULT_LABEL)) {
      return HeadedPlayerTypes.HUMAN_PLAYER;
    } else if (previousSelection != null && !previousSelection.equals("no_one")) {
      // Note: "no_one" comes from `whoAmI` in GamePlayer.java.
      Optional<PlayerTypes.Type> type =
          visiblePlayerTypes.stream().filter(t -> t.getLabel().equals(previousSelection)).findAny();
      if (type.isPresent()) {
        return type.get();
      }
    }

    if (player != null && player.isDefaultTypeAi()) {
      return PlayerTypes.PRO_AI;
    } else if (player != null && player.isDefaultTypeDoesNothing()) {
      return HeadedPlayerTypes.DOES_NOTHING_PLAYER;
    } else if (playerName.startsWith("Neutral") || playerName.startsWith("AI")) {
      return PlayerTypes.PRO_AI;
    } else {
      return HeadedPlayerTypes.HUMAN_PLAYER;
    }
  }

  class PlayerRow {
    private final JLabel nameLabel;
    private final JLabel playerLabel;
    private final JCheckBox localCheckBox;
    private final JCheckBox enabledCheckBox;
    @Getter private final JComboBox<PlayerTypes.Type> type;
    @Getter private final JButton alliance;

    PlayerRow(
        final String playerName,
        final PlayerTypes.Type playerType,
        final Collection<PlayerTypes.Type> playerTypes,
        final Collection<String> playerAlliances) {
      nameLabel = new JLabel(playerName);
      playerLabel = new JLabel(model.getMessenger().getLocalNode().getName());
      type = new JComboBox<>(SwingComponents.newComboBoxModel(playerTypes));
      type.setSelectedItem(playerType);
      model.setLocalPlayerType(nameLabel.getText(), playerType);
      localCheckBox = new JCheckBox();
      localCheckBox.addActionListener(
          e -> {
            if (localCheckBox.isSelected()) {
              model.takePlayer(nameLabel.getText());
            } else {
              model.releasePlayer(nameLabel.getText());
            }
            setWidgetActivation();
          });
      localCheckBox.setSelected(true);
      enabledCheckBox = new JCheckBox();
      enabledCheckBox.addActionListener(
          e -> {
            if (enabledCheckBox.isSelected()) {
              model.enablePlayer(nameLabel.getText());
              type.setSelectedItem(HeadedPlayerTypes.HUMAN_PLAYER);
            } else {
              model.disablePlayer(nameLabel.getText());
              type.setSelectedItem(PlayerTypes.WEAK_AI);
            }
            setWidgetActivation();
          });
      // this gets updated later
      enabledCheckBox.setSelected(true);
      if (playerAlliances.contains(playerName)) {
        alliance = new JButton();
        alliance.setVisible(false);
      } else {
        alliance = new JButton(playerAlliances.toString());
        alliance.setToolTipText(
            I18nEngineFramework.get()
                .getText("startup.SetupPanel.PlayerRow.alliance.Play.Tltp", playerAlliances));
      }
      type.addActionListener(
          e ->
              model.setLocalPlayerType(
                  nameLabel.getText(), (PlayerTypes.Type) type.getSelectedItem()));
    }

    public void takePlayerAction() {
      model.takePlayer(nameLabel.getText());
      alliance.setToolTipText(
          I18nEngineFramework.get()
              .getText("startup.SetupPanel.PlayerRow.alliance.Release.Tltp", alliance.getText()));
      setWidgetActivation();
    }

    public void releasePlayerAction() {
      model.releasePlayer(nameLabel.getText());
      alliance.setToolTipText(
          I18nEngineFramework.get()
              .getText("startup.SetupPanel.PlayerRow.alliance.Play.Tltp", alliance.getText()));
      setWidgetActivation();
    }

    public JLabel getName() {
      return nameLabel;
    }

    public JLabel getPlayer() {
      return playerLabel;
    }

    public JCheckBox getLocal() {
      return localCheckBox;
    }

    JCheckBox getEnabledPlayer() {
      return enabledCheckBox;
    }

    public boolean isSelectedByHostPlayer() {
      return playerLabel.getText().equals(model.getMessenger().getLocalNode().getName());
    }

    public void update(
        final Map<String, String> playersToNodes, final Map<String, Boolean> playersEnabled) {
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
      enabledCheckBox.setEnabled(
          model.getPlayersAllowedToBeDisabled().contains(nameLabel.getText()));
    }
  }

  @Override
  public ChatModel getChatModel() {
    return model.getChatModel();
  }

  @Override
  public synchronized Optional<? extends ILauncher> getLauncher() {
    return model.getLauncher();
  }

  @Override
  public List<Action> getUserActions() {
    final List<Action> actions = new ArrayList<>();
    actions.add(new BootPlayerAction(this, model.getMessenger()));
    actions.add(new BanPlayerAction(this, model.getMessenger()));
    actions.add(
        new SetPasswordAction(
            this,
            Optional.ofNullable(model.getLobbyWatcherThread())
                .map(LobbyWatcherThread::getLobbyWatcher)
                .orElse(null),
            (ClientLoginValidator) model.getMessenger().getLoginValidator()));

    Optional.ofNullable(model.getLobbyWatcherThread())
        .map(LobbyWatcherThread::getLobbyWatcher)
        .filter(InGameLobbyWatcherWrapper::isActive)
        .ifPresent(
            watcher -> {
              actions.add(new EditGameCommentAction(watcher, ServerSetupPanel.this));
              actions.add(new RemoveGameFromLobbyAction(watcher));
            });
    return actions;
  }

  @Override
  public boolean isCancelButtonVisible() {
    return true;
  }
}
