package games.strategy.engine.framework.startup.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.AbstractAction;
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
import games.strategy.engine.framework.ui.SaveGameFileChooser;

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
    clientModel.setRemoteModelListener(remoteModelListener);
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
    private final JCheckBox m_enabledCheckBox;
    private final JLabel m_playerNameLabel;
    private final JLabel m_playerLabel;
    private JComponent m_playerComponent;
    private final String m_localPlayerType;
    private final JLabel m_alliance;

    PlayerRow(final String playerName, final Collection<String> playerAlliances, final String localPlayerType,
        final boolean enabled) {
      m_playerNameLabel = new JLabel(playerName);
      m_playerLabel = new JLabel("");
      m_playerComponent = new JLabel("");
      m_localPlayerType = localPlayerType;
      m_enabledCheckBox = new JCheckBox();
      m_enabledCheckBox.addActionListener(m_disablePlayerActionListener);
      m_enabledCheckBox.setSelected(enabled);
      if (playerAlliances.contains(playerName)) {
        m_alliance = new JLabel();
      } else {
        m_alliance = new JLabel(playerAlliances.toString());
      }
    }

    public JLabel getName() {
      return m_playerNameLabel;
    }

    public JLabel getPlayer() {
      return m_playerLabel;
    }

    public String getPlayerName() {
      return m_playerNameLabel.getText();
    }

    public JLabel getAlliance() {
      return m_alliance;
    }

    public JCheckBox getEnabledPlayer() {
      return m_enabledCheckBox;
    }

    public void update(final String playerName, final boolean disableable) {
      if (playerName == null) {
        m_playerLabel.setText("-");
        final JButton button = new JButton(m_takeAction);
        button.setMargin(buttonInsets);
        m_playerComponent = button;
      } else {
        m_playerLabel.setText(playerName);
        if (playerName.equals(clientModel.getMessenger().getLocalNode().getName())) {
          final JButton button = new JButton(m_dontTakeAction);
          button.setMargin(buttonInsets);
          m_playerComponent = button;
        } else {
          m_playerComponent = new JLabel("");
        }
      }
      setWidgetActivation(disableable);
    }

    private void setWidgetActivation(final boolean disableable) {
      m_playerNameLabel.setEnabled(m_enabledCheckBox.isSelected());
      m_playerLabel.setEnabled(m_enabledCheckBox.isSelected());
      m_playerComponent.setEnabled(m_enabledCheckBox.isSelected());
      m_alliance.setEnabled(m_enabledCheckBox.isSelected());
      m_enabledCheckBox.setEnabled(disableable);
    }

    public boolean isPlaying() {
      return m_playerLabel.getText().equals(clientModel.getMessenger().getLocalNode().getName());
    }

    public JComponent getPlayerComponent() {
      return m_playerComponent;
    }

    public String getLocalType() {
      return m_localPlayerType;
    }

    private final Action m_takeAction = new AbstractAction("Play") {
      private static final long serialVersionUID = 9086754428763609790L;

      @Override
      public void actionPerformed(final ActionEvent e) {
        clientModel.takePlayer(m_playerNameLabel.getText());
      }
    };
    private final Action m_dontTakeAction = new AbstractAction("Dont Play") {
      private static final long serialVersionUID = 8735891444454338978L;

      @Override
      public void actionPerformed(final ActionEvent e) {
        clientModel.releasePlayer(m_playerNameLabel.getText());
      }
    };
    private final ActionListener m_disablePlayerActionListener = new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        if (m_enabledCheckBox.isSelected()) {
          clientModel.enablePlayer(m_playerNameLabel.getText());
        } else {
          clientModel.disablePlayer(m_playerNameLabel.getText());
        }
        setWidgetActivation(true);
      }
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
    final List<Action> rVal = new ArrayList<>();
    rVal.add(clientModel.getHostBotSetMapClientAction(this));
    rVal.add(clientModel.getHostBotChangeGameOptionsClientAction(this));
    rVal.add(clientModel.getHostBotChangeGameToSaveGameClientAction());
    rVal.add(clientModel.getHostBotChangeToAutosaveClientAction(this, SaveGameFileChooser.AUTOSAVE_TYPE.AUTOSAVE));
    rVal.add(clientModel.getHostBotChangeToAutosaveClientAction(this, SaveGameFileChooser.AUTOSAVE_TYPE.AUTOSAVE2));
    rVal.add(clientModel.getHostBotChangeToAutosaveClientAction(this, SaveGameFileChooser.AUTOSAVE_TYPE.AUTOSAVE_ODD));
    rVal.add(clientModel.getHostBotChangeToAutosaveClientAction(this, SaveGameFileChooser.AUTOSAVE_TYPE.AUTOSAVE_EVEN));
    rVal.add(clientModel.getHostBotGetGameSaveClientAction(this));
    return rVal;
  }

  @Override
  public boolean isMetaSetupPanelInstance() {
    return false;
  }

}
