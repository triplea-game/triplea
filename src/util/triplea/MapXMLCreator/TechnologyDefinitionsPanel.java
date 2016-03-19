package util.triplea.MapXMLCreator;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;


public class TechnologyDefinitionsPanel extends DynamicRowsPanel {

  private TreeSet<String> playerNames = new TreeSet<String>();

  public TechnologyDefinitionsPanel(final JPanel stepActionPanel) {
    super(stepActionPanel);
  }

  public static void layout(final MapXMLCreator mapXMLCreator, final JPanel stepActionPanel) {
    if (me == null || !(me instanceof TechnologyDefinitionsPanel))
      me = new TechnologyDefinitionsPanel(stepActionPanel);
    DynamicRowsPanel.layout(mapXMLCreator, stepActionPanel);
  }

  protected ActionListener getAutoFillAction() {
    return null;
  }

  protected void layoutComponents() {

    final JLabel lTechnologyName = new JLabel("Technology Name");
    Dimension dimension = lTechnologyName.getPreferredSize();
    dimension.width = DynamicRow.INPUT_FIELD_SIZE_MEDIUM;
    lTechnologyName.setPreferredSize(dimension);
    final JLabel lPlayerName = new JLabel("Player Name");
    lPlayerName.setPreferredSize(dimension);
    final JLabel lAlreadyEnabled = new JLabel("Already Enabled");
    dimension = (Dimension) dimension.clone();
    dimension.width = 85;
    lAlreadyEnabled.setPreferredSize(dimension);

    // <1> Set panel layout
    GridBagLayout gbl_stepActionPanel = new GridBagLayout();
    setColumns(gbl_stepActionPanel);
    setRows(gbl_stepActionPanel, MapXMLHelper.technologyDefinitions.size());
    ownPanel.setLayout(gbl_stepActionPanel);

    // <2> Add Row Labels: Player Name, Alliance Name, Buy Quantity
    GridBagConstraints gbc_lTechnologyName = new GridBagConstraints();
    gbc_lTechnologyName.insets = new Insets(0, 0, 5, 5);
    gbc_lTechnologyName.gridy = 0;
    gbc_lTechnologyName.gridx = 0;
    gbc_lTechnologyName.anchor = GridBagConstraints.WEST;
    ownPanel.add(lTechnologyName, gbc_lTechnologyName);

    GridBagConstraints gbc_lPlayerName = (GridBagConstraints) gbc_lTechnologyName.clone();
    gbc_lPlayerName.gridx = 1;
    ownPanel.add(lPlayerName, gbc_lPlayerName);

    GridBagConstraints gbc_lAlreadyEnabled = (GridBagConstraints) gbc_lTechnologyName.clone();
    gbc_lAlreadyEnabled.gridx = 2;
    ownPanel.add(lAlreadyEnabled, gbc_lAlreadyEnabled);

    // <3> Add Main Input Rows
    int yValue = 1;

    final String[] playerNamesArray = playerNames.toArray(new String[playerNames.size()]);
    for (final Entry<String, List<String>> technologyDefinition : MapXMLHelper.technologyDefinitions
        .entrySet()) {
      GridBagConstraints gbc_tTechnologyName = (GridBagConstraints) gbc_lTechnologyName.clone();
      gbc_tTechnologyName.gridx = 0;
      gbc_lTechnologyName.gridy = yValue;
      final List<String> definition = technologyDefinition.getValue();
      final String techKey = technologyDefinition.getKey();
      final TechnologyDefinitionsRow newRow = new TechnologyDefinitionsRow(this, ownPanel,
          techKey.substring(0, techKey.lastIndexOf(definition.get(0)) - 1), definition.get(0), playerNamesArray,
          definition.get(1));
      newRow.addToComponent(ownPanel, yValue, gbc_tTechnologyName);
      rows.add(newRow);
      ++yValue;
    }

    // <4> Add Final Button Row
    final JButton bAddTechnology = new JButton("Add Technology");

    bAddTechnology.setFont(new Font("Tahoma", Font.PLAIN, 11));
    bAddTechnology.addActionListener(new AbstractAction("Add Technology") {
      private static final long serialVersionUID = 6322566373692205163L;

      public void actionPerformed(final ActionEvent e) {
        String newTechnologyName = JOptionPane.showInputDialog(ownPanel, "Enter a new technology name:",
            "Technology" + (MapXMLHelper.technologyDefinitions.size() + 1));
        if (newTechnologyName == null || newTechnologyName.isEmpty())
          return;
        newTechnologyName = newTechnologyName.trim();
        String suggestedPlayerName = null;
        for (final String playerName : MapXMLHelper.playerName) {
          if (!MapXMLHelper.technologyDefinitions.containsKey(newTechnologyName + "_" + playerName))
            suggestedPlayerName = playerName;
        }
        if (suggestedPlayerName == null) {
          JOptionPane.showMessageDialog(ownPanel,
              "Technology '" + newTechnologyName + "' already exists for all players.", "Input error",
              JOptionPane.ERROR_MESSAGE);
          return;
        }
        final String newRowName = newTechnologyName + "_" + suggestedPlayerName;

        final ArrayList<String> newValue = new ArrayList<String>();
        newValue.add(suggestedPlayerName);
        newValue.add("false");
        MapXMLHelper.putTechnologyDefinitions(newRowName, newValue);

        // UI Update
        setRows((GridBagLayout) ownPanel.getLayout(), MapXMLHelper.technologyDefinitions.size());
        addRowWith(newTechnologyName, suggestedPlayerName, "false");
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            ownPanel.revalidate();
            ownPanel.repaint();
          }
        });
      }
    });
    addButton(bAddTechnology);

    GridBagConstraints gbc_bAddUnit = (GridBagConstraints) gbc_lTechnologyName.clone();
    gbc_bAddUnit.gridx = 0;
    gbc_bAddUnit.gridy = yValue;
    addFinalButtonRow(gbc_bAddUnit);
  }

  private DynamicRow addRowWith(final String newTechnologyName, final String playerName, final String alreadyEnabled) {
    final TechnologyDefinitionsRow newRow = new TechnologyDefinitionsRow(this, ownPanel, newTechnologyName,
        playerName, playerNames.toArray(new String[playerNames.size()]), alreadyEnabled);
    addRow(newRow);
    return newRow;
  }


  protected void initializeSpecifics() {
    playerNames.clear();
    playerNames.addAll(MapXMLHelper.playerName);
  }

  protected void setColumns(GridBagLayout gbl_panel) {
    gbl_panel.columnWidths = new int[] {50, 60, 50, 30};
    gbl_panel.columnWeights = new double[] {0.0, 0.0, 0.0, 0.0};
  }
}
