package util.triplea.MapXMLCreator;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;


public class PlayerAndAlliancesPanel extends DynamicRowsPanel {

  private TreeSet<String> alliances = new TreeSet<String>();

  public PlayerAndAlliancesPanel(final JPanel stepActionPanel) {
    super(stepActionPanel);
  }

  public static void layout(final MapXMLCreator mapXMLCreator, final JPanel stepActionPanel) {
    if (DynamicRowsPanel.me == null || !(DynamicRowsPanel.me instanceof PlayerAndAlliancesPanel))
      DynamicRowsPanel.me = new PlayerAndAlliancesPanel(stepActionPanel);
    DynamicRowsPanel.layout(mapXMLCreator, stepActionPanel);
  }

  protected ActionListener getAutoFillAction() {
    return null;
  }

  protected void layoutComponents() {

    final JLabel lPlayerName = new JLabel("Player Name");
    Dimension dimension = lPlayerName.getPreferredSize();
    dimension.width = DynamicRow.INPUT_FIELD_SIZE_MEDIUM;
    lPlayerName.setPreferredSize(dimension);
    final JLabel lPlayerAlliance = new JLabel("Player Alliance");
    lPlayerAlliance.setPreferredSize(dimension);
    final JLabel lInitialResource = new JLabel("Initial Resource");
    dimension = (Dimension) dimension.clone();
    dimension.width = 80;
    lInitialResource.setPreferredSize(dimension);

    // <1> Set panel layout
    GridBagLayout gbl_stepActionPanel = new GridBagLayout();
    setColumns(gbl_stepActionPanel);
    setRows(gbl_stepActionPanel, MapXMLHelper.playerName.size());
    ownPanel.setLayout(gbl_stepActionPanel);

    // <2> Add Row Labels: Player Name, Alliance Name, Initial Resource
    GridBagConstraints gbc_lPlayerName = new GridBagConstraints();
    gbc_lPlayerName.insets = new Insets(0, 0, 5, 5);
    gbc_lPlayerName.gridy = 0;
    gbc_lPlayerName.gridx = 0;
    gbc_lPlayerName.anchor = GridBagConstraints.WEST;
    ownPanel.add(lPlayerName, gbc_lPlayerName);

    GridBagConstraints gbc_lPlayerAlliance = (GridBagConstraints) gbc_lPlayerName.clone();
    gbc_lPlayerAlliance.gridx = 1;
    ownPanel.add(lPlayerAlliance, gbc_lPlayerAlliance);

    GridBagConstraints gbc_lInitialResource = (GridBagConstraints) gbc_lPlayerName.clone();
    gbc_lInitialResource.gridx = 2;
    ownPanel.add(lInitialResource, gbc_lInitialResource);

    // <3> Add Main Input Rows
    final String[] alliancesArray = alliances.toArray(new String[alliances.size()]);
    int yValue = 1;
    for (final String playerName : MapXMLHelper.playerName) {
      GridBagConstraints gbc_tPlayerName = (GridBagConstraints) gbc_lPlayerName.clone();
      gbc_tPlayerName.gridx = 0;
      gbc_lPlayerName.gridy = yValue;
      final PlayerAndAlliancesRow newRow = new PlayerAndAlliancesRow(this, ownPanel, playerName,
          MapXMLHelper.playerAlliance.get(playerName), alliancesArray,
          MapXMLHelper.playerInitResources.get(playerName));
      newRow.addToComponent(ownPanel, yValue, gbc_tPlayerName);
      rows.add(newRow);
      ++yValue;
    }

    // <4> Add Final Button Row
    final JButton bAddPlayer = new JButton("Add Player");
    final JButton bAddAlliance = new JButton("Add Alliance");
    final JButton bRemoveAlliance = new JButton("Remove Alliance");

    bAddPlayer.setFont(new Font("Tahoma", Font.PLAIN, 11));
    bAddPlayer.addActionListener(new AbstractAction("Add Player") {
      private static final long serialVersionUID = 6322566373692205163L;

      public void actionPerformed(final ActionEvent e) {
        String newPlayerName = JOptionPane.showInputDialog(ownPanel, "Enter a new player name:",
            "Player" + (MapXMLHelper.playerName.size() + 1));
        if (newPlayerName == null || newPlayerName.isEmpty())
          return;
        if (MapXMLHelper.playerName.contains(newPlayerName)) {
          JOptionPane.showMessageDialog(ownPanel, "Player '" + newPlayerName + "' already exists.", "Input error",
              JOptionPane.ERROR_MESSAGE);
          return;
        }
        newPlayerName = newPlayerName.trim();

        String allianceName;
        if (alliances.isEmpty()) {
          allianceName = JOptionPane.showInputDialog(ownPanel,
              "Which alliance should player '" + newPlayerName + "' join?", "Alliance1");
          if (allianceName == null)
            return;
          allianceName = allianceName.trim();
          alliances.add(allianceName);
        } else
          allianceName = (String) JOptionPane.showInputDialog(ownPanel,
              "Which alliance should player '" + newPlayerName + "' join?",
              "Choose Player's Alliance",
              JOptionPane.QUESTION_MESSAGE, null,
              alliances.toArray(new String[alliances.size()]), // Array of choices
              alliances.iterator().next()); // Initial choice

        MapXMLHelper.addPlayerName(newPlayerName);
        MapXMLHelper.putPlayerAlliance(newPlayerName, allianceName);
        MapXMLHelper.putPlayerInitResources(newPlayerName, 0);

        // UI Update
        setRows((GridBagLayout) ownPanel.getLayout(), MapXMLHelper.playerName.size());
        addRowWith(newPlayerName, allianceName, 0);
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            ownPanel.revalidate();
            ownPanel.repaint();
          }
        });
      }
    });
    addButton(bAddPlayer);

    bAddAlliance.setFont(new Font("Tahoma", Font.PLAIN, 11));
    bAddAlliance.addActionListener(new AbstractAction("Add Alliance") {
      private static final long serialVersionUID = 6322566373692205163L;

      public void actionPerformed(final ActionEvent e) {
        String newAllianceName = JOptionPane.showInputDialog(ownPanel, "Enter a new alliance name:",
            "Alliance" + (alliances.size() + 1));
        if (newAllianceName == null || newAllianceName.isEmpty())
          return;
        if (alliances.contains(newAllianceName)) {
          JOptionPane.showMessageDialog(ownPanel, "Alliance '" + newAllianceName + "' already exists.", "Input error",
              JOptionPane.ERROR_MESSAGE);
          return;
        }
        newAllianceName = newAllianceName.trim();

        alliances.add(newAllianceName);
        if (alliances.size() > 1)
          bRemoveAlliance.setEnabled(true);

        // UI Update
        addToComboBoxesAlliance(newAllianceName);
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            ownPanel.revalidate();
            ownPanel.repaint();
          }
        });
      }
    });
    addButton(bAddAlliance);

    bRemoveAlliance.setFont(new Font("Tahoma", Font.PLAIN, 11));
    bRemoveAlliance.setEnabled(alliances.size() > 1);
    bRemoveAlliance.addActionListener(new AbstractAction("Remove Alliance") {
      private static final long serialVersionUID = 6322566373692205163L;

      public void actionPerformed(final ActionEvent e) {
        String removeAllianceName = (String) JOptionPane.showInputDialog(ownPanel,
            "Which alliance should get removed?", "Remove Alliance", JOptionPane.QUESTION_MESSAGE,
            null, alliances.toArray(new String[alliances.size()]), // Array of choices
            alliances.iterator().next()); // Initial choice
        if (removeAllianceName == null || removeAllianceName.isEmpty())
          return;
        final ArrayList<String> playerStillUsing = new ArrayList<String>();
        for (final DynamicRow row : rows) {
          if (((PlayerAndAlliancesRow) row).isAllianceSelected(removeAllianceName))
            playerStillUsing.add(row.getRowName());
        }
        if (!playerStillUsing.isEmpty()) {
          StringBuilder formattedPlayerList = new StringBuilder();
          final boolean plural = playerStillUsing.size() > 1;
          for (final String playerString : playerStillUsing)
            formattedPlayerList.append("\r - " + playerString);
          JOptionPane.showMessageDialog(ownPanel, "Cannot remove alliance.\rThe following player"
              + (plural ? "s are" : " is") + " still assigned to alliance '"
              + removeAllianceName + "':"
              + formattedPlayerList, "Input error",
              JOptionPane.ERROR_MESSAGE);
          return;
        }

        alliances.remove(removeAllianceName);
        if (alliances.size() <= 1)
          bRemoveAlliance.setEnabled(false);

        // UI Update
        removeFromComboBoxesAlliance(removeAllianceName);
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            ownPanel.revalidate();
            ownPanel.repaint();
          }
        });
      }
    });
    addButton(bRemoveAlliance);

    GridBagConstraints gbc_bAddPlayer = (GridBagConstraints) gbc_lPlayerName.clone();
    gbc_bAddPlayer.gridx = 0;
    gbc_bAddPlayer.gridy = yValue;
    addFinalButtonRow(gbc_bAddPlayer);
  }

  protected void addToComboBoxesAlliance(final String newAlliance) {
    for (final DynamicRow row : rows) {
      ((PlayerAndAlliancesRow) row).updateComboBoxesAlliance(newAlliance);
    }
  }

  protected void removeFromComboBoxesAlliance(final String removeAlliance) {
    for (final DynamicRow row : rows) {
      ((PlayerAndAlliancesRow) row).removeFromComboBoxesAlliance(removeAlliance);
    }
  }

  private DynamicRow addRowWith(final String newPlayerName, final String allianceName, final int initialResource) {
    final PlayerAndAlliancesRow newRow = new PlayerAndAlliancesRow(this, ownPanel, newPlayerName, allianceName,
        alliances.toArray(new String[alliances.size()]), initialResource);
    addRow(newRow);
    return newRow;
  }


  protected void initializeSpecifics() {
    if (!MapXMLHelper.playerAlliance.isEmpty()) {
      alliances.clear();
    }
    alliances.addAll(MapXMLHelper.playerAlliance.values());
  }

  protected void setColumns(GridBagLayout gbl_panel) {
    gbl_panel.columnWidths = new int[] {50, 60, 50, 30};
    gbl_panel.columnWeights = new double[] {0.0, 0.0, 0.0, 0.0};
  }
}
