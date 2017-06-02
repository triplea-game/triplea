package games.strategy.engine.framework.startup.ui;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;

class PlayerComboBoxSelector {

  private final JCheckBox m_enabledCheckBox;
  private final String m_playerName;
  private final JComboBox<String> m_playerTypes;
  private boolean m_enabled = true;
  private final JLabel m_name;
  private final JLabel m_alliances;
  private final Collection<String> m_disableable;
  private final String[] m_types;
  private final SetupPanel m_parent;

  PlayerComboBoxSelector(final String playerName, final Map<String, String> reloadSelections,
      final Collection<String> disableable, final HashMap<String, Boolean> playersEnablementListing,
      final Collection<String> playerAlliances, final String[] types, final SetupPanel parent) {
    m_playerName = playerName;
    m_name = new JLabel(m_playerName + ":");
    m_enabledCheckBox = new JCheckBox();
    final ActionListener m_disablePlayerActionListener = new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        if (m_enabledCheckBox.isSelected()) {
          m_enabled = true;
          // the 1st in the list should be human
          m_playerTypes.setSelectedItem(m_types[0]);
        } else {
          m_enabled = false;
          // the 2nd in the list should be Weak AI
          m_playerTypes.setSelectedItem(m_types[Math.max(0, Math.min(m_types.length - 1, 1))]);
        }
        setWidgetActivation();
      }
    };
    m_enabledCheckBox.addActionListener(m_disablePlayerActionListener);
    m_enabledCheckBox.setSelected(playersEnablementListing.get(playerName));
    m_enabledCheckBox.setEnabled(disableable.contains(playerName));
    m_disableable = disableable;
    m_parent = parent;
    m_types = types;
    m_playerTypes = new JComboBox<>(types);
    String previousSelection = reloadSelections.get(playerName);
    if (previousSelection.equalsIgnoreCase("Client")) {
      previousSelection = types[0];
    }
    if (!(previousSelection.equals("no_one")) && Arrays.asList(types).contains(previousSelection)) {
      m_playerTypes.setSelectedItem(previousSelection);
    } else if (m_playerName.startsWith("Neutral") || playerName.startsWith("AI")) {
      // the 4th in the list should be Pro AI (Hard AI)
      m_playerTypes.setSelectedItem(types[Math.max(0, Math.min(types.length - 1, 3))]);
    }
    // we do not set the default for the combobox because the default is the top item, which in this case is human
    String m_playerAlliances;
    if (playerAlliances.contains(playerName)) {
      m_playerAlliances = "";
    } else {
      m_playerAlliances = playerAlliances.toString();
    }
    m_alliances = new JLabel(m_playerAlliances);
    setWidgetActivation();
  }

  public void layout(final int row, final Container container) {
    int gridx = 0;
    if (!m_disableable.isEmpty()) {
      container.add(m_enabledCheckBox, new GridBagConstraints(gridx++, row, 1, 1, 0, 0, GridBagConstraints.WEST,
          GridBagConstraints.NONE, new Insets(0, 5, 5, 0), 0, 0));
    }
    container.add(m_name, new GridBagConstraints(gridx++, row, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(0, 5, 5, 0), 0, 0));
    container.add(m_playerTypes, new GridBagConstraints(gridx++, row, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(0, 5, 5, 0), 0, 0));
    container.add(m_alliances, new GridBagConstraints(gridx++, row, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(0, 7, 5, 5), 0, 0));
  }

  public String getPlayerName() {
    return m_playerName;
  }

  public String getPlayerType() {
    return (String) m_playerTypes.getSelectedItem();
  }

  public boolean isPlayerEnabled() {
    return m_enabledCheckBox.isSelected();
  }

  private void setWidgetActivation() {
    m_name.setEnabled(m_enabled);
    m_alliances.setEnabled(m_enabled);
    m_enabledCheckBox.setEnabled(m_disableable.contains(m_playerName));
    m_parent.notifyObservers();
  }

}
