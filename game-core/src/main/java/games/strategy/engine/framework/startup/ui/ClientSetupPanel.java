package games.strategy.engine.framework.startup.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import games.strategy.engine.chat.IChatPanel;
import games.strategy.engine.framework.IGameLoader;
import games.strategy.engine.framework.startup.mc.ClientModel;
import games.strategy.engine.framework.startup.mc.IRemoteModelListener;
import games.strategy.engine.framework.ui.SaveGameFileChooser.AUTOSAVE_TYPE;
import games.strategy.ui.SwingAction;

/**
 * Network client game staging panel, can be used to select sides and chat.
 */
public class ClientSetupPanel extends SetupPanel {
  private static final long serialVersionUID = 6942605803526295372L;
  private final Insets buttonInsets = new Insets(0, 0, 0, 0);
  private final ClientModel clientModel;
  private List<PlayerRow> playerRows = Collections.emptyList();
  private final IRemoteModelListener remoteModelListener = new IRemoteModelListener() {
    @Override
    public void playersTakenChanged() {}

    @Override
    public void playerListChanged() {
      SwingUtilities.invokeLater(() -> internalPlayersChanged());
    }
  };

  public ClientSetupPanel(final ClientModel model) {
    clientModel = model;
    layoutComponents();
    setupListeners();
    setWidgetActivation();
  }

  private void internalPlayersChanged() {
    final Map<String, String> players = clientModel.getPlayerToNodesMapping();
    final Map<String, Collection<String>> playerNamesAndAlliancesInTurnOrder =
        clientModel.getPlayerNamesAndAlliancesInTurnOrderLinkedHashMap();
    final Map<String, Boolean> enabledPlayers = clientModel.getPlayersEnabledListing();
    final Collection<String> disableable = clientModel.getPlayersAllowedToBeDisabled();
    if (!clientModel.getIsServerHeadlessCached()) {
      // clients only get to change bot settings
      disableable.clear();
    }
    playerRows = new ArrayList<>();
    final Set<String> playerNames = playerNamesAndAlliancesInTurnOrder.keySet();
    for (final String name : playerNames) {
      final PlayerRow playerRow = new PlayerRow(name, playerNamesAndAlliancesInTurnOrder.get(name),
          IGameLoader.CLIENT_PLAYER_TYPE, enabledPlayers.get(name));
      playerRows.add(playerRow);
      playerRow.update(players.get(name), disableable.contains(name));
    }
    layoutComponents();
  }

  private void layoutComponents() {
    removeAll();
    setLayout(new BorderLayout());
    final JPanel info = new JPanel();
    info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
    info.add(new JLabel(" "));
    add(info, BorderLayout.NORTH);
    final JPanel players = new JPanel();
    final GridBagLayout layout = new GridBagLayout();
    players.setLayout(layout);
    final Insets spacing = new Insets(3, 16, 0, 0);
    final Insets lastSpacing = new Insets(3, 16, 0, 16);
    int gridx = 0;
    final GridBagConstraints enabledPlayerConstraints = new GridBagConstraints();
    final boolean disableable = clientModel.getPlayersAllowedToBeDisabled().isEmpty();
    if (!disableable) {
      enabledPlayerConstraints.anchor = GridBagConstraints.WEST;
      enabledPlayerConstraints.gridx = gridx++;
      enabledPlayerConstraints.insets = new Insets(3, 20, 0, -10);
    }
    final GridBagConstraints nameConstraints = new GridBagConstraints();
    nameConstraints.anchor = GridBagConstraints.WEST;
    nameConstraints.gridx = gridx++;
    nameConstraints.insets = spacing;
    gridx++;
    final GridBagConstraints playerConstraints = new GridBagConstraints();
    playerConstraints.anchor = GridBagConstraints.WEST;
    playerConstraints.gridx = gridx++;
    playerConstraints.insets = spacing;
    final GridBagConstraints playConstraints = new GridBagConstraints();
    playConstraints.anchor = GridBagConstraints.WEST;
    playConstraints.gridx = gridx++;
    playConstraints.insets = spacing;
    final GridBagConstraints allianceConstraints = new GridBagConstraints();
    allianceConstraints.anchor = GridBagConstraints.WEST;
    allianceConstraints.gridx = gridx++;
    allianceConstraints.insets = lastSpacing;
    if (!disableable) {
      final JLabel enableLabel = new JLabel("Use");
      enableLabel.setForeground(Color.black);
      layout.setConstraints(enableLabel, enabledPlayerConstraints);
      players.add(enableLabel);
    }
    final JLabel nameLabel = new JLabel("Name");
    nameLabel.setForeground(Color.black);
    layout.setConstraints(nameLabel, nameConstraints);
    players.add(nameLabel);
    final JLabel playerLabel = new JLabel("Played By");
    playerLabel.setForeground(Color.black);
    layout.setConstraints(playerLabel, playerConstraints);
    players.add(playerLabel);
    final JLabel playedByLabel = new JLabel("                    ");
    layout.setConstraints(playedByLabel, playConstraints);
    players.add(playedByLabel);
    final JLabel allianceLabel = new JLabel("Alliance");
    // allianceLabel.setForeground(Color.black);
    layout.setConstraints(allianceLabel, allianceConstraints);
    players.add(allianceLabel);
    for (final PlayerRow row : playerRows) {
      if (!disableable) {
        layout.setConstraints(row.getEnabledPlayer(), enabledPlayerConstraints);
        players.add(row.getEnabledPlayer());
      }
      layout.setConstraints(row.getName(), nameConstraints);
      players.add(row.getName());
      layout.setConstraints(row.getPlayer(), playerConstraints);
      players.add(row.getPlayer());
      layout.setConstraints(row.getPlayerComponent(), playConstraints);
      players.add(row.getPlayerComponent());
      layout.setConstraints(row.getAlliance(), allianceConstraints);
      players.add(row.getAlliance());
    }
    add(players, BorderLayout.CENTER);
    validate();
  }

