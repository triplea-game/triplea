package games.strategy.engine.framework.startup.ui;

import games.strategy.engine.chat.ChatPanel;
import games.strategy.engine.framework.startup.launcher.ILauncher;
import games.strategy.engine.framework.startup.mc.ClientModel;
import games.strategy.engine.framework.startup.mc.IRemoteModelListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import lombok.Getter;
import org.triplea.domain.data.UserName;
import org.triplea.swing.SwingAction;

/** Network client game staging panel, can be used to select sides and chat. */
public class ClientSetupPanel extends SetupPanel {
  private static final long serialVersionUID = 6942605803526295372L;
  private final Insets buttonInsets = new Insets(0, 0, 0, 0);
  private final ClientModel clientModel;
  private final List<PlayerRow> playerRows = new ArrayList<>();

  public ClientSetupPanel(final ClientModel model) {
    clientModel = model;
    layoutComponents();
    clientModel.setRemoteModelListener(
        new IRemoteModelListener() {
          @Override
          public void playersTakenChanged() {
            // nothing to do
          }

          @Override
          public void playerListChanged() {
            SwingUtilities.invokeLater(ClientSetupPanel.this::internalPlayersChanged);
          }
        });
  }

  private void internalPlayersChanged() {
    final Map<String, String> players = clientModel.getPlayerToNodesMapping();
    final Map<String, Collection<String>> playerNamesAndAlliancesInTurnOrder =
        clientModel.getPlayerNamesAndAlliancesInTurnOrder();
    final Map<String, Boolean> enabledPlayers = clientModel.getPlayersEnabledListing();
    final Collection<String> disableable = clientModel.getPlayersAllowedToBeDisabled();
    if (!clientModel.getIsServerHeadlessCached()) {
      // clients only get to change bot settings
      disableable.clear();
    }
    playerRows.clear();
    final Set<String> playerNames = playerNamesAndAlliancesInTurnOrder.keySet();
    for (final String name : playerNames) {
      final PlayerRow playerRow =
          new PlayerRow(
              name, playerNamesAndAlliancesInTurnOrder.get(name), enabledPlayers.get(name));
      playerRows.add(playerRow);
      SwingUtilities.invokeLater(
          () ->
              playerRow.update(
                  Optional.ofNullable(players.get(name)).map(UserName::of).orElse(null),
                  disableable.contains(name)));
    }
    SwingUtilities.invokeLater(this::layoutComponents);
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
    allianceConstraints.gridx = gridx;
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
      layout.setConstraints(row.getAllianceComponent(), allianceConstraints);
      players.add(row.getAllianceComponent());
      row.getAlliance().addActionListener(e -> fireAllianceButtonClicked(row));
    }
    add(players, BorderLayout.CENTER);
    validate();
  }

  private void fireAllianceButtonClicked(final PlayerRow playerRow) {
    final boolean playAlliance = !playerRow.isSelectedByLocalPlayer();

    playerRows.stream()
        .filter(row -> row.getAlliance().getText().equals(playerRow.getAlliance().getText()))
        .forEach(
            row -> {
              if (playAlliance) {
                row.executeTakePlayerActionIfEnabled();
              } else {
                row.executeReleasePlayerActionIfEnabled();
              }
            });
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

  @Override
  public void postStartGame() {
    // nothing to do
  }

  @Override
  public ChatPanel getChatModel() {
    return clientModel.getChatPanel();
  }

  @Override
  public List<Action> getUserActions() {
    return List.of();
  }

  @Override
  public boolean isCancelButtonVisible() {
    return true;
  }

  @Override
  public Optional<ILauncher> getLauncher() {
    throw new UnsupportedOperationException();
  }

  class PlayerRow {
    private static final String PLAY_TEXT = "Play";
    private final JCheckBox enabledCheckBox = new JCheckBox();
    private final JLabel playerNameLabel = new JLabel();
    private final JLabel playerLabel = new JLabel();
    @Getter private final JButton alliance;
    @Getter private JComponent playerComponent = new JLabel();
    @Getter private JComponent allianceComponent = new JLabel();
    private final Action takeAction =
        SwingAction.of(PLAY_TEXT, e -> clientModel.takePlayer(playerNameLabel.getText()));
    private final Action dontTakeAction =
        SwingAction.of("Don't Play", e -> clientModel.releasePlayer(playerNameLabel.getText()));

    private final ActionListener disablePlayerActionListener =
        e -> {
          if (enabledCheckBox.isSelected()) {
            clientModel.enablePlayer(playerNameLabel.getText());
          } else {
            clientModel.disablePlayer(playerNameLabel.getText());
          }
          setWidgetActivation(true);
        };

    PlayerRow(
        final String playerName, final Collection<String> playerAlliances, final boolean enabled) {
      playerNameLabel.setText(playerName);
      enabledCheckBox.addActionListener(disablePlayerActionListener);
      enabledCheckBox.setSelected(enabled);
      final boolean hasAlliance = !playerAlliances.contains(playerName);
      alliance = new JButton(hasAlliance ? playerAlliances.toString() : "");
      alliance.setVisible(hasAlliance);
    }

    public JLabel getName() {
      return playerNameLabel;
    }

    public JLabel getPlayer() {
      return playerLabel;
    }

    public JCheckBox getEnabledPlayer() {
      return enabledCheckBox;
    }

    public void executeTakePlayerActionIfEnabled() {
      if (playerComponent.isVisible()
          && playerComponent instanceof JButton
          && ((JButton) playerComponent).getText().equals(PLAY_TEXT)) {
        takeAction.actionPerformed(null);
      }
    }

    public void executeReleasePlayerActionIfEnabled() {
      if (playerComponent.isVisible()
          && playerComponent instanceof JButton
          && !((JButton) playerComponent).getText().equals(PLAY_TEXT)) {
        dontTakeAction.actionPerformed(null);
      }
    }

    public boolean isSelectedByLocalPlayer() {
      return playerLabel
          .getText()
          .equals(clientModel.getClientMessenger().getLocalNode().getName());
    }

    public void update(final UserName userName, final boolean disableable) {
      if (userName == null) {
        playerLabel.setText("-");
        final JButton button = new JButton(takeAction);
        button.setMargin(buttonInsets);
        playerComponent = button;
        alliance.setToolTipText("Click to play " + alliance.getText());
        allianceComponent = alliance;
      } else {
        playerLabel.setText(userName.getValue());
        if (userName.equals(clientModel.getClientMessenger().getLocalNode().getPlayerName())) {
          final JButton button = new JButton(dontTakeAction);
          button.setMargin(buttonInsets);
          playerComponent = button;
          alliance.setToolTipText("Click to release " + alliance.getText());
          allianceComponent = alliance;
        } else {
          playerComponent = new JLabel("");
          allianceComponent = new JLabel("");
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
  }
}
