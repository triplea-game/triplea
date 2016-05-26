package util.triplea.mapXmlCreator;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import games.strategy.common.swing.SwingAction;


public class TechnologyDefinitionsPanel extends DynamicRowsPanel {

  private final TreeSet<String> playerNames = new TreeSet<>();

  public TechnologyDefinitionsPanel(final JPanel stepActionPanel) {
    super(stepActionPanel);
  }

  public static void layout(final MapXmlCreator mapXmlCreator) {
    if (!DynamicRowsPanel.me.isPresent() || !(me.get() instanceof TechnologyDefinitionsPanel))
      me = Optional.of(new TechnologyDefinitionsPanel(mapXmlCreator.getStepActionPanel()));
    DynamicRowsPanel.layout(mapXmlCreator);
  }

  @Override
  protected ActionListener getAutoFillAction() {
    return null;
  }

  @Override
  protected void layoutComponents() {

    final JLabel labelTechnologyName = new JLabel("Technology Name");
    Dimension dimension = labelTechnologyName.getPreferredSize();
    dimension.width = DynamicRow.INPUT_FIELD_SIZE_MEDIUM;
    labelTechnologyName.setPreferredSize(dimension);
    final JLabel labelPlayerName = new JLabel("Player Name");
    labelPlayerName.setPreferredSize(dimension);
    final JLabel labelAlreadyEnabled = new JLabel("Already Enabled");
    dimension = (Dimension) dimension.clone();
    dimension.width = 85;
    labelAlreadyEnabled.setPreferredSize(dimension);

    // <1> Set panel layout
    GridBagLayout gbl_stepActionPanel = new GridBagLayout();
    setColumns(gbl_stepActionPanel);
    setRows(gbl_stepActionPanel, MapXmlHelper.getTechnologyDefinitionsMap().size());
    getOwnPanel().setLayout(gbl_stepActionPanel);

    // <2> Add Row Labels: Player Name, Alliance Name, Buy Quantity
    GridBagConstraints gridBadConstLabelTechnologyName = new GridBagConstraints();
    gridBadConstLabelTechnologyName.insets = new Insets(0, 0, 5, 5);
    gridBadConstLabelTechnologyName.gridy = 0;
    gridBadConstLabelTechnologyName.gridx = 0;
    gridBadConstLabelTechnologyName.anchor = GridBagConstraints.WEST;
    getOwnPanel().add(labelTechnologyName, gridBadConstLabelTechnologyName);

    GridBagConstraints gridBadConstLabelPlayerName = (GridBagConstraints) gridBadConstLabelTechnologyName.clone();
    gridBadConstLabelPlayerName.gridx = 1;
    getOwnPanel().add(labelPlayerName, gridBadConstLabelPlayerName);

    GridBagConstraints gridBadConstLabelAlreadyEnabled = (GridBagConstraints) gridBadConstLabelTechnologyName.clone();
    gridBadConstLabelAlreadyEnabled.gridx = 2;
    getOwnPanel().add(labelAlreadyEnabled, gridBadConstLabelAlreadyEnabled);

    // <3> Add Main Input Rows
    int yValue = 1;

    final String[] playerNamesArray = playerNames.toArray(new String[playerNames.size()]);
    for (final Entry<String, List<String>> technologyDefinition : MapXmlHelper.getTechnologyDefinitionsMap()
        .entrySet()) {
      GridBagConstraints gbc_tTechnologyName = (GridBagConstraints) gridBadConstLabelTechnologyName.clone();
      gbc_tTechnologyName.gridx = 0;
      gridBadConstLabelTechnologyName.gridy = yValue;
      final List<String> definition = technologyDefinition.getValue();
      final String techKey = technologyDefinition.getKey();
      final TechnologyDefinitionsRow newRow = new TechnologyDefinitionsRow(this, getOwnPanel(),
          techKey.substring(0, techKey.lastIndexOf(definition.get(0)) - 1), definition.get(0), playerNamesArray,
          definition.get(1));
      newRow.addToParentComponentWithGbc(getOwnPanel(), yValue, gbc_tTechnologyName);
      rows.add(newRow);
      ++yValue;
    }

    // <4> Add Final Button Row
    final JButton buttonAddTechnology = new JButton("Add Technology");

    buttonAddTechnology.setFont(MapXmlUIHelper.defaultMapXMLCreatorFont);
    buttonAddTechnology.addActionListener(SwingAction.of("Add Technology", e -> {
      String newTechnologyName = JOptionPane.showInputDialog(getOwnPanel(), "Enter a new technology name:",
          "Technology" + (MapXmlHelper.getTechnologyDefinitionsMap().size() + 1));
      if (newTechnologyName == null || newTechnologyName.isEmpty())
        return;
      newTechnologyName = newTechnologyName.trim();
      String suggestedPlayerName = null;
      for (final String playerName : MapXmlHelper.getPlayerNames()) {
        if (!MapXmlHelper.getTechnologyDefinitionsMap().containsKey(newTechnologyName + "_" + playerName))
          suggestedPlayerName = playerName;
      }
      if (suggestedPlayerName == null) {
        JOptionPane.showMessageDialog(getOwnPanel(),
            "Technology '" + newTechnologyName + "' already exists for all players.", "Input error",
            JOptionPane.ERROR_MESSAGE);
        return;
      }
      final String newRowName = newTechnologyName + "_" + suggestedPlayerName;

      final ArrayList<String> newValue = new ArrayList<>();
      newValue.add(suggestedPlayerName);
      newValue.add("false");
      MapXmlHelper.putTechnologyDefinitions(newRowName, newValue);

      // UI Update
      setRows((GridBagLayout) getOwnPanel().getLayout(), MapXmlHelper.getTechnologyDefinitionsMap().size());
      addRowWith(newTechnologyName, suggestedPlayerName, "false");
      SwingUtilities.invokeLater(() -> {
        getOwnPanel().revalidate();
        getOwnPanel().repaint();
      });
    }));
    addButton(buttonAddTechnology);

    GridBagConstraints gridBadConstButtonAddUnit = (GridBagConstraints) gridBadConstLabelTechnologyName.clone();
    gridBadConstButtonAddUnit.gridx = 0;
    gridBadConstButtonAddUnit.gridy = yValue;
    addFinalButtonRow(gridBadConstButtonAddUnit);
  }

  private DynamicRow addRowWith(final String newTechnologyName, final String playerName, final String alreadyEnabled) {
    final TechnologyDefinitionsRow newRow = new TechnologyDefinitionsRow(this, getOwnPanel(), newTechnologyName,
        playerName, playerNames.toArray(new String[playerNames.size()]), alreadyEnabled);
    addRow(newRow);
    return newRow;
  }


  @Override
  protected void initializeSpecifics() {
    playerNames.clear();
    playerNames.addAll(MapXmlHelper.getPlayerNames());
  }

  @Override
  protected void setColumns(GridBagLayout gbl_panel) {
    gbl_panel.columnWidths = new int[] {50, 60, 50, 30};
    gbl_panel.columnWeights = new double[] {0.0, 0.0, 0.0, 0.0};
  }
}
