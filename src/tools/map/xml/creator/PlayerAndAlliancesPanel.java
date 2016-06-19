package tools.map.xml.creator;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Optional;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import games.strategy.ui.SwingAction;


public class PlayerAndAlliancesPanel extends DynamicRowsPanel {

  private final TreeSet<String> alliances = new TreeSet<>();

  public PlayerAndAlliancesPanel(final JPanel stepActionPanel) {
    super(stepActionPanel);
  }

  public static void layout(final MapXmlCreator mapXmlCreator) {
    if (!me.isPresent() || !(me.get() instanceof PlayerAndAlliancesPanel))
      me = Optional.of(new PlayerAndAlliancesPanel(mapXmlCreator.getStepActionPanel()));
    DynamicRowsPanel.layout(mapXmlCreator);
  }

  @Override
  protected ActionListener getAutoFillAction() {
    return null;
  }

  @Override
  protected void layoutComponents() {

    final JLabel labelPlayerName = new JLabel("Player Name");
    Dimension dimension = labelPlayerName.getPreferredSize();
    dimension.width = DynamicRow.INPUT_FIELD_SIZE_MEDIUM;
    labelPlayerName.setPreferredSize(dimension);
    final JLabel labelPlayerAlliance = new JLabel("Player Alliance");
    labelPlayerAlliance.setPreferredSize(dimension);
    final JLabel labelInitialResource = new JLabel("Initial Resource");
    dimension = (Dimension) dimension.clone();
    dimension.width = 80;
    labelInitialResource.setPreferredSize(dimension);

    // <1> Set panel layout
    GridBagLayout gbl_stepActionPanel = new GridBagLayout();
    setColumns(gbl_stepActionPanel);
    setRows(gbl_stepActionPanel, MapXmlHelper.getPlayerNames().size());
    getOwnPanel().setLayout(gbl_stepActionPanel);

    // <2> Add Row Labels: Player Name, Alliance Name, Initial Resource
    GridBagConstraints gridBadConstLabelPlayerName = new GridBagConstraints();
    gridBadConstLabelPlayerName.insets = new Insets(0, 0, 5, 5);
    gridBadConstLabelPlayerName.gridy = 0;
    gridBadConstLabelPlayerName.gridx = 0;
    gridBadConstLabelPlayerName.anchor = GridBagConstraints.WEST;
    getOwnPanel().add(labelPlayerName, gridBadConstLabelPlayerName);

    GridBagConstraints gridBadConstLabelPlayerAlliance = (GridBagConstraints) gridBadConstLabelPlayerName.clone();
    gridBadConstLabelPlayerAlliance.gridx = 1;
    getOwnPanel().add(labelPlayerAlliance, gridBadConstLabelPlayerAlliance);

    GridBagConstraints gridBadConstLabelInitialResource = (GridBagConstraints) gridBadConstLabelPlayerName.clone();
    gridBadConstLabelInitialResource.gridx = 2;
    getOwnPanel().add(labelInitialResource, gridBadConstLabelInitialResource);

    // <3> Add Main Input Rows
    final String[] alliancesArray = alliances.toArray(new String[alliances.size()]);
    int yValue = 1;
    for (final String playerName : MapXmlHelper.getPlayerNames()) {
      GridBagConstraints gbc_tPlayerName = (GridBagConstraints) gridBadConstLabelPlayerName.clone();
      gbc_tPlayerName.gridx = 0;
      gridBadConstLabelPlayerName.gridy = yValue;
      final PlayerAndAlliancesRow newRow = new PlayerAndAlliancesRow(this, getOwnPanel(), playerName,
          MapXmlHelper.getPlayerAllianceMap().get(playerName), alliancesArray,
          MapXmlHelper.getPlayerInitResourcesMap().get(playerName));
      newRow.addToParentComponentWithGbc(getOwnPanel(), yValue, gbc_tPlayerName);
      rows.add(newRow);
      ++yValue;
    }

    // <4> Add Final Button Row
    final JButton buttonAddPlayer = new JButton("Add Player");
    final JButton buttonAddAlliance = new JButton("Add Alliance");
    final JButton buttonRemoveAlliance = new JButton("Remove Alliance");

    buttonAddPlayer.setFont(MapXmlUIHelper.defaultMapXMLCreatorFont);
    buttonAddPlayer.addActionListener(SwingAction.of("Add Player", e -> {
      String newPlayerName = JOptionPane.showInputDialog(getOwnPanel(), "Enter a new player name:",
          "Player" + (MapXmlHelper.getPlayerNames().size() + 1));
      if (newPlayerName == null || newPlayerName.isEmpty())
        return;
      if (MapXmlHelper.getPlayerNames().contains(newPlayerName)) {
        JOptionPane.showMessageDialog(getOwnPanel(), "Player '" + newPlayerName + "' already exists.", "Input error",
            JOptionPane.ERROR_MESSAGE);
        return;
      }
      newPlayerName = newPlayerName.trim();

      String allianceName;
      if (alliances.isEmpty()) {
        allianceName = JOptionPane.showInputDialog(getOwnPanel(),
            "Which alliance should player '" + newPlayerName + "' join?", "Alliance1");
        if (allianceName == null)
          return;
        allianceName = allianceName.trim();
        alliances.add(allianceName);
      } else
        allianceName = (String) JOptionPane.showInputDialog(getOwnPanel(),
            "Which alliance should player '" + newPlayerName + "' join?",
            "Choose Player's Alliance",
            JOptionPane.QUESTION_MESSAGE, null,
            alliances.toArray(new String[alliances.size()]), // Array of choices
            alliances.iterator().next()); // Initial choice

      MapXmlHelper.addPlayerName(newPlayerName);
      MapXmlHelper.putPlayerAlliance(newPlayerName, allianceName);
      MapXmlHelper.putPlayerInitResources(newPlayerName, 0);

      // UI Update
      setRows((GridBagLayout) getOwnPanel().getLayout(), MapXmlHelper.getPlayerNames().size());
      addRowWith(newPlayerName, allianceName, 0);
      SwingUtilities.invokeLater(() -> {
        getOwnPanel().revalidate();
        getOwnPanel().repaint();
      });
    }));
    addButton(buttonAddPlayer);

    buttonAddAlliance.setFont(MapXmlUIHelper.defaultMapXMLCreatorFont);
    buttonAddAlliance.addActionListener(SwingAction.of("Add Alliance", e -> {
      String newAllianceName = JOptionPane.showInputDialog(getOwnPanel(), "Enter a new alliance name:",
          "Alliance" + (alliances.size() + 1));
      if (newAllianceName == null || newAllianceName.isEmpty())
        return;
      if (alliances.contains(newAllianceName)) {
        JOptionPane.showMessageDialog(getOwnPanel(), "Alliance '" + newAllianceName + "' already exists.", "Input error",
            JOptionPane.ERROR_MESSAGE);
        return;
      }
      newAllianceName = newAllianceName.trim();

      alliances.add(newAllianceName);
      if (alliances.size() > 1)
        buttonRemoveAlliance.setEnabled(true);

      // UI Update
      addToComboBoxesAlliance(newAllianceName);
      SwingUtilities.invokeLater(() -> {
        getOwnPanel().revalidate();
        getOwnPanel().repaint();
      });
    }));
    addButton(buttonAddAlliance);

    buttonRemoveAlliance.setFont(MapXmlUIHelper.defaultMapXMLCreatorFont);
    buttonRemoveAlliance.setEnabled(alliances.size() > 1);
    buttonRemoveAlliance.addActionListener(SwingAction.of("Remove Alliance", e -> {
      String removeAllianceName = (String) JOptionPane.showInputDialog(getOwnPanel(),
          "Which alliance should get removed?", "Remove Alliance", JOptionPane.QUESTION_MESSAGE,
          null, alliances.toArray(new String[alliances.size()]), // Array of choices
          alliances.iterator().next()); // Initial choice
      if (removeAllianceName == null || removeAllianceName.isEmpty())
        return;
      final ArrayList<String> playerStillUsing = new ArrayList<>();
      for (final DynamicRow row : rows) {
        if (((PlayerAndAlliancesRow) row).isAllianceSelected(removeAllianceName))
          playerStillUsing.add(row.getRowName());
      }
      if (!playerStillUsing.isEmpty()) {
        StringBuilder formattedPlayerList = new StringBuilder();
        final boolean plural = playerStillUsing.size() > 1;
        for (final String playerString : playerStillUsing)
          formattedPlayerList.append("\r - ").append(playerString);
        JOptionPane.showMessageDialog(getOwnPanel(), "Cannot remove alliance.\rThe following player"
            + (plural ? "s are" : " is") + " still assigned to alliance '"
            + removeAllianceName + "':"
            + formattedPlayerList, "Input error",
            JOptionPane.ERROR_MESSAGE);
        return;
      }

      alliances.remove(removeAllianceName);
      if (alliances.size() <= 1)
        buttonRemoveAlliance.setEnabled(false);

      // UI Update
      removeFromComboBoxesAlliance(removeAllianceName);
      SwingUtilities.invokeLater(() -> {
        getOwnPanel().revalidate();
        getOwnPanel().repaint();
      });
    }));
    addButton(buttonRemoveAlliance);

    GridBagConstraints gridBadConstButtonAddPlayer = (GridBagConstraints) gridBadConstLabelPlayerName.clone();
    gridBadConstButtonAddPlayer.gridx = 0;
    gridBadConstButtonAddPlayer.gridy = yValue;
    addFinalButtonRow(gridBadConstButtonAddPlayer);
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
    final PlayerAndAlliancesRow newRow = new PlayerAndAlliancesRow(this, getOwnPanel(), newPlayerName, allianceName,
        alliances.toArray(new String[alliances.size()]), initialResource);
    addRow(newRow);
    return newRow;
  }


  @Override
  protected void initializeSpecifics() {
    if (!MapXmlHelper.getPlayerAllianceMap().isEmpty()) {
      alliances.clear();
    }
    alliances.addAll(MapXmlHelper.getPlayerAllianceMap().values());
  }

  @Override
  protected void setColumns(GridBagLayout gbl_panel) {
    gbl_panel.columnWidths = new int[] {50, 60, 50, 30};
    gbl_panel.columnWeights = new double[] {0.0, 0.0, 0.0, 0.0};
  }
}