  private void setupListeners() {
    clientModel.setRemoteModelListener((remoteModelListener == null)
        ? IRemoteModelListener.NULL_LISTENER
        : remoteModelListener);
  }

  @Override
  public void setWidgetActivation() {}

  @Override
  public void shutDown() {
    clientModel.shutDown();
  }

  @Override
  public void cancel() {
    clientModel.cancel();
  }

  @Override
  public boolean canGameStart() {
    // our server must handle this
    return false;
  }

  class PlayerRow {
    private final JCheckBox enabledCheckBox = new JCheckBox();
    private final JLabel playerNameLabel = new JLabel();
    private final JLabel playerLabel = new JLabel();
    private JComponent playerComponent = new JLabel();
    private final String localPlayerType;
    private final JLabel alliance;

    PlayerRow(final String playerName, final Collection<String> playerAlliances, final String localPlayerType,
        final boolean enabled) {
      playerNameLabel.setText(playerName);
      this.localPlayerType = localPlayerType;
      enabledCheckBox.addActionListener(disablePlayerActionListener);
      enabledCheckBox.setSelected(enabled);
      alliance = new JLabel(playerAlliances.contains(playerName) ? "" : playerAlliances.toString());
    }

    public JLabel getName() {
      return playerNameLabel;
    }

    public JLabel getPlayer() {
      return playerLabel;
    }

    public String getPlayerName() {
      return playerNameLabel.getText();
    }

    public JLabel getAlliance() {
      return alliance;
    }

    public JCheckBox getEnabledPlayer() {
      return enabledCheckBox;
    }

    public void update(final String playerName, final boolean disableable) {
      if (playerName == null) {
        playerLabel.setText("-");
        final JButton button = new JButton(takeAction);
        button.setMargin(buttonInsets);
        playerComponent = button;
      } else {
        playerLabel.setText(playerName);
        if (playerName.equals(clientModel.getMessenger().getLocalNode().getName())) {
          final JButton button = new JButton(dontTakeAction);
          button.setMargin(buttonInsets);
          playerComponent = button;
        } else {
          playerComponent = new JLabel("");
        }
      }
      setWidgetActivation(disableable);
    }

    private void setWidgetActivation(final boolean disableable) {
      playerNameLabel.setEnabled(enabledCheckBox.isSelected());
      playerLabel.setEnabled(enabledCheckBox.isSelected());
      playerComponent.setEnabled(enabledCheckBox.isSelected());
      alliance.setEnabled(enabledCheckBox.isSelected());
      enabledCheckBox.setEnabled(disableable);
    }

    public boolean isPlaying() {
      return playerLabel.getText().equals(clientModel.getMessenger().getLocalNode().getName());
    }

    public JComponent getPlayerComponent() {
      return playerComponent;
    }

    public String getLocalType() {
      return localPlayerType;
    }

    private final Action takeAction =
        SwingAction.of("Play", e -> clientModel.takePlayer(playerNameLabel.getText()));
    private final Action dontTakeAction =
        SwingAction.of("Dont Play", e -> clientModel.releasePlayer(playerNameLabel.getText()));
    private final ActionListener disablePlayerActionListener = e -> {
      if (enabledCheckBox.isSelected()) {
        clientModel.enablePlayer(playerNameLabel.getText());
      } else {
        clientModel.disablePlayer(playerNameLabel.getText());
      }
      setWidgetActivation(true);
    };
  }

  @Override
  public IChatPanel getChatPanel() {
    return clientModel.getChatPanel();
  }

  @Override
  public List<Action> getUserActions() {
    if (clientModel == null) {
      return new ArrayList<>();
    }
    final boolean isServerHeadless = clientModel.getIsServerHeadlessCached();
    if (!isServerHeadless) {
      return new ArrayList<>();
    }
    final List<Action> actions = new ArrayList<>();
    actions.add(clientModel.getHostBotSetMapClientAction(this));
    actions.add(clientModel.getHostBotChangeGameOptionsClientAction(this));
    actions.add(clientModel.getHostBotChangeGameToSaveGameClientAction());
    actions.add(clientModel.getHostBotChangeToAutosaveClientAction(this, AUTOSAVE_TYPE.AUTOSAVE));
    actions.add(clientModel.getHostBotChangeToAutosaveClientAction(this, AUTOSAVE_TYPE.AUTOSAVE2));
    actions.add(clientModel.getHostBotChangeToAutosaveClientAction(this, AUTOSAVE_TYPE.AUTOSAVE_ODD));
    actions.add(clientModel.getHostBotChangeToAutosaveClientAction(this, AUTOSAVE_TYPE.AUTOSAVE_EVEN));
    actions.add(clientModel.getHostBotGetGameSaveClientAction(this));
    return actions;
  }

  @Override
  public boolean isMetaSetupPanelInstance() {
    return false;
  }

}
