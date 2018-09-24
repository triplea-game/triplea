package games.strategy.engine.framework.startup.ui;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.framework.startup.launcher.local.PlayerCountrySelection;
import games.strategy.triplea.Constants;

/**
 * Represents a player selection row worth of data, during initial setup this is a row where a player can choose
 * to play a country, set it to AI, etc.
 */
public class PlayerSelectorRow implements PlayerCountrySelection {

  private final JCheckBox enabledCheckBox;
  private final String playerName;
  private final PlayerID player;
  private final JComboBox<String> playerTypes;
  private JComponent incomePercentage;
  private final JLabel incomePercentageLabel;
  private JComponent puIncomeBonus;
  private final JLabel puIncomeBonusLabel;
  private boolean enabled = true;
  private final JLabel name;
  private JButton alliances;
  private final Collection<String> disableable;
  private final SetupPanel parent;

  PlayerSelectorRow(final List<PlayerSelectorRow> playerRows, final PlayerID player,
      final Map<String, String> reloadSelections, final Collection<String> disableable,
      final HashMap<String, Boolean> playersEnablementListing, final Collection<String> playerAlliances,
      final SetupPanel parent, final GameProperties gameProperties) {
    this.disableable = disableable;
    this.parent = parent;
    playerName = player.getName();
    this.player = player;
    name = new JLabel(playerName + ":");

    enabledCheckBox = new JCheckBox();
    final ActionListener disablePlayerActionListener = new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        if (enabledCheckBox.isSelected()) {
          enabled = true;
          // the 1st in the list should be human
          playerTypes.setSelectedItem(PlayerType.HUMAN_PLAYER);
        } else {
          enabled = false;
          // the 2nd in the list should be Weak AI
          playerTypes.setSelectedItem(PlayerType.WEAK_AI);
        }
        setWidgetActivation();
      }
    };
    enabledCheckBox.addActionListener(disablePlayerActionListener);
    enabledCheckBox.setSelected(playersEnablementListing.get(playerName));
    enabledCheckBox.setEnabled(disableable.contains(playerName));

    playerTypes = new JComboBox<>(PlayerType.playerTypes());
    String previousSelection = reloadSelections.get(playerName);
    if (previousSelection.equalsIgnoreCase("Client")) {
      previousSelection = PlayerType.HUMAN_PLAYER.name();
    }
    if (Arrays.asList(PlayerType.playerTypes()).contains(previousSelection)) {
      playerTypes.setSelectedItem(previousSelection);
    } else {
      setDefaultPlayerType();
    }

    alliances = null;
    if (!playerAlliances.contains(playerName)) {
      final String alliancesLabelText = playerAlliances.toString();
      alliances = new JButton(alliancesLabelText);
      alliances.setToolTipText("Set all " + alliancesLabelText + " to " + playerTypes.getSelectedItem().toString());
      alliances.addActionListener(e -> {
        final String currentType = playerTypes.getSelectedItem().toString();
        playerRows.stream()
            .filter(row -> row.alliances != null && row.alliances.getText().equals(alliancesLabelText))
            .forEach(row -> row.setPlayerType(currentType));
      });
    }

    // TODO: remove null check for next incompatible release
    incomePercentage = null;
    if (gameProperties.getPlayerProperty(Constants.getIncomePercentageFor(player)) != null) {
      incomePercentage =
          gameProperties.getPlayerProperty(Constants.getIncomePercentageFor(player)).getEditorComponent();
    }
    incomePercentageLabel = new JLabel("%");

    // TODO: remove null check for next incompatible release
    puIncomeBonus = null;
    if (gameProperties.getPlayerProperty(Constants.getPuIncomeBonus(player)) != null) {
      puIncomeBonus =
          gameProperties.getPlayerProperty(Constants.getPuIncomeBonus(player)).getEditorComponent();
    }
    puIncomeBonusLabel = new JLabel("PUs");

    setWidgetActivation();
  }

  void layout(final int row, final Container container) {
    int gridx = 0;
    if (!disableable.isEmpty()) {
      container.add(enabledCheckBox, new GridBagConstraints(gridx++, row, 1, 1, 0, 0, GridBagConstraints.WEST,
          GridBagConstraints.NONE, new Insets(0, 5, 5, 0), 0, 0));
    }
    container.add(name, new GridBagConstraints(gridx++, row, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(0, 5, 5, 0), 0, 0));
    container.add(playerTypes, new GridBagConstraints(gridx++, row, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(0, 5, 5, 0), 0, 0));
    if (alliances != null) {
      container.add(alliances, new GridBagConstraints(gridx, row, 1, 1, 0, 0, GridBagConstraints.WEST,
          GridBagConstraints.NONE, new Insets(0, 7, 5, 5), 0, 0));
    }
    gridx++;
    // TODO: remove null check for next incompatible release
    if (incomePercentage != null) {
      container.add(incomePercentage, new GridBagConstraints(gridx++, row, 1, 1, 0, 0, GridBagConstraints.WEST,
          GridBagConstraints.NONE, new Insets(0, 20, 2, 0), 0, 0));
      container.add(incomePercentageLabel,
          new GridBagConstraints(gridx++, row, 1, 1, 0, 0, GridBagConstraints.WEST,
              GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    }
    // TODO: remove null check for next incompatible release
    if (puIncomeBonus != null) {
      container.add(puIncomeBonus, new GridBagConstraints(gridx++, row, 1, 1, 0, 0, GridBagConstraints.WEST,
          GridBagConstraints.NONE, new Insets(0, 20, 2, 0), 0, 0));
      container.add(puIncomeBonusLabel,
          new GridBagConstraints(gridx, row, 1, 1, 0, 0, GridBagConstraints.WEST,
              GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    }
  }

  void setResourceModifiersVisble(final boolean isVisible) {
    // TODO: remove null check for next incompatible release
    if (incomePercentage != null) {
      incomePercentage.setVisible(isVisible);
      incomePercentageLabel.setVisible(isVisible);
    }
    // TODO: remove null check for next incompatible release
    if (puIncomeBonus != null) {
      puIncomeBonus.setVisible(isVisible);
      puIncomeBonusLabel.setVisible(isVisible);
    }
  }

  void setPlayerType(final String playerType) {
    if (enabled && !player.isHidden()) {
      playerTypes.setSelectedItem(playerType);
    }
  }

  void setDefaultPlayerType() {
    if (player.isDefaultTypeAi()) {
      playerTypes.setSelectedItem(PlayerType.PRO_AI.getLabel());
    } else if (player.isDefaultTypeDoesNothing()) {
      playerTypes.setSelectedItem(PlayerType.DOES_NOTHING_AI.getLabel());
    } else {
      playerTypes.setSelectedItem(PlayerType.HUMAN_PLAYER.getLabel());
    }
  }

  @Override
  public String getPlayerName() {
    return playerName;
  }

  @Override
  public PlayerType getPlayerType() {
    return PlayerType.fromLabel(String.valueOf(playerTypes.getSelectedItem()));
  }

  @Override
  public boolean isPlayerEnabled() {
    return enabledCheckBox.isSelected();
  }

  private void setWidgetActivation() {
    name.setEnabled(enabled);
    if (alliances != null) {
      alliances.setEnabled(enabled);
    }
    enabledCheckBox.setEnabled(disableable.contains(playerName));
    // TODO: remove null check for next incompatible release
    if (incomePercentage != null) {
      incomePercentage.setEnabled(enabled);
    }
    // TODO: remove null check for next incompatible release
    if (puIncomeBonus != null) {
      puIncomeBonus.setEnabled(enabled);
    }
    parent.notifyObservers();
  }

}
