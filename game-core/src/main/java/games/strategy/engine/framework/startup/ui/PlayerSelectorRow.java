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
import games.strategy.triplea.TripleA;

/**
 * Represents a player selection row worth of data, during initial setup this is a row where a player can choose
 * to play a country, set it to AI, etc.
 */
public class PlayerSelectorRow implements PlayerCountrySelection {

  private static final String PLAYER_TYPE_AI = "AI";
  private static final String PLAYER_TYPE_DOES_NOTHING = "DoesNothing";

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
      final String[] types, final SetupPanel parent, final GameProperties gameProperties) {
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
          playerTypes.setSelectedItem(types[0]);
        } else {
          enabled = false;
          // the 2nd in the list should be Weak AI
          playerTypes.setSelectedItem(types[Math.max(0, Math.min(types.length - 1, 1))]);
        }
        setWidgetActivation();
      }
    };
    enabledCheckBox.addActionListener(disablePlayerActionListener);
    enabledCheckBox.setSelected(playersEnablementListing.get(playerName));
    enabledCheckBox.setEnabled(disableable.contains(playerName));

    playerTypes = new JComboBox<>(types);
    String previousSelection = reloadSelections.get(playerName);
    if (previousSelection.equalsIgnoreCase("Client")) {
      previousSelection = types[0];
    }
    if (!(previousSelection.equals("no_one")) && Arrays.asList(types).contains(previousSelection)) {
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
            .filter(row -> (row.alliances != null) && row.alliances.getText().equals(alliancesLabelText))
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
          new GridBagConstraints(gridx++, row, 1, 1, 0, 0, GridBagConstraints.WEST,
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
    if (PLAYER_TYPE_AI.equals(player.getDefaultType())) {
      playerTypes.setSelectedItem(TripleA.PRO_COMPUTER_PLAYER_TYPE);
    } else if (PLAYER_TYPE_DOES_NOTHING.equals(player.getDefaultType())) {
      playerTypes.setSelectedItem(TripleA.DOESNOTHINGAI_COMPUTER_PLAYER_TYPE);
    } else {
      playerTypes.setSelectedItem(TripleA.HUMAN_PLAYER_TYPE);
    }
  }

  @Override
  public String getPlayerName() {
    return playerName;
  }

  @Override
  public String getPlayerType() {
    return (String) playerTypes.getSelectedItem();
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
